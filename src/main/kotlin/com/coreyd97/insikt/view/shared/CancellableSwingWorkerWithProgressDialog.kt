package com.coreyd97.insikt.view.shared

import com.coreyd97.montoyautilities.panelBuilder
import java.awt.Component
import java.awt.Dialog
import java.awt.Window
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JProgressBar
import javax.swing.SwingWorker

abstract class CancellableSwingWorkerWithProgressDialog<T, S>(
    dialogOwner: Window, title: String = "", message: String
) : SwingWorker<T, S>() {
    private val jProgressBar: JProgressBar = JProgressBar(0, 100)
    private val dialog: JDialog = JDialog(dialogOwner, title, Dialog.ModalityType.MODELESS)

    init {
        val cancelButton = JButton(object : AbstractAction("Cancel") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                this@CancellableSwingWorkerWithProgressDialog.cancel(true)
            }
        })

        val panel = panelBuilder {
            row {
                label(message)
            }
            row {
                gbc.weightx = 1.0
                add(jProgressBar)
                gbc.weightx = 0.0
                add(cancelButton)
            }
        }

        dialog.add(panel)
        dialog.setResizable(false)
        dialog.pack()

        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE)
    }

    @Throws(Exception::class)
    override fun doInBackground(): T {
        dialog.isVisible = true
        jProgressBar.maximum = totalTasks()
        return doWork()
    }

    override fun process(chunks: List<S>) {
        jProgressBar.setValue(jProgressBar.value + chunks.size)
        handleProgress(chunks)
    }

    override fun done() {
        dialog.dispose()
        super.done()
    }

    abstract fun totalTasks(): Int
    abstract fun doWork(): T
    abstract fun handleProgress(chunk: List<S>)
}