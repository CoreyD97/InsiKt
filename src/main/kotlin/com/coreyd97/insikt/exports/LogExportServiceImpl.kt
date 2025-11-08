package com.coreyd97.insikt.exports

import com.coreyd97.insikt.logging.logentry.LogEntry
import com.google.inject.Inject
import com.google.inject.Singleton

interface LogExportService {
    val enabledExporters: MutableSet<LogExporter>
    fun enableExporter(logExporter: LogExporter)
    fun disableExporter(logExporter: LogExporter)
    fun exportNewEntry(logEntry: LogEntry)
    fun exportUpdatedEntry(logEntry: LogEntry)
}

@Singleton
class LogExportServiceImpl : LogExportService {
    override val enabledExporters: MutableSet<LogExporter> = mutableSetOf()

    @Throws(Exception::class)
    override fun enableExporter(logExporter: LogExporter) {
        logExporter.setup()
        this.enabledExporters.add(logExporter)
    }

    @Throws(Exception::class)
    override fun disableExporter(logExporter: LogExporter) {
        this.enabledExporters.remove(logExporter)
        logExporter.shutdown()
    }

    override fun exportNewEntry(logEntry: LogEntry) {
        for (exporter in this.enabledExporters) {
            exporter.exportNewEntry(logEntry)
        }
    }

    override fun exportUpdatedEntry(logEntry: LogEntry) {
        for (exporter in this.enabledExporters) {
            exporter.exportUpdatedEntry(logEntry)
        }
    }
}
