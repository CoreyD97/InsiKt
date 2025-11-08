import com.coreyd97.insikt.filter.ColorizingRule
import com.coreyd97.insikt.view.colorizingdialog.ColorizingRuleDialog
import javax.swing.SwingUtilities
import com.coreyd97.insikt.filter.ASTExpression
import com.coreyd97.insikt.filter.FilterExpression
import com.coreyd97.insikt.filter.FilterLibrary
import com.coreyd97.insikt.filter.ParseResult
import com.coreyd97.insikt.filter.ParserService
import com.coreyd97.insikt.view.shared.FilterEditor
import org.mockito.kotlin.*


fun main(args: Array<String>) {

    var parserService: ParserService? = null
    val ast = mock<ASTExpression> {
        on { getFilterString() } doReturn "Example"
    }
    val expr = mock<FilterExpression> {

    }
    try{
        parserService = mock<ParserService> {
            on { parse(any()) } doReturn ParseResult(expr, listOf<String>())
        }
    }catch (e: Exception){
        e.printStackTrace()
    }

//    val injector = Guice.createInjector(InsiktModule(montoyaStub()))
//    MontoyaUtilities(montoyaStub())
    FilterExpression.setParserService(parserService!!)
    SwingUtilities.invokeLater {
        val elems = mutableListOf<ColorizingRule>()
        elems.add(ColorizingRule.fromString("A", "B"))
        elems.add(ColorizingRule.fromString("A1", "B1"))
        elems.add(ColorizingRule.fromString("A2", "B2"))
        elems.add(ColorizingRule.fromString("A3", "B3"))
        val filterLibrary = mock<FilterLibrary> {}
        val parserService = mock<ParserService> {}
        val window = ColorizingRuleDialog(filterLibrary, FilterEditor(parserService), "Test", "Testing", null, elems, {})
//        window.setSize(400, 300)

        window.setLocationRelativeTo(null)
        window.isVisible = true

//        window.addMouseListener(object : MouseAdapter() {
//            override fun mouseClicked(e: MouseEvent?) {
//                if (e == null) return
//                if (e.button == MouseEvent.BUTTON1) {
//                    println("Left click at (${e.x}, ${e.y})")
//                } else if (e.button == MouseEvent.BUTTON3) {
//                    println("Right click at (${e.x}, ${e.y})")
//                    window.contentPane.removeAll()
//                    window.contentPane.add(test())
//                    window.revalidate()
//                    window.repaint()
//                }
//            }
//        })
    }

}