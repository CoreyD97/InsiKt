package com.coreyd97.insikt.view.shared

import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.util.PREF_SAVED_FIELD_SELECTIONS
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.panelBuilder
import java.awt.BorderLayout
import java.awt.Frame
import java.awt.event.ActionEvent
import java.awt.event.ItemEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.ArrayList
import java.util.LinkedHashMap
import java.util.Locale
import javax.swing.AbstractAction
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JTable
import javax.swing.table.AbstractTableModel

class FieldSelectorDialog(
    owner: Frame?, title: String,
    defaults: Set<LogEntryField> = emptySet(),
    val callback: (Set<LogEntryField>) -> Unit
) : JDialog(owner, title, true) {
    private val fieldList: MutableSet<LogEntryField> =
        LogEntryField.entries.filter { it != LogEntryField.NUMBER }.toMutableSet()
    private val savedPresets: MutableMap<String, MutableMap<LogEntryField, Boolean>> by Preference(
        PREF_SAVED_FIELD_SELECTIONS,
        mutableMapOf()
    )
    private val selectedFields: MutableMap<LogEntryField, Boolean> =
        fieldList.associateWith { defaults.contains(it) }.toMutableMap()
    private lateinit var savedSelectionSelector: JComboBox<String>
    private lateinit var saveSelectionButton: JButton
    private lateinit var deleteSelectionButton: JButton
    private lateinit var okButton: JButton

    init {
        this.layout = BorderLayout()
        buildDialog()
        isVisible = true
    }

    private fun buildDialog() {
        val fieldTable = JTable(FieldSelectorTableModel())
        fieldTable.setDefaultRenderer(Boolean::class.java, BooleanRenderer())
        fieldTable.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_SPACE) {
                    var initial: Boolean? = null
                    for (selectedRow in fieldTable.selectedRows) {
                        if (initial == null) {
                            initial = !(fieldTable.getValueAt(selectedRow, 1) as Boolean)
                        }
                        fieldTable.setValueAt(initial, selectedRow, 0)
                    }
                }
            }
        })
        fieldTable.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                showContextMenu(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                showContextMenu(e)
            }

            fun showContextMenu(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val source = e.source as JTable
                    val popupMenu = JPopupMenu("Field Selection")

                    if (source.selectedRowCount > 1) {
                        popupMenu.add(JMenuItem(object : AbstractAction("Enable Selected") {
                            override fun actionPerformed(e: ActionEvent?) {
                                for (selectedRow in source.selectedRows) {
                                    source.setValueAt(true, selectedRow, 0)
                                }
                            }
                        }))
                        popupMenu.add(JMenuItem(object : AbstractAction("Disable Selected") {
                            override fun actionPerformed(e: ActionEvent?) {
                                for (selectedRow in source.selectedRows) {
                                    source.setValueAt(false, selectedRow, 0)
                                }
                            }
                        }))
                        popupMenu.add(JSeparator())
                    }
                    popupMenu.add(JMenuItem(object : AbstractAction("Enable All") {
                        override fun actionPerformed(e: ActionEvent?) {
                            for (i in 0..<source.getRowCount()) {
                                source.setValueAt(true, i, 0)
                            }
                        }
                    }))
                    popupMenu.add(JMenuItem(object : AbstractAction("Disable All") {
                        override fun actionPerformed(e: ActionEvent?) {
                            for (i in 0..<source.getRowCount()) {
                                source.setValueAt(false, i, 0)
                            }
                        }
                    }))

                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        })
        val fieldScrollPane = JScrollPane(fieldTable)
        okButton = JButton("OK")
        okButton!!.addActionListener { onSuccess() }

        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener { onCancel() }

        val selectAllButton = JButton("Select All")
        selectAllButton.addActionListener {
            selectedFields.replaceAll { _: LogEntryField, _: Boolean -> true }
            (fieldTable.model as FieldSelectorTableModel).fireTableDataChanged()
            setPresetState()
        }
        val selectNoneButton = JButton("Select None")
        selectNoneButton.addActionListener {
            selectedFields.replaceAll { _: LogEntryField?, _: Boolean? -> false }
            (fieldTable.model as FieldSelectorTableModel).fireTableDataChanged()
            setPresetState()
        }

        val savedKeys: MutableList<String?> = ArrayList<String?>()
        savedKeys.add("Unsaved")
        savedKeys.addAll(savedPresets.keys)
        savedSelectionSelector = JComboBox<String>(savedKeys.toTypedArray())

        savedSelectionSelector!!.addItemListener { e: ItemEvent? ->
            if (e!!.stateChange == ItemEvent.SELECTED) {
                val key = e.item as String
                if (key == "Unsaved") {
                    val preset = this.matchedPreset
                    if (preset != null) {
                        savedSelectionSelector!!.setSelectedItem(preset)
                    }
                } else {
                    val selection: MutableMap<LogEntryField, Boolean> = savedPresets.get(key)!!
                    selectedFields.forEach { (field: LogEntryField, _: Boolean?) ->
                        //Should new fields be added after a user has saved a selection,
                        //We must do this in a way that will preserve the new fields so cannot simply
                        //Clear selectedFields and add all from saved selection. Instead default not found keys to false.
                        selectedFields[field] = selection.getOrDefault(field, false)!!
                    }
                }
                setPresetState()
                okButton!!.setEnabled(selectedFields.containsValue(true))
                (fieldTable.model as FieldSelectorTableModel).fireTableDataChanged()
            }
        }

        saveSelectionButton = JButton(object : AbstractAction("Save") {
            override fun actionPerformed(e: ActionEvent?) {
                val key = JOptionPane.showInputDialog(
                    JOptionPane.getFrameForComponent(saveSelectionButton),
                    "Enter name for saved selection preset:", "Saving Selection Preset",
                    JOptionPane.PLAIN_MESSAGE
                )

                if (key == null || key.isEmpty()) {
                    JOptionPane.showMessageDialog(
                        JOptionPane.getFrameForComponent(saveSelectionButton),
                        "Saving cancelled.", "Selection Preset", JOptionPane.INFORMATION_MESSAGE
                    )
                } else {
                    if (savedPresets.containsKey(key.lowercase(Locale.getDefault()))) {
                        JOptionPane.showMessageDialog(
                            JOptionPane.getFrameForComponent(saveSelectionButton),
                            "Cannot save selection as " + key + ". A preset with that name already exists.",
                            "Selection Preset", JOptionPane.ERROR_MESSAGE
                        )
                    } else {
                        savedPresets.put(
                            key.lowercase(Locale.getDefault()),
                            LinkedHashMap<LogEntryField, Boolean>(selectedFields)
                        )
                        savedSelectionSelector!!.addItem(key.lowercase())
                        savedSelectionSelector!!.setSelectedItem(key.lowercase())
                        setPresetState()
                    }
                }
            }
        })

        deleteSelectionButton = JButton(object : AbstractAction("Delete") {
            override fun actionPerformed(e: ActionEvent?) {
                val selectedKey = savedSelectionSelector!!.selectedItem as String?
                if (selectedKey == null) {
                    return
                }
                val outcome = JOptionPane.showConfirmDialog(
                    JOptionPane.getFrameForComponent(saveSelectionButton),
                    "Are you sure you wish to delete the preset \"" + selectedKey + "\"?",
                    "Delete Selection Preset?", JOptionPane.YES_NO_OPTION
                )
                if (outcome == JOptionPane.OK_OPTION) {
                    savedPresets.remove(selectedKey.lowercase(Locale.getDefault()))
                    savedSelectionSelector!!.setSelectedItem("Unsaved")
                    savedSelectionSelector!!.removeItem(selectedKey.lowercase(Locale.getDefault()))
                    setPresetState()
                }
            }
        })

        okButton.setEnabled(selectedFields.containsValue(true))
        setPresetState()

        val panel = panelBuilder {
            label("Select fields to be exported:")
            row {
                add(savedSelectionSelector)
                filler(1)
                add(saveSelectionButton)
                add(deleteSelectionButton)
            }
            add(fieldScrollPane)
            row {
                add(selectAllButton)
                add(selectNoneButton)
                filler(1)
                add(okButton)
                add(cancelButton)
            }
        }

        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10))
        this.add(panel, BorderLayout.CENTER)
        this.pack()
    }

    fun onSuccess() {
        callback(selectedFields.filter { it.value }.keys.toSet())
        this.dispose()
    }

    fun onCancel() {
        this.dispose()
    }

    fun getSelectedFields(): MutableList<LogEntryField> {
        val selectedList = mutableListOf<LogEntryField>()
        selectedFields.forEach { (field: LogEntryField, selected: Boolean) ->
            if (selected) {
                selectedList.add(field)
            }
        }

        return selectedList
    }

    private val matchedPreset: String
        get() {
            return savedPresets.filter {
                val presetEnabled = it.value.filterValues { it }.keys
                val currentEnabled = selectedFields.filterValues { it }.keys
                presetEnabled.containsAll(currentEnabled) && currentEnabled.containsAll(presetEnabled)
            }.keys.first()
        }

    private fun setPresetState() {
        val matchedPreset = this.matchedPreset
        if (matchedPreset == null) {
            savedSelectionSelector.setSelectedItem("Unsaved")
            saveSelectionButton.setEnabled(true)
            deleteSelectionButton.setEnabled(false)
        } else {
            savedSelectionSelector.setSelectedItem(matchedPreset)
            saveSelectionButton.setEnabled(false)
            deleteSelectionButton.setEnabled(true)
        }
    }

    private inner class FieldSelectorTableModel : AbstractTableModel() {
        override fun getRowCount(): Int {
            return selectedFields.size
        }

        override fun getColumnCount(): Int {
            return 2
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            return if (columnIndex == 0) String::class.java else Boolean::class.java
        }

        override fun getColumnName(column: Int): String {
            return if (column == 0) "Field" else "Enabled"
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 1
        }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            val field = fieldList.elementAt(rowIndex)
            selectedFields[field] = value as Boolean

            //Check presets to see if our current selection matches a preset
            setPresetState()

            okButton.setEnabled(selectedFields.containsValue(true))
            this.fireTableRowsUpdated(rowIndex, rowIndex)
        }

        override fun getValueAt(row: Int, col: Int): Any? {
            return if (col == 0) {
                fieldList.elementAt(row)
            } else {
                selectedFields[fieldList.elementAt(row)]
            }
        }
    }
}