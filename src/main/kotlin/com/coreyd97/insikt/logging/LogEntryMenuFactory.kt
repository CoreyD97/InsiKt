package com.coreyd97.insikt.logging

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.BurpSuiteEdition
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.scanner.AuditConfiguration
import burp.api.montoya.scanner.BuiltInAuditConfiguration
import burp.api.montoya.scanner.CrawlConfiguration
import com.coreyd97.insikt.exports.ContextMenuExportProvider
import com.coreyd97.insikt.exports.LogExporter
import com.coreyd97.insikt.filter.*
import com.coreyd97.insikt.filter.FilterLibrary
import com.google.inject.Inject
import com.coreyd97.insikt.filter.TableColorService
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.view.logtable.LogTable
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.util.loggerDateFormat
import com.coreyd97.insikt.view.logtable.LogView
import com.google.inject.Singleton
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.util.*
import java.util.stream.Collectors
import javax.swing.AbstractAction
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

/**
 * Created by corey on 24/08/17.
 */
@Singleton
class LogEntryMenuFactory @Inject constructor(
    val montoya: MontoyaApi,
    val logRepository: LogRepository,
    val filterLibrary: FilterLibrary,
    val tableFilterService: TableFilterService,
    val logView: LogView,
    val colorService: TableColorService,
    val exporters: Set<LogExporter>
){
    fun singleEntryMenu(entry: LogEntry, selectedField: LogEntryField, logTable: LogTable): JPopupMenu{
        return SingleLogEntryMenu(montoya, logRepository, filterLibrary, tableFilterService, colorService, logView, exporters, logTable, entry, selectedField)
    }
    
    fun multipleEntryMenu(selectedEntries: List<LogEntry>, logTable: LogTable): JPopupMenu{
        return MultipleLogEntryMenu(montoya, logRepository, filterLibrary, tableFilterService, colorService, exporters, logTable, selectedEntries)
    }
}

class SingleLogEntryMenu(
    val montoya: MontoyaApi,
    val logRepository: LogRepository,
    val libraryController: FilterLibrary,
    val tableFilterService: TableFilterService,
    val colorService: TableColorService,
    val logView: LogView,
    val exporters: Set<LogExporter>,
    val logTable: LogTable,
    entry: LogEntry,
    selectedField: LogEntryField
) : JPopupMenu() {

    init {
        val columnName = selectedField.fullLabel
        val columnValue = entry.getValueByKey(selectedField)

        val columnValueString = if (columnValue != null) {
            if (columnValue is Date) {
                "\"" + loggerDateFormat.format(columnValue) + "\""
            } else {
                if (columnValue is Number) columnValue.toString() else "\"" + columnValue + "\""
            }
        } else {
            "\"\""
        }

        val isPro = montoya.burpSuite().version().edition() == BurpSuiteEdition.PROFESSIONAL
        var title = entry.getValueByKey(LogEntryField.URL).toString()
        if (title.length > 50) title = title.substring(0, 47) + "..."
        this.add(JMenuItem(title))
        this.add(Separator())

        if (selectedField != LogEntryField.NUMBER) {
            val useAsFilter =
                JMenuItem(object : AbstractAction("Use $columnName Value As LogFilter") {
                    override fun actionPerformed(actionEvent: ActionEvent) {
                        tableFilterService.setFilter(
                            FilterRule.fromString(
                                "$columnName==$columnValueString"
                            )
                        )
                    }
                })
            this.add(useAsFilter)

            if (logTable.currentFilter != null) {
                val currentFilter = logTable.currentFilter!!
                val addToCurrentFilter = JMenu("Add $columnName Value To LogFilter")
                val andFilter = JMenuItem(object : AbstractAction(LogicalOperator.AND.label) {
                    override fun actionPerformed(actionEvent: ActionEvent) {
                        tableFilterService.setFilter(
                            currentFilter.withAddedCondition(
                                LogicalOperator.AND,
                                selectedField,
                                ComparisonOperator.EQUAL,
                                columnValueString
                            )
                        )
                    }
                })

                val andNotFilter = JMenuItem(object : AbstractAction("AND NOT") {
                    override fun actionPerformed(actionEvent: ActionEvent) {
                        tableFilterService.setFilter(
                            currentFilter.withAddedCondition(
                                LogicalOperator.AND,
                                selectedField,
                                ComparisonOperator.NOT_EQUAL,
                                columnValueString
                            )
                        )
                    }
                })

                val orFilter = JMenuItem(object : AbstractAction(LogicalOperator.OR.label) {
                    override fun actionPerformed(actionEvent: ActionEvent) {
                        tableFilterService.setFilter(
                            currentFilter.withAddedCondition(
                                LogicalOperator.OR,
                                selectedField,
                                ComparisonOperator.EQUAL,
                                columnValueString
                            )
                        )
                    }
                })
                addToCurrentFilter.add(andFilter)
                addToCurrentFilter.add(andNotFilter)
                addToCurrentFilter.add(orFilter)
                this.add(addToCurrentFilter)
            }

            val colorFilterItem =
                JMenuItem(object : AbstractAction("Set $columnName Value as Color Filter") {
                    override fun actionPerformed(actionEvent: ActionEvent) {
                        val tableColorRule =
                            ColorizingRule.fromString("New Filter", "$columnName == $columnValueString")
                        logView.displayColorDialogWithAdditions(listOf(tableColorRule))
                        //todo make dialog visible
                    }
                })
            this.add(colorFilterItem)
        }

        this.add(Separator())
        val inScope =
            montoya.scope().isInScope(entry.getValueByKey(LogEntryField.URL).toString())
        val scopeItem: JMenuItem
        if (!inScope) {
            scopeItem = JMenu("Add to scope")
            scopeItem.add(JMenuItem(object : AbstractAction("Domain") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    montoya.scope().includeInScope(entry.getHttpService().toString())
                }
            }))
            scopeItem.add(JMenuItem(object : AbstractAction("Domain + Path") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    montoya.scope().includeInScope(entry.getUrl())
                }
            }))
        } else {
            scopeItem = JMenu("Remove from scope")
            scopeItem.add(JMenuItem(object : AbstractAction("Domain") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    montoya.scope().excludeFromScope(entry.getHttpService().toString())
                }
            }))

            scopeItem.add(JMenuItem(object : AbstractAction("Domain + Path") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    montoya.scope().excludeFromScope(entry.getUrl())
                }
            }))
        }
        this.add(scopeItem)

        val exportMenu = JMenu("Export as...")
        for (exporter in exporters) {
            if (exporter is ContextMenuExportProvider) {
                val item =
                    (exporter as ContextMenuExportProvider).getExportEntriesMenuItem(listOf(entry))
                if (item != null) exportMenu.add(item)
            }
        }

        if (exportMenu.itemCount > 0) {
            this.add(Separator())
            this.add(exportMenu)
        }

        this.add(Separator())

        val spider = JMenuItem(object : AbstractAction("Crawl from here") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                val config = CrawlConfiguration.crawlConfiguration(entry.getUrl())
                montoya.scanner().startCrawl(config)
            }
        })
        this.add(spider)

        val activeScan = JMenuItem(object : AbstractAction("Do an active scan") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                val auditConfiguration =
                    AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS)
                val scan = montoya.scanner().startAudit(auditConfiguration)
                scan.addRequestResponse(
                    HttpRequestResponse.httpRequestResponse(
                        entry.request,
                        entry.response
                    )
                )
            }
        })
        this.add(activeScan)
        activeScan.isEnabled = isPro

        val passiveScan = JMenuItem(object : AbstractAction("Do a passive scan") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                val auditConfiguration =
                    AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_PASSIVE_AUDIT_CHECKS)
                val scan = montoya.scanner().startAudit(auditConfiguration)
                scan.addRequestResponse(
                    HttpRequestResponse.httpRequestResponse(
                        entry.request,
                        entry.response
                    )
                )
            }
        })
        passiveScan.isEnabled = isPro
        this.add(passiveScan)

        this.add(Separator())

        val sendToRepeater = JMenuItem(object : AbstractAction("Send to Repeater") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                montoya.repeater().sendToRepeater(entry.request)
            }
        })
        this.add(sendToRepeater)

        val sendToIntruder = JMenuItem(object : AbstractAction("Send to Intruder") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                montoya.intruder().sendToIntruder(entry.request)
            }
        })
        this.add(sendToIntruder)

        val sendToComparer = JMenu("Send to Comparer")
        val comparerRequest = JMenuItem(object : AbstractAction("Request") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                montoya.comparer().sendToComparer(entry.request.toByteArray())
            }
        })
        sendToComparer.add(comparerRequest)
        val comparerResponse = JMenuItem(object : AbstractAction("Response") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                montoya.comparer().sendToComparer(entry.response!!.toByteArray())
            }
        })
        comparerResponse.isEnabled = entry.response != null
        sendToComparer.add(comparerResponse)
        this.add(sendToComparer)

        this.add(Separator())

        val removeItem = JMenuItem(object : AbstractAction("Remove Item") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                logRepository.remove(entry)
            }
        })
        this.add(removeItem)
    }
}

