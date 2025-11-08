package com.coreyd97.insikt.filter

import com.coreyd97.montoyautilities.NullablePreference
import com.coreyd97.montoyautilities.StorageType
import com.google.inject.Singleton

interface TableFilterListener {
    fun onFilterApplied(filter: FilterRule)
    fun onFilterCleared(previousFilter: FilterRule?)
    fun onFilterPaused()
    fun onFilterResumed(filter: FilterRule)
}


interface TableFilterService {
    val listeners: MutableList<TableFilterListener>

    fun activeFilter(): FilterRule?
    fun setFilter(filter: FilterRule)
    fun clearFilter()
    fun pauseFilter()
    fun resumeFilter()
    fun isPaused(): Boolean

    fun addListener(listener: TableFilterListener, triggerCurrentStatus: Boolean = true){
        listeners.add(listener)
        if(!triggerCurrentStatus){ return }
        if(activeFilter() != null){
            listener.onFilterApplied(activeFilter()!!)
        }else{
            listener.onFilterCleared(null)
        }
    }
    fun removeListener(listener: TableFilterListener){
        listeners.remove(listener)
    }
}

@Singleton
class TableFilterServiceImpl : TableFilterService {
    override val listeners: MutableList<TableFilterListener> = mutableListOf()
    var activeFilter: FilterRule? by NullablePreference("activeFilter", storage = StorageType.TEMP)
        private set
    var paused: Boolean = false
        private set

    init {
        if(activeFilter != null) {
            setFilter(activeFilter!!)
        }
    }

    override fun activeFilter(): FilterRule? {
        return if(paused) null else activeFilter
    }

    override fun setFilter(filter: FilterRule) {
        if(filter == activeFilter) return
        require(filter.isValid) { "Cannot apply invalid filter" }
        activeFilter = filter
        paused = false
        listeners.forEach {
            runCatching {
                it.onFilterApplied(filter)
            }
        }
    }
    override fun clearFilter() {
        activeFilter = null
        listeners.forEach {
            runCatching { it.onFilterCleared(activeFilter) } }
    }

    override fun pauseFilter() {
        require(!paused) { "Already paused." }
        listeners.forEach {
            runCatching {
                it.onFilterPaused()
            }
        }
    }

    override fun resumeFilter() {
        require(paused) { "Not paused." }
        requireNotNull(activeFilter) { "The active filter must not be null." }
        listeners.forEach {
            runCatching {
                it.onFilterResumed(activeFilter!!)
            }
        }
    }

    override fun isPaused(): Boolean {
        return paused
    }
}