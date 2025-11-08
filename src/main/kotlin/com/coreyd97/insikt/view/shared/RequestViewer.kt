package com.coreyd97.insikt.view.shared

import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.editor.EditorOptions
import burp.api.montoya.ui.editor.HttpRequestEditor
import burp.api.montoya.ui.editor.HttpResponseEditor
import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.util.PREF_MESSAGE_VIEW_LAYOUT
import com.coreyd97.montoyautilities.PopOutPanel
import com.coreyd97.montoyautilities.VariableViewPanel
import com.google.inject.Inject

class RequestViewer @Inject constructor(montoya: MontoyaApi): PopOutPanel() {
    val requestEditor: HttpRequestEditor =
        montoya.userInterface().createHttpRequestEditor(EditorOptions.READ_ONLY)
    val responseEditor: HttpResponseEditor =
        montoya.userInterface().createHttpResponseEditor(EditorOptions.READ_ONLY)
    val variableViewPanel: VariableViewPanel = VariableViewPanel(
        PREF_MESSAGE_VIEW_LAYOUT,
        requestEditor.uiComponent(), "Request",
        responseEditor.uiComponent(), "Response",
        VariableViewPanel.View.HORIZONTAL
    )

    private var currentEntry: LogEntry? = null

    init {
        this.setComponent(variableViewPanel)
        this.setTitle("Request/Response Viewer")
    }

    fun setDisplayedEntity(logEntry: LogEntry?) {
        // Only update message if it's new. This fixes issue #164 and improves performance during heavy scanning.
        if (this.currentEntry == logEntry) {
            return
        }

        this.currentEntry = logEntry
        requestEditor.request = logEntry?.request
        responseEditor.response = logEntry?.response
    }

    fun setSearchExpression(searchExpression: String) {
        requestEditor.setSearchExpression(searchExpression)
        responseEditor.setSearchExpression(searchExpression)
    }
}