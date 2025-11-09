package com.coreyd97.insikt.view.logtable

import burp.api.montoya.MontoyaApi
import co.elastic.clients.util.DateTime
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.TableFilterListener
import com.coreyd97.insikt.filter.TableFilterService
import com.coreyd97.insikt.logging.LogEntryMenuFactory
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.PREF_AUTO_SCROLL
import com.coreyd97.insikt.util.PREF_SORT_COLUMN
import com.coreyd97.insikt.util.PREF_SORT_ORDER
import com.coreyd97.insikt.view.shared.BooleanRenderer
import com.coreyd97.insikt.view.shared.RequestViewer
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.StorageType
import com.google.inject.name.Named
import org.apache.logging.log4j.LogManager
import java.awt.Component
import java.awt.event.MouseEvent
import java.time.Instant
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.RowSorterEvent
import javax.swing.event.TableModelEvent
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

class LogTable(
    val montoya: MontoyaApi,
    val filterLibrary: FilterLibrary,
    val filterService: TableFilterService,
    @param:Named("LogViewSelected") val requestViewer: RequestViewer,
    var logEntryMenuFactory: LogEntryMenuFactory,
    val dataModel: LogTableModel,
    val columnModel: LogTableColumnModel
): JTable(dataModel, columnModel), TableFilterListener {

    val logger = LogManager.getLogger(this.javaClass)

    val sorter: TableRowSorter<LogTableModel> = TableRowSorter(this.dataModel)
    var sortOrder by Preference(PREF_SORT_ORDER, SortOrder.UNSORTED)
    var sortColumn by Preference(PREF_SORT_COLUMN, -1)
    var isFiltering by Preference("tableIsFiltering", false, storage = StorageType.TEMP)

    var currentFilter: FilterRule? = null
        private set

    val autoScroll by Preference(PREF_AUTO_SCROLL, true)

    init {
        this.tableHeader = TableHeader(this)
        filterService.addListener(this)

        this.setAutoResizeMode(AUTO_RESIZE_OFF) // to have horizontal scroll bar
        this.setRowHeight(20) // As we are not using Burp customised UI, we have to define the row height to make it more pretty
        this.setDefaultRenderer(Boolean::class.java, BooleanRenderer()) //Fix grey checkbox background
        (this.getDefaultRenderer(Boolean::class.java) as JComponent).isOpaque =
            true  // remove the white background of the checkboxes!


        sorter.let {
            it.maxSortKeys = 1
            it.sortsOnUpdates = true

            runCatching {
                it.sortKeys = mutableListOf(RowSorter.SortKey(sortColumn, sortOrder))
            }

            it.addRowSorterListener { rowSorterEvent: RowSorterEvent ->
                if (rowSorterEvent.type == RowSorterEvent.Type.SORT_ORDER_CHANGED) {
                    val sortKeys = it.sortKeys
                    if (sortKeys.isEmpty()) {
                        sortOrder = SortOrder.UNSORTED
                        sortColumn = -1
                    } else {
                        val sortKey: RowSorter.SortKey = sortKeys.first()
                        sortOrder = sortKey.sortOrder
                        sortColumn = sortKey.column
                    }
                }
            }
        }
        this.rowSorter = sorter


        this.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
            if (e.valueIsAdjusting) {
                return@addListSelectionListener
            }
            val selectedRow = this.selectedRow
            if (selectedRow == -1) {
                requestViewer.setDisplayedEntity(null)
            } else {
                // Always go via the model API, never touch internal lists
                val logEntry = dataModel.getRow(convertRowIndexToModel(selectedRow))
                requestViewer.setDisplayedEntity(logEntry)
            }
        }

        registerListeners()
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        return getPreferredSize().width < parent.width
    }

    //Sneak in row coloring just before rendering the cell.
    override fun prepareRenderer(renderer: TableCellRenderer?, row: Int, column: Int): Component {
        var entry: LogEntry? = null
        val modelRow: Int?
        try {
            modelRow = convertRowIndexToModel(row)
            entry = dataModel.getRow(modelRow)
        } catch (ignored: NullPointerException) {
            ignored.printStackTrace()
        }

        val c = super.prepareRenderer(renderer, row, column)

        val selectedRows = IntStream.of(*this.selectedRows)

        if (selectedRows.anyMatch { i: Int -> i == row }) {
            c.setBackground(this.getSelectionBackground())
            c.setForeground(this.getSelectionForeground())
        } else {
            if (entry == null) {
                throw RuntimeException("Could not find entry for row.")
            }
            val rule = entry.matchingColorFilters.filter {it.enabled}.minByOrNull { it.priority }
            if (rule != null) {
                c.setForeground(rule.foregroundColor)
                c.setBackground(rule.backgroundColor)
            } else {
                c.setForeground(this.getForeground())
                c.setBackground(this.getBackground())
            }
        }
        return c
    }

    private fun registerListeners() {
        this.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                onMouseEvent(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                onMouseEvent(e)
            }

            override fun mousePressed(e: MouseEvent) {
                onMouseEvent(e)
            }

            fun onMouseEvent(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    val p = e.getPoint()
                    val rowAtPoint = rowAtPoint(p)
                    if (rowAtPoint == -1) {
                        return
                    }

                    if (IntStream.of(*selectedRows)
                            .noneMatch { i: Int -> i == rowAtPoint }
                    ) {
                        //We right clicked an unselected row. Set it as the selected row and update our selected
                        setRowSelectionInterval(rowAtPoint, rowAtPoint)
                    }

                    if (selectedRowCount == 1) {
                        val logEntry = dataModel.getRow(convertRowIndexToModel(rowAtPoint))
                        val logField = (columnModel
                            .getColumn(columnAtPoint(p)) as LogTableColumn).identifier

                        if (e.isPopupTrigger && e.component is JTable) {
                            logEntryMenuFactory.singleEntryMenu(logEntry, logField, this@LogTable)
                                .show(e.component, e.x, e.y)
                        }
                    } else {
                        val selectedEntries = IntStream.of(*selectedRows)
                            .mapToObj { selectedRow: Int ->
                                dataModel.getRow(convertRowIndexToModel(selectedRow))
                            }
                            .collect(Collectors.toList())

                        if (e.isPopupTrigger && e.component is JTable) {
                            logEntryMenuFactory.multipleEntryMenu(selectedEntries, this@LogTable)
                                .show(e.component, e.x, e.y)
                        }
                    }
                }
            }
        })

        // Prefer lastRow for autoscroll on INSERT; guard visibility
        dataModel.addTableModelListener { tableModelEvent: TableModelEvent? ->
            if (tableModelEvent == null) return@addTableModelListener
            if (tableModelEvent.type == TableModelEvent.INSERT && autoScroll) {
                if (!isVisible) return@addTableModelListener
                val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) as? JScrollPane
                    ?: return@addTableModelListener
                val vBar = scrollPane.verticalScrollBar
                val atBottom = vBar.value + vBar.visibleAmount >= vBar.maximum - 2

                if (!atBottom) return@addTableModelListener

                SwingUtilities.invokeLater {
                    scrollRectToVisible(getCellRect(rowCount-1, 0 , true))
                }
            }
        }
    }

    var filteringWorker: SwingWorker<Unit, Pair<Int, LogEntry>>? = null
    private fun asyncRowFilter(filter: FilterRule) {
        if(filteringWorker != null) {
            filteringWorker!!.cancel(true)
        }
        isFiltering = true
        filteringWorker = dataModel.buildFilterList(filter, {
            sorter.rowFilter = object : RowFilter<LogTableModel, Int>() {
                override fun include(entry: Entry<out LogTableModel, out Int>): Boolean {
                    val logEntry = entry.model.getRow(entry.identifier)
                    return dataModel.entriesMatchingFilter.contains(logEntry)
                }
            }
            isFiltering = false
            filteringWorker = null
        })
        filteringWorker!!.execute()
    }

    override fun onFilterApplied(filter: FilterRule) {
        asyncRowFilter(filter)

        if (selectedRow != -1) {
            scrollRectToVisible(getCellRect(selectedRow, 0, true))
        }
    }

    override fun onFilterCleared(previousFilter: FilterRule?) {
        sorter.rowFilter = null
        dataModel.entriesMatchingFilter.clear()

        if (selectedRow != -1) {
            scrollRectToVisible(getCellRect(selectedRow, 0, true))
        }
    }

    override fun onFilterPaused() {
        sorter.rowFilter = null
    }

    override fun onFilterResumed(filter: FilterRule) {
        onFilterApplied(filter)
    }
}
