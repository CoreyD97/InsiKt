package com.coreyd97.insikt.view.shared

import java.awt.Component
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.table.TableCellRenderer

/**
 * Created by corey on 22/08/17.
 */
class ButtonRenderer(onClick: (row: Int, col: Int)->Unit = {_, _ -> }) : TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val button = value as JButton
        button.setOpaque(false)
        button.setForeground(table.getForeground())
        button.setBackground(UIManager.getColor("Button.background"))
        return button
    }
}
