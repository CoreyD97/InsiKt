package com.coreyd97.insikt.logging

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.handler.HttpRequestToBeSent
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.ResponseReceivedAction
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.proxy.ProxyHttpRequestResponse
import burp.api.montoya.proxy.http.*
import com.coreyd97.insikt.ExecutorNames
import com.coreyd97.insikt.exports.LogExportService
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.TableColorService
import com.coreyd97.insikt.filter.TagService
import com.coreyd97.insikt.logging.logentry.FieldGroup
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryFactory
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.reflection.ReflectionService
import com.coreyd97.insikt.util.*
import com.coreyd97.montoyautilities.NullablePreference
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.StorageType
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import org.apache.logging.log4j.LogManager
import java.text.ParseException
import java.time.Instant
import java.util.*
import java.util.concurrent.*
import java.util.stream.Collectors
import kotlin.math.max

/**
 * Created by corey on 07/09/17.
 */
@Singleton
class LogProcessorImpl @Inject constructor(
    val montoya: MontoyaApi,
    val repository: LogRepository,
    val tableColorService: TableColorService,
    val tagService: TagService,
    val filterLibrary: FilterLibrary,
    val reflectionService: ReflectionService,
    val exportService: LogExportService,
    val entryImportFactory: EntryImportWorkerFactory,
    val logEntryFactory: LogEntryFactory,
    @param:Named(ExecutorNames.ENTRY_PROCESS) override val entryProcessExecutor: PausableThreadPoolExecutor,
    @param:Named(ExecutorNames.ENTRY_IMPORT) val entryImportExecutor: PausableThreadPoolExecutor,
    @param:Named(ExecutorNames.CLEANUP) val cleanupExecutor: ScheduledExecutorService,
) : LogProcessor {
    val log = LogManager.getLogger(LogProcessorImpl::class.java)

    var enabled by Preference(PREF_ENABLED, true, storage = StorageType.TEMP)

    var enableForAll: Boolean by Preference(PREF_LOG_ALL, true)
    var enableForProxy: Boolean by Preference(PREF_LOG_PROXY, true)
    var enableForRepeater: Boolean by Preference(PREF_LOG_REPEATER, true)
    var enableForIntruder: Boolean by Preference(PREF_LOG_INTRUDER, true)
    var enableForExtensions: Boolean by Preference(PREF_LOG_EXTENSIONS, true)
    var enableForScanner: Boolean by Preference(PREF_LOG_SCANNER, true)
    var enableForSequencer: Boolean by Preference(PREF_LOG_SEQUENCER, true)
    var enableForSuite: Boolean by Preference(PREF_LOG_SUITE, true)
    var enableForTarget: Boolean by Preference(PREF_LOG_TARGET_TAB, true)
    var enableForRecordedLogins: Boolean by Preference(PREF_LOG_RECORDED_LOGINS, true)
    val enableReflectionChecks by Preference(PREF_ENABLE_REFLECTIONS, true)

    var restrictToScope: Boolean by Preference(PREF_RESTRICT_TO_SCOPE, false)
    val maxRespSizeBytes: Int by Preference(PREF_MAX_RESP_SIZE, 10)

    //todo drop this and use future status instead
    val entriesPendingProcessing = ConcurrentHashMap<Int, LogEntry>()
    val entryProcessingFutures = ConcurrentHashMap<Int, Future<LogEntry?>>()

    var doNotLogFilter: FilterRule? by NullablePreference(PREF_DO_NOT_LOG_IF_MATCH)
    val colorFilters = tableColorService.entries
    val tagFilters = tagService.entries

    /**
     * Capture incoming requests and responses.
     * Logic to allow requests independently and match them to responses once received.
     */
    init {
        cleanupExecutor.scheduleAtFixedRate(
            AbandonedRequestCleanupRunnable(),
            30,
            30,
            TimeUnit.SECONDS
        )

        val autoImport by Preference(PREF_AUTO_IMPORT_PROXY_HISTORY, false)
        if(autoImport){
            importProxyHistory(false)
        }
    }

    override fun handleHttpRequestToBeSent(requestToBeSent: HttpRequestToBeSent): RequestToBeSentAction {
//        println("Http - Request To Be Sent ${requestToBeSent.messageId()} ${requestToBeSent.url()} ${requestToBeSent.hashCode()}")
        if (!isValidTool(requestToBeSent.toolSource().toolType()) || !isUrlInScope(requestToBeSent.url())) {
            return RequestToBeSentAction.continueWith(requestToBeSent)
        }
        val arrivalTime = Date()

        //If we're handling a new request, create a log entry.
        //We must also handle proxy messages here, since the HTTP listener operates after the proxy listener
        val logEntry = logEntryFactory.fromRequest(requestToBeSent.toolSource().toolType(),
            requestToBeSent, arrivalTime)

        //Set the entry's identifier to the HTTP request's hashcode.
        // For non-proxy messages, this doesn't change when we receive the response
        val identifier = System.identityHashCode(requestToBeSent)

        logEntry.identifier = identifier
        val annotations = LogProcessorHelper.addIdentifierInComment(
            identifier,
            requestToBeSent.annotations()
        )
        //Submit a new task to process the entry
        submitNewEntryProcessingRunnable(logEntry)

        return RequestToBeSentAction.continueWith(requestToBeSent, annotations)
    }

    override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {
//        println("Http - Response Received ${responseReceived.messageId()} ${responseReceived.initiatingRequest().url()} ${responseReceived.initiatingRequest().hashCode()}")
        if (!enableForAll || !isValidTool(
                responseReceived.toolSource().toolType()
            )
            || !isUrlInScope(responseReceived.initiatingRequest().url())
        ) {
            return ResponseReceivedAction.continueWith(responseReceived)
        }
        val arrivalTime = Date()

        var annotations = responseReceived.annotations()
        if (responseReceived.toolSource().isFromTool(ToolType.PROXY)) {
            //If the request came from the proxy, the response isn't final yet.
            //Instead, the response must be taken from the proxy response handler.
            //Just tag the comment with the identifier so we can match it up later.
//                    Integer identifier = System.identityHashCode(responseReceived.initiatingRequest());
//                    annotations = LogProcessorHelper.addIdentifierInComment(identifier, annotations);
//                    return ResponseResult.responseResult(response, annotations); //Process proxy responses using processProxyMessage
        } else {
            //Otherwise, we have the final HTTP response, and can use the request hashcode to match it up with the log entry.
            val (identifier, extractedAnnotations) =
                LogProcessorHelper.extractAndRemoveIdentifierFromRequestResponseComment(
                    responseReceived.annotations()
                )

            updateRequestWithResponse(identifier!!, arrivalTime, responseReceived)
            annotations = extractedAnnotations
        }
        return ResponseReceivedAction.continueWith(responseReceived, annotations)
    }

    private fun isUrlInScope(url: String): Boolean =
        !restrictToScope || montoya.scope().isInScope(url)


    override fun handleRequestReceived(interceptedRequest: InterceptedRequest): ProxyRequestReceivedAction {
//        println("Proxy Request Received: id ${interceptedRequest.messageId()}, url: ${interceptedRequest.url()} ${interceptedRequest.hashCode()}")
        return ProxyRequestReceivedAction.continueWith(interceptedRequest)
    }

    override fun handleRequestToBeSent(interceptedRequest: InterceptedRequest): ProxyRequestToBeSentAction {
//        println("Proxy Request to be sent: id ${interceptedRequest.messageId()}, url: ${interceptedRequest.url()} ${interceptedRequest.hashCode()}")
        return ProxyRequestToBeSentAction.continueWith(interceptedRequest)
    }

    override fun handleResponseReceived(interceptedResponse: InterceptedResponse): ProxyResponseReceivedAction {
//        println("Proxy Response Received: id ${interceptedResponse.messageId()}, url: ${interceptedResponse.request().url()} ${interceptedResponse.initiatingRequest().hashCode()}")
        return ProxyResponseReceivedAction.continueWith(interceptedResponse) //Do nothing. This isn't even its final form!
    }

    override fun handleResponseToBeSent(interceptedResponse: InterceptedResponse): ProxyResponseToBeSentAction {
//        println("Proxy Response To Be Sent: id ${interceptedResponse.messageId()}, url: ${interceptedResponse.request().url()} ${interceptedResponse.initiatingRequest().hashCode()}")
        if (!enableForAll || !enableForProxy || !isUrlInScope(interceptedResponse.initiatingRequest().url())) {
            return ProxyResponseToBeSentAction.continueWith(interceptedResponse)
        }

        val arrivalTime = Date()
        val (identifier, annotations) =
            LogProcessorHelper.extractAndRemoveIdentifierFromRequestResponseComment(
                interceptedResponse.annotations()
            )

        if(identifier != null){
            updateRequestWithResponse(identifier, arrivalTime, interceptedResponse)
        }

        return ProxyResponseToBeSentAction.continueWith(interceptedResponse, annotations)
    }

    /**
     * When a response comes in, determine if the request has already been processed or not.
     * If it has not yet been processed, add the response information to the entry and let the original job handle it.
     * Otherwise, create a new job to process the response.
     * Unknown UUID's signify the response arrived after the pending request was cleaned up.
     *
     * @param entryIdentifier The unique UUID for the log entry.
     * @param arrivalTime     The arrival time of the response.
     * @param response The HTTP request response object.
     */
    private fun updateRequestWithResponse(
        entryIdentifier: Int,
        arrivalTime: Date,
        response: HttpResponse
    ) {
//        log.debug("Updating entry with response for ID: $entryIdentifier")
        if (entriesPendingProcessing.containsKey(entryIdentifier)) {
            //Not yet started processing the entry, we can add the response so it is processed in the first pass
            val logEntry = entriesPendingProcessing[entryIdentifier]!!
            //Update the response with the new one, and tell it when it arrived.
            logEntry.response = response
            logEntry.responseDateTime = arrivalTime

            //Do nothing now, there's already a runnable submitted to process it somewhere in the queue.
            return
        } else if (entryProcessingFutures.containsKey(entryIdentifier)) {
            //Already started processing.
            //Get the processing thread
            val processingFuture = entryProcessingFutures[entryIdentifier]!!

            //Submit a job for the processing of its response.
            //This will block on the request finishing processing, then update the response and process it separately.
            entryProcessExecutor.submit(
                createEntryUpdateRunnable(
                    processingFuture,
                    response,
                    arrivalTime
                )
            )
        } else {
            //Unknown Identifier. Potentially for a request which was ignored or cleaned up already?
        }
    }

    fun truncateResponseBody(logEntry: LogEntry) {
        val response = logEntry.response!!
        if(response.body().length() > maxRespSizeBytes * 1000000L){
            logEntry.response = response.withBody(
                response.body().subArray(0, maxRespSizeBytes * 1000000)
                    .withAppended("...(truncated)"))
        }
    }

    fun findReflections(logEntry: LogEntry){
        if(!enableReflectionChecks) return
        if(logEntry.response == null) throw IllegalArgumentException("Cannot check reflections without response")
        val reflections = logEntry.request.parameters().parallelStream()
            .filter { parameter: ParsedHttpParameter ->
                !reflectionService.isParameterFiltered(parameter) && reflectionService.validReflection(
                    logEntry.response!!.bodyToString(), parameter
                )
            }
            .map { obj: ParsedHttpParameter -> obj.name() }.collect(Collectors.toList())
        logEntry.reflectedParameters.addAll(reflections)
    }

    override fun testColorFilters(logEntry: LogEntry){
        //Check against color filters
        for (tableColorRule in colorFilters) {
            tableColorService.testAndUpdateEntry(tableColorRule, logEntry, true)
        }
    }

    override fun testTags(logEntry: LogEntry){
        //Check against tags
        for (tag in tagFilters) {
            tagService.testAndUpdateEntry(tag, logEntry, true)
        }
    }


    private fun needsDeferredIgnoreCheck(): Boolean {
        if (doNotLogFilter == null) return false
        //TODO check if the null-safe works here
        return doNotLogFilter?.expression?.requiredContexts?.contains(FieldGroup.RESPONSE) == true
    }

    override fun shouldIgnoreEntry(logEntry: LogEntry): Boolean {
        if(doNotLogFilter == null) return false
        return filterLibrary.test(doNotLogFilter!!, logEntry)
    }

    private fun submitNewEntryProcessingRunnable(logEntry: LogEntry) {
        entriesPendingProcessing[logEntry.identifier!!] = logEntry
        val processingRunnable: RunnableFuture<LogEntry?> = FutureTask(
            Callable {
                synchronized(logEntry) {
                    entriesPendingProcessing.remove(logEntry.identifier)
                    //If the response is here, or if the ignore check doesn't need deferring
                    if (logEntry.response != null || !needsDeferredIgnoreCheck()) {
                        //Check if we should ignore
                        if (shouldIgnoreEntry(logEntry)) {
                            entryProcessingFutures.remove(logEntry.identifier)
                            return@Callable null
                        }
                    }

                    testTags(logEntry)
                    testColorFilters(logEntry)

                    addNewEntry(logEntry, true)

                    if(logEntry.response != null){
                        //If the entry was fully processed, remove it from the processing list.
                        entryProcessingFutures.remove(logEntry.identifier)
                    }else {
                        //We're waiting on the response, we'll use this future to know we're done later.
                    }
                    return@Callable logEntry
                }
            })
        entryProcessingFutures[logEntry.identifier!!] = processingRunnable
        entryProcessExecutor.submit(processingRunnable)
    }

    private fun createEntryUpdateRunnable(
        requestProcessor: Future<LogEntry?>,
        response: HttpResponse,
        arrivalTime: Date
    ): RunnableFuture<LogEntry> {
        return FutureTask(Callable {
            //Block until initial processing is complete.
            val logEntry = requestProcessor.get()
                ?: //Request was filtered during response processing. We can just ignore the response.
                return@Callable null

            //Request was processed successfully... now process the response.
            logEntry.response = response
            logEntry.responseDateTime = arrivalTime

            if(needsDeferredIgnoreCheck() && shouldIgnoreEntry(logEntry)) {
                removeExistingEntry(logEntry)
                entryProcessingFutures.remove(logEntry.identifier)
                return@Callable null
            }

            truncateResponseBody(logEntry)
            findReflections(logEntry)
            //TODO Optimize so we only retest ones containing response field group
            testColorFilters(logEntry)
            testTags(logEntry)

            //If the entry was fully processed, remove it from the processing list.
            entryProcessingFutures.remove(logEntry.identifier)

            updateExistingEntry(logEntry)
            return@Callable logEntry
        })
    }

    override fun importProxyHistory(sendToAutoExporters: Boolean) {
        //TODO Fix time bug for imported results. Multithreading means results will likely end up mixed.
        //Build list of entries to import
        val proxyHistory = montoya.proxy().history()
        val maxEntries: Int by Preference(PREF_MAXIMUM_ENTRIES, 1000000)
        val startIndex = max((proxyHistory.size - maxEntries).toDouble(), 0.0).toInt()

        val entriesToImport = mutableListOf<ProxyHttpRequestResponse>()
        repository.clear()
        //Build and start import worker
        entriesToImport.addAll(proxyHistory.subList(startIndex, proxyHistory.size))
        entryImportFactory.workerForProxyEntries(entriesToImport, sendToAutoExporters).execute()
    }

    private fun isValidTool(toolType: ToolType): Boolean {
        if (enableForAll) return true

        return when (toolType) {
            ToolType.PROXY -> {
                enableForProxy
            }

            ToolType.INTRUDER -> {
                enableForIntruder
            }

            ToolType.REPEATER -> {
                enableForRepeater
            }

            ToolType.EXTENSIONS -> {
                enableForExtensions
            }

            ToolType.SCANNER -> {
                enableForScanner
            }

            ToolType.SEQUENCER -> {
                enableForSequencer
            }

            ToolType.SUITE -> {
                enableForSuite
            }

            ToolType.TARGET -> {
                enableForTarget
            }

            ToolType.RECORDED_LOGIN_REPLAYER -> {
                enableForRecordedLogins
            }

            else -> {
                false
            }
        }
    }

    override fun shutdown() {
        cleanupExecutor.shutdownNow()
        entryProcessExecutor.shutdownNow()
        entryImportExecutor.shutdownNow()
    }

    override fun addNewEntry(logEntry: LogEntry, sendToAutoExporters: Boolean) {
        if (sendToAutoExporters) exportService.exportNewEntry(logEntry)
        repository.add(logEntry)
    }

    fun updateExistingEntry(logEntry: LogEntry) {
        exportService.exportUpdatedEntry(logEntry)
        repository.update(logEntry)
    }

    fun removeExistingEntry(logEntry: LogEntry) {
        repository.remove(logEntry)
    }



    /*************************
     *
     * Private worker implementations
     *
     */
    var responseTimeout by Preference(PREF_RESPONSE_TIMEOUT, 60)
    private inner class AbandonedRequestCleanupRunnable : Runnable {
        override fun run() {
            val cutoff = Instant.now().minusSeconds(responseTimeout.toLong())
            synchronized(entryProcessingFutures) {
                try {
                    val iter
                            : MutableIterator<Map.Entry<Int?, Future<LogEntry?>>> =
                        entryProcessingFutures.entries.iterator()

                    while (iter.hasNext()) {
                        val (entryId, entryProcessor) = iter.next()
                        when (entryProcessor.state()) {
                            Future.State.RUNNING -> continue
                            Future.State.SUCCESS -> {
                                val logEntry = entryProcessor.get()

                                //LogEntry should never be null, but just in case
                                if (logEntry == null || isPastTimeout(logEntry, cutoff)) {
                                    iter.remove()
                                    if (logEntry?.tool == ToolType.PROXY) {
                                        //Remove the identifier from the comment.
                                        //TODO Fix Comment cleanup
//                                    LogEntry.extractAndRemoveIdentifierFromComment(logEntry);
                                    }
                                }
                            }

                            Future.State.FAILED -> {
                                log.error("Found entry that failed processing: $entryId", entryProcessor.exceptionNow())
                                entriesPendingProcessing.remove(entryId)
                                entryProcessingFutures.remove(entryId)
                            }

                            Future.State.CANCELLED -> {}
                        }

                    }
                } catch (e: Exception) {
//                    log.error(e)
                }
            }
        }

        fun isPastTimeout(logEntry: LogEntry, cutoff: Instant): Boolean {
            return logEntry.requestDateTime.toInstant().isBefore(cutoff)
        }
    }

    override fun parseDateResponseHeader(response: HttpResponse?): Date? {
        val dateHeader = response?.headers()?.find { httpHeader -> httpHeader.name().equals("date") }?.value()
            ?: return null
        try {
            return serverDateFormat.parse(dateHeader)
        }catch (_: ParseException){}
        return null
    }
}
