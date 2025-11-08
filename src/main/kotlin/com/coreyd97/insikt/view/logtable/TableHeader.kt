package com.coreyd97.insikt.view.logtable

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.table.JTableHeader

// This was used to create tool tips
class TableHeader(val logTable: LogTable) : JTableHeader(logTable.columnModel) {
    init {
        this.setTable(logTable)

        this.addMouseListener(object : MouseAdapter() {
            override fun mouseReleased(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    // get the coordinates of the mouse click
                    val p = e.getPoint()
                    val columnIndex = columnAtPoint(p)
                    val column = getColumnModel().getColumn(columnIndex) as LogTableColumn

                    val tblHeaderMenu = TableHeaderMenu(logTable, column)
                    tblHeaderMenu.showMenu(e)
                }
            }
        })
    }

    override fun getToolTipText(e: MouseEvent): String {
        // get the coordinates of the mouse click

        val p = e.getPoint()
        val columnID = this@TableHeader.getTable()
            .convertColumnIndexToModel(this@TableHeader.getTable().columnAtPoint(p))
        val column = this@TableHeader.getTable().columnModel
            .getColumn(columnID) as LogTableColumn

        var retStr: String
        try {
            retStr = column.description
        } catch (ex: NullPointerException) {
            retStr = ""
        } catch (ex: ArrayIndexOutOfBoundsException) {
            retStr = ""
        }
        if (retStr.length < 1) {
            retStr = super.getToolTipText(e)
        }
        return retStr
    }
}
