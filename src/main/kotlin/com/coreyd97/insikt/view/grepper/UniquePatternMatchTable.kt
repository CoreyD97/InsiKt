package com.coreyd97.insikt.view.grepper

import com.coreyd97.insikt.grepper.GrepResults
import com.coreyd97.insikt.grepper.GrepperListener
import com.coreyd97.insikt.grepper.GrepperService
import java.util.regex.Pattern
import javax.swing.JTable
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

class UniquePatternMatchTable(private val controller: GrepperService) : JTable(),
    GrepperListener {
    private val entryKeys = mutableListOf<String>()
    private val valueCountMap = mutableMapOf<String, UniqueMatch>()

    init {
        this.setModel(UniqueValueTableModel())
        this.setAutoCreateRowSorter(true)
        this.setColumnSelectionAllowed(true)

        this.controller.addListener(this)
    }

    fun reset() {
        synchronized(this.valueCountMap) {
            this.valueCountMap.clear()
        }
        synchronized(this.entryKeys) {
            this.entryKeys.clear()
        }
        SwingUtilities.invokeLater(
            Runnable { (this.model as UniqueValueTableModel).fireTableDataChanged() })
    }

    fun addEntry(entry: GrepResults) {
        synchronized(valueCountMap) {
            synchronized(entryKeys) {
                for (result in entry.matches) {
                    val key = result.groups[0]
                    val index = entryKeys.indexOf(key)
                    if (index == -1) {
                        entryKeys.add(key)
                        valueCountMap.put(key, UniqueMatch(result.groups))
                    } else {
                        valueCountMap.get(key)?.increment()
                    }
                }
            }
        }
    }

    override fun onSearchStarted(pattern: Pattern, searchEntries: Int) {
        reset()
        (this@UniquePatternMatchTable.model as UniqueValueTableModel).groups = pattern.matcher("").groupCount()
        (this.model as AbstractTableModel).fireTableStructureChanged()
    }

    override fun onEntryProcessed(entryResults: GrepResults) {
        addEntry(entryResults)
    }

    override fun onSearchComplete() {
        (this.model as AbstractTableModel).fireTableDataChanged()
    }

    override fun onResetRequested() {
        reset()
    }

    override fun onShutdownInitiated() {
    }

    override fun onShutdownComplete() {
        (this.model as AbstractTableModel).fireTableDataChanged()
    }

    internal class UniqueMatch(match: List<String>) {
        var key: String = match[0]
        var groups: List<String> = match
        var count: Int = 1

        fun increment() {
            this.count++
        }
    }

    inner class UniqueValueTableModel : AbstractTableModel() {
        var groups: Int = 0

        override fun getColumnName(column: Int): String {
            if (column == 0) {
                return "Value"
            }
            if (column == groups + 1) {
                return "Count"
            }
            return "Group " + column
        }

        override fun getRowCount(): Int {
            return valueCountMap.size
        }

        override fun getColumnCount(): Int {
            return groups + 2
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            if (columnIndex == groups + 1) {
                return Int::class.java
            }
            return String::class.java
        }

        override fun getValueAt(row: Int, col: Int): Any {
            val key = entryKeys.get(row)
            if (col == 0) {
                return key
            }
            val match: UniqueMatch = valueCountMap.get(key)!!
            if (col == groups + 1) {
                return match.count
            }
            return match.groups[col]
        }
    }
}