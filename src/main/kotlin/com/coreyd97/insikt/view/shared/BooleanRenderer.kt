package com.coreyd97.insikt.view.shared

import java.awt.Component
import javax.swing.JCheckBox
import javax.swing.JTable
import javax.swing.UIManager
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.plaf.UIResource
import javax.swing.table.TableCellRenderer

/**
 * Created by corey on 07/09/17.
 */
class BooleanRenderer : JCheckBox(), TableCellRenderer, UIResource {
    init {
        this.setHorizontalAlignment(0)
        this.setBorderPainted(true)
        this.setOpaque(true)
    }

    override fun getTableCellRendererComponent(
        var1: JTable, var2: Any?, var3: Boolean,
        var4: Boolean, var5: Int, var6: Int
    ): Component {
        if (var3) {
            this.setForeground(var1.selectionForeground)
            super.setBackground(var1.selectionBackground)
        } else {
            this.setForeground(var1.getForeground())
            this.setBackground(var1.getBackground())
        }

        this.setSelected(var2 != null && (var2 as Boolean))
        if (var4) {
            this.setBorder(UIManager.getBorder("LogTable.focusCellHighlightBorder"))
        } else {
            this.setBorder(noFocusBorder)
        }

        return this
    }

    companion object {
        private val noFocusBorder: Border = EmptyBorder(1, 1, 1, 1)
    }
}