class MultipleLogEntryMenu(
    val montoya: MontoyaApi,
    val logRepository: LogRepository,
    val libraryController: FilterLibrary,
    val tableFilterService: TableFilterService,
    val colorService: TableColorService,
    val exporters: Set<LogExporter>,
    val logTable: LogTable,
    selectedEntries: List<LogEntry>
) : JPopupMenu() {

    init {
        val isPro =
            montoya.burpSuite().version().edition() == BurpSuiteEdition.PROFESSIONAL

        this.add(JMenuItem(selectedEntries.size.toString() + " items"))
        this.add(Separator())

        val copySelectedDomains: JMenuItem = JMenu("Copy selected hostnames")
        copySelectedDomains.add(JMenuItem(object : AbstractAction("All") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.DOMAIN, false)
            }
        }))
        copySelectedDomains.add(JMenuItem(object : AbstractAction("Unique") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.DOMAIN, true)
            }
        }))
        this.add(copySelectedDomains)

        val copySelectedPaths: JMenuItem = JMenu("Copy selected paths")
        copySelectedPaths.add(JMenuItem(object : AbstractAction("All") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.PATH, false)
            }
        }))
        copySelectedPaths.add(JMenuItem(object : AbstractAction("Unique") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.PATH, true)
            }
        }))
        this.add(copySelectedPaths)

        val copySelectedUrls: JMenuItem = JMenu("Copy selected URLs")
        copySelectedUrls.add(JMenuItem(object : AbstractAction("All") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.URL, false)
            }
        }))
        copySelectedUrls.add(JMenuItem(object : AbstractAction("Unique") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                copySelected(selectedEntries, Scope.URL, true)
            }
        }))
        this.add(copySelectedUrls)

        val exportMenu = JMenu("Export entries as...")
        for (exporter in exporters) {
            if (exporter is ContextMenuExportProvider) {
                val item = (exporter as ContextMenuExportProvider).getExportEntriesMenuItem(
                    selectedEntries
                )
                if (item != null) {
                    exportMenu.add(item)
                }
            }
        }

        if (exportMenu.itemCount > 0) {
            this.add(Separator())
            this.add(exportMenu)
        }

        this.add(Separator())

        val scanner = JMenuItem(
            object : AbstractAction("Crawl selected " + selectedEntries.size + " urls") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    val urls =
                        selectedEntries.stream().map { obj: LogEntry? -> obj!!.getUrl() }
                    val config = CrawlConfiguration.crawlConfiguration(
                        *urls.toArray { size: Int -> arrayOfNulls(size) })
                    val crawl = montoya.scanner().startCrawl(config)
                }
            })
        this.add(scanner)

        val activeScan = JMenuItem(
            object : AbstractAction("Active scan selected " + selectedEntries.size + " urls") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    val auditConfiguration = AuditConfiguration.auditConfiguration(
                        BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS
                    )
                    val scan = montoya.scanner().startAudit(auditConfiguration)
                    for (selectedEntry in selectedEntries) {
                        scan.addRequestResponse(
                            HttpRequestResponse.httpRequestResponse(
                                selectedEntry.request,
                                selectedEntry.response
                            )
                        )
                    }
                }
            })
        this.add(activeScan)
        activeScan.setEnabled(isPro)

        val passiveScan = JMenuItem(
            object : AbstractAction("Passive scan selected " + selectedEntries.size + " urls") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    val auditConfiguration = AuditConfiguration.auditConfiguration(
                        BuiltInAuditConfiguration.LEGACY_PASSIVE_AUDIT_CHECKS
                    )
                    val scan = montoya.scanner().startAudit(auditConfiguration)
                    for (selectedEntry in selectedEntries) {
                        scan.addRequestResponse(
                            HttpRequestResponse.httpRequestResponse(
                                selectedEntry.request,
                                selectedEntry.response
                            )
                        )
                    }
                }
            })
        passiveScan.setEnabled(isPro)
        this.add(passiveScan)

        this.add(Separator())

        val sendToRepeater = JMenuItem(
            object :
                AbstractAction("Send " + selectedEntries.size + " selected items to Repeater") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    for (entry in selectedEntries) {
                        montoya.repeater().sendToRepeater(entry.request)
                    }
                }
            })
        this.add(sendToRepeater)

        val sendToIntruder = JMenuItem(
            object :
                AbstractAction("Send " + selectedEntries.size + " selected items to Intruder") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    for (entry in selectedEntries) {
                        montoya.intruder().sendToIntruder(entry.request)
                    }
                }
            })
        this.add(sendToIntruder)

        val sendToComparer = JMenu(
            "Send " + selectedEntries.size + " selected items to Comparer"
        )
        val comparerRequest = JMenuItem(object : AbstractAction("Requests") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                for (entry in selectedEntries) {
                    montoya.comparer().sendToComparer(entry.request.toByteArray())
                }
            }
        })
        sendToComparer.add(comparerRequest)
        val comparerResponse = JMenuItem(object : AbstractAction("Responses") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                for (entry in selectedEntries) {
                    if (entry.response != null) { //Do not add entries without a response
                        montoya.comparer().sendToComparer(entry.response!!.toByteArray())
                    }
                }
            }
        })
        sendToComparer.add(comparerResponse)
        this.add(sendToComparer)

        this.add(Separator())

        val removeItem = JMenuItem(
            object : AbstractAction("Remove " + selectedEntries.size + " selected items") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    //If we don't clear the selection, the table will select the next entry after row is deleted
                    //This causes the request response viewer to change after each and slow the process.
                    logTable.getSelectionModel().clearSelection()
                    logRepository.removeAll(selectedEntries)
                }
            })
        this.add(removeItem)
    }

    private fun copySelected(items: List<LogEntry>, scope: Scope, onlyUnique: Boolean) {
        val clipboard = toolkit.systemClipboard
        val values: MutableCollection<String?>?
        if (onlyUnique) {
            values = LinkedHashSet<String?>()
        } else {
            values = LinkedList<String?>()
        }
        for (item in items) {
            when (scope) {
                Scope.URL -> values.add(item.getUrl())
                Scope.PATH -> values.add(item.getPath())
                Scope.DOMAIN -> values.add(item.getHostname())
            }
        }

        val result = values.stream().collect(Collectors.joining("\n"))
        clipboard.setContents(StringSelection(result), null)
    }

    internal enum class Scope {
        URL, DOMAIN, PATH
    }
}

