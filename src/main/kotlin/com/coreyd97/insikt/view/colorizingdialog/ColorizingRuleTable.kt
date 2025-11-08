package com.coreyd97.insikt.view.colorizingdialog

import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.view.shared.ColorEditor
import com.coreyd97.insikt.view.shared.ColorRenderer
import com.coreyd97.insikt.view.shared.FilterEditor
import com.coreyd97.insikt.view.shared.FilterRenderer
import java.awt.Dimension
import javax.swing.*

/**
 * Created by corey on 19/07/17.
 */
class ColorizingRuleTable internal constructor(
    model: ColorizingRuleTableModel,
    filterEditor: FilterEditor,
) :
    JTable(model) {
    init {
        this.autoResizeMode = AUTO_RESIZE_OFF
        this.fillsViewportHeight = true
        this.autoCreateRowSorter = false
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        this.rowHeight = 25
        (this.getDefaultRenderer(Boolean::class.java) as JComponent).setOpaque(
            true
        ) // to remove the white background of the checkboxes!
        (this.getDefaultRenderer(JButton::class.java) as JComponent).setOpaque(true)

        this.getColumnModel().getColumn(1).cellRenderer = FilterRenderer()
        this.getColumnModel().getColumn(1).cellEditor = filterEditor
        this.getColumnModel().getColumn(2).cellRenderer = ColorRenderer(true)
        this.getColumnModel().getColumn(2).cellEditor = ColorEditor()
        this.getColumnModel().getColumn(3).cellRenderer = ColorRenderer(true)
        this.getColumnModel().getColumn(3).cellEditor = ColorEditor()
        this.setDefaultRenderer(FilterRule::class.java, FilterRenderer())

        this.tableHeader.reorderingAllowed = false

        val minWidths = intArrayOf(100, 200, 75, 80, 60)
        val preferredWidths = intArrayOf(100, 300, 75, 80, 60)
        val maxWidths = intArrayOf(9999, 9999, 75, 80, 60)
        for (i in minWidths.indices) {
            this.getColumnModel().getColumn(i).minWidth = minWidths[i]
            this.getColumnModel().getColumn(i).preferredWidth = preferredWidths[i]
            this.getColumnModel().getColumn(i).maxWidth = maxWidths[i]
        }

        minimumSize = Dimension(minWidths.sum() + 50, 200)
        preferredScrollableViewportSize = Dimension(preferredWidths.sum(), rowHeight * 15)
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        val parentWidth = (parent as? JViewport)?.width ?: return false
        return preferredSize.width < parentWidth
    }
}
