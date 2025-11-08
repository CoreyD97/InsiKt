import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JTable
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.AbstractCellEditor
import javax.swing.JDialog
import javax.swing.JPopupMenu
import javax.swing.SwingUtilities

open class ButtonCell<T>(
    private val labelProvider: (row: Int, column: Int) -> String,
    private val targetResolver: (table: JTable, viewRow: Int, viewColumn: Int) -> T,
    private val onClick: (target: T, table: JTable, row: Int, column: Int) -> Unit
) : AbstractCellEditor(), TableCellRenderer, TableCellEditor {

    private val button = JButton()
    private var tableRef: JTable? = null
    private var rowRef = -1
    private var colRef = -1

    private val clickListener = ActionListener {
        val table = tableRef ?: return@ActionListener
        fireEditingStopped() // keep table state consistent
        val target = targetResolver(table, rowRef, colRef)
        onClick(target, table, rowRef, colRef)
    }

    init {
        button.isFocusable = false
        button.addActionListener(clickListener)
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        button.text = labelProvider(row, column)
        button.isEnabled = table.isCellEditable(row, column)
        return button
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        tableRef = table
        rowRef = row
        colRef = column
        button.text = labelProvider(row, column)
        return button
    }

    override fun getCellEditorValue(): Any? = null
}

/**
 * A ButtonCell that shows a JPopupMenu when clicked.
 *
 * menuFactory builds a popup menu for the resolved target and cell context.
 * The menu is invoked at the button location within the table.
 */
class MenuButtonCell<T>(
    labelProvider: (row: Int, column: Int) -> String,
    targetResolver: (table: JTable, viewRow: Int, viewColumn: Int) -> T,
    private val menuFactory: (target: T, table: JTable, row: Int, column: Int) -> JPopupMenu
) : ButtonCell<T>(
    labelProvider,
    targetResolver,
    onClick = { target, table, row, column ->
        // Ensure editor is stopped so the popup doesn't conflict with editing lifecycle
        (table.cellEditor as? AbstractCellEditor)?.stopCellEditing()

        val popup = menuFactory(target, table, row, column)

        // Prefer showing at the button bounds inside the table cell.
        val cellRect = table.getCellRect(row, column, true)
        val inv = SwingUtilities.convertRectangle(table.parent, cellRect, table)
        popup.show(table, inv.x + (cellRect.width / 2) - (popup.width / 2), inv.y + inv.height)
    }
)