package com.coreyd97.insikt

import burp.api.montoya.http.message.HttpMessage
import burp.api.montoya.ui.contextmenu.ContextMenuEvent
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider
import burp.api.montoya.ui.contextmenu.InvocationType
import com.coreyd97.insikt.filter.ColorizingRule
import com.coreyd97.insikt.filter.ComparisonOperator
import com.coreyd97.insikt.filter.FilterRule
import com.coreyd97.insikt.filter.LogicalOperator
import com.coreyd97.insikt.filter.TableFilterService
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.coreyd97.insikt.view.logtable.LogTable
import com.coreyd97.insikt.view.logtable.LogView
import com.coreyd97.insikt.util.APP_NAME
import com.coreyd97.insikt.util.PREF_COLOR_FILTERS
import com.coreyd97.montoyautilities.PreferenceProxy
import com.google.inject.Inject
import com.google.inject.Singleton
import org.apache.commons.text.StringEscapeUtils
import java.awt.Component
import java.awt.event.ActionEvent
import java.util.*
import javax.swing.AbstractAction
import javax.swing.JMenu
import javax.swing.JMenuItem

@Singleton
class LoggerContextMenuFactory @Inject constructor(
    var logView: LogView,
    val filterService: TableFilterService
) : ContextMenuItemsProvider {

    override fun provideMenuItems(event: ContextMenuEvent): List<Component> {
        val filterMenu: JMenuItem = JMenu(APP_NAME)

        //We're handling a message editor context menu
        //And we have a selection
        val requestResponse = event.messageEditorRequestResponse().orElseThrow()
        val selectedRange = requestResponse.selectionOffsets().orElseThrow()
        val target: HttpMessage

        val context: LogEntryField
        var selectedBytes: ByteArray
        when (event.invocationType()) {
            InvocationType.MESSAGE_EDITOR_REQUEST, InvocationType.MESSAGE_VIEWER_REQUEST -> {
                target = requestResponse.requestResponse().request()
                context = if (selectedRange.startIndexInclusive() <= target.bodyOffset()) {
                    LogEntryField.REQUEST_HEADERS
                } else {
                    LogEntryField.REQUEST_BODY
                }
                selectedBytes = target.toByteArray().bytes.copyOfRange(
                    selectedRange.startIndexInclusive(),
                    selectedRange.endIndexExclusive()
                )
            }

            InvocationType.MESSAGE_EDITOR_RESPONSE, InvocationType.MESSAGE_VIEWER_RESPONSE -> {
                target = requestResponse.requestResponse().response()
                context = if (selectedRange.startIndexInclusive() <= target.bodyOffset()) {
                    LogEntryField.RESPONSE_HEADERS
                } else {
                    LogEntryField.RESPONSE_BODY
                }
                selectedBytes = target.toByteArray().bytes.copyOfRange(
                    selectedRange.startIndexInclusive(),
                    selectedRange.endIndexExclusive()
                )
            }

            else -> {
                return emptyList()
            }
        }
        val selectedText = StringEscapeUtils.escapeJava(String(selectedBytes))

        val useAsFilter = JMenuItem(object : AbstractAction("Use Selection As LogFilter") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                filterService.setFilter(
                    FilterRule.fromString(
                        context.fullLabel + " CONTAINS \"" + selectedText + "\""
                    )
                )
            }
        })

        filterMenu.add(useAsFilter)

        if (filterService.activeFilter() != null) {
            val currentFilter = filterService.activeFilter()!!
            val addToCurrentFilter = JMenu("Add Selection To LogFilter")
            //TODO Expand with more options
            val andFilter = JMenuItem(object : AbstractAction("AND Contains") {
                override fun actionPerformed(actionEvent: ActionEvent) {

                    val newFilter = currentFilter.withAddedCondition(
                        LogicalOperator.AND, context, ComparisonOperator.CONTAINS, selectedText
                    )
                    require(newFilter.isValid) { "The constructed filter is invalid. ${newFilter.errors.joinToString(", ")}"}
                    filterService.setFilter(newFilter)
                }
            })

            val andNotFilter = JMenuItem(object : AbstractAction("AND Not Containing") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    val addition = FilterRule.fromString("!(${context.fullLabel} CONTAINS \"${selectedText}\"")
                    val newFilter = currentFilter.withAddedCondition(
                        LogicalOperator.AND, addition
                    )
                    require(newFilter.isValid) { "The constructed filter is invalid. ${newFilter.errors.joinToString(", ")}" }
                    filterService.setFilter(newFilter)
                }
            })

            val orFilter = JMenuItem(object : AbstractAction("OR") {
                override fun actionPerformed(actionEvent: ActionEvent) {
                    val newFilter = currentFilter.withAddedCondition(
                        LogicalOperator.OR, context, ComparisonOperator.CONTAINS, selectedText
                    )
                    require(newFilter.isValid) { "The constructed filter is invalid. ${newFilter.errors.joinToString(", ")}"}
                    filterService.setFilter(newFilter)
                }
            })
            addToCurrentFilter.add(andFilter)
            addToCurrentFilter.add(andNotFilter)
            addToCurrentFilter.add(orFilter)
            filterMenu.add(addToCurrentFilter)
        }

        val colorFilterItem = JMenuItem(object : AbstractAction("Set Selection as Color Filter") {
            override fun actionPerformed(actionEvent: ActionEvent) {
                val tableColorRule =
                    ColorizingRule.fromString(
                        "New Filter",
                        context.fullLabel + " CONTAINS \"" + selectedText + "\""
                    )
                val colorFilters by PreferenceProxy<MutableSet<ColorizingRule>>(PREF_COLOR_FILTERS)
                colorFilters.add(tableColorRule)
                //TODO Make dialog visible
            }
        })
        filterMenu.add(colorFilterItem)
        return listOf(filterMenu)
    }
}
