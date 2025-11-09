package com.coreyd97.insikt.view

import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.PREF_LAYOUT
import com.coreyd97.insikt.util.PREF_LOG_LEVEL
import com.coreyd97.insikt.util.PREF_MESSAGE_VIEW_LAYOUT
import com.coreyd97.insikt.view.logtable.LogView
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.VariableViewPanel
import com.google.inject.Inject
import org.apache.logging.log4j.Level
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.AbstractButton
import javax.swing.ButtonGroup
import javax.swing.JMenu
import javax.swing.JRadioButtonMenuItem

/**
 * Created by corey on 07/09/17.
 */
class LoggerMenu @Inject constructor(
    val mainPanel: InsiktPanel, val logView: LogView
) : JMenu(APP_NAME) {

    init {
        this.add(mainPanel.getPopoutWrapper().getPopoutMenuItem())
        this.add(logView.requestViewer.getPopoutMenuItem())

        var viewMenu = JMenu("View")
        val currentView by Preference(PREF_LAYOUT, VariableViewPanel.View.VERTICAL)
        var bGroup = ButtonGroup()
        var viewMenuItem = JRadioButtonMenuItem(
            object : AbstractAction("Top/Bottom Split") {
                override fun actionPerformed(actionEvent: ActionEvent?) {
                    logView.setPanelLayout(VariableViewPanel.View.VERTICAL)
                }
            })
        viewMenuItem.setSelected(currentView == VariableViewPanel.View.VERTICAL)
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        viewMenuItem = JRadioButtonMenuItem(object : AbstractAction("Left/Right Split") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logView.setPanelLayout(VariableViewPanel.View.HORIZONTAL)
            }
        })
        viewMenuItem.setSelected(currentView == VariableViewPanel.View.HORIZONTAL)
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        viewMenuItem = JRadioButtonMenuItem(object : AbstractAction("Tabs") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logView.setPanelLayout(VariableViewPanel.View.TABS)
            }
        })
        viewMenuItem.setSelected(currentView == VariableViewPanel.View.TABS)
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        this.add(viewMenu)

        viewMenu = JMenu("Request/Response View")
        val currentReqRespView by Preference(PREF_MESSAGE_VIEW_LAYOUT, VariableViewPanel.View.VERTICAL)
        bGroup = ButtonGroup()
        viewMenuItem = JRadioButtonMenuItem(object : AbstractAction("Top/Bottom Split") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logView.setEntryViewerLayout(VariableViewPanel.View.VERTICAL)
            }
        })
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        viewMenuItem.setSelected(currentReqRespView == VariableViewPanel.View.VERTICAL)
        viewMenuItem = JRadioButtonMenuItem(object : AbstractAction("Left/Right Split") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logView.setEntryViewerLayout(VariableViewPanel.View.HORIZONTAL)
            }
        })
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        viewMenuItem.setSelected(currentReqRespView == VariableViewPanel.View.HORIZONTAL)
        viewMenuItem = JRadioButtonMenuItem(object : AbstractAction("Tabs") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                logView.setEntryViewerLayout(VariableViewPanel.View.TABS)
            }
        })
        viewMenu.add(viewMenuItem)
        bGroup.add(viewMenuItem)
        viewMenuItem.setSelected(currentReqRespView == VariableViewPanel.View.TABS)
        this.add(viewMenu)

        val logLevelGroup = ButtonGroup()
        // Use the same serializer as LoggingController so persisted value is consistent
        var logLevel by Preference(PREF_LOG_LEVEL, Level.INFO, serializer = com.coreyd97.insikt.LevelSerializer())
        val debug: AbstractButton = JRadioButtonMenuItem(
            Level.DEBUG.toString(),
            logLevel.compareTo(Level.DEBUG) == 0
        )
        debug.addActionListener { actionEvent: ActionEvent? ->
            logLevel = Level.DEBUG
        }
        val info: AbstractButton = JRadioButtonMenuItem(
            Level.INFO.toString(),
            logLevel.compareTo(Level.INFO) == 0
        )
        info.addActionListener { actionEvent: ActionEvent? ->
            logLevel = Level.INFO
        }

        logLevelGroup.add(debug)
        logLevelGroup.add(info)
        val logLevelMenu = JMenu("Log Level")
        logLevelMenu.add(debug)
        logLevelMenu.add(info)

        this.add(logLevelMenu)
    }
}