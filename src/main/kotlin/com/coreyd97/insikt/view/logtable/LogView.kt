package com.coreyd97.insikt.view.logtable

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.filter.*
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.logview.repository.LogRepository
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.PREF_AUTO_SCROLL
import com.coreyd97.insikt.util.PREF_FILTER_HISTORY
import com.coreyd97.insikt.util.PREF_LAYOUT
import com.coreyd97.insikt.util.PREF_STICK_TO_TOP
import com.coreyd97.insikt.view.colorizingdialog.ColorizingRuleDialog
import com.coreyd97.insikt.view.colorizingdialog.ColorizingRuleDialogFactory
import com.coreyd97.insikt.view.shared.RequestViewer
import com.coreyd97.montoyautilities.*
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener


@Singleton
class LogView @Inject constructor(
    val montoya: MontoyaApi,
    val logRepository: LogRepository,
    val logTable: LogTable,
    val filterLibrary: FilterLibrary,
    val filterService: TableFilterService,
    val colorService: TableColorService,
    val tagService: TagService,
    @param:Named("LogViewSelected") val requestViewer: RequestViewer,
    val colorizingRuleDialogFactory: ColorizingRuleDialogFactory
): JPanel(BorderLayout()), TableFilterListener {

    var tagDialog: ColorizingRuleDialog? = null
    var colorDialog: ColorizingRuleDialog? = null

    private var stickToBottom by Preference(PREF_AUTO_SCROLL, true)
    private var stickToTop by Preference(PREF_STICK_TO_TOP, false)

    private val filterField = HistoryField(
        PREF_FILTER_HISTORY, 25,
        validateInput = { str -> validateFilter(str) },
        onChange = { filter -> filterChanged(filter) })

    private val tableScrollPane = JScrollPane(
        logTable,
        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
    )

    val tableAndViewerSplit = VariableViewPanel(
        PREF_LAYOUT,
        tableScrollPane, "Log Table",
        requestViewer, "Request/Response", VariableViewPanel.View.VERTICAL
    )

    init {

        tableScrollPane.addMouseWheelListener {
            val scrollBar = tableScrollPane.verticalScrollBar
            stickToBottom = scrollBar.value + scrollBar.height >= scrollBar.maximum
            stickToTop = scrollBar.value <= scrollBar.minimum + (logTable.rowHeight * 3)
        }
        tableScrollPane.verticalScrollBar.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(mouseEvent: MouseEvent) {
                val scrollBar = tableScrollPane.verticalScrollBar
                stickToBottom = scrollBar.value + scrollBar.height >= scrollBar.maximum
            }
        })

        // Keep the viewport steady when rows are inserted at the top
        logTable.model.addTableModelListener(object : TableModelListener {
            var delta: AtomicInteger = AtomicInteger(0)
            override fun tableChanged(e: TableModelEvent) {
                val vBar = tableScrollPane.verticalScrollBar
                val sortedByNumber = logTable.sortColumn == logTable.convertColumnIndexToView(logTable.columnModel.getColumnIndex(LogEntryField.NUMBER))
                if (stickToTop || stickToBottom || !sortedByNumber) return // If user is at the very top, let them see new rows

                synchronized(delta) {
                    when (e.type) {
                        TableModelEvent.INSERT -> {
                            delta.set(delta.get() + (e.lastRow - e.firstRow + 1).coerceAtLeast(0))
                        }

                        TableModelEvent.DELETE -> {
//                            delta.set(delta.get() - (e.lastRow - e.firstRow + 1).coerceAtLeast(0))
                        }

                        else -> return
                    }
                }

                // Apply after layout so row heights are up-to-date
                SwingUtilities.invokeLater {
                    val deltaY = logTable.rowHeight * delta.getAndSet(0)
                    vBar.value = (vBar.value + deltaY).coerceAtMost(vBar.maximum)
                }
            }
        })

        val newPanel = panelBuilder(childAlignment = Alignment.FILL) {
            row {
                label("Filter: ")
                component(filterField, weightX = 1)
                button("Tags", onClick = { displayTagDialog() })
                button("Colorize", onClick = { displayColorDialog() })
                button("Clear Logs", onClick = { onClearButton() })
                button("Settings", onClick = { montoya.userInterface().openSettingsWindow() })
            }
            gbc.fill = GridBagConstraints.BOTH
            component(tableAndViewerSplit, weightY = 1, weightX = 1)
        }
        add(newPanel, BorderLayout.CENTER)

        filterService.addListener(this)
        montoya.extension().registerUnloadingHandler { unload() }
    }

    private fun validateFilter(filter: String): String? {
        if(filter.isEmpty()) return filter
        val parseResult = FilterExpression.fromString(filter)
        if(!parseResult.valid())
            ToastNotification.show(filterField, "Invalid filter: ${parseResult.errors.joinToString(", ")}")

        return parseResult.expression?.toString()
    }

    private fun filterChanged(filterString: String){
        if(filterString.isEmpty()) {
            filterService.clearFilter()
        }else{
            filterService.setFilter(FilterRule.fromString(filterString))
        }
    }

    override fun onFilterApplied(filter: FilterRule) {
        filterField.setValueSilently(filter.expression.toString())
    }

    override fun onFilterCleared(previousFilter: FilterRule?) {
        filterField.setValueSilently(null)
    }

    override fun onFilterPaused() {
        //Do nothing for now. TODO Maybe disable filter field or show disabled in some way?
    }

    override fun onFilterResumed(filter: FilterRule) {
        //Do nothing. TODO See onFilterPaused
    }

    fun displayColorDialog() {
        displayColorDialogWithAdditions()
    }

    fun displayColorDialogWithAdditions(additions: List<ColorizingRule> = emptyList()) {
        if(colorDialog != null && colorDialog!!.isVisible) {
            colorDialog!!.requestFocusInWindow()
            return
        }
        colorDialog = colorizingRuleDialogFactory.showDialog(
            "Highlighting Rules",
            "${APP_NAME} - Color Rules",
            colorService.entries.plus(additions),
            { colorRules ->
                //Update rules
                colorService.updateColorFilters(colorRules)
                colorDialog = null
            }
        )
        colorDialog!!.isVisible = true
    }

    fun displayTagDialog() {
        displayTagDialogWithAdditions()
    }

    fun displayTagDialogWithAdditions(additions: List<ColorizingRule> = emptyList()) {
        if(tagDialog != null && tagDialog!!.isVisible) {
            tagDialog!!.requestFocusInWindow()
            return
        }
        tagDialog = colorizingRuleDialogFactory.showDialog(
            "Tags",
            "${APP_NAME} - Tag Rules",
            tagService.entries.plus(additions),
            { tags ->
                //Update tags
                tagService.updateTags(tags)
                tagDialog = null
            }
        )
        tagDialog!!.isVisible = true
    }

    private fun onClearButton() {
        val res = JOptionPane.showConfirmDialog(
            logTable,
            "Are you sure you want to clear the log table?",
            "Clear Logs",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        if (res == JOptionPane.YES_OPTION) {
            // Clear the repository; table updates via repository listener
            logRepository.clear()
        }
    }

    fun setPanelLayout(view: VariableViewPanel.View) {
        tableAndViewerSplit.view = view
    }

    //TODO Remove this and inject RequestViewer in dependents
    fun setEntryViewerLayout(view: VariableViewPanel.View) {
        requestViewer.variableViewPanel.view = view
    }

    fun unload(){
        if(requestViewer.isPoppedOut){
            requestViewer.popoutFrame.dispose()
        }
        filterService.removeListener(this)
    }
}