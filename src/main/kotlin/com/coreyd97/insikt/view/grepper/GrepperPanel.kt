package com.coreyd97.insikt.view.grepper

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.grepper.GrepResults
import com.coreyd97.insikt.grepper.GrepperListener
import com.coreyd97.insikt.grepper.GrepperService
import com.coreyd97.insikt.view.grepper.UniquePatternMatchTable
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.view.logtable.LogTable
import com.coreyd97.insikt.util.PREF_GREP_HISTORY
import com.coreyd97.insikt.view.InsiktPanel
import com.coreyd97.insikt.view.shared.RequestViewer
import com.coreyd97.montoyautilities.Alignment
import com.coreyd97.montoyautilities.HistoryField
import com.coreyd97.montoyautilities.panelBuilder
import com.google.inject.Inject
import com.google.inject.Singleton
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JProgressBar
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager
import javax.swing.event.TreeSelectionEvent

class GrepperPanel @Inject constructor(
    val montoya: MontoyaApi,
    val grepperService: GrepperService,
    val logTable: LogTable,
    val mainPanel: InsiktPanel
) : JPanel(), GrepperListener {

    private val searchField: HistoryField = HistoryField(PREF_GREP_HISTORY, 15)
    private val searchButton: JButton
    private val resetButton: JButton
    private val progressBar: JProgressBar
    private val searchRequests: JCheckBox
    private val searchResponses: JCheckBox
    private val inScopeOnly: JCheckBox
    private val resultsPane: JTabbedPane
    private val grepResultsTable: GrepResultsTable
    private val requestViewerController: RequestViewer
    private val uniqueTable: UniquePatternMatchTable

    init {
        searchField.editor.editorComponent.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    startSearch()
                }
            }
        })

        this.progressBar = JProgressBar()
        this.inScopeOnly = JCheckBox("In Scope Only")
        this.searchRequests = JCheckBox("Search Requests", true)
        this.searchResponses = JCheckBox("Search Responses", true)

        this.searchButton = JButton(object : AbstractAction("Search") {
            override fun actionPerformed(e: ActionEvent?) {
                if (grepperService.isSearching) {
                    grepperService.cancelSearch()
                } else {
                    startSearch()
                }
            }
        })

        this.resetButton = JButton(object : AbstractAction("Reset") {
            override fun actionPerformed(e: ActionEvent?) {
                grepperService.reset()
            }
        })

        this.grepResultsTable = GrepResultsTable(grepperService)

        grepResultsTable.apply {
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if(!SwingUtilities.isRightMouseButton(e)) return
                    val row = grepResultsTable.rowAtPoint(e.getPoint())
                    val path = grepResultsTable.getPathForRow(row) ?: return
                    var obj = path.lastPathComponent
                    var selected: LogEntry
                    if (obj is GrepResults) {
                        selected = obj.logEntry
                    } else if (obj is GrepResults.Match) {
                        selected = (path.getPathComponent(path.getPathCount() - 2) as GrepResults).logEntry
                    }

                    //todo Pull actual row from somewhere (previously indexOf in logRepo)
                    val menu = buildContextMenu(row)
                    menu.show(grepResultsTable, e.x, e.y)
                }
            })
        }
        this.requestViewerController = RequestViewer(montoya)

        grepResultsTable.addTreeSelectionListener { treeSelectionEvent: TreeSelectionEvent? ->
            val selectedPath = treeSelectionEvent!!.path
            val grepResultEntry = selectedPath.getPath()[1] as GrepResults
            var selectedMatch: GrepResults.Match? = null
            if (selectedPath.getPath().size > 2) {
                selectedMatch = selectedPath.getPath()[2] as GrepResults.Match?
            }

            //Todo Show exact match if the API ever allows...
            val requestResponse = grepResultEntry.logEntry
            requestViewerController.setDisplayedEntity(requestResponse)
            requestViewerController.setSearchExpression(getPatternInput())
        }

        val resultsSplitPane = JSplitPane(
            JSplitPane.VERTICAL_SPLIT,
            JScrollPane(grepResultsTable), requestViewerController
        )
        this.uniqueTable = UniquePatternMatchTable(grepperService)

        this.resultsPane = JTabbedPane()
        this.resultsPane.addTab("Results", resultsSplitPane)
        this.resultsPane.addTab("Unique Results", JScrollPane(uniqueTable))

        val panel = panelBuilder(childAlignment = Alignment.FILL) {
            row {
                gbc.weightx = 0.0
                label("Regex: ")
                add(searchField, weightX = 1)
                add(searchRequests)
                add(searchResponses)
                add(inScopeOnly)
                add(searchButton)
                add(resetButton)
            }
            gbc.fill = GridBagConstraints.BOTH
            row(weightY = 1) {
                gbc.fill = GridBagConstraints.BOTH
                add(resultsPane, weightY = 1)
            }
            gbc.fill = GridBagConstraints.HORIZONTAL
            row {
                add(progressBar)
            }
        }

        this.setLayout(BorderLayout())
        this.add(panel, BorderLayout.CENTER)

        this.grepperService.addListener(this)
    }

    private fun getPatternInput(): String {
        return (this.searchField.editor
            .editorComponent as JTextField).getText()
    }

    private fun startSearch() {
        val pattern: Pattern?
        try {
            pattern = Pattern.compile(getPatternInput(), Pattern.CASE_INSENSITIVE)
        } catch (e: PatternSyntaxException) {
            JOptionPane.showMessageDialog(
                JOptionPane.getFrameForComponent(
                    this
                ), "Pattern Syntax Invalid",
                "Invalid Pattern", JOptionPane.ERROR_MESSAGE
            )
            return
        }

        this.grepperService.beginSearch(
            pattern, this.inScopeOnly.isSelected,
            this.searchRequests.isSelected, this.searchResponses.isSelected
        )
    }

    override fun onSearchStarted(pattern: Pattern, totalRequests: Int) {
        SwingUtilities.invokeLater {
            this.searchRequests.setEnabled(false)
            this.searchResponses.setEnabled(false)
            this.searchField.setEnabled(false)
            this.resetButton.setEnabled(false)
            this.searchButton.setText("Cancel")
            this.progressBar.maximum = totalRequests
            this.progressBar.setValue(0)
        }
    }

    @Synchronized
    override fun onEntryProcessed(entryResults: GrepResults) {
        SwingUtilities.invokeLater {
            this.progressBar.setValue(this.progressBar.value + 1)
        }
    }

    override fun onSearchComplete() {
        unlockUI()
    }

    override fun onResetRequested() {
    }

    override fun onShutdownInitiated() {
        SwingUtilities.invokeLater {
            this.searchButton.setText("Stopping...")
        }
    }

    override fun onShutdownComplete() {
        unlockUI()
    }

    private fun unlockUI() {
        SwingUtilities.invokeLater {
            this.searchButton.setText("Search")
            this.progressBar.setValue(0)
            this.searchField.setEnabled(true)
            this.resetButton.setEnabled(true)
            this.searchRequests.setEnabled(true)
            this.searchResponses.setEnabled(true)
        }
    }

    private fun buildContextMenu(row: Int): JPopupMenu {
        val menu = JPopupMenu()
        val viewIndex = logTable.convertRowIndexToView(row)
        val viewInLogs = JMenuItem(object : AbstractAction("View in Logs") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logTable.changeSelection(
                    viewIndex,
                    1,
                    false,
                    false
                )
                mainPanel.showTab(0)
            }
        })
        menu.add(viewInLogs)
        if (viewIndex == -1) {
            viewInLogs.setEnabled(false)
            viewInLogs.setToolTipText("Unavailable. Hidden by filter.")
            viewInLogs.addMouseListener(object : MouseAdapter() {
                val defaultTimeout: Int =
                    ToolTipManager.sharedInstance().initialDelay

                override fun mouseEntered(e: MouseEvent?) {
                    ToolTipManager.sharedInstance().initialDelay = 0
                }

                override fun mouseExited(e: MouseEvent?) {
                    ToolTipManager.sharedInstance().initialDelay = defaultTimeout
                }
            })
        }
        return menu
    }
}