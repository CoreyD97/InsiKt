package com.coreyd97.insikt.view.logtable

import com.coreyd97.insikt.logging.logentry.FieldGroup
import com.coreyd97.insikt.util.MoreHelp
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class TableHeaderMenu(
    private val logTable: LogTable,
    private val columnObj: LogTableColumn
) :
    JPopupMenu() {

    fun showMenu(e: MouseEvent) {
        val menu = JPopupMenu("Popup")
        var item = JMenuItem(
            columnObj.visibleName + " (" + columnObj.identifier.fullLabel + ")"
        )

        item.isEnabled = false
        menu.add(item)
        menu.addSeparator()

        item = JMenuItem("Rename")
        item.addActionListener {
            var newValue = MoreHelp.showPlainInputMessage(
                "Rename the \"" + columnObj.defaultVisibleName +
                        "\" column", "Rename column name", columnObj.visibleName
            )
            if (newValue.isNullOrEmpty()) {
                newValue = columnObj.defaultVisibleName
            }
            // Save it only if it is different! no need to refresh the columns
            if (newValue != columnObj.visibleName) {
                columnObj.visibleName = newValue
            }
        }
        menu.add(item)

        item = JMenuItem("Hide")
        item.addActionListener { logTable.columnModel.toggleHidden(columnObj) }
        menu.add(item)

        val subMenuVisibleCols = JMenu("Visible columns")
        item = JMenuItem("Make all visible")
        item.addActionListener {
            for (column in logTable.columnModel.getAllColumns()) {
                logTable.columnModel.showColumn(column)
            }
        }
        subMenuVisibleCols.add(item)

        val groupMenus: MutableMap<FieldGroup, JMenu> = HashMap()

        for (logTableColumn in logTable.columnModel.getAllColumns()) {
            val group = logTableColumn.identifier.fieldGroup
            if (!groupMenus.containsKey(group)) {
                groupMenus[group] = JMenu(group.label)
            }
            val fieldGroupMenu = groupMenus[group]

            val visibleItem: JMenuItem = JCheckBoxMenuItem(logTableColumn.visibleName)
            visibleItem.isSelected = logTableColumn.visible
            visibleItem.addActionListener { e1: ActionEvent? ->
                logTable.columnModel.toggleHidden(
                    logTableColumn
                )
            }
            fieldGroupMenu!!.add(visibleItem)
        }

        val fieldGroups: List<FieldGroup> = ArrayList(groupMenus.keys).sortedWith { obj: FieldGroup, o: FieldGroup -> obj.compareTo(o) }

        for (fieldGroup in fieldGroups) {
            subMenuVisibleCols.add(groupMenus[fieldGroup])
        }

        menu.add(subMenuVisibleCols)

        menu.show(e.component, e.x, e.y)
    }
}


