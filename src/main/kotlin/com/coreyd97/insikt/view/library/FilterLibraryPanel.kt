package com.coreyd97.insikt.view.library

import ButtonCell
import MenuButtonCell
import com.coreyd97.insikt.filter.*
import com.coreyd97.insikt.view.InsiktPanel
import com.coreyd97.insikt.view.logtable.LogView
import com.coreyd97.insikt.view.shared.FilterEditor
import com.google.inject.Inject
import com.google.inject.Provider
import jakarta.inject.Singleton
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

/**
 * Created by corey on 27/08/17.
 */
@Singleton
class FilterLibraryPanel @Inject constructor(
    private val filterLibrary: FilterLibrary,
    private val filterEditorProvider: Provider<FilterEditor>,
    private val colorService: TableColorService,
    private val tagService: TagService,
    private val filterService: TableFilterService,
    private val insiktPanel: InsiktPanel,
    private val logView: LogView
) : JPanel(BorderLayout()) {
    init {
        val libraryModel = FilterLibraryTableModel(this.filterLibrary)
        val targetResolver: (JTable, Int, Int) -> FilterRule = { table, viewRow, _ ->
            val modelRow = table.convertRowIndexToModel(viewRow)
            filterLibrary.snippets.elementAt(modelRow)
        }

        val setFilterCell = MenuButtonCell(
            labelProvider = { _, _ -> "Use as..." },
            targetResolver = targetResolver,
            menuFactory = { target, _, _, _  -> buildPopup(target) }
        )

        val libraryTable = JTable(libraryModel).apply {
            setRowHeight(25)
            setFillsViewportHeight(true)
            setAutoCreateRowSorter(false)
            autoCreateColumnsFromModel = false
            autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
            (getDefaultRenderer(JButton::class.java) as JComponent).setOpaque(true)
            columnModel.getColumn(0).apply {
                minWidth = 150
                preferredWidth = 150
            }
            columnModel.getColumn(1).apply {
                cellEditor = filterEditorProvider.get()
                minWidth = 200
                preferredWidth = 800
            }
            columnModel.getColumn(2).apply {
                cellEditor = setFilterCell
                cellRenderer = setFilterCell
                minWidth = 100
                preferredWidth = 100
                maxWidth = 100
            }
        }


        val controlPanel = JPanel(GridLayout(1, 0))
        val addFilterButton = JButton("Add Snippet")
        addFilterButton.preferredSize = Dimension(0, 75)
        addFilterButton.addActionListener { _: ActionEvent? ->
            filterLibrary.addToLibrary(
                FilterRule.fromString("Response.body CONTAINS \"Example\"")
            )
        }
        val removeSelectedButton = JButton("Remove Selected")
        removeSelectedButton.minimumSize = Dimension(0, 75)
        removeSelectedButton.addActionListener(ActionListener {
            val selectedRow = libraryTable.selectedRow
            if (selectedRow == -1) {
                return@ActionListener
            }
            val filter = filterLibrary.snippets.elementAt(selectedRow)
            filterLibrary.removeFromLibrary(filter)
        })
        controlPanel.add(addFilterButton)
        controlPanel.add(removeSelectedButton)

        val tableScrollPane = JScrollPane(libraryTable)
        this.add(tableScrollPane, BorderLayout.CENTER)
        this.add(controlPanel, BorderLayout.SOUTH)
    }

    fun buildPopup(filter: FilterRule): JPopupMenu {
        val popup = JPopupMenu()
        popup.add(filter.name)
        popup.addSeparator()
        popup.add(JMenuItem(object :AbstractAction("Use as LogFilter"){
            override fun actionPerformed(e: ActionEvent?) {
                val result = FilterRule.fromString("#${filter.name}")
                filterService.setFilter(result)
                insiktPanel.showTab(0)
            }
        }))

        popup.add(JMenuItem(object :AbstractAction("Use as Color Rule"){
            override fun actionPerformed(e: ActionEvent?) {
                val addition = listOf((ColorizingRule.fromString(filter.name!!, "#${filter.name}")))
                logView.displayColorDialogWithAdditions(addition)
            }
        }))

        popup.add(JMenuItem(object :AbstractAction("Use as Tag"){
            override fun actionPerformed(e: ActionEvent?) {
                val addition = listOf(ColorizingRule.fromString(filter.name ?: "New Tag", "#${filter.name}"))
                logView.displayTagDialogWithAdditions(addition)
            }
        }))
        return popup
    }
}
