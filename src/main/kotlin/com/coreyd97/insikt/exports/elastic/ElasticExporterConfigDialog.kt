package com.coreyd97.insikt.exports.elastic

import com.coreyd97.insikt.filter.FilterExpression
import com.coreyd97.insikt.filter.validateFilterString
import com.coreyd97.montoyautilities.Alignment
import com.coreyd97.montoyautilities.PreferenceProxy
import com.coreyd97.insikt.util.*
import com.coreyd97.insikt.view.shared.FieldSelectorDialog
import com.coreyd97.montoyautilities.HistoryField
import com.coreyd97.montoyautilities.KPanel
import com.coreyd97.montoyautilities.NullablePreference
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.ToastNotification
import com.coreyd97.montoyautilities.panelBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

class ElasticExporterConfigDialog internal constructor(
    owner: Frame?,
    elasticExporter: ElasticExporter
) : JDialog(owner, "Elastic Exporter Configuration", true) {

    lateinit var filterHistoryField: HistoryField
    var filter by NullablePreference<FilterExpression>(PREF_ELASTIC_FILTER, null)

    val validateFilter: (String) -> String? = { str ->
        FilterExpression.fromString(str).let {
            if(!it.valid()){
                ToastNotification.show(filterHistoryField, "Invalid filter: ${it.errors.joinToString(", ")}")
            }
            it.expression?.toString()
        }
    }

    init {
        this.layout = BorderLayout()

        val configureFieldsButton = JButton(object : AbstractAction("Configure") {
            override fun actionPerformed(actionEvent: ActionEvent?) {
                FieldSelectorDialog(null, "Elastic Exporter", elasticExporter.exportFields) {
                    val selected = it
                    if (selected.isEmpty()) {
                        JOptionPane.showMessageDialog(
                            this@ElasticExporterConfigDialog,
                            "No fields were selected. No changes have been made.",
                            "Elastic Exporter", JOptionPane.INFORMATION_MESSAGE
                        )
                        return@FieldSelectorDialog
                    }

                    elasticExporter.exportFields.clear()
                    elasticExporter.exportFields.addAll(selected)
                }
            }
        })

        val panel = panelBuilder(childAlignment = Alignment.FILL) {
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            gbc.insets = Insets(5, 5, 5, 5)
                grid(0, 2, "Connection", 1.0) {
                    minimumSize = Dimension(250, 0)
                    (layout as GridBagLayout).columnWeights = doubleArrayOf(0.0, 1.0)
                    gbc.insets = Insets(0, 2, 0, 2)
                    label("Address:")
                    textByPreference(PREF_ELASTIC_ADDRESS)
                    label("Port:")
                    spinnerByPreference(PREF_ELASTIC_PORT, 1, 0, 65535)
                    label("Protocol:")
                    dropdownByPreference(PREF_ELASTIC_PROTOCOL, Protocol.entries.toTypedArray())
                    label("Index:")
                    textByPreference(PREF_ELASTIC_INDEX)
                }
                panel("Authentication", 1.0) {
                    dropdownByPreference(PREF_ELASTIC_AUTH, ElasticAuthType.entries.toTypedArray())
                    lateinit var apiKeyPanel: KPanel
                    lateinit var basicAuthPanel: KPanel
                    val authType by PreferenceProxy<ElasticAuthType>(PREF_ELASTIC_AUTH) { _, new ->
                        apiKeyPanel.isVisible = false
                        basicAuthPanel.isVisible = false
                        when (new) {
                            ElasticAuthType.ApiKey -> apiKeyPanel.isVisible = true
                            ElasticAuthType.Basic -> basicAuthPanel.isVisible = true
                            else -> {}
                        }
                    }

                    apiKeyPanel = grid(0, 2) {
                        (layout as GridBagLayout).columnWeights = doubleArrayOf(0.0, 1.0)
                        isVisible = authType == ElasticAuthType.ApiKey
                        label("Key ID:")
                        textByPreference(PREF_ELASTIC_API_KEY_ID)
                        label("Key Secret: ")
                        textByPreference(PREF_ELASTIC_API_KEY_SECRET)
                    }
                    basicAuthPanel = grid(0, 2) {
                        (layout as GridBagLayout).columnWeights = doubleArrayOf(0.0, 1.0)
                        isVisible = authType == ElasticAuthType.Basic
                        label("Username:")
                        textByPreference(PREF_ELASTIC_USERNAME)
                        label("Password:")
                        textByPreference(PREF_ELASTIC_PASSWORD)
                    }
                }
            grid(0, 2,"Misc") {
                label("Upload Frequency (Seconds): ")
                spinnerByPreference(PREF_ELASTIC_DELAY, 30, 10, 60000)

                label("Exported Fields: ")
                add(configureFieldsButton)

                label("Log Filter: ")
                filterHistoryField = HistoryField(PREF_ELASTIC_FILTER_HISTORY, 10,
                    validateFilter,
                    { str ->
                        filter = FilterExpression.fromString(str).expression
                        filterHistoryField.setValueSilently(filter.toString())
                    }
                )
                add(filterHistoryField)

                label("Autostart Exporter (All Projects): ")
                checkBoxByPreference("", PREF_ELASTIC_AUTOSTART_GLOBAL)

                label("Autostart Exporter (This Project): ")
                checkBoxByPreference("", PREF_ELASTIC_AUTOSTART_PROJECT)
            }
            panel {
                gbc.anchor = GridBagConstraints.EAST
                button("Close", { dispose() })
            }
        }

        this.add(panel, BorderLayout.CENTER)

        this.minimumSize = Dimension(500, 200)

        this.pack()
        this.setResizable(true)
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE)
    }
}
