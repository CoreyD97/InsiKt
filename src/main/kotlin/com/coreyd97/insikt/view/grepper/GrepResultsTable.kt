package com.coreyd97.insikt.view.grepper

import com.coreyd97.insikt.grepper.GrepResults
import com.coreyd97.insikt.grepper.GrepperListener
import com.coreyd97.insikt.grepper.GrepperService
import org.jdesktop.swingx.JXTree
import org.jdesktop.swingx.JXTreeTable
import org.jdesktop.swingx.tree.DefaultXTreeCellRenderer
import org.jdesktop.swingx.treetable.AbstractTreeTableModel
import java.util.regex.Pattern
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel

class GrepResultsTable(
    private val controller: GrepperService
) : JXTreeTable(), GrepperListener {
    private val tableModel: GrepTableModel

    init {
        this.tableModel = GrepTableModel()

        this.treeTableModel = this.tableModel

        this.autoCreateRowSorter = true
        this.setColumnSelectionAllowed(true)

        this.setAutoResizeMode(AUTO_RESIZE_OFF)

        val renderer =
            ((this.treeCellRenderer as JXTree.DelegatingRenderer).delegateRenderer as DefaultXTreeCellRenderer)
        renderer.setBackground(null)
        renderer.setOpaque(true)

        this.controller.addListener(this)
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        return getPreferredSize().width < parent.width
    }

    override fun onSearchStarted(pattern: Pattern, searchEntries: Int) {
        tableModel.reset()
        tableModel.setPatternGroups(pattern.matcher("").groupCount())
    }

    override fun onEntryProcessed(entryResults: GrepResults) {
        if (entryResults != null) {
            tableModel.addEntry(entryResults)
        }
    }

    override fun onSearchComplete() {
        tableModel.reload()
    }

    override fun onResetRequested() {
        tableModel.reset()
    }

    override fun onShutdownInitiated() {
    }

    override fun onShutdownComplete() {
        tableModel.reload()
    }


    internal inner class GrepTableModel : AbstractTreeTableModel(Any()) {
        private val columns = listOf(
            "Entry", "Request Matches", "Response Matches",
            "Total Matches", "Complete Match"
        )
        private val matchingEntries: ArrayList<GrepResults>
        private var patternGroups = 0

        init {
            this.matchingEntries = ArrayList<GrepResults>()
        }

        fun setPatternGroups(count: Int) {
            this.patternGroups = count
            (this@GrepResultsTable.model as AbstractTableModel).fireTableStructureChanged()
        }

        fun addEntry(matches: GrepResults) {
            if (matches.matches.isEmpty()) {
                return
            }
            synchronized(this.matchingEntries) {
                this.matchingEntries.add(matches)
            }
        }

        fun reload() {
            synchronized(matchingEntries) {
                modelSupport.fireNewRoot()
            }
        }

        override fun getColumnCount(): Int {
            return columns.size + patternGroups
        }

        override fun getColumnName(column: Int): String {
            if (column < columns.size) {
                return columns[column]
            } else {
                return "Group " + (column - columns.size + 1)
            }
        }

        override fun isLeaf(node: Any?): Boolean {
            return node is GrepResults.Match
        }

        override fun getValueAt(node: Any?, column: Int): Any? {
            if (node is GrepResults) {
                if (column == 1) {
                    return node.requestMatches
                }
                if (column == 0) {
                    return node.logEntry.getUrl()
                }
                if (column == 2) {
                    return node.responseMatches
                }
                if (column == 3) {
                    return node.matches.size
                } else {
                    return ""
                }
            }
            if (node is GrepResults.Match) {
                if (column == 0) {
                    return if (node.isRequest) "REQUEST" else "RESPONSE"
                }
                if (column >= 1 && column <= 3) {
                    return null
                }
                return node.groups[column - 4]
            }
            return ""
        }

        override fun getChild(parent: Any?, i: Int): Any {
            if (parent is GrepResults) {
                return parent.matches[i]
            }
            return this.matchingEntries[i]
        }

        override fun getChildCount(parent: Any?): Int {
            if (parent is GrepResults) {
                return parent.matches.size
            }
            synchronized(matchingEntries) {
                return this.matchingEntries.size
            }
        }

        override fun getIndexOfChild(parent: Any?, child: Any?): Int {
            if (parent is GrepResults) {
                return parent.matches.indexOf(child)
            }
            return -1
        }

        fun reset() {
            synchronized(matchingEntries) {
                matchingEntries.clear()
            }
            SwingUtilities.invokeLater(Runnable {
                modelSupport.fireNewRoot()
            })
        }
    }
}