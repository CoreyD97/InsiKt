package com.coreyd97.insikt.grepper

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Marker
import burp.api.montoya.http.message.HttpRequestResponse
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.util.NamedThreadFactory
import com.coreyd97.insikt.util.PREF_AUTO_GREP
import com.coreyd97.insikt.util.PREF_SEARCH_THREADS
import com.coreyd97.montoyautilities.Preference
import com.google.inject.Inject
import com.google.inject.Singleton
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import java.util.regex.Pattern
import javax.swing.SwingWorker

@Singleton
class GrepperService @Inject constructor(
    val montoya: MontoyaApi,
    val repository: LogRepository
) : LogRepository.Listener {

    private val listeners = mutableListOf<GrepperListener>()
    private val remainingEntries: AtomicInteger = AtomicInteger(0)
    private var searchThreads by Preference(PREF_SEARCH_THREADS, 5)
    private var searchExecutor = Executors.newFixedThreadPool(
        searchThreads,
        NamedThreadFactory("LPP-Grepper")
    )
    private var autoGrep by Preference(PREF_AUTO_GREP, false)

    val isSearching: Boolean
        get() = remainingEntries.get() > 0

    fun reset() { //TODO SwingWorker
        for (listener in this.listeners) {
            try {
                listener.onResetRequested()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addMarkers(
        requestResponse: HttpRequestResponse,
        matches: MutableList<GrepResults.Match>
    ): HttpRequestResponse? {
        val requestMarkers: MutableList<Marker?> = ArrayList()
        val responseMarkers: MutableList<Marker?> = ArrayList()
        for (match in matches) {
            val marker = Marker.marker(match.startIndex, match.endIndex)
            if (match.isRequest) {
                requestMarkers.add(marker)
            } else {
                responseMarkers.add(marker)
            }
        }

        return requestResponse.withRequestMarkers(requestMarkers)
            .withResponseMarkers(responseMarkers)
    }

    fun beginSearch(
        pattern: Pattern, inScopeOnly: Boolean,
        searchRequests: Boolean, searchResponses: Boolean
    ) {

        val logEntries = repository.snapshot()
        if(logEntries.isEmpty()) return
        remainingEntries.getAndSet(logEntries.size)

        this.listeners.forEach(Consumer { listener: GrepperListener? ->
            listener!!.onSearchStarted(pattern, logEntries.size)
        })

        object: SwingWorker<Unit, Int>() {
            override fun doInBackground() {
                for (logEntry in logEntries) {
                    searchExecutor.submit(
                        createProcessThread(
                            logEntry,
                            pattern,
                            inScopeOnly,
                            searchRequests,
                            searchResponses
                        )
                    )
                }
            }
        }.execute()
    }

    private fun processEntry(
        entry: LogEntry, pattern: Pattern, searchRequests: Boolean,
        searchResponses: Boolean
    ): GrepResults {
        var grepResults = GrepResults(entry)
        if (searchRequests) {
            processMatches(grepResults, pattern, entry.requestBytes, true)
        }
        if (entry.responseBytes != null && searchResponses) {
            processMatches(grepResults, pattern, entry.responseBytes!!, false)
        }
        return grepResults
    }

    private fun processMatches(
        grepResults: GrepResults, pattern: Pattern, content: ByteArray,
        isRequest: Boolean
    ) {
        val respMatcher = pattern.matcher(String(content))
        while (respMatcher.find() && !Thread.currentThread().isInterrupted) {
            val groups: MutableList<String> = mutableListOf()

            for (i in 0..respMatcher.groupCount()) {
                groups.add(respMatcher.group(i))
            }

            if (isRequest) {
                grepResults.addRequestMatch(
                    GrepResults.Match(groups, true, respMatcher.start(), respMatcher.end())
                )
            } else {
                grepResults.addResponseMatch(
                    GrepResults.Match(groups, false, respMatcher.start(), respMatcher.end())
                )
            }
        }
    }

    private fun createProcessThread(
        logEntry: LogEntry, pattern: Pattern,
        inScopeOnly: Boolean, searchRequests: Boolean, searchResponses: Boolean
    ): Runnable {
        return Runnable {
            try {
                if (Thread.currentThread().isInterrupted) {
                    return@Runnable
                }

                if (!inScopeOnly || montoya.scope().isInScope(logEntry.getUrl())) {
                    var grepResults = processEntry(logEntry, pattern, searchRequests, searchResponses)

                    for (listener in this.listeners) {
                        try {
                            listener.onEntryProcessed(grepResults)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } finally {
                val remaining = remainingEntries.decrementAndGet()
                if (remaining == 0) {
                    for (listener in listeners) {
                        try {
                            listener.onSearchComplete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    fun cancelSearch() {
        Thread {
            for (listener in listeners) {
                listener.onShutdownInitiated()
            }
            searchExecutor.shutdownNow()
            while (!searchExecutor.isTerminated) {
                try {
                    Thread.sleep(1000)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }

            for (listener in listeners) {
                try {
                    listener.onShutdownComplete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            remainingEntries.set(0)
        }.start()
    }

    fun addListener(listener: GrepperListener?) {
        synchronized(this.listeners) {
            this.listeners.add(listener!!)
        }
    }

    fun removeListener(listener: GrepperListener?) {
        synchronized(this.listeners) {
            this.listeners.remove(listener)
        }
    }

    override fun onAdded(index: Int) {
        val entry = repository.getByIndex(index)

    }

    override fun onUpdated(index: Int) {
        val entry = repository.getByIndex(index)

    }
}
