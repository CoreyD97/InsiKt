package com.coreyd97.insikt.exports

import com.coreyd97.insikt.logging.logentry.LogEntry
import javax.swing.JMenuItem

interface ContextMenuExportProvider {
    fun getExportEntriesMenuItem(entries: List<LogEntry>): JMenuItem?
}
