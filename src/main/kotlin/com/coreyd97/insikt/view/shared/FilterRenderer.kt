package com.coreyd97.insikt.view.shared

import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.ParserService
import com.coreyd97.montoyautilities.ToastNotification
import com.google.inject.Inject
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultCellEditor
import javax.swing.JTable
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer

/**
 * Created by corey on 22/08/17.
 */
class FilterRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val c = super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row,
            column
        )
        val validFilter: Boolean = value is FilterRule && value.isValid

        if (validFilter) {
            c.setBackground(Color(76, 255, 155))
            c.setForeground(Color.BLACK)
        } else {
            c.setBackground(Color(221, 70, 57))
            c.setForeground(Color.WHITE)
        }

        return c
    }
}


class FilterEditor @Inject constructor(
    private val parserService: ParserService
) : DefaultCellEditor(JTextField()) {

    init {
        // Commit on focus lost like defaults; select-all for convenience
        (component as JTextField).also { field ->
            field.selectAll()
        }
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        val field = component as JTextField
        field.text = value?.toString() ?: ""
        SwingUtilities.invokeLater { field.selectAll() }
        return super.getTableCellEditorComponent(table, value, isSelected, row, column)
    }

    // Let the table stop editing only if the text parses
    override fun stopCellEditing(): Boolean {
        val text = (component as JTextField).text
        val parseResult = parserService.parse(text)
        return if (parseResult.errors.isNotEmpty()) {
            ToastNotification.show(component, parseResult.errors.joinToString("\n\n"))
            false
        } else {
            super.stopCellEditing()
        }
    }

    // Return parsed object when valid; otherwise raw text (table won't commit if invalid)
    override fun getCellEditorValue(): Any {
        val text = (component as JTextField).text
        val parseResult = parserService.parse(text)
        return if (parseResult.errors.isEmpty()) parseResult.expression else text
    }
}