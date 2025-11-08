package com.coreyd97.insikt.filter

import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.google.inject.Inject
import org.apache.commons.lang3.time.DateUtils
import java.math.BigDecimal
import java.util.*
import java.util.regex.Pattern


class FilterEvaluationVisitor @Inject constructor(
    private val filterLibrary: FilterLibrary
): FilterParserVisitor {
    override fun visit(node: SimpleNode, data: VisitorData): Boolean {
        return false
    }

    fun visit(node: ASTExpression, logEntry: LogEntry?): Boolean {
        val visitorData = VisitorData()
        visitorData.setData(LOG_ENTRY, logEntry)
        return visit(node, visitorData)
    }

    override fun visit(node: ASTExpression, visitorData: VisitorData): Boolean {
        val firstNode = node.children[0]
        var result = evaluateNode(firstNode, visitorData)

        if (node.op != null) {
            run {
                val op = node.op
                for (i in 1 until node.children.size) {
                    //If we're processing an OR expression and the value is true.
                    //Or we're processing an AND expression and the value was false. Don't bother evaluating the other nodes.
                    if ((op == LogicalOperator.OR && result) || (op == LogicalOperator.AND && !result)) break

                    val child = node.children[i]
                    val childResult = evaluateNode(child, visitorData)

                    result = when (op) {
                        LogicalOperator.AND, LogicalOperator.OR -> {
                            childResult
                        }

                        LogicalOperator.XOR -> {
                            result xor childResult
                        }
                    }
                }
            }
        }

        return result xor node.inverse
    }

    override fun visit(node: ASTComparison, visitorData: VisitorData): Boolean {
        //Must pull the value from the entry for fields, otherwise the node itself is the value.
        val left = if (node.left is LogEntryField) getValueForField(
            visitorData,
            node.left as LogEntryField
        ) else node.left
        val right = if (node.right is LogEntryField) getValueForField(
            visitorData,
            node.right as LogEntryField
        ) else node.right

        return compare(node.comparisonOperator, left, right)
    }

    override fun visit(node: ASTStringSearch, data: VisitorData): Boolean {
        val entry: LogEntry = data.data.get(LOG_ENTRY) as LogEntry
        val haystack = node.searchValue
        return Collections.indexOfSubList(entry.requestBytes.asList(), haystack.toByteArray().asList()) != -1
                || Collections.indexOfSubList(entry.responseBytes?.asList().orEmpty(), haystack.toByteArray().asList()) != -1
    }

    private fun getValueForField(visitorData: VisitorData, field: LogEntryField): Any? {
        return (visitorData.data[LOG_ENTRY] as LogEntry?)!!.getValueByKey(field, true)
    }

    override fun visit(node: ASTAlias, data: VisitorData): Boolean {
        for (savedFilter in filterLibrary.snippets) {
            if (node.identifier.equals(savedFilter.name, ignoreCase = true)) {
                return visit(savedFilter.expression!!.astExpression, data)
            }
        }
        return false
    }

    private fun evaluateNode(node: Node, visitorData: VisitorData): Boolean {
        if (node is ASTExpression) return visit(node, visitorData)
        else if (node is ASTComparison) return visit(node, visitorData)
        else if (node is ASTAlias) return visit(node, visitorData)
        else if (node is ASTStringSearch) return visit(node, visitorData)
        else {
            visitorData.addError("Node was not an expression or comparison. This shouldn't happen!")
            return false
        }
    }

    private fun compareNumbers(op: ComparisonOperator, left: Number, right: Number): Boolean {
        //Numerical Comparison
        val leftBigDecimal = BigDecimal(left.toString())
        val rightBigDecimal = BigDecimal(right.toString())
        return when (op) {
            ComparisonOperator.EQUAL -> leftBigDecimal.compareTo(rightBigDecimal) == 0
            ComparisonOperator.NOT_EQUAL -> leftBigDecimal.compareTo(rightBigDecimal) != 0
            ComparisonOperator.GREATER_THAN -> leftBigDecimal.compareTo(rightBigDecimal) > 0
            ComparisonOperator.LESS_THAN -> leftBigDecimal.compareTo(rightBigDecimal) < 0
            ComparisonOperator.GREATER_THAN_EQUAL -> leftBigDecimal.compareTo(rightBigDecimal) >= 0
            ComparisonOperator.LESS_THAN_EQUAL -> leftBigDecimal.compareTo(rightBigDecimal) <= 0
            else -> false
        }
    }

    private fun matchPattern(value: String, pattern: Pattern): Boolean {
        return pattern.matcher(value).matches()
    }

    private fun findPattern(value: String, pattern: Pattern): Boolean {
        return pattern.matcher(value).find()
    }

    private fun compareDate(op: ComparisonOperator, left: Date, right: Date): Boolean {
        val rightDate = DateUtils.truncate(right, Calendar.SECOND)
        when (op) {
            ComparisonOperator.EQUAL -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) == 0

            ComparisonOperator.NOT_EQUAL -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) != 0

            ComparisonOperator.GREATER_THAN -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) > 0

            ComparisonOperator.LESS_THAN -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) < 0

            ComparisonOperator.GREATER_THAN_EQUAL -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) >= 0

            ComparisonOperator.LESS_THAN_EQUAL -> return DateUtils.truncate(
                left as Date?,
                Calendar.SECOND
            ).compareTo(rightDate) <= 0

            else -> return false
        }
    }

    private fun inList(needle: String, haystack: Collection<*>): Boolean {
        return haystack.any {
            it.toString().equals(needle, ignoreCase = true)
        }
    }

    private fun compare(op: ComparisonOperator, left: Any?, right: Any?): Boolean {
        return when {
            op == ComparisonOperator.IN && right is String -> right.contains(left.toString())
            op == ComparisonOperator.IN && right is Collection<*> -> inList(left.toString(), right)
            op == ComparisonOperator.CONTAINS && left is String -> left.contains(right.toString())
            op == ComparisonOperator.CONTAINS && left is Collection<*> -> inList(right.toString(), left)

            left is Number && right is Number -> compareNumbers(op, left, right)

            op == ComparisonOperator.MATCHES -> matchPattern(left.toString(), right as Pattern)
            right is Pattern -> matchPattern(left.toString(), right) xor (op == ComparisonOperator.NOT_EQUAL)
            left is Date -> compareDate(op, left, right as Date)
            op == ComparisonOperator.EQUAL -> left.toString().equals(right.toString(), ignoreCase = true)
            op == ComparisonOperator.NOT_EQUAL -> !left.toString().equals(right.toString(), ignoreCase = true)

            else -> false
        }
    }

//    private fun compare(op: ComparisonOperator, left: Any?, right: Any?): Boolean {
//        var left = left
//        var right = right
//        if (left == null) left = ""
//        if (right == null) right = ""
//        try {
//            if (Number::class.java.isAssignableFrom(left.javaClass) && Number::class.java.isAssignableFrom(
//                    right.javaClass
//                )
//            ) {
//                //Numerical Comparison
//                val leftBigDecimal = BigDecimal(left.toString())
//                val rightBigDecimal = BigDecimal(right.toString())
//                when (op) {
//                    ComparisonOperator.EQUAL -> return leftBigDecimal.compareTo(rightBigDecimal) == 0
//                    ComparisonOperator.NOT_EQUAL -> return leftBigDecimal.compareTo(rightBigDecimal) != 0
//                    ComparisonOperator.GREATER_THAN -> return leftBigDecimal.compareTo(
//                        rightBigDecimal
//                    ) > 0
//
//                    ComparisonOperator.LESS_THAN -> return leftBigDecimal.compareTo(rightBigDecimal) < 0
//                    ComparisonOperator.GREATER_THAN_EQUAL -> return leftBigDecimal.compareTo(
//                        rightBigDecimal
//                    ) >= 0
//
//                    ComparisonOperator.LESS_THAN_EQUAL -> return leftBigDecimal.compareTo(
//                        rightBigDecimal
//                    ) <= 0
//                    else -> return false
//                }
//            } else if (op == ComparisonOperator.MATCHES) {
//                val m = (right as Pattern).matcher(left.toString())
//                return m.matches()
//            } else if (right is Pattern) {
//                val m = right.matcher(left.toString())
//                return m.find() xor (op == ComparisonOperator.NOT_EQUAL)
//            } else if (left is Date) {
//                try {
//                    val rightDate = DateUtils.truncate(right, Calendar.SECOND)
//                    when (op) {
//                        ComparisonOperator.EQUAL -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) == 0
//
//                        ComparisonOperator.NOT_EQUAL -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) != 0
//
//                        ComparisonOperator.GREATER_THAN -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) > 0
//
//                        ComparisonOperator.LESS_THAN -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) < 0
//
//                        ComparisonOperator.GREATER_THAN_EQUAL -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) >= 0
//
//                        ComparisonOperator.LESS_THAN_EQUAL -> return DateUtils.truncate(
//                            left as Date?,
//                            Calendar.SECOND
//                        ).compareTo(rightDate) <= 0
//                        else -> return false
//                    }
//                } catch (e: Exception) {
//                    return false
//                }
//            } else if (op == ComparisonOperator.IN) {
//                //Request.Host IN ["https://twitter.com", "https://google.com"]
//                val leftString = left.toString()
//                for (item in right as Collection<*>) {
//                    if (leftString.equals(item.toString(), ignoreCase = true)) return true
//                }
//                return false
//            } else if (op == ComparisonOperator.CONTAINS) {
//                //Request.Parameters CONTAINS "A"
//                val finalRight: Any = right
//                return if (MutableCollection::class.java.isAssignableFrom(left.javaClass)) {
//                    (left as Collection<*>).stream().anyMatch { o ->
//                        o.toString().equals(finalRight.toString(), ignoreCase = true)
//                    }
//                } else {
//                    StringUtils.containsIgnoreCase(left.toString(), right.toString())
//                }
//            } else if (left is String || right is String) { //String comparison last.
//                return left.toString().equals(
//                    right.toString(),
//                    ignoreCase = true
//                ) xor (op != ComparisonOperator.EQUAL)
//            } else {
//                when (op) {
//                    ComparisonOperator.EQUAL -> return left == right
//                    ComparisonOperator.NOT_EQUAL -> return left != right
//                    else -> return false
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            return false
//        }
//
//        return false
//    }

    companion object {
        private const val LOG_ENTRY = "logEntry"
    }
}