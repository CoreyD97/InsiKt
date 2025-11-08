package com.coreyd97.insikt.view.shared

import java.awt.Color
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.TableCellEditor

//https://docs.oracle.com/javase/tutorial/uiswing/components/table.html
class ColorEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
    var currentColor: Color? = null
    var button: JButton
    var colorChooser: JColorChooser
    var dialog: JDialog

    init {
        button = JButton()
        button.actionCommand = EDIT
        button.addActionListener(this)
        button.setBorderPainted(false)

        //Set up the dialog that the button brings up.
        colorChooser = JColorChooser()
        dialog = JColorChooser.createDialog(
            button,
            "Pick a Color",
            true,  //modal
            colorChooser,
            this,  //OK button handler
            null
        ) //no CANCEL button handler
    }

    override fun actionPerformed(e: ActionEvent) {
        if (EDIT == e.actionCommand) {
            //The user has clicked the cell, so
            //bring up the dialog.
            button.setBackground(currentColor)
            colorChooser.color = currentColor
            dialog.isVisible = true

            fireEditingStopped() //Make the renderer reappear.
        } else { //User pressed dialog's "OK" button.
            currentColor = colorChooser.color
        }
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    override fun getCellEditorValue(): Any? {
        return currentColor
    }

    //Implement the one method defined by TableCellEditor.
    override fun getTableCellEditorComponent(
        table: JTable?, value: Any?, isSelected: Boolean,
        row: Int, column: Int
    ): Component {
        currentColor = value as Color?
        return button
    }

    companion object {
        protected const val EDIT: String = "edit"
    }
}