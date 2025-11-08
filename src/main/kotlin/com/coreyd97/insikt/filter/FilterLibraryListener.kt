package com.coreyd97.insikt.filter

interface FilterLibraryListener {
    fun onFilterAdded(filterRule: FilterRule, index: Int)
    fun onFilterRemoved(filterRule: FilterRule, index: Int)
    fun onFilterModified(filterRule: FilterRule, index: Int)
    fun activeFilterChanged(filterRule: FilterRule)
}

open class FilterLibraryListenerAdapter : FilterLibraryListener {
    override fun onFilterAdded(filterRule: FilterRule, index: Int) {}
    override fun onFilterRemoved(filterRule: FilterRule, index: Int) {}
    override fun onFilterModified(filterRule: FilterRule, index: Int) {}
    override fun activeFilterChanged(filterRule: FilterRule) {}
}
