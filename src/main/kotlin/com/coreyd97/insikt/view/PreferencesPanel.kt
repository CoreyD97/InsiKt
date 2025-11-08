package com.coreyd97.insikt.view

import burp.api.montoya.MontoyaApi
import burp.api.montoya.ui.settings.SettingsPanel
import com.coreyd97.insikt.exports.LogExportService
import com.coreyd97.insikt.exports.LogExporter
import com.coreyd97.insikt.filter.*
import com.coreyd97.insikt.logging.LogProcessor
import com.coreyd97.insikt.reflection.ReflectionService
import com.coreyd97.insikt.util.*
import com.coreyd97.montoyautilities.*
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.Window
import javax.swing.*

class PreferencesPanel @Inject constructor(
    val montoya: MontoyaApi,
    val exportService: LogExportService,
    val exporters: MutableSet<LogExporter>,
    val reflectionService: ReflectionService,
    val tagService: TagService,
    val colorService: TableColorService,
    val filterLibrary: FilterLibrary,
    val logProcessor: LogProcessor,
    @param:Named("extension") val extensionWindow: Provider<Window>
) : SettingsPanel {
    val log = LogManager.getLogger(PreferencesPanel::class.java)
    private val JSON = Json { ignoreUnknownKeys = true }

    private val esValueChangeWarning = JLabel(
        "Warning: Changing preferences while running will disable the upload service and clear all pending uploads."
    ).also { it.foreground = Color.RED }

    private lateinit var ignoreFilterField: JTextField
    private var doNotLogFilter: FilterRule? by NullablePreference(PREF_DO_NOT_LOG_IF_MATCH) { _, new ->
        //Only update on valid filter. Otherwise we overwrite input when disabling.
        if(new != null) doNotLogString = new.toString()
    }
    private var doNotLogString: String by Preference(PREF_DO_NOT_LOG_IF_MATCH_STRING, "")

    private var panel: KPanel = generateMainPanel()

    fun generateMainPanel(): KPanel {
        return panelBuilder(childAlignment = Alignment.TOPMIDDLE) {
                panel("Status") {
                    toggleButtonByPreference(PREF_ENABLED, "${APP_NAME} is stopped", "${APP_NAME} is running", 1.0)
                }
                panel("Log Filter") {
                    label("Do not log entries matching filter: ")
                    row {
                        ignoreFilterField = textByPreference(PREF_DO_NOT_LOG_IF_MATCH_STRING)
                        toggleButtonVetoable(
                            "Enable", "Disable", initial = doNotLogFilter != null,
                            onToggleAttempt = { selected -> tryToggleIgnoreFilter(selected) }
                        )
                    }
                }
                row {
                    panel("Log From") {
                        checkBoxByPreference("In-Scope Only", PREF_RESTRICT_TO_SCOPE)
                        fixedSpacer(height = 1)
                        separator(SwingConstants.HORIZONTAL)
                        fixedSpacer(height = 1)
                        checkBoxByPreference("All Tools", PREF_LOG_ALL)
                        checkBoxByPreference("Proxy", PREF_LOG_PROXY)
                        checkBoxByPreference("Repeater", PREF_LOG_REPEATER)
                        checkBoxByPreference("Intruder", PREF_LOG_INTRUDER)
                        checkBoxByPreference("Scanner", PREF_LOG_SCANNER)
                        checkBoxByPreference("Sequencer", PREF_LOG_SEQUENCER)
                        checkBoxByPreference("Extensions", PREF_LOG_EXTENSIONS)
                        checkBoxByPreference("Target Tab", PREF_LOG_TARGET_TAB)
                    }
                    panel(weightY = 1) {
                        panel("Import") {
                            checkBoxByPreference("Import Proxy History on Startup", PREF_AUTO_IMPORT_PROXY_HISTORY)
                            button("Import Burp Proxy History", weightX = 1, onClick = { importProxyHistory() })
                        }

                        exporters.forEach {
                            panel {
                                gbc.fill = GridBagConstraints.HORIZONTAL
                                add(it.exportPanel)
                            }
                        }

                        panel("Reflections") {
                            toggleButtonByPreference(PREF_ENABLE_REFLECTIONS, "Enable Reflection Checks", "Disable Reflection Checks")
                            button(
                                "Configure Reflection Filters",
                                weightX = 1.0,
                                onClick = { configureReflectionFilters() })
                            button(
                                "Configure Transformation Detectors",
                                weightX = 1.0,
                                onClick = { configureTransformationDetectors() })
                        }
                        panel(weightY = 10) {
                            fixedSpacer(1, 1)
                        } //Fill remaining space
                    }
                }
                row {
                    panel("Saved Filters") {
                        button("Import Saved Filters", weightX = 1.0, onClick = { importSavedFilters() })
                        button("Export Saved Filters", weightX = 1.0, onClick = { exportSavedFilters() })
                    }
                    panel("Color Filters") {
                        button("Import Color Filters", weightX = 1.0, onClick = { importColorFilters() })
                        button("Export Color Filters", weightX = 1.0, onClick = { exportColorFilters() })
                    }
                    panel("Tags") {
                        button("Import Tags", weightX = 1.0, onClick = { importTags() })
                        button("Export Tags", weightX = 1.0, onClick = { exportTags() })
                    }
                }

                grid(0, 2,"Other") {
                    label("Response Timeout (Seconds): ")
                    spinnerByPreference(PREF_RESPONSE_TIMEOUT, 10, 10, 600, 1.0)
                    label("Maximum Log Entries: ")
                    spinnerByPreference(PREF_MAXIMUM_ENTRIES, 1000, 10, Int.MAX_VALUE, 1.0)
                    label("Grepper Search Threads: ")
                    spinnerByPreference(
                        PREF_SEARCH_THREADS,
                        1,
                        1,
                        3 * Runtime.getRuntime().availableProcessors(),
                        1.0
                    )
                    label("Max Response Size (MB): ")
                    spinnerByPreference(PREF_MAX_RESP_SIZE, 1, 0, 1000000, 1.0)
                }
        }
    }

    private fun configureTransformationDetectors() {
        reflectionService.showValueTransformerDialog()
    }

    private fun configureReflectionFilters() {
        reflectionService.showFilterConfigDialog()
    }

    private fun tryToggleIgnoreFilter(enabled: Boolean): Boolean {
        val currentValue = doNotLogString
        if(!enabled){
            // Always allow disabling
            doNotLogFilter = null
            return true
        }
        return if (currentValue.isBlank()) {
            // Cannot enable if the filter is blank.
            ToastNotification.show(ignoreFilterField, "Cannot enable empty filter", 2000)
            false
        } else {
            val parseResult = FilterExpression.fromString(currentValue)
            if(!parseResult.valid()){
                ToastNotification.show(ignoreFilterField, "Filter expression is invalid.\n${parseResult.errors.joinToString(",")}", 2000)
            }else{
                doNotLogFilter = FilterRule(parseResult.expression)
            }
            parseResult.valid()
        }
    }

    private fun exportTags() {
        val tags: Set<ColorizingRule> by PreferenceProxy(PREF_TAG_FILTERS)
        val jsonOutput = JSON.encodeToString(serializer(tags.javaClass), tags)
        MoreHelp.showLargeOutputDialog("Export Tags", jsonOutput)
    }

    private fun importTags() {
        val json = MoreHelp.showLargeInputDialog("Import Tags", null)
        if (json.isNullOrEmpty()) {
            return
        }

        val tags = JSON.decodeFromString<Set<ColorizingRule>>(json)
        tags.forEach { tag ->
            tagService.addTag(tag)
        }
    }

    private fun exportColorFilters() {
        val colorFilters: Set<ColorizingRule> by PreferenceProxy(PREF_COLOR_FILTERS)
        val jsonOutput = JSON.encodeToString(serializer(colorFilters.javaClass), colorFilters)
        MoreHelp.showLargeOutputDialog("Export Color Filters", jsonOutput)
    }

    private fun importColorFilters() {
        val json = MoreHelp.showLargeInputDialog("Import Color Filters", null)
        if(json.isNullOrEmpty()){
            return
        }

        val importedColorFilters: Set<ColorizingRule> = JSON.decodeFromString(json)

        importedColorFilters.forEach { filter ->
            colorService.addColorFilter(filter)
        }
    }

    private fun exportSavedFilters() {
        val filters: Set<FilterRule> by PreferenceProxy(PREF_SAVED_FILTERS)
        val jsonOutput = JSON.encodeToString(serializer(), filters)
        MoreHelp.showLargeOutputDialog("Export Saved Filters", jsonOutput)
    }

    fun importProxyHistory(){
        val historySize = montoya.proxy().history().size
        var maxEntries: Int by PreferenceProxy(PREF_MAXIMUM_ENTRIES)
        var message =
            ("""Import $historySize items from burp suite proxy history? This will clear the current entries.
Large imports may take a few minutes to process.""")
        if (historySize > maxEntries) {
            message += "\nNote: History will be truncated to $maxEntries entries."
        }

        val result = MoreHelp.askConfirmMessage(
            "Burp Proxy Import", message,
            arrayOf("Import", "Cancel"),
            this.uiComponent()
        )

        if (result == JOptionPane.OK_OPTION) {
            var sendToAutoExporters = false
            //TODO reimplement
            if (exportService.enabledExporters.isNotEmpty()) {
                val res = JOptionPane.showConfirmDialog(
                    extensionWindow.get(),
                    "One or more auto-exporters are currently enabled. " +
                            "Do you want the imported entries to also be sent to the auto-exporters?",
                    "Auto-exporters Enabled",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                sendToAutoExporters = res == JOptionPane.YES_OPTION
            }
            logProcessor.importProxyHistory(sendToAutoExporters)
        }
    }

    fun importSavedFilters(){
        val json = MoreHelp.showLargeInputDialog("Import Saved Filters", null)
        if(json.isNullOrEmpty()){
            return
        }

        runCatching {
            val importedFilters: Set<FilterRule> =
                JSON.decodeFromString(json)
            importedFilters.forEach { filter ->
                filterLibrary.addToLibrary(filter)
            }
        }.onFailure { error ->
            log.error(error.message, error)
        }

    }

    override fun uiComponent(): JComponent {
        return panel
    }

    override fun keywords(): Set<String?> {
        return setOf("insikt", "logging", "logger", "filter", "export", "import", "reflection", "tag", "color" )
    }
}