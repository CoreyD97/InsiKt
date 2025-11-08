package com.coreyd97.insikt.exports.elastic

import com.coreyd97.montoyautilities.panelBuilder
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.util.concurrent.ExecutionException
import javax.swing.*

class ElasticExporterControlPanel(private val elasticExporter: ElasticExporter) : JPanel() {
    var logger: Logger = LogManager.getLogger(this)

    init {
        this.setLayout(BorderLayout())

        val showConfigDialogButton = JButton(object : AbstractAction("Configure Elastic Exporter") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                ElasticExporterConfigDialog(null, elasticExporter).isVisible = true
            }
        })

        val exportButton = JToggleButton("Start Elastic Exporter")
        exportButton.addActionListener(object : AbstractAction() {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                val buttonNowActive = exportButton.isSelected
                exportButton.setEnabled(false)
                exportButton.setText(if (buttonNowActive) STARTING_TEXT else STOPPING_TEXT)
                object : SwingWorker<Boolean, Void?>() {
                    var exception: Exception? = null

                    @Throws(Exception::class)
                    override fun doInBackground(): Boolean {
                        var success = false
                        try {
                            if (exportButton.isSelected) {
                                enableExporter()
                            } else {
                                disableExporter()
                            }
                            success = true
                        } catch (e: Exception) {
                            this.exception = e
                        }
                        return success
                    }

                    override fun done() {
                        try {
                            if (exception != null) {
                                JOptionPane.showMessageDialog(
                                    exportButton, "Could not start elastic exporter: " +
                                            exception!!.message + "\nSee the logs for more information.",
                                    "Elastic Exporter", JOptionPane.ERROR_MESSAGE
                                )
                                logger.error("Could not start elastic exporter.", exception)
                            }
                            val success = get()
                            val isRunning = buttonNowActive xor !success
                            exportButton.setSelected(isRunning)
                            showConfigDialogButton.setEnabled(!isRunning)

                            exportButton.setText(if (isRunning) STOP_TEXT else START_TEXT)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        } catch (e: ExecutionException) {
                            e.printStackTrace()
                        }
                        exportButton.setEnabled(true)
                    }
                }.execute()
            }
        })

        if (this.isExporterEnabled) {
            exportButton.setSelected(true)
            exportButton.setText(STOP_TEXT)
            showConfigDialogButton.setEnabled(false)
        }

        val kPanel = panelBuilder {
            add(showConfigDialogButton)
            add(exportButton)
        }

        this.add(kPanel, BorderLayout.CENTER)

        this.setBorder(BorderFactory.createTitledBorder("Elastic Exporter"))
    }

    @Throws(Exception::class)
    private fun enableExporter() {
        this.elasticExporter.exportService.enableExporter(this.elasticExporter)
    }

    @Throws(Exception::class)
    private fun disableExporter() {
        this.elasticExporter.exportService.disableExporter(this.elasticExporter)
    }

    private val isExporterEnabled: Boolean
        get() = this.elasticExporter.exportService.enabledExporters
            .contains(this.elasticExporter)

    companion object {
        private const val STARTING_TEXT = "Starting Elastic Exporter..."
        private const val STOPPING_TEXT = "Stopping Elastic Exporter..."
        private const val START_TEXT = "Start Elastic Exporter"
        private const val STOP_TEXT = "Stop Elastic Exporter"
    }
}
