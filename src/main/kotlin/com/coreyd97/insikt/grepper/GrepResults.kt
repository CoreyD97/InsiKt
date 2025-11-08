package com.coreyd97.insikt.grepper

import com.coreyd97.insikt.logging.logentry.LogEntry

class GrepResults(val logEntry: LogEntry) {
    val matches = mutableListOf<Match>()
    var requestMatches: Int = 0
        private set
    var responseMatches: Int = 0
        private set

    fun addRequestMatch(match: Match) {
        this.matches.add(match)
        this.requestMatches++
    }

    fun addResponseMatch(match: Match) {
        this.matches.add(match)
        this.responseMatches++
    }

    class Match internal constructor(
        val groups: List<String>,
        val isRequest: Boolean,
        val startIndex: Int,
        val endIndex: Int
    )
}
