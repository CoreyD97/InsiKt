package com.coreyd97.insikt.view.colorizingdialog

import burp.api.montoya.MontoyaApi
import com.coreyd97.insikt.filter.ColorizingRule
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.view.shared.FilterEditor
import com.coreyd97.montoyautilities.Alignment
import com.coreyd97.montoyautilities.panelBuilder
import com.google.inject.Provider
import com.google.inject.Singleton
import com.google.inject.name.Named
import jakarta.inject.Inject
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Frame
import java.awt.GridBagConstraints
import java.awt.Window
import javax.swing.*

/**
 * Created by corey on 19/07/17.
 */

@Singleton
class ColorizingRuleDialogFactory @Inject constructor(
    val filterLibrary: FilterLibrary,
    val filterEditorProvider: Provider<FilterEditor>,
    montoyaApi: MontoyaApi,
    @Named("extension") val extensionWindow: Provider<Window>
) {
    val dialogs = mutableListOf<ColorizingRuleDialog>()

    init {
        montoyaApi.extension().registerUnloadingHandler {
            dialogs.forEach { it.dispose() }
        }
    }

    fun showDialog(tableTitle: String, dialogTitle: String,
                   elems: List<ColorizingRule>, onApply: (List<ColorizingRule>) -> Unit,
                   owner: Frame? = null): ColorizingRuleDialog {
        val dialogOwner = owner ?: extensionWindow.get()
        val dialog = ColorizingRuleDialog(filterLibrary, filterEditorProvider.get(), tableTitle, dialogTitle, dialogOwner, elems, onApply)
        dialogs.add(dialog)
        return dialog
    }

}

class ColorizingRuleDialog internal constructor(
    val filterLibrary: FilterLibrary,
    val filterEditor: FilterEditor,
    val tableTitle: String,
    dialogTitle: String,
    owner: Window?,
    elems: List<ColorizingRule>,
    onApply: (List<ColorizingRule>) -> Unit) : JDialog(owner, dialogTitle, ModalityType.MODELESS) {
    private val tableModel: ColorizingRuleTableModel = ColorizingRuleTableModel(elems)
    private val rulesTable: ColorizingRuleTable = ColorizingRuleTable(tableModel, filterEditor)

    val addButtonHandler = {
        tableModel.addBlankRule()
    }
    val removeButtonHandler = {
        tableModel.remove(rulesTable.selectedRow)
    }
    val deleteAllButtonHandler = {
        val res = JOptionPane.showConfirmDialog(this@ColorizingRuleDialog, "Are you sure you want to delete all rules?", "Confirm", JOptionPane.YES_NO_OPTION)
        if(res == JOptionPane.YES_OPTION)
            tableModel.removeAll()
    }
    val moveUpButtonHandler = {
        val index = rulesTable.selectedRow
        tableModel.switchRows(index, index - 1)
        rulesTable.selectionModel.setSelectionInterval(index-1, index-1)
    }
    val moveDownButtonHandler = {
        val index = rulesTable.selectedRow
        tableModel.switchRows(index, index + 1)
        rulesTable.selectionModel.setSelectionInterval(index+1, index+1)
    }
    val applyButtonHandler = {
        onApply(tableModel.dataCopy)
        tableModel.resetRetestFlags()
    }
    val okButtonHandler = {
        onApply(tableModel.dataCopy)
        dispose()
    }

    init {
//        minimumSize = Dimension(900, 300)
        this.add(buildDialog(), BorderLayout.CENTER)
        pack()
    }

    private fun buildDialog(): Component {
        return panelBuilder(childAlignment = Alignment.FILL) {
            gbc.fill = GridBagConstraints.BOTH
            row(weightY = 1) {
                gbc.fill = GridBagConstraints.BOTH
                panel(weightX = 1) {
                    gbc.fill = GridBagConstraints.BOTH
                    row {
                        label(tableTitle, weightX = 1)
                        button("+", addButtonHandler)
                        button("-", removeButtonHandler) {
                            isEnabled = false
                            rulesTable.selectionModel.addListSelectionListener { l ->
                                isEnabled = l.firstIndex > -1
                            }
                        }
                    }
                    row(weightY = 1) {
                        gbc.fill = GridBagConstraints.BOTH
                        gbc.weighty = 1.0
                        val tableScrollPane = JScrollPane(rulesTable)
                        add(tableScrollPane)
                    }
                }
                row(weightX = 0, weightY = 1) {
                    gbc.anchor = GridBagConstraints.CENTER
                    panel {
                        button("▲", moveUpButtonHandler){
                            isEnabled = false
                            rulesTable.selectionModel.addListSelectionListener { l ->
                                if(l.valueIsAdjusting) return@addListSelectionListener
                                isEnabled = rulesTable.selectedRow > 0
                            }
                        }
                        button("▼", moveDownButtonHandler){
                            isEnabled = false
                            rulesTable.selectionModel.addListSelectionListener { l ->
                                if(l.valueIsAdjusting) return@addListSelectionListener
                                isEnabled = rulesTable.selectedRow >= 0 && rulesTable.selectedRow < tableModel.rowCount-1
                            }
                        }
                    }
                }
            }
            row {
                button("Delete All", deleteAllButtonHandler)
                filler(weightX = 1)
                button("Cancel", {dispose()})
                button("Apply", applyButtonHandler)
                button("OK", okButtonHandler)
            }
        }
    }
}
