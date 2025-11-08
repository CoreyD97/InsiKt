package com.coreyd97.insikt.exports.elastic

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.coreyd97.insikt.exports.ContextMenuExportProvider
import com.coreyd97.insikt.exports.LogExportService
import com.coreyd97.insikt.exports.LogExporter
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.logging.logentry.Status
import com.coreyd97.insikt.util.*
import com.coreyd97.montoyautilities.NullablePreference
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.StorageType
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.serialization.serializer
import org.apache.commons.lang3.StringUtils
import org.apache.http.Header
import org.apache.http.HttpHost
import org.apache.http.message.BasicHeader
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.elasticsearch.client.RestClient
import java.io.IOException
import java.net.ConnectException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.JMenuItem
import javax.swing.JOptionPane

@Singleton
class ElasticExporter @Inject constructor(
    val exportService: LogExportService,
    val filterLibrary: FilterLibrary,
) : LogExporter,
    ContextMenuExportProvider {
    var address by Preference(PREF_ELASTIC_ADDRESS, "127.0.0.1")
    var port by Preference(PREF_ELASTIC_PORT, 9200)
    var protocol by Preference(PREF_ELASTIC_PROTOCOL, Protocol.HTTP)
    var auth by Preference(PREF_ELASTIC_AUTH, ElasticAuthType.Basic)
    var clusterName by Preference(PREF_ELASTIC_CLUSTER_NAME, "elasticsearch")
    var apiKeyId by Preference(PREF_ELASTIC_API_KEY_ID, "")
    var apiKeySecret by Preference(PREF_ELASTIC_API_KEY_SECRET, "")
    var username by Preference(PREF_ELASTIC_USERNAME, "")
    var password by Preference(PREF_ELASTIC_PASSWORD, "")
    
    var indexName by Preference(PREF_ELASTIC_INDEX, "logger")
    var exportDelay by Preference(PREF_ELASTIC_DELAY, 120)
    var filterString by Preference(PREF_ELASTIC_FILTER, "")
    var elasticFilterProjPrevious:String? by NullablePreference(
        PREF_ELASTIC_FILTER_PROJECT_PREVIOUS,
        storage = StorageType.PROJECT,
        customSerializer = serializer(String::class.javaObjectType)
    )
    
    var globalAutoStart by Preference(PREF_ELASTIC_AUTOSTART_GLOBAL, false)
    var autoStartForProject by Preference(PREF_ELASTIC_AUTOSTART_PROJECT, globalAutoStart, StorageType.PROJECT)
    
    var globalExportFields by Preference(PREF_ELASTIC_FIELDS, mutableSetOf<LogEntryField>())
    var exportFields by Preference(PREF_ELASTIC_FIELDS_PROJ, globalExportFields, StorageType.PROJECT)
        
        
    override val exportPanel = ElasticExporterControlPanel(this)
    val log = LogManager.getLogger(ElasticExporter::class.java)

    var elasticClient: ElasticsearchClient? = null
    var pendingEntries: MutableList<LogEntry> = mutableListOf()
    var logFilter: FilterRule? = null

    private var indexTask: ScheduledFuture<*>? = null
    private var connectFailedCounter = 0

    private val executorService = Executors.newScheduledThreadPool(1)

    private val mapper: ObjectMapper by lazy {
        val module = SimpleModule("LogEntry Serializer", Version(0, 1, 0, "", null, null))
        module.addSerializer(LogEntry::class.java, EntrySerializer())
        ObjectMapper().registerModule(module)
    }

    private val logger: Logger = LogManager.getLogger(
        this
    )

    init {
        if (globalAutoStart
            || autoStartForProject
        ) {
            //Autostart exporter.
            try {
                this.exportService.enableExporter(this)
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(
                    null,
                    """
                        Could not start elastic exporter: ${e.message}
                        See the logs for more information.
                        """.trimIndent(), "Elastic Exporter", JOptionPane.ERROR_MESSAGE
                )
                logger.error("Could not automatically start elastic exporter:", e)
            }
        }
    }

    @Throws(Exception::class)
    override fun setup() {
        if (exportFields.isEmpty()) throw Exception("No fields configured for export.")

        if (!StringUtils.isBlank(filterString)) {
            logFilter = filterLibrary.createFilter(filterString)
            if(!logFilter!!.isValid) {
                val errors = logFilter!!.errors.joinToString { "\n" }
                logger.error(
                    """
                    The log filter configured for the Elastic exporter is invalid!
                    Errors: ${errors}
                    """.trimIndent(), errors
                )
                throw Exception(
                    "The log filter configured for the Elastic exporter is invalid!"
                )
            }
        }

        val restClientBuilder = RestClient.builder(HttpHost(address, port, protocol.name))
        logger.info(
            String.format(
                "Starting ElasticSearch exporter. %s://%s:%s/%s",
                protocol,
                address,
                port,
                indexName
            )
        )

        val authToken = when (auth) {
            ElasticAuthType.ApiKey -> "$apiKeyId:$apiKeySecret"
            ElasticAuthType.Basic -> "$username:$password"
            ElasticAuthType.None -> null
        }?.toByteArray(StandardCharsets.UTF_8)?.let { Base64.getEncoder().encodeToString(it) }

        if (authToken != null) {
            logger.info(
                java.lang.String.format(
                    "ElasticSearch using %s",
                    auth
                )
            )
            restClientBuilder.setDefaultHeaders(
                arrayOf<Header>(
                    BasicHeader(
                        "Authorization",
                        java.lang.String.format("%s %s", auth, authToken)
                    )
                )
            )
        }


        val transport: ElasticsearchTransport = RestClientTransport(
            restClientBuilder.build(), JacksonJsonpMapper(
                this.mapper
            )
        )

        elasticClient = ElasticsearchClient(transport)

        createIndices()
        pendingEntries.clear()
        indexTask = executorService.scheduleAtFixedRate(
            { this.indexPendingEntries() },
            exportDelay.toLong(),
            exportDelay.toLong(),
            TimeUnit.SECONDS
        )
    }

    override fun exportNewEntry(logEntry: LogEntry) {
        if (logEntry.status === Status.PROCESSED) {
            if(logFilter == null || filterLibrary.test(logFilter!!, logEntry)){
                pendingEntries.add(logEntry)
            }
        }
    }

    override fun exportUpdatedEntry(updatedEntry: LogEntry) {
        if (updatedEntry.status === Status.PROCESSED) {
            if(logFilter == null || filterLibrary.test(logFilter!!, updatedEntry)){
                pendingEntries.add(updatedEntry)
            }
        }
    }

    @Throws(Exception::class)
    override fun shutdown() {
        if (this.indexTask != null) {
            indexTask!!.cancel(true)
        }
        this.pendingEntries.clear()
    }

    @Throws(IOException::class)
    private fun createIndices() {
        val existsRequest = ExistsRequest.Builder().index(
            this.indexName
        ).build()

        val exists = elasticClient!!.indices().exists(existsRequest)

        if (!exists.value()) {
            val createIndexRequest = CreateIndexRequest.Builder().index(this.indexName).build()
            elasticClient!!.indices().create(createIndexRequest)
        }
    }

    //    public JsonObject serializeLogEntry(LogEntry logEntry) {
    //        //Todo Better serialization of entries
    //        JsonObject jsonObject = new JsonObject();
    //        for (LogEntryField field : this.fields) {
    //            Object value = formatValue(logEntry.getValueByKey(field));
    //            try {
    //                jsonObject.addProperty(field.getFullLabel(), gson.toJson(value));
    //            }catch (Exception e){
    //                log.error("ElasticExporter: " + value);
    //                log.error("ElasticExporter: " + e.getMessage());
    //                throw e;
    //            }
    //        }
    //        return jsonObject;
    //    }
    private fun indexPendingEntries() {
        try {
            if (pendingEntries.size == 0) return

            val bulkBuilder = BulkRequest.Builder()

            var entriesInBulk: ArrayList<LogEntry>
            synchronized(pendingEntries) {
                entriesInBulk = ArrayList(pendingEntries)
                pendingEntries.clear()
            }

            for (logEntry in entriesInBulk) {
                try {
                    bulkBuilder.operations { op: BulkOperation.Builder ->
                        op
                            .index { idx: IndexOperation.Builder<Any?> ->
                                idx
                                    .index(this.indexName)
                                    .document(logEntry)
                            }
                    }
                } catch (e: Exception) {
                    log.error("Could not build elastic export request for entry: " + e.message)
                    //Could not build index request. Ignore it?
                }
            }

            try {
                val bulkResponse = elasticClient!!.bulk(bulkBuilder.build())
                if (bulkResponse.errors()) {
                    for (bulkResponseItem in bulkResponse.items()) {
                        log.error(bulkResponseItem.error()!!.reason())
                    }
                }
                connectFailedCounter = 0
            } catch (e: ConnectException) {
                connectFailedCounter++
                if (connectFailedCounter > 5) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Elastic exporter could not connect after 5 attempts. Elastic exporter shutting down...",
                        "Elastic Exporter - Connection Failed", JOptionPane.ERROR_MESSAGE
                    )
                    shutdown()
                }
            } catch (e: IOException) {
                log.error(e)
            }
        } catch (e: Exception) {
            log.error(e)
        }
    }

    private inner class EntrySerializer : StdSerializer<LogEntry>(LogEntry::class.java) {
        @Throws(IOException::class)
        override fun serialize(
            logEntry: LogEntry,
            gen: JsonGenerator,
            provider: SerializerProvider
        ) {
            gen.writeStartObject()
            for (field in this@ElasticExporter.exportFields) {
                val value: Any = logEntry.getValueByKey(field) ?: continue
                try {
                    when (field.type.getSimpleName()) {
                        "Integer" -> gen.writeNumberField(
                            field.fullLabel,
                            (value as Int)
                        )

                        "Short" -> gen.writeNumberField(field.fullLabel, (value as Short))
                        "Double" -> gen.writeNumberField(
                            field.fullLabel,
                            (value as Double)
                        )

                        "String" -> gen.writeStringField(field.fullLabel, value.toString())
                        "Boolean" -> gen.writeBooleanField(
                            field.fullLabel,
                            (value as Boolean)
                        )

                        "Date" -> gen.writeNumberField(field.fullLabel, (value as Date).time)
                        else -> log.error(
                            "Unhandled field type: " + field.type.getSimpleName()
                        )
                    }
                } catch (e: Exception) {
                    log.error("ElasticExporter: Couldn't serialize field. The field was ommitted from the export.")
                }
            }
            gen.writeEndObject()
        }
    }

    override fun getExportEntriesMenuItem(entries: List<LogEntry>): JMenuItem? {
        return null
    }
}