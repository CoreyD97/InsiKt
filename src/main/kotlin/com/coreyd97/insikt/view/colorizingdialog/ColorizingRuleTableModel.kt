package com.coreyd97.insikt.view.colorizingdialog

import com.coreyd97.insikt.filter.ColorizingRule
import com.coreyd97.insikt.filter.FilterExpression
import com.coreyd97.insikt.util.ensureEdt
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.awt.Color
import javax.swing.table.AbstractTableModel

/**
 * Created by corey on 19/07/17.
 */
class ColorizingRuleTableModel internal constructor(val original: List<ColorizingRule>) : AbstractTableModel() {

    interface Listener {
        fun onAdded(rule: ColorizingRule)
        fun onRemoved(rule: ColorizingRule)
        fun onUpdated(rule: ColorizingRule)
        fun onReset()
    }

    private val columnNames = arrayOf<String?>(
        "Name", "Filter", "Foreground", "Background",
        "Enabled"
    )

    val data: MutableList<ColorizingRule>
    val dataCopy: List<ColorizingRule>
        get() {
            val serialized = Json.encodeToString(serializer(), data)
            val out = Json.decodeFromString<List<ColorizingRule>>(serializer(), serialized)
            return out
        }

    init {
        val serialized = Json.encodeToString(serializer(), original)
        data = Json.decodeFromString(serializer(), serialized)
        //Sort existing filters by their priority before adding to table.
        data.sortBy { it.priority }
        data.forEach { it.shouldRetest = false }
    }

    override fun getRowCount(): Int {
        return data.size
    }

    override fun getColumnCount(): Int {
        return columnNames.size
    }

    override fun getColumnName(i: Int): String? {
        return columnNames[i]
    }

    override fun getValueAt(row: Int, col: Int): Any? {
        val tag = data[row]
        return when (col) {
            0 -> (tag.name ?: "")
            1 -> tag
            2 -> (tag.foregroundColor ?: Color.BLACK)
            3 -> (tag.backgroundColor ?: Color.WHITE)
            4 -> tag.enabled
            else -> false
        }
    }

    fun getTagAtRow(row: Int): ColorizingRule {
        return data[row]
    }

    override fun setValueAt(value: Any?, row: Int, col: Int) {
        val tag = data[row]
        when (col) {
            0 -> tag.name = value as String?
            1 -> {
                when(value) {
                    is FilterExpression -> tag.expression = value
                    //TODO Handle fallback
                }
            }
            2 -> tag.foregroundColor = value as Color?
            3 -> tag.backgroundColor = value as Color?
            4 -> tag.enabled = (value as Boolean?)!!
            else -> return
        }
    }


    override fun getColumnClass(columnIndex: Int): Class<*> {
        return when (columnIndex) {
            2, 3 -> Color::class.java
            4 -> java.lang.Boolean::class.java
            else -> String::class.java
        }
    }

    override fun isCellEditable(row: Int, col: Int): Boolean {
        return true
    }

    fun addBlankRule(){
        val rule = ColorizingRule.fromString("", "Request.hostname == \"example.com\"")
        rule.shouldRetest = true
        rule.enabled = true
        add(rule)
    }

    fun add(tag: ColorizingRule) {
        val i = data.size
        data.add(tag)
        tag.priority = i
        this.fireTableRowsInserted(i, i)
    }

    fun remove(row: Int) {
        synchronized(data) {
            data.removeAt(row)
            fixPriorities()
        }
        ensureEdt {
            fireTableRowsDeleted(row, row)
        }
    }

    fun remove(tag: ColorizingRule) {
        var index: Int
        synchronized(data) {
            index = data.indexOf(tag)
            data.removeAt(index)
            fixPriorities()
        }
        ensureEdt {
            fireTableRowsDeleted(index, index)
        }
    }

    fun fixPriorities(){
        data.forEachIndexed { i, tag -> tag.priority = i }
    }

    fun resetRetestFlags(){
        data.forEach { it.shouldRetest = false }
    }

    fun switchRows(from: Int, to: Int) {
        synchronized(data) {
            val target = data.removeAt(from)
            data.add(to, target)
            data.forEachIndexed { i, tag -> tag.priority = i }
            fixPriorities()
        }
        ensureEdt {
            this.fireTableRowsUpdated(from, from)
            this.fireTableRowsUpdated(to, to)
        }
    }

    fun removeAll() {
        synchronized(data) {
            data.clear()
        }
        ensureEdt {
            this.fireTableDataChanged()
        }
    }
}
