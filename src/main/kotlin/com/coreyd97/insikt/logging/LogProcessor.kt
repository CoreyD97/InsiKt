package com.coreyd97.insikt.logging

import burp.api.montoya.http.handler.HttpHandler
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.proxy.http.ProxyRequestHandler
import burp.api.montoya.proxy.http.ProxyResponseHandler
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.PausableThreadPoolExecutor
import java.util.Date

/**
 * LogProcessor public API used across the app. Implementations must also implement
 * Montoya handler interfaces so they can be registered with Burp.
 */
interface LogProcessor : HttpHandler, ProxyResponseHandler, ProxyRequestHandler {
    val entryProcessExecutor: PausableThreadPoolExecutor

    fun shutdown()

    fun importProxyHistory(sendToAutoExporters: Boolean)

    fun parseDateResponseHeader(response: HttpResponse?): Date?

    fun shouldIgnoreEntry(logEntry: LogEntry): Boolean

    fun testTags(logEntry: LogEntry)

    fun testColorFilters(logEntry: LogEntry)

    fun addNewEntry(logEntry: LogEntry, sendToAutoExporters: Boolean)
}
