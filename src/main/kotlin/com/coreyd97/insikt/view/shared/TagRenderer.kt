package com.coreyd97.insikt.view.shared

import com.coreyd97.insikt.filter.ColorizingRule
import org.jdesktop.swingx.HorizontalLayout
import java.awt.Component
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.TableCellRenderer


/**
 * Created by corey on 22/08/17.
 */
class TagRenderer : TableCellRenderer {
    override fun getTableCellRendererComponent(
        table: JTable?, value: Any?, isSelected: Boolean,
        hasFocus: Boolean, row: Int, column: Int
    ): Component {
        val tagWrapper = JPanel()
        tagWrapper.setLayout(HorizontalLayout(2))
        tagWrapper.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))

        if (value is Collection<*>) {
            for (o in value) {
                if(o !is ColorizingRule) return tagWrapper
                val c = JButton(o.name)
                c.putClientProperty("JButton.buttonType", "roundRect")
                c.setMargin(Insets(7, 4, 7, 4))
                c.setBackground(o.backgroundColor)
                c.setForeground(o.foregroundColor)
                tagWrapper.add(c)
            }
        }

        return tagWrapper
    }
}
