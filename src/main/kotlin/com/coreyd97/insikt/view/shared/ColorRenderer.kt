package com.coreyd97.insikt.view.shared

import java.awt.Color
import java.awt.Component
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.border.Border
import javax.swing.table.TableCellRenderer

/*
* Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
* https://docs.oracle.com/javase/tutorial/uiswing/examples/components/TableDialogEditDemoProject/src/components/ColorRenderer.java
*/

class ColorRenderer(isBordered: Boolean) : JLabel(), TableCellRenderer {
    var unselectedBorder: Border? = null
    var selectedBorder: Border? = null
    var isBordered: Boolean = true

    init {
        this.isBordered = isBordered
        setOpaque(true)
    }

    override fun getTableCellRendererComponent(
        table: JTable, color: Any?,
        isSelected: Boolean, hasFocus: Boolean,
        row: Int, column: Int
    ): Component {
        val newColor = color as Color?
        setBackground(newColor)
        if (isBordered) {
            if (isSelected) {
                if (selectedBorder == null) {
                    selectedBorder = BorderFactory.createMatteBorder(
                        2, 5, 2, 5,
                        table.selectionBackground
                    )
                }
                setBorder(selectedBorder)
            } else {
                if (unselectedBorder == null) {
                    unselectedBorder = BorderFactory.createMatteBorder(
                        2, 5, 2, 5,
                        table.getBackground()
                    )
                }
                setBorder(unselectedBorder)
            }
        }
        if (newColor != null) {
            setToolTipText(
                ("RGB value: " + newColor.red + ", "
                        + newColor.green + ", "
                        + newColor.blue)
            )
        }
        return this
    }
}