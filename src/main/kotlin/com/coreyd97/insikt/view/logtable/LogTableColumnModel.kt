package com.coreyd97.insikt.view.logtable

import com.coreyd97.insikt.util.DEFAULT_LOG_TABLE_COLUMNS_JSON
import com.coreyd97.insikt.util.PREF_LOG_TABLE_SETTINGS
import com.coreyd97.insikt.util.PREF_TABLE_PILL_STYLE
import com.coreyd97.insikt.view.shared.TagRenderer
import com.coreyd97.montoyautilities.Preference
import com.google.inject.Singleton
import kotlinx.serialization.json.Json
import javax.swing.event.TableColumnModelEvent
import javax.swing.table.TableColumn

//The visible columns are stored in the underlying class.
@Singleton
class LogTableColumnModel : javax.swing.table.DefaultTableColumnModel() {
    private val allColumns = mutableListOf<LogTableColumn>()
    private val columnList: MutableList<LogTableColumn> by Preference(
        PREF_LOG_TABLE_SETTINGS,
        Json { ignoreUnknownKeys = true }.decodeFromString(DEFAULT_LOG_TABLE_COLUMNS_JSON)
    )

    init {
        // Sorting based on order number
        columnList.sort()
        this.allColumns.addAll(columnList)

        // Ensure modelIndex stays stable and refers to the model position
        for (i in allColumns.indices) {
            allColumns[i].modelIndex = i
        }

        allColumns.forEach { col ->
            if (col.visible) addColumn(col)
        }

        val tagColumn = allColumns.stream()
            .filter { logTableColumn: LogTableColumn -> logTableColumn.name == "Tags" }.findFirst()
        if (tagColumn.isPresent) {
            tagColumn.get().cellRenderer = TagRenderer()
        }

        initialize()
    }

    private fun initialize() {
    }

    override fun getColumnCount(): Int {
        return tableColumns.size
    }

    override fun addColumn(column: TableColumn) {
        //We should add the column at the correct position based on its order value.
        //Find the first element with a greater order than the one to be added and add it before it.
        var newPosition = -1
        for (i in tableColumns.indices) {
            val currentOrderAtIndex = (tableColumns[i] as LogTableColumn).order
            if (currentOrderAtIndex > (column as LogTableColumn).order) {
                newPosition = i
                break
            }
        }
        if (newPosition == -1) { //No elements with a greater order value. Add it to the end.
            newPosition = tableColumns.size
        }

        tableColumns.add(newPosition, column)

        // After insertion, recompute order and modelIndex for ALL visible columns
        for (i in 0 until tableColumns.size) {
            val c = tableColumns[i] as LogTableColumn
            c.order = i
            // Do NOT touch c.modelIndex here; it must remain the model column index
        }

        column.addPropertyChangeListener(this)
        // Fire event for the actual inserted position
        this.fireColumnAdded(TableColumnModelEvent(this, newPosition, newPosition))
    }


    override fun moveColumn(viewFrom: Int, viewTo: Int) {
        super.moveColumn(viewFrom, viewTo)
        if (viewFrom == viewTo) return

        // After a move, recompute order for ALL visible columns
        for (i in 0 until tableColumns.size) {
            val col = getColumn(i) as LogTableColumn
            col.order = i
            // Do NOT touch col.modelIndex
        }
    }

    override fun removeColumn(column: TableColumn) {
        val columnIndex = tableColumns.indexOf(column)

        if (columnIndex != -1) {
            // Adjust for the selection
            if (selectionModel != null) {
                selectionModel.removeIndexInterval(columnIndex, columnIndex)
            }

            column.removePropertyChangeListener(this)
            tableColumns.removeElementAt(columnIndex)

            // After removal, recompute order for ALL remaining visible columns
            for (i in 0 until tableColumns.size) {
                val c = tableColumns[i] as LogTableColumn
                c.order = i
                // Do NOT touch c.modelIndex
            }

            // Post columnRemoved event notification.  (JTable and JTableHeader
            // listens so they can adjust size and redraw)
            fireColumnRemoved(
                TableColumnModelEvent(
                    this,
                    columnIndex, columnIndex
                )
            )
        }
    }

    fun toggleHidden(logTableColumn: LogTableColumn) {
        logTableColumn.visible = !logTableColumn.visible
        if (logTableColumn.visible) { //&& logTableColumn.isEnabled()){
            //Add the column to the view
            addColumn(logTableColumn)
        } else {
            //Remove the column from the view and adjust others to fit.
            removeColumn(logTableColumn)
        }
    }

    fun showColumn(column: LogTableColumn) {
        if (!column.visible) {
            column.visible = true
            addColumn(column)
        }
    }

    fun hideColumn(column: LogTableColumn) {
        if (column.visible) {
            column.visible = false
            removeColumn(column)
        }
    }

    fun getAllColumns(): List<LogTableColumn> {
        return this.allColumns
    }
}
