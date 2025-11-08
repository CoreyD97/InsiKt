package com.coreyd97.insikt.view

import com.coreyd97.insikt.logging.logentry.FieldGroup
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.montoyautilities.panelBuilder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSeparator
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

/**
 * Created by corey on 27/08/17.
 */
class HelpPanel : JPanel() {
    init {
        setup()
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    this@HelpPanel.removeAll()
                    setup()
                    revalidate()
                    repaint()
                }
            }
        })
    }

    private fun setup() {
        this.setLayout(BorderLayout())
        val fontColor: Color
        val font = this.font.family
        if (UIManager.getLookAndFeel().name.equals("Darcula", ignoreCase = true)) {
            fontColor = UIManager.getColor("darcula.textForeground")
        } else {
            fontColor = this.getForeground()
        }
        val colorHex = String.format(
            "#%02x%02x%02x", fontColor.red, fontColor.green,
            fontColor.blue
        )

        val editorKit = HTMLEditorKit()
        var doc = editorKit.createDefaultDocument() as HTMLDocument
        val styleSheet = doc.styleSheet
        styleSheet.addRule("body {font-family: " + font + "; color: " + colorHex + ";}")
        styleSheet.addRule("a {color: #7777FF; }")

        val overviewTitle = JTextPane()
        overviewTitle.putClientProperty("html.disable", null)
        overviewTitle.setContentType("text/html")
        overviewTitle.document = doc
        overviewTitle.isEditable = false
        overviewTitle.text = ("<body><h1>InsiKt</h1>" +
                "InsiKt was developed as an alternative to the log history included within Burp Suite. " +
                "Advantages over the original implementation are a more comprehensive number of fields, " +
                "the ability to show only specific entries to better monitor activity via the use of adaptable "
                +
                "filters from various fields and row coloring to highlight interesting entries which match a specific filter.</body>")

        val overviewPane = JTextPane()
        overviewPane.putClientProperty("html.disable", null)
        overviewPane.setContentType("text/html")
        overviewPane.isEditable = false
        doc = editorKit.createDefaultDocument() as HTMLDocument
        doc.styleSheet.addStyleSheet(styleSheet)
        overviewPane.document = doc
        overviewPane.text = ("<body><h2>Creating Filters</h2>" +
                "Filters were developed with the intention of being highly customisable and therefore may be "
                +
                "as simple or complex as you require. Once a filter has been entered, the color of the input field "
                +
                "will change to reflect the validity of the filter.<br />" +
                "<br/>" +
                "Basic filters take the form: <b>Field Operation Value</b><br>" +
                "See <a href=\"#aliases\">Aliases</a> for info on defining reusable filter snippets which can be used in log and color filters."
                +
                "<h3>Filter Fields</h3>" +
                "With the new parser, fields have been separated into groups. A list of fields and their group can be found to the right.<br />"
                +
                "<i>E.g. Request.Method</i><br />" +
                "<h3>Basic operations</h3>" +
                "Comparative operation to be evaluated.<br>" +
                "<br>" +
                "== - Equal, valid on all fields.<br>" +
                "!= - Not Equal, valid on all fields.<br>" +
                "&lt; - Less Than, only valid on numeric fields (Integer, Short, Date, ...)<br>" +
                "&gt; - Greater Than, only valid on numeric fields (Integer, Short, Date, ...)<br>" +
                "&lt;= - Less Than Or Equal, only valid on numeric fields (Integer, Short, Date, ...)<br>" +
                "&gt;= - Greater Than Or Equal, only valid on numeric fields (Integer, Short, Date, ...)<br>"
                +
                "<br />" +
                "<h3>Special operations</h3>" +
                "CONTAINS - True if value is found anywhere in the string<br>" +
                "<ul><li>Request.Body <b>CONTAINS</b> \"SEARCHVALUE\"</li></ul>" +
                "IN - True if value is found within the provided array<br>" +
                "<ul><li>Response.InferredType <b>IN</b> [\"JSON\", \"HTML\", \"XML\"]</li></ul>" +
                "MATCHES - True if value matches the provided regular expression<br>" +
                "Note: <i>The matches operation expects the entire string to match the expression. <br>" +
                "See <a href=\"#regex\">Regular Expressions</a> for more info on regular expressions.</i>" +
                "<ul><li>Request.Path <b>MATCHES</b> \"/api/(account|payments)/.*\"</li></ul>" +
                "<br>" +
                "<h3>Compound Operations</h3>" +
                "Multiple filters can be combined into compound filters using the following operators.<br>"
                +
                "<br>" +
                "&&, AND - True only if all components evaluate to true.<br>" +
                "||, OR - True if any one of the components evaluates to true.<br>" +
                "^, XOR - True only if components differ in evaluation result.<br>" +
                "<br>" +
                "Note: <i>Compound operations cannot be mixed without explicitly specifying order of precedence using parenthesis</i><br>"
                +
                "Request.Body == \"A\" AND Response.Status == 200 OR Response.Status == 302 - <b>Invalid</b><br>"
                +
                "Request.Body == \"A\" AND ( Response.Status == 200 OR Response.Status == 302 ) - <b>Valid</b><br>"
                +
                "<br />" +
                "<h3>Expression Negation</h3>" +
                "Sometimes, you may wish to match only entries which do not match a certain filter.<br>" +
                "This can be achieved by wrapping the section of the filter to negate like follows.<br>" +
                "<ul><li><b>!(</b> Request.Body CONTAINS \"CSRF\" <b>)</b></li></ul>" +
                "<br>" +
                "<a name=\"regex\"><h3>Regular Expressions</h3>" +
                "In addition to the MATCHES operator, regular expressions can also be used with the == and != operators.<br>"
                +
                "To do so simply wrap the regular expression with forward slashes like /regex/<br />" +
                "<ul><li>Request.QueryParams == <b>/</b>u?id=0<b>/</b></li></ul><br />" +
                "<hr>" +
                "<a name=\"aliases\"><h2>Aliases</h2>" +
                "Aliases are reusable filter snippets which can be used in both log and color filters.<br>"
                +
                "For example, you may wish to define an alias for POST requests without a CSRF parameter.<br>"
                +
                "The \"Filter Library\" tab of the extension can be used to view and manage the available aliases,<br>"
                +
                "to add an alias, click the 'Add Snippet' button at the bottom of the panel, add your snippet and set an appropriate alias for it.<br>"
                +
                "<br>" +
                "To use a defined alias in a log or color filter, prefix the alias name with the # symbol as shown in the below examples."
                +
                "<ul>" +
                "<li><b>#ALIAS_NAME</b></li>" +
                "<li>Response.Status == 200 OR <b>#ALIAS_NAME</b></li>" +
                "<li>!( <b>#ALIAS_NAME</b> )</li>" +
                "</ul>" +
                "<br>" +
                "<hr>" +
                "<h2>Color Filters</h2>" +
                "In addition to standard filters, color filters can be set by clicking the 'Colorize' button in the main tab.<br>"
                +
                "To add a filter press the add button and enter a filter as above, and optionally set the title, foreground and background colors.<br>"
                +
                "Changes can be observed instantly.<br />" +
                "<br>" +
                "<hr>" +
                "<h2>Tips and Tricks</h2>" +
                "<ul>" +
                "<li>Filters can be generated by right clicking a log entry field, or right clicking within a request / response viewer with selected text.</li>"
                +
                "<li>Right-clicking in the main filter text box will show a dropdown list of the available fields to be used.</li>"
                +
                "</ul></body>")
        overviewPane.addHyperlinkListener(HyperlinkListener { e: HyperlinkEvent? ->
            if (e!!.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.description.startsWith("#")) {
                    overviewPane.scrollToReference(e.description.substring(1))
                }
            }
        })

        val fieldTitle = JTextPane()
        fieldTitle.putClientProperty("html.disable", null)
        fieldTitle.setContentType("text/html")
        fieldTitle.isEditable = false
        doc = editorKit.createDefaultDocument() as HTMLDocument
        doc.styleSheet.addStyleSheet(styleSheet)
        fieldTitle.document = doc
        fieldTitle.text = "<body><h1>Filter Fields</h1>" +
                "A number of fields are available to use from the requests within your filters. These are listed below.<body>"

        val fieldPane = JTextPane()
        fieldPane.putClientProperty("html.disable", null)
        fieldPane.setContentType("text/html")
        fieldPane.isEditable = false
        doc = editorKit.createDefaultDocument() as HTMLDocument
        doc.styleSheet.addStyleSheet(styleSheet)
        fieldPane.document = doc
        fieldPane.text = "<body>" + getFormattedFields(FieldGroup.REQUEST) +
                getFormattedFields(FieldGroup.RESPONSE) +
                getFormattedFields(FieldGroup.ENTRY) + "</body>"

        val overviewScroll = JScrollPane(overviewPane)
        val fieldScroll = JScrollPane(fieldPane)

        val panel = panelBuilder {
            column {
                add(overviewTitle)
                gbc.weighty = 1.0
                add(overviewScroll)
            }
            separator(JSeparator.VERTICAL)
            column {
                add(fieldTitle)
                gbc.weighty = 1.0
                add(fieldScroll)
            }
        }

        this.add(panel, BorderLayout.CENTER)
    }

    private fun getFormattedFields(fieldGroup: FieldGroup): String {
        val output = StringBuilder()
        val fields = ArrayList<LogEntryField?>(LogEntryField.getFieldsInGroup(fieldGroup))
        for (i in fields.indices) {
            if (i != 0) {
                output.append("<br><br>")
            }
            output.append(fields.get(i)!!.descriptiveMessage.replace("\n".toRegex(), "<br>"))
        }
        return String.format("<h3>Group: %s</h3><hr>%s<br><br>", fieldGroup.label, output)
    }
}