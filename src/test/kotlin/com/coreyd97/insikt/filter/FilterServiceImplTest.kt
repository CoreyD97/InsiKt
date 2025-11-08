package com.coreyd97.insikt.filter

import com.coreyd97.insikt.logging.logentry.LogEntry
import com.coreyd97.insikt.logging.logentry.LogEntryField
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.*

class FilterServiceImplTest {

    private lateinit var parserService: ParserService
    private lateinit var library: FilterLibrary
    private lateinit var evaluator: FilterEvaluationVisitor
    private lateinit var entry: LogEntry

    @BeforeEach
    fun setup() {
        // For these tests, we evaluate directly using FilterEvaluationVisitor and only need a mocked service
        parserService = mock()
        library = mock()
        evaluator = FilterEvaluationVisitor(library)
        entry = mock()

        // Default stubs for commonly used fields
        stubField(LogEntryField.METHOD, "GET")
        stubField(LogEntryField.PATH, "/api/v1/items")
        stubField(LogEntryField.QUERY, "id=123&token=abc")
        stubField(LogEntryField.STATUS, 200)
        stubField(LogEntryField.PORT, 443)
        stubField(LogEntryField.ENTRY_INSCOPE, true)
        stubField(LogEntryField.INSCOPE, true)
        stubField(LogEntryField.REQUEST_BODY, "username=admin&password=secret")
        stubField(LogEntryField.RESPONSE_BODY, "<html><title>OK</title></html>")
        stubField(LogEntryField.RESPONSE_LENGTH, 1024)
        stubField(LogEntryField.REQUEST_TIME, Date(1715027400000)) // 2024/05/06 12:30:00
        stubField(LogEntryField.RESPONSE_TIME, Date(1715027405000))

        // String search uses request/response raw bytes
        Mockito.`when`(entry.requestBytes).thenReturn("This is the request body containing csrf token".toByteArray())
        Mockito.`when`(entry.responseBytes).thenReturn("The response mentions success".toByteArray())
    }

    private fun stubField(field: LogEntryField, value: Any?) {
        // Match on the specific field and any boolean flag
        whenever(entry.getValueByKey(org.mockito.kotlin.eq(field), any())).thenReturn(value)
    }

    private fun expr(filter: String): FilterExpression {
        val res = parserService.parse(filter)
        assertNotNull(res.expression, "Parse should succeed for valid test setup: $filter. Errors: ${'$'}{res.errors}")
        return res.expression!!
    }

    @Nested
    inner class Basic {
        @Test
        fun equalsAndOrdering() {
            assertTrue(evaluator.visit(expr("Response.Status == 200").astExpression, entry))
            assertTrue(evaluator.visit(expr("Response.Status >= 199").astExpression, entry))
            assertTrue(evaluator.visit(expr("Response.Status <= 200").astExpression, entry))
            assertFalse(evaluator.visit(expr("Response.Status > 200").astExpression, entry))
            // Identifier to identifier numeric comparison
            assertTrue(evaluator.visit(expr("Response.Status <= Request.Port").astExpression, entry))
        }

        @Test
        fun stringContainsAndRegex() {
            assertTrue(evaluator.visit(expr("Request.Body CONTAINS 'password'").astExpression, entry))
            assertTrue(evaluator.visit(expr("Request.Path MATCHES '/api/.*/items'").astExpression, entry))
            assertTrue(evaluator.visit(expr("Request.Query == /id=\\d+&token=abc/").astExpression, entry))
        }

        @Test
        fun booleanEvaluation() {
            // Explicit comparison
            assertTrue(evaluator.visit(expr("Request.InScope == TRUE").astExpression, entry))
            assertTrue(evaluator.visit(expr("Entry.InScope == true").astExpression, entry))
            // No-operator boolean (grammar allows evaluating as boolean)
            val res = parserService.parse("Request.InScope")
            if (res.expression != null && res.errors.isEmpty()) {
                assertTrue(evaluator.visit(res.expression!!.astExpression, entry))
            }
        }
    }

    @Nested
    inner class CollectionsAndArrays {
        @Test
        fun inOperatorWithNumbersAndStrings() {
            assertTrue(evaluator.visit(expr("Response.Status IN [200,302,404]").astExpression, entry))
            assertTrue(evaluator.visit(expr("Request.Method IN ['GET','POST']").astExpression, entry))
            assertFalse(evaluator.visit(expr("Request.Method IN ['PUT','DELETE']").astExpression, entry))
        }
    }

    @Nested
    inner class CompoundAndNegation {
        @Test
        fun andOrXor() {
            assertTrue(evaluator.visit(expr("Request.Method == 'GET' AND Response.Status == 200").astExpression, entry))
            assertTrue(evaluator.visit(expr("Request.Method == 'GET' OR Response.Status == 500").astExpression, entry))
            assertFalse(evaluator.visit(expr("Request.Method == 'POST' AND Response.Status == 200").astExpression, entry))
            // XOR: true xor false -> true; chaining maintained by visitor
            assertTrue(evaluator.visit(expr("(Request.Method == 'GET') XOR (Response.Status == 500)").astExpression, entry))
            assertFalse(evaluator.visit(expr("(Request.Method == 'GET') XOR (Response.Status == 200)").astExpression, entry))
            assertTrue(evaluator.visit(expr("(Request.Method == 'POST') XOR (Response.Status == 200)").astExpression, entry))
        }

        @Test
        fun negation() {
            assertTrue(evaluator.visit(expr("!( Request.Method == 'POST' )").astExpression, entry))
        }
    }

    @Nested
    inner class StringSearchMode {
        @Test
        fun bareStringSearchAgainstRequestAndResponse() {
            assertTrue(evaluator.visit(expr("'csrf'").astExpression, entry)) // present in request bytes
            assertTrue(evaluator.visit(expr("'success'").astExpression, entry)) // present in response bytes
            assertFalse(evaluator.visit(expr("'does-not-exist'").astExpression, entry))
        }
    }
}
