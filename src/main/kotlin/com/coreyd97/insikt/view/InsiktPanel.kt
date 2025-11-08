package com.coreyd97.insikt.view

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.view.library.FilterLibraryPanel
import com.coreyd97.insikt.view.grepper.GrepperPanel
import com.coreyd97.insikt.grepper.GrepperService
import com.coreyd97.insikt.view.logtable.LogTable
import com.coreyd97.insikt.view.logtable.LogView
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.PREF_ENABLED
import com.coreyd97.insikt.view.shared.RequestViewer
import com.coreyd97.montoyautilities.PopOutPanel
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.PreferenceProxy
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.name.Named
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.JPanel
import javax.swing.JTabbedPane

interface InsiktPanel {
    fun showTab(index: Int)
    fun getComponent(): Component
    fun getPopoutWrapper(): PopOutPanel
    fun unload()
}

@Singleton
class InsiktPanelImpl @Inject constructor(
    val montoya: MontoyaApi,
    val filterLibraryPanel: FilterLibraryPanel,
    val grepperService: GrepperService,
    val logView: LogView,
    val logTable: LogTable
): JPanel(BorderLayout()), InsiktPanel {
    val popOutWrapper: PopOutPanel
    val tabbedPanel: JTabbedPane = JTabbedPane()

    val grepperPanel = GrepperPanel(montoya, grepperService, logTable, this)
    val _enabled by Preference(PREF_ENABLED, true) { _, isEnabled ->
        if (!isEnabled) tabbedPanel.setTitleAt(0, "View Logs (PAUSED)")
        else tabbedPanel.setTitleAt(0,"View Logs")
    }

    init {
        tabbedPanel.addTab("View Logs", null, logView, null)
        tabbedPanel.addTab(
            "Filter Library",
            null,
            filterLibraryPanel,
            null
        )
        tabbedPanel.addTab("Grep Values", null, grepperPanel, null)
        tabbedPanel.addTab("Help", null, HelpPanel(), null)
        popOutWrapper = PopOutPanel(tabbedPanel, APP_NAME)
        add(popOutWrapper, BorderLayout.CENTER)
    }

    override fun showTab(index: Int){
        tabbedPanel.selectedIndex = index
    }

    override fun getComponent(): Component {
        return this
    }

    override fun getPopoutWrapper(): PopOutPanel {
        return popOutWrapper
    }

    override fun unload(){
        if (popOutWrapper.isPoppedOut) {
            popOutWrapper.popoutFrame.dispose()
        }
    }
}