package com.coreyd97.insikt.reflection

import burp.api.montoya.http.message.params.HttpParameter
import com.coreyd97.insikt.reflection.filter.BlacklistFilter
import com.coreyd97.insikt.reflection.filter.LengthFilter
import com.coreyd97.insikt.reflection.filter.ParameterFilter
import com.coreyd97.insikt.reflection.transformer.*
import com.coreyd97.insikt.view.shared.BooleanRenderer
import com.coreyd97.insikt.view.shared.ButtonRenderer
import com.coreyd97.montoyautilities.KPanel
import com.coreyd97.montoyautilities.panelBuilder
import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.name.Named
import java.awt.BorderLayout
import java.awt.Dialog
import java.awt.Window
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.table.DefaultTableModel

class ReflectionService @Inject constructor(
    @param:Named("extension") val extensionWindow: Provider<Window>
) {
    var filterList = mutableListOf<ParameterFilter>()
    var transformerList = mutableListOf<ParameterValueTransformer>()

    init {
        filterList.add(BlacklistFilter())
        filterList.add(LengthFilter())

        transformerList.add(HexEncodeTransformer())
        transformerList.add(URLEncodeTransformer())
        transformerList.add(URLDecodeTransformer())
        transformerList.add(Base64EncodeTransformer())
        transformerList.add(Base64DecodeTransformer())
        transformerList.add(HTMLEscapeTransformer())
        transformerList.add(HTMLUnescapeTransformer())
        transformerList.add(JsonEscapeTransformer())
        transformerList.add(JsonUnescapeTransformer())
        transformerList.add(XMLEscapeTransformer())
        transformerList.add(XMLUnescapeTransformer())
    }

    fun filterParameters(allParameters: MutableList<out HttpParameter>): MutableList<HttpParameter> {
        val interestingParameters = mutableListOf<HttpParameter>()
        for (parameter in allParameters) {
            if (!isParameterFiltered(parameter)) {
                interestingParameters.add(parameter)
            }
        }
        return interestingParameters
    }

    fun isParameterFiltered(parameter: HttpParameter): Boolean {
        for (filter in filterList) {
            if (!filter.enabled) {
                continue
            }
            if (filter.isFiltered(parameter)) {
                return true
            }
        }
        return false
    }

    fun validReflection(responseBody: String, param: HttpParameter): Boolean {
        if (param.name().isEmpty() || param.value().isEmpty()) {
            return false
        }

        if (responseBody.contains(param.value())) {
            return true
        }

        for (transformer in transformerList) {
            try {
                if (transformer.enabled) {
                    val pattern = Pattern.compile(
                        "\\Q" + transformer.transform(param.value()) + "\\E",
                        Pattern.CASE_INSENSITIVE
                    )
                    if (pattern.matcher(responseBody).find()) {
                        return true
                    }
                }
            } catch (e: Exception) {
                //Transformation failed. Ignore and continue.
            }
        }
        return false
    }

    fun showFilterConfigDialog() {
        val wrapper = JPanel(BorderLayout())

        val configurationTable = JTable(FilterTableModel())
        configurationTable.setRowHeight(25)
        configurationTable.columnModel.getColumn(0).setCellRenderer(BooleanRenderer())
        configurationTable.columnModel.getColumn(1).setMinWidth(200)
        configurationTable.columnModel.getColumn(2).setCellRenderer(ButtonRenderer())
        configurationTable.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val col = configurationTable.columnAtPoint(e.getPoint())
                val row = configurationTable.rowAtPoint(e.getPoint())
                if (col == 2) {
                    filterList[row].showConfigDialog()
                }
            }
        })

        val panel = panelBuilder {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            label("Configure parameter filters for reflections:")
            add(JScrollPane(configurationTable))
        }

        wrapper.add(panel, BorderLayout.CENTER)

        val dialog = JDialog(
            extensionWindow.get(),
            "InsiKt - Reflections Filters", Dialog.ModalityType.MODELESS
        )
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)

        dialog.add(wrapper)
        dialog.pack()
        dialog.isVisible = true
    }

    fun showValueTransformerDialog() {
        val wrapper = JPanel(BorderLayout())

        val configurationTable = JTable(TransformerTableModel())
        configurationTable.setRowHeight(25)
        configurationTable.columnModel.getColumn(0).setCellRenderer(BooleanRenderer())
        configurationTable.columnModel.getColumn(1).setMinWidth(200)

        val panel = panelBuilder {
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            label("Configure parameter value transformers for reflections:")
            add(JScrollPane(configurationTable))
        }

        wrapper.add(panel, BorderLayout.CENTER)

        val dialog = JDialog(
            extensionWindow.get(),
            "InsiKt - Reflections Value Transformers", Dialog.ModalityType.MODELESS
        )
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
        dialog.add(wrapper)
        dialog.pack()
        dialog.isVisible = true
    }

    private inner class FilterTableModel : DefaultTableModel() {
        override fun getRowCount(): Int {
            return filterList.size
        }

        override fun getColumnCount(): Int {
            return 3
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            when (columnIndex) {
                0 -> return Boolean::class.java
                2 -> return JButton::class.java
                else -> return String::class.java
            }
        }

        override fun getColumnName(column: Int): String {
            when (column) {
                0 -> return "Enabled"
                1 -> return "Name"
                else -> return ""
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            when (columnIndex) {
                0 -> return filterList[rowIndex].enabled
                1 -> return filterList[rowIndex].name
                2 -> return JButton("Configure")
            }
            return ""
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == 0
        }

        override fun setValueAt(value: Any?, row: Int, column: Int) {
            if (column == 0) {
                filterList[row].enabled = (value as Boolean)
            }
        }
    }

    private inner class TransformerTableModel : DefaultTableModel() {
        override fun getRowCount(): Int {
            return transformerList.size
        }

        override fun getColumnCount(): Int {
            return 2
        }

        override fun getColumnClass(columnIndex: Int): Class<*> {
            when (columnIndex) {
                0 -> return Boolean::class.java
                else -> return String::class.java
            }
        }

        override fun getColumnName(column: Int): String {
            when (column) {
                0 -> return "Enabled"
                1 -> return "Name"
                else -> return ""
            }
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            when (columnIndex) {
                0 -> return transformerList[rowIndex].enabled
                1 -> return transformerList[rowIndex].name
                else -> return ""
            }
        }

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return column == 0
        }

        override fun setValueAt(`val`: Any?, row: Int, column: Int) {
            if (column == 0) {
                transformerList[row].enabled = (`val` as Boolean)
            }
        }
    }
}
