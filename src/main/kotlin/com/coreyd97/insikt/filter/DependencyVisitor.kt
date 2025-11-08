package com.coreyd97.insikt.filter

import com.coreyd97.insikt.logging.logentry.FieldGroup
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.google.inject.Inject
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.stream.Collectors

class DependencyVisitor(
    val filterLibrary: FilterLibrary,
    val checkAliasesExist: Boolean = true,
) : FilterParserVisitor {
    private val log: Logger = LogManager.getLogger(DependencyVisitor::class.java)


    fun defaultVisit(node: SimpleNode, data: VisitorData): VisitorData {
        node.childrenAccept(this, data)
        return data
    }

    override fun visit(node: SimpleNode, data: VisitorData): VisitorData {
        return defaultVisit(node, data)
    }

    fun visit(node: SimpleNode): Results {
        val visitorData = VisitorData()
        val dependencies = HashSet<String>()
        val contexts = HashSet<FieldGroup>()
        visitorData.setData("dependencies", dependencies)
        visitorData.setData("contexts", contexts)
        val visitStack = Stack<String>()
        visitorData.setData("aliasVisitList", visitStack)
        visit(node, visitorData)
        return Results(dependencies, contexts, visitorData.errors)
    }

    override fun visit(node: ASTExpression, data: VisitorData): VisitorData {
        return defaultVisit(node, data)
    }

    override fun visit(node: ASTComparison, visitorData: VisitorData): VisitorData {
        val contexts = visitorData.data.get("contexts") as HashSet<FieldGroup>
        if (node.left is LogEntryField) {
            contexts.add((node.left as LogEntryField).fieldGroup)
        }
        if (node.right is LogEntryField) {
            contexts.add((node.right as LogEntryField).fieldGroup)
        }
        defaultVisit(node, visitorData)
        return visitorData
    }

    override fun visit(node: ASTStringSearch, data: VisitorData?): Any? {
        return data
    }

    override fun visit(node: ASTAlias, data: VisitorData): VisitorData {
        //Add this alias to our dependencies
        val aliasVisitList = data.data.get("aliasVisitList") as Stack<String>
        try {
            log.debug("Visiting " + node.identifier)
            if (aliasVisitList.contains(node.identifier.uppercase(Locale.getDefault()))) {
                //We're recursing, don't continue!
                aliasVisitList.push(node.identifier.uppercase(Locale.getDefault()))
                val visitOrder = aliasVisitList.stream().collect(Collectors.joining("->"))
                data.addError("Recursion detected in filter. Alias trace: " + visitOrder)
                return data
            }

            aliasVisitList.push(node.identifier.uppercase(Locale.getDefault()))
            log.debug("Current Visit Queue " + aliasVisitList)

            (data.data.get("dependencies") as HashSet<String>).add(
                node.identifier.uppercase(
                    Locale.getDefault()
                )
            )

            if(checkAliasesExist) {
                //Now sanity check on the aliased filter with our existing data
                val aliasedFilter = filterLibrary.snippets
                    .stream().filter { savedFilter: FilterRule? ->
                        savedFilter!!.name.equals(
                            node.identifier,
                            ignoreCase = true
                        )
                    }
                    .findFirst()
                if (aliasedFilter.isPresent) {
                    visit(aliasedFilter.get().expression!!.astExpression, data)
                } else {
                    data.addError("Could not find a filter in the library for alias: " + node.identifier)
                }
            }

            return data
        } finally {
            log.debug("Leaving " + node.identifier)
            aliasVisitList.pop()
            log.debug("Current Visit Queue " + aliasVisitList)
        }
    }

    data class Results(val snippets : HashSet<String>, val contexts: HashSet<FieldGroup>, val errors: List<String>)
}
