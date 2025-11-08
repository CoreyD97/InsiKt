package com.coreyd97.insikt.grepper

import java.util.regex.Pattern

interface GrepperListener {
    fun onSearchStarted(pattern: Pattern, searchEntries: Int)

    fun onEntryProcessed(entryResults: GrepResults)

    fun onResetRequested()

    fun onSearchComplete()

    fun onShutdownInitiated()

    fun onShutdownComplete()
}
