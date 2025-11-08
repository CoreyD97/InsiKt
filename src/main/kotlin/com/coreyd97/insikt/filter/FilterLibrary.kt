package com.coreyd97.insikt.filter

import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.DEFAULT_LIBRARY_ENTRIES
import com.coreyd97.insikt.util.PREF_SAVED_FILTERS
import com.coreyd97.montoyautilities.Preference
import com.google.inject.Inject
import com.google.inject.Singleton
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.LogManager

interface FilterLibrary {
    val snippets: MutableSet<FilterRule>
    val snippetListeners: MutableList<FilterLibraryListener>

    fun createFilter(filter: String): FilterRule
    fun test(rule: FilterRule, logEntry: LogEntry): Boolean

    fun addToLibrary(filter: FilterRule)
    fun removeFromLibrary(filter: FilterRule)
    fun snippetUpdated(filter: FilterRule)

    fun addListener(listener: FilterLibraryListener) {
        snippetListeners.add(listener)
    }

    fun removeListener(listener: FilterLibraryListener) {
        snippetListeners.remove(listener)
    }
}

@Singleton
class FilterLibraryImpl @Inject constructor(
) : FilterLibrary {
    val log = LogManager.getLogger(FilterLibraryImpl::class.java)
    val defaultLibraryFilters = Json { ignoreUnknownKeys = true }.decodeFromString<MutableSet<FilterRule>>(DEFAULT_LIBRARY_ENTRIES)
    override val snippets by Preference(PREF_SAVED_FILTERS, defaultLibraryFilters)
    override val snippetListeners = mutableListOf<FilterLibraryListener>()

    override fun createFilter(filter: String): FilterRule {
        return FilterRule.fromString(filter)
    }

    fun test(filterExpression: FilterExpression, logEntry: LogEntry): Boolean {
        return FilterEvaluationVisitor(this).visit(filterExpression.astExpression, logEntry)
    }

    override fun test(rule: FilterRule, logEntry: LogEntry): Boolean {
        if (!rule.isValid) {
            return false
        }
        return test(rule.expression!!, logEntry)
    }

    override fun addToLibrary(filter: FilterRule) {
        var index: Int
        synchronized(this.snippets) {
            index = snippets.size - 1
            if (filter.name == null) filter.name = "NewFilter${index}"
            snippets.add(filter)
        }
        for (listener in this.snippetListeners) {
            try {
                listener.onFilterAdded(filter, index)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun removeFromLibrary(filter: FilterRule) {
        var index: Int
        synchronized(this.snippets) {
            index = snippets.indexOf(filter)
            snippets.remove(filter)
        }
        for (listener in this.snippetListeners) {
            try {
                listener.onFilterRemoved(filter, index)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun snippetUpdated(filter: FilterRule) {
        snippetListeners.forEach { it.onFilterModified(filter, -1) }
    }


}