package com.coreyd97.insikt.util

import java.awt.Component
import java.awt.Dimension
import java.awt.Toolkit
import java.text.SimpleDateFormat
import javax.swing.JOptionPane
import javax.swing.JScrollPane
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.html.HTMLEditorKit

val loggerDateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
val serverDateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")

object MoreHelp {
    // Show a message to the user
    fun showPlainInputMessage(
        strMessage: String?, strTitle: String?,
        defaultValue: String?
    ): String? {
        var output = JOptionPane.showInputDialog(
            JOptionPane.getFrameForComponent(null),
            strMessage, strTitle, JOptionPane.PLAIN_MESSAGE, null, null, defaultValue
        ) as String?
        if (output == null) {
            output = defaultValue
        }
        return output
    }

    // Common method to ask a multiple question
    fun askConfirmMessage(
        strTitle: String?, strQuestion: String?,
        msgOptions: Array<String>,
        parent: Component? = null
    ): Int {
        val choice = IntArray(1)
        choice[0] = 0
        choice[0] = JOptionPane.showOptionDialog(
            parent, strQuestion, strTitle,
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE, null, msgOptions, msgOptions[0]
        )
        return choice[0]
    }

    fun showLargeOutputDialog(title: String?, output: String?) {
        val outputArea = JTextPane()
        outputArea.editorKit = HTMLEditorKit()
        outputArea.text = output
        outputArea.setCaretPosition(0)
        outputArea.putClientProperty("html.disable", false)
        val scrollPane = JScrollPane(outputArea)
        scrollPane.putClientProperty("html.disable", false)
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        scrollPane.preferredSize = Dimension((screenSize.getWidth() / 2.0).toInt(), (screenSize.getHeight() / 2.0).toInt())
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
        JOptionPane.showMessageDialog(
            JOptionPane.getFrameForComponent(null),
            scrollPane, title, JOptionPane.INFORMATION_MESSAGE
        )
    }

    fun showLargeInputDialog(title: String?, message: String?): String? {
        return JOptionPane.showInputDialog(title)
    }
}

inline fun <T> ensureEdt(crossinline action: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return action()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait {
        result = runCatching { action() }
    }
    return result!!.getOrThrow()
}
