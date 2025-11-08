package com.coreyd97.insikt.logview.repository

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.PREF_MAXIMUM_ENTRIES
import com.coreyd97.montoyautilities.Preference
import jakarta.inject.Inject
import org.apache.logging.log4j.LogManager
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max

interface LogRepository {
    fun size(): Int
    fun getByIndex(index: Int): LogEntry?
    fun entries(): List<LogEntry>
    fun snapshot(fromIndex: Int = 0, toIndex: Int = Int.MAX_VALUE): List<LogEntry>
    fun add(entry: LogEntry)
    fun update(entry: LogEntry)
    fun remove(entry: LogEntry)
    // Remove multiple entries at once; implementers should emit appropriate remove/reset events
    fun removeAll(entries: Collection<LogEntry>)
    // Allow global clear (used by UI "Clear Logs" and for resets)
    fun clear()

    fun addListener(listener: Listener)
    fun removeListener(listener: Listener)

    interface Listener {
        fun onAdded(index: Int) {}
        fun onUpdated(index: Int) {}
        fun onRemoved(index: Int) {}
        fun onReset() {}
    }
}


class InMemoryLogRepository @Inject constructor(
    val montoya: MontoyaApi
) : LogRepository {
    private val log = LogManager.getLogger(InMemoryLogRepository::class.java)
    private val entries = mutableListOf<LogEntry>()
    private val listeners = CopyOnWriteArrayList<WeakReference<LogRepository.Listener>>()
    private val maxEntries by Preference(PREF_MAXIMUM_ENTRIES, 1_000_000)

    init {
        montoya.extension().registerUnloadingHandler {
            clear()
            listeners.clear()
        }
    }

    override fun size(): Int = synchronized(entries) { entries.size }

    override fun getByIndex(index: Int): LogEntry? = synchronized(entries) {
        if (index < 0 || index >= entries.size) null else entries[index]
    }

    override fun entries(): List<LogEntry> = entries

    override fun snapshot(fromIndex: Int, toIndex: Int): List<LogEntry> = synchronized(entries) {
        val end = maxOf(0, minOf(entries.size, toIndex))
        val start = maxOf(0, minOf(end, fromIndex))
        entries.subList(start, end).toList()
    }

    override fun add(entry: LogEntry) {
        var addedIndex = -1
        var overflowRemovals = 0
        synchronized(entries) {
            entries.add(entry)
            val excess = max(entries.size - maxEntries, 0)
            overflowRemovals = excess
            repeat(excess) {
                entries.removeAt(0) // remove oldest
            }
            addedIndex = entries.lastIndex
        }

        // Fire notifications outside the lock and without any EDT concerns.
        repeat(overflowRemovals) {
            fireRemoved(0)
        }
        fireAdded(addedIndex)
    }

    override fun update(entry: LogEntry) {
        val index = synchronized(entries) { entries.indexOf(entry) }
        if (index >= 0) fireUpdated(index)
    }

    override fun remove(entry: LogEntry) {
        var removedIndex = -1
        synchronized(entries) {
            val idx = entries.indexOf(entry)
            if (idx >= 0) {
                entries.removeAt(idx)
                removedIndex = idx
            }
        }
        if (removedIndex >= 0) fireRemoved(removedIndex)
    }

    // Remove a collection of entries efficiently; fires per-index removed notifications in descending order.
    override fun removeAll(entriesToRemove: Collection<LogEntry>) {
        if (entriesToRemove.isEmpty()) return

        // Collect indices to remove in one pass to avoid O(n*m) scans.
        val indicesDesc: List<Int> = synchronized(entries) {
            if (entries.isEmpty()) return
            val toRemoveSet = entriesToRemove.toHashSet()
            val indices = ArrayList<Int>(toRemoveSet.size.coerceAtMost(entries.size))
            for (i in entries.indices) {
                if (entries[i] in toRemoveSet) indices.add(i)
            }
            if (indices.isEmpty()) return
            // Remove in descending index order to avoid shifting problems
            indices.sortDescending()
            for (idx in indices) {
                entries.removeAt(idx)
            }
            indices
        } ?: return

        // Notify removals in descending order so indices correspond to the model at the moment of each removal.
        for (idx in indicesDesc) {
            fireRemoved(idx)
        }
    }


    override fun clear() {
        val hadAny = synchronized(entries) { entries.isNotEmpty().also { entries.clear() } }
        if (hadAny) fireReset()
    }

    override fun addListener(listener: LogRepository.Listener) {
        listeners.add(WeakReference(listener))
    }

    override fun removeListener(listener: LogRepository.Listener) {
        listeners.remove(WeakReference(listener))
    }

    private fun fireAdded(index: Int) {
        listeners.forEach { it.get()?.onAdded(index) }
    }

    private fun fireUpdated(index: Int) {
        listeners.forEach { it.get()?.onUpdated(index) }
    }

    private fun fireRemoved(index: Int) {
        listeners.forEach { it.get()?.onRemoved(index) }
    }

    private fun fireReset() {
        listeners.forEach { it.get()?.onReset() }
    }
}
