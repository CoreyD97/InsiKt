package com.coreyd97.insikt.logging

import burp.api.montoya.core.ToolType
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.proxy.ProxyHttpRequestResponse
import com.coreyd97.insikt.ExecutorNames
import com.coreyd97.insikt.filter.TableFilterService
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryFactory
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.view.shared.CancellableSwingWorkerWithProgressDialog
import com.coreyd97.insikt.util.PausableThreadPoolExecutor
import com.google.inject.Inject
import com.google.inject.name.Named
import java.awt.Window
import java.time.Instant
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.stream.Collectors

class EntryImportWorkerFactory @Inject constructor(
    @param:Named("extension") val extensionFrame: Window,
    val logProcessor: LogProcessor,
    val logEntryFactory: LogEntryFactory,
    val filterService: TableFilterService,
    @param:Named(ExecutorNames.ENTRY_IMPORT) val importExecutor: PausableThreadPoolExecutor,
    @param:Named(ExecutorNames.ENTRY_PROCESS) val entryProcessExecutor: PausableThreadPoolExecutor,

    ){
    fun workerForTool(origin: ToolType, entries: List<HttpRequestResponse>): EntryImportWorker {
        return EntryImportWorker(logProcessor, logEntryFactory,
        filterService, entryProcessExecutor, importExecutor,
            dialogOwner = extensionFrame,
            title = "${APP_NAME} - Import",
            message = "Importing ${entries.size} entries from ${origin.toolName()}",
            httpEntries = entries, originatingTool = origin)
    }

    fun workerForProxyEntries(entries: List<ProxyHttpRequestResponse>, sendToAutoExporters: Boolean): EntryImportWorker {
        return EntryImportWorker(logProcessor, logEntryFactory,
            filterService, entryProcessExecutor, importExecutor,
            dialogOwner = extensionFrame,
            title = "${APP_NAME} - Import",
            message = "Importing ${entries.size} entries from Proxy",
            proxyEntries = entries, originatingTool = ToolType.PROXY,
            sendToAutoExporters = sendToAutoExporters)
    }
}

class EntryImportWorker internal constructor(
    val logProcessor: LogProcessor,
    val logEntryFactory: LogEntryFactory,
    val tableFilterService: TableFilterService,
    val entryProcessExecutor: PausableThreadPoolExecutor,
    val entryImportExecutor: PausableThreadPoolExecutor,
    val httpEntries: List<HttpRequestResponse>? = null,
    val proxyEntries: List<ProxyHttpRequestResponse>? = null,
    val chunkSize: Int = 5000,
    dialogOwner: Window,
    title: String = "${APP_NAME} - Import",
    message: String = "Importing ${(httpEntries?.size ?: 0) + (proxyEntries?.size ?: 0) } entries",
    var originatingTool: ToolType = ToolType.EXTENSIONS,
    var callback: Runnable? = null,
    var sendToAutoExporters: Boolean = false
) :
    CancellableSwingWorkerWithProgressDialog<Unit, Int>(
        dialogOwner,
        title,
        message
    ) {

    @Throws(Exception::class)
    override fun doWork() {
        entryProcessExecutor.pause() //Pause the processor, we don't want it mixing with our import.
        tableFilterService.pauseFilter()
        val entries = httpEntries ?: proxyEntries ?: listOf()

        val countDownLatch = CountDownLatch(entries.size)

        // Process entries in chunks to reduce the number of executor jobs
        var globalIndex = 0
        val total = entries.size
        while (globalIndex < total) {
            if (entryImportExecutor.isShutdown || this.isCancelled) return

            val endExclusive = minOf(globalIndex + chunkSize, total)
            val startIndexForChunk = globalIndex
            val subList = entries.subList(startIndexForChunk, endExclusive)

            entryImportExecutor.submit {
                try {
                    var localIndex = 0
                    for (entry in subList) {
                        if (this.isCancelled) break
                        val currentIndex = startIndexForChunk + localIndex
                        try {
                            val response = when(entry){
                                is HttpRequestResponse -> entry.response()
                                is ProxyHttpRequestResponse -> entry.response()
                                else -> null
                            }
                            val responseDate = logProcessor.parseDateResponseHeader(response)
                                ?: Date.from(Instant.now())
                            val logEntry = when(entry){
                                is HttpRequestResponse -> {
                                    logEntryFactory.fromRequestResponse(originatingTool, entry, responseDate, responseDate)
                                }
                                is ProxyHttpRequestResponse -> {
                                    logEntryFactory.fromProxyRequestResponse(originatingTool, entry, responseDate, responseDate)
                                }
                                else -> throw IllegalArgumentException("")
                            }

                            if (!logProcessor.shouldIgnoreEntry(logEntry)) {
                                logProcessor.testTags(logEntry)
                                logProcessor.testColorFilters(logEntry)
                                logProcessor.addNewEntry(logEntry, sendToAutoExporters)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        } finally {
                            publish(currentIndex)
                            countDownLatch.countDown()
                        }
                        localIndex++
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }

            }

            globalIndex = endExclusive
        }

        countDownLatch.await()
        tableFilterService.resumeFilter()
    }

    override fun handleProgress(chunks: List<Int>) {
        //Do nothing? Ui is handled by super.
    }

    override fun done() {
        logProcessor.entryProcessExecutor.resume()
        this.callback?.run()
        super.done()
    }

    override fun totalTasks(): Int {
        return (httpEntries?.size ?: 0) + (proxyEntries?.size ?: 0)
    }
}