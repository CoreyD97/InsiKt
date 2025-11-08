package com.coreyd97.insikt.exports

import com.coreyd97.insikt.logging.logentry.LogEntry
import javax.swing.JComponent

interface LogExporter {
    /**
     * Configure the exporter ready for use
     *
     * @throws Exception Setup not completed
     */
    @Throws(Exception::class)
    fun setup()

    /**
     * Handle the export of a received entry
     *
     * @param logEntry
     */
    fun exportNewEntry(logEntry: LogEntry)

    /**
     * Handle the export of a received entry
     *
     * @param logEntry
     */
    fun exportUpdatedEntry(logEntry: LogEntry)

    /**
     * Clean up the exporter and its resources
     *
     * @throws Exception
     */
    @Throws(Exception::class)
    fun shutdown()

    /**
     * Build the control panel to be displayed in the preferences tab
     *
     * @return JComponent Component to be displayed
     */
    val exportPanel: JComponent?
}
