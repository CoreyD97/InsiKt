package com.coreyd97.insikt.view.library

import com.coreyd97.insikt.filter.FilterExpression
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.FilterLibraryListener
import com.coreyd97.insikt.filter.ParseResult
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

class FilterLibraryTableModel(
    val filterLibrary: FilterLibrary
) : AbstractTableModel(),
    FilterLibraryListener {
    private val columnNames = arrayOf("Alias", "Snippet", "")

    init {
        filterLibrary.addListener(this)
    }

    override fun getRowCount(): Int {
        return filterLibrary.snippets.size
    }

    override fun getColumnCount(): Int {
        return columnNames.size
    }

    override fun getValueAt(row: Int, column: Int): Any {
        val savedFilter = filterLibrary.snippets.elementAt(row)
        return when (column) {
            0 -> savedFilter.name ?: ""
            1 -> {
                savedFilter
            }
            else -> ""
        }
    }

    override fun getColumnName(column: Int): String {
        return columnNames[column]
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return true
    }

    override fun setValueAt(value: Any?, row: Int, column: Int) {
        val savedFilter = filterLibrary.snippets.elementAt(row)
        if (column == 0) {
            savedFilter.name = value as String
            if (!value.equals(savedFilter.name, ignoreCase = true)) {
                JOptionPane.showMessageDialog(
                    null, """
     Alias names may only contain alphanumeric characters and the symbols period (.) and underscore (_)
     Invalid characters have been replaced with an underscore.
     """.trimIndent(), "Alias Error", JOptionPane.WARNING_MESSAGE
                )
            }
        }
        if (column == 1) {
            when (value) {
                is ParseResult -> {
                    savedFilter.expression = value.expression
                }
                is FilterExpression -> {
                    savedFilter.expression = value
                }
            }

            filterLibrary.snippetUpdated(savedFilter)
            //Not a valid filter...
//            ToastNotification.show()
        }
    }

    override fun onFilterAdded(filterRule: FilterRule, index: Int) {
        SwingUtilities.invokeLater {
            this.fireTableRowsInserted(index, index)
        }
    }

    override fun onFilterModified(filterRule: FilterRule, index: Int) {
        SwingUtilities.invokeLater {
            this.fireTableRowsUpdated(index, index)
        }
    }

    override fun onFilterRemoved(filterRule: FilterRule, index: Int) {
        SwingUtilities.invokeLater {
            this.fireTableRowsDeleted(index, index)
        }
    }

    override fun activeFilterChanged(filterRule: FilterRule) {
        //Do nothing
    }
}
