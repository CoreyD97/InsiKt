package com.coreyd97.insikt.view.logtable

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.filter.ColorizingRuleListener
import com.coreyd97.insikt.filter.ColorizingRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.TableColorService
import com.coreyd97.insikt.filter.TableFilterListener
import com.coreyd97.insikt.filter.TableFilterService
import com.coreyd97.insikt.filter.TagService
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.util.loggerDateFormat
import com.google.inject.Inject
import com.google.inject.Singleton
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.Future
import javax.swing.SwingUtilities
import javax.swing.SwingWorker

@Singleton
class LogTableModel @Inject constructor(
    val montoyaApi: MontoyaApi,
    val repository: LogRepository,
    private val columnModel: LogTableColumnModel,
    val filterLibrary: FilterLibrary,
    val tableFilterService: TableFilterService,
    val tagService: TagService,
    val colorService: TableColorService,
) : javax.swing.table.AbstractTableModel(), LogRepository.Listener {

    val logger = LogManager.getLogger(this.javaClass)

    // Make snapshot mutable so we can apply deltas incrementally.
    var snapshot: MutableList<LogEntry> = ArrayList(repository.snapshot())
    val entriesMatchingFilter = mutableSetOf<LogEntry>()
    val tagListener: TagListener
    val colorFilterListener: ColorFilterListener

    // Event coalescing state
    private sealed class RepoEvent {
        data class Added(val index: Int) : RepoEvent()
        data class Updated(val index: Int) : RepoEvent()
        data class Removed(val index: Int) : RepoEvent()
        data object Reset : RepoEvent()
    }
    private val eventQueue = ArrayDeque<RepoEvent>()
    @Volatile private var flushScheduled = false

    init {
        this.tagListener = TagListener()
        this.colorFilterListener = ColorFilterListener()
        // Subscribe to repository changes and mirror them as table events
        repository.addListener(this)
        tagService.addListener(tagListener)
        colorService.addListener(colorFilterListener)
    }

    fun dispose() {
        repository.removeListener(this)
        tagService.removeListener(tagListener)
        colorService.removeListener(tagListener)
        snapshot.clear()
        fireTableDataChanged()
    }

    private fun enqueue(event: RepoEvent) {
        synchronized(eventQueue) {
            eventQueue.addLast(event)
            if (!flushScheduled) {
                flushScheduled = true
                SwingUtilities.invokeLater { flushEventsOnEdt() }
            }
        }
    }

    private fun flushEventsOnEdt() {
        // Must be on EDT
        val drained = ArrayList<RepoEvent>()
        synchronized(eventQueue) {
            while (eventQueue.isNotEmpty()) drained.add(eventQueue.removeFirst())
            flushScheduled = false
        }
        if (drained.isEmpty()) return

        // If there is a reset, rebuild snapshot once and issue a full refresh.
        if (drained.any { it is RepoEvent.Reset } || drained.size > 1000) {
            snapshot = repository.snapshot().toMutableList()
            fireTableDataChanged()
            return
        }
        val activeFilter = tableFilterService.activeFilter()

        // Apply events incrementally in the order they occurred to preserve index semantics.
        for (e in drained) {
            when (e) {
                is RepoEvent.Added -> {
                    val idx = e.index
                    val entry = repository.getByIndex(idx)
                    if (entry != null) {
                        val insertAt = idx.coerceIn(0, snapshot.size) // guard against late indices
                        snapshot.add(insertAt, entry)
                        if(activeFilter != null){
                            val matches = filterLibrary.test(activeFilter, entry)
                            if(matches) entriesMatchingFilter.add(entry)
                        }
                        fireTableRowsInserted(insertAt, insertAt)
                    } else {
                        // Repository no longer has the item at idx; skip safely.
                        // We intentionally avoid a full refresh to preserve selection.
                    }
                }
                is RepoEvent.Updated -> {
                    val idx = e.index
                    if (idx in 0 until snapshot.size) {
                        val entry = repository.getByIndex(idx)
                        if (entry != null) {
                            snapshot[idx] = entry
                            fireTableRowsUpdated(idx, idx)
                        } else {
                            // If entry missing, keep current row but still notify to repaint.
                            fireTableRowsUpdated(idx, idx)
                        }
                    } else {
                        // Index out of current bounds; ignore to avoid selection reset.
                    }
                }
                is RepoEvent.Removed -> {
                    val idx = e.index
                    if (idx in 0 until snapshot.size) {
                        val removed = snapshot.removeAt(idx)
                        entriesMatchingFilter.remove(removed)
                        fireTableRowsDeleted(idx, idx)
                    } else {
                        // Index already shifted/processed; safely ignore.
                    }
                }
                is RepoEvent.Reset -> {
                    // Already handled above
                }
            }
        }
    }

    override fun onAdded(index: Int) {
        enqueue(RepoEvent.Added(index))
    }

    override fun onUpdated(index: Int) {
        enqueue(RepoEvent.Updated(index))
    }

    override fun onRemoved(index: Int) {
        enqueue(RepoEvent.Removed(index))
    }

    override fun onReset() {
        enqueue(RepoEvent.Reset)
    }

    override fun getRowCount(): Int {
        return snapshot.size
    }

    override fun getColumnCount(): Int {
        // Model describes all columns (visible or hidden). Visibility is handled by TableColumnModel.
        return columnModel.getAllColumns().size
    }

    override fun isCellEditable(rowModelIndex: Int, columnModelIndex: Int): Boolean {
        // Use model index to access the full column definition
        val all = columnModel.getAllColumns()
        if (columnModelIndex < 0 || columnModelIndex >= all.size) return false
        return !all[columnModelIndex].readOnly
    }

    override fun setValueAt(value: Any, rowModelIndex: Int, columnModelIndex: Int) {
        // No-op (comment field support could fetch & modify the same LogEntry instance via repository)
        fireTableCellUpdated(rowModelIndex, columnModelIndex)
    }

    override fun getColumnClass(columnModelIndex: Int): Class<*> {
        if (snapshot.isEmpty()) return Any::class.java
        val v = getValueAt(0, columnModelIndex)
        return v?.javaClass ?: Any::class.java
    }

    override fun getValueAt(rowIndex: Int, colModelIndex: Int): Any? {
        if (rowIndex < 0 || rowIndex >= snapshot.size) {
            return null
        }

        val all = columnModel.getAllColumns()
        if (colModelIndex < 0 || colModelIndex >= all.size) return null
        val column = all[colModelIndex]

        if (column.identifier == LogEntryField.NUMBER) {
            return rowIndex + 1
        }

        val logEntry = snapshot[rowIndex]
        val value = logEntry.getValueByKey(column.identifier)

        if (value is Date) {
            return loggerDateFormat.format(value)
        }
        return value
    }

    val data: List<LogEntry>
        get() = snapshot

    fun getRow(row: Int): LogEntry {
        return snapshot[row]
    }

    fun reset() {
        // Let repository decide whether to clear globally; the model only reflects
        fireTableDataChanged()
    }

    fun buildFilterList(filter: FilterRule, onComplete: () -> Unit): SwingWorker<Unit, Pair<Int, LogEntry>> {
        entriesMatchingFilter.clear()
        return RuleTestingWorker(
            { e -> filterLibrary.test(filter, e)},
            { _, e ->
                runCatching {
                    entriesMatchingFilter.add(e)
                }.onFailure { e ->
                    e.printStackTrace()
                }
            },
            onComplete
        )
    }

    inner class ColorFilterListener : ColorizingRuleListener {
        override fun onExpressionChange(rule: ColorizingRule) {
            RuleTestingWorker(
                {e -> colorService.testAndUpdateEntry(rule, e, true) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onAttributeChange(rule: ColorizingRule) {
            RuleTestingWorker(
                {e -> e.matchingColorFilters.contains(rule) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onAdd(rule: ColorizingRule) {
            if (!rule.enabled || rule.expression == null) {
                return
            }
            RuleTestingWorker(
                {e -> colorService.testAndUpdateEntry(rule, e, false) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onRemove(rule: ColorizingRule) {
            if (!rule.enabled || rule.expression == null) {
                return
            }
            RuleTestingWorker(
                {e -> e.matchingColorFilters.remove(rule) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }
    }

    inner class TagListener : ColorizingRuleListener {
        override fun onExpressionChange(rule: ColorizingRule) {
            RuleTestingWorker(
                {e -> tagService.testAndUpdateEntry(rule, e, true) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onAttributeChange(rule: ColorizingRule) {
            RuleTestingWorker(
                {e -> e.matchingTags.contains(rule) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onAdd(rule: ColorizingRule) {
            RuleTestingWorker(
                {e -> tagService.testAndUpdateEntry(rule, e, false) },
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }

        override fun onRemove(rule: ColorizingRule) {
            RuleTestingWorker(
                {entry -> entry.matchingTags.contains(rule)}, 
                {i, _ -> enqueue(RepoEvent.Updated(i))}
            ).execute()
        }
    }

    inner class RuleTestingWorker(
        val test: (LogEntry) -> Boolean,
        val onMatch: (Int, LogEntry) -> Unit,
        val onComplete: () -> Unit = {}
    ) : javax.swing.SwingWorker<Unit, Pair<Int, LogEntry>>() {

        override fun doInBackground() {
            val n = repository.size()
            if (n <= 0) return

            val threads = Runtime.getRuntime().availableProcessors()
            val chunkSize = 512

            val executor = java.util.concurrent.Executors.newFixedThreadPool(threads) { r ->
                Thread(r, "InsiKt-RuleTest").apply { isDaemon = true }
            }

            try {
                val futures = mutableListOf<Future<*>>()
                var start = n - 1
                while (start >= 0 && !isCancelled) {
                    val end = maxOf(0, start - chunkSize + 1)
                    val startIdx = start
                    val endIdx = end
                    futures += executor.submit {
                        val localMatches = ArrayList<Pair<Int, LogEntry>>(minOf(chunkSize, startIdx - endIdx + 1))
                        var i = startIdx
                        while (i >= endIdx && !isCancelled) {
                            val entry = repository.getByIndex(i)
                            if (entry != null) {
                                try {
                                    if (test(entry)) localMatches.add(Pair(i, entry))
                                } catch (t: Throwable) {
                                    logger.error(t.message, t)
                                }
                            }
                            i--
                        }
                        if (localMatches.isNotEmpty()) publish(*localMatches.toTypedArray())
                    }
                    start = end - 1
                }

                for (f in futures) if (!isCancelled) f.get()
            } finally {
                executor.shutdownNow()
            }
            onComplete.invoke()
        }

        override fun process(rows: List<Pair<Int, LogEntry>>) {
            // Batch delivered on the EDT; invoke onMatch for each
            for (row in rows) onMatch(row.first, row.second)
        }
    }
}

