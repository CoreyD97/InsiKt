

@file:Suppress("SpellCheckingInspection")

package com.coreyd97.insikt.filter

import com.google.inject.Provider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

internal class FilterParserTest {

    private lateinit var filterLibraryProvider: Provider<FilterLibrary>
    private lateinit var parserService: ParserService

    @BeforeEach
    fun setUp() {
        val mockLibrary = mock(FilterLibrary::class.java)
        whenever(mockLibrary.snippets).thenReturn(mutableSetOf())

        filterLibraryProvider = Provider { mockLibrary }
        parserService = ParserServiceImpl(filterLibraryProvider)
    }

    private fun assertValid(input: String) {
        val res = parserService.parse(input)
        assertNotNull(res.expression, "Expression should be produced for: $input. Got Error: ${res.errors.joinToString()}")
        assertTrue(res.errors.isEmpty(), "Expected no errors for: $input, but got: ${res.errors}")
        // Round-trip toString should produce a non-empty filter string
        assertTrue(res.expression.toString().isNotBlank())
    }

    private fun assertInvalid(input: String, vararg expectedErrorSubstrings: String) {
        val res = parserService.parse(input)
        assertNotNull(res, "ParseResult should be returned")
        assertFalse(res.errors.isEmpty(), "Expected errors for: $input")
        expectedErrorSubstrings.forEach { substr ->
            assertTrue(
                res.errors.any { it.contains(substr, ignoreCase = true) },
                "Expected error containing '$substr' for: $input, but got: ${res.errors}"
            )
        }
    }

    @Nested
    inner class BasicComparisons {
        @Test
        fun `string equality with single and double quotes`() {
            assertValid("Request.Method == 'GET'")
            assertValid("Request.Method == \"GET\"")
        }

        @Test
        fun `string escaping inside quotes`() {
            // Escaped quote within double quotes
            assertValid("Request.Path == \"/\\\"api\\\"/v1\"")
            // Backslash in single quotes
            assertValid("Request.Path == '/v1\\resource'")
        }

        @Test
        fun `numeric equality and ordering`() {
            assertValid("Response.Status == 200")
            // Use >= and <= as strict > or < are equivalent for grammar purposes
            assertValid("Response.Status >= 199")
            assertValid("Response.Status >= 200")
            assertValid("Response.Status <= 400")
            assertValid("Response.Status <= 404")
            // Numeric identifier on RHS
            assertValid("Response.Status >= Request.Port")
        }

        @Test
        fun `numeric operators invalid on non-numeric fields`() {
            assertInvalid(
                "Request.Method > 10",
                "Numeric operators cannot be used"
            )
        }

        @Test
        fun `date comparisons on date field`() {
            // Request.Time is a Date
            assertValid("Request.Time >= '2023/01/01 00:00:00'")
            assertValid("Request.Time < \"2026/12/31 23:59:59\"")
            // Invalid date format should error (reported as invalid RHS for numeric comparison)
            assertInvalid("Request.Time >= '01-01-2023'", "Invalid right hand value for comparison")
            // String comparison against a date-like string is still a string comparison and should be valid
            assertValid("Request.Method == '2023/01/01 00:00:00'")
        }

        @Test
        fun `boolean field does not require explicit comparison`() {
            // Explicit boolean comparison
            assertValid("Request.InScope == TRUE")
            assertValid("Entry.InScope == false")
            assertValid("Request.InScope")
        }
    }

    @Nested
    inner class SpecialOperators {
        @Test
        fun `CONTAINS with strings and identifiers`() {
            assertValid("Request.Body CONTAINS 'csrf'")
            assertValid("Request.Body CONTAINS Request.Method")
            // Contains with number
            assertValid("Response.Status CONTAINS 0")
        }

        @Test
        fun `IN with homogeneous arrays`() {
            assertValid("Response.Status IN [200,302,500]")
            assertValid("Request.Method IN ['GET','POST','PUT']")
        }

        @Test
        fun `IN with malformed arrays`() {
            assertInvalid("Response.Status IN [200,'A']", "Array elements must all be of the same type")
            assertInvalid(
                "Response.Status IN '200'",
                "must be used on an array"
            )
        }

        @Test
        fun `MATCHES with regex string or slashes`() {
            assertValid("Request.Path MATCHES '/api/(account|payments)/.*'")
            assertValid("Request.Path MATCHES \"/api/.*/\"")
        }

        @Test
        fun `regex literal with equality on stringy fields`() {
            assertValid("Request.Query == /id=\\d+/")
            assertValid("Request.Query != /session=[A-F0-9]{32}/")
            // Regex against non-string field should error
            assertInvalid(
                "Response.Status == /200/",
                "Regex patterns can only be used on fields"
            )
        }
    }

    @Nested
    inner class CompoundAndNegation {
        @Test
        fun `AND OR XOR with keywords and symbols`() {
            assertValid("Request.Body CONTAINS 'A' AND Response.Status == 200")
            assertValid("Request.Body CONTAINS 'A' && Response.Status == 200")
            assertValid("Request.Body CONTAINS 'A' OR Response.Status == 200")
            assertValid("Request.Body CONTAINS 'A' || Response.Status == 200")
            assertValid("Request.Body CONTAINS 'A' ^ Response.Status == 200")
            assertValid("Request.Body CONTAINS 'A' XOR Response.Status == 200")
        }

        @Test
        fun `mixing operators without parentheses is invalid`() {
            assertInvalid(
                "Request.Body == 'A' AND Response.Status == 200 OR Response.Status == 302",
                "Cannot mix operators"
            )
        }

        @Test
        fun `parentheses remove ambiguity`() {
            assertValid("Request.Body == 'A' AND ( Response.Status == 200 OR Response.Status == 302 )")
        }

        @Test
        fun `expression negation with bang and NOT`() {
            assertValid("!( Request.Body CONTAINS 'CSRF' )")
            // Explicit boolean comparison required inside negation
            assertValid("!( Request.InScope == TRUE )")
            // NOT keyword variant
            assertValid("NOT ( Request.Body CONTAINS 'CSRF' )")
        }

        @Test
        fun `unbalanced parentheses`() {
            assertInvalid("( Request.Method == 'GET'", "Unbalanced brackets")
        }

        @Test
        fun `XOR chaining with same operator is allowed`() {
            assertValid("Request.Method == 'GET' XOR Request.Method == 'POST' XOR Request.Method == 'PUT'")
        }
    }

    @Nested
    inner class IdentifiersAndLiterals {
        @Test
        fun `invalid field group`() {
            assertInvalid("Foo.Method == 'GET'", "Invalid field group")
        }

        @Test
        fun `invalid field in valid group`() {
            assertInvalid("Request.NonExistent == 'GET'", "Invalid field \"Request.NonExistent\"")
        }

        @Test
        fun `ephemeral number field is not allowed`() {
            assertInvalid("Entry.Number == 1", "ephemeral")
        }

        @Test
        fun `missing closing quote in string`() {
            assertInvalid("Request.Method == 'GET", "Invalid right hand value for comparison")
            assertInvalid("Request.Method == \"GET", "Invalid right hand value for comparison")
        }

        @Test
        fun `unterminated regex in slashes`() {
            // Equality branch wraps underlying tokenization error as a generic RHS error
            assertInvalid("Request.Path == /abc", "Invalid right hand value for comparison")
        }

        @Test
        fun `identifier to identifier equality for compatible types`() {
            // String to string
            assertValid("Request.Method == Response.ContentType")
            // Number to number
            assertValid("Response.Status == Request.Port")
        }

        @Test
        fun `date equality uses date parser`() {
            assertValid("Request.Time == '2024/05/06 12:30:00'")
        }
    }

    @Nested
    inner class Aliases {
        @Test
        fun `alias token is parsed but yields dependency error when unknown`() {
            val res = parserService.parse("#MY_SNIPPET")
            assertNotNull(res.expression, "Expression should be produced for alias usage")
            assertFalse(res.errors.isEmpty(), "Unknown alias should produce dependency error")
            assertTrue(
                res.errors.any { it.contains("Could not find a filter in the library for alias", ignoreCase = true) },
                "Expected missing alias error, got: ${res.errors}"
            )
        }

        @Test
        fun `alias can be part of a compound expression syntactically`() {
            val res = parserService.parse("Response.Status == 200 OR #OTHER")
            assertNotNull(res.expression)
            assertFalse(res.errors.isEmpty())
        }
    }

    @Nested
    inner class CaseInsensitivityAndSpacing {
        @Test
        fun `keywords and identifiers are case-insensitive`() {
            assertValid("request.method == 'get'")
            assertValid("REQUEST.METHOD CONTAINS 'g'")
            assertValid("response.status in [200,404]")
            assertValid("request.path matches '/API/.*/'")
            assertValid("request.isssl") // boolean default to true
            // lower-case logical operators
            assertValid("request.method == 'get' and response.status == 200")
            assertValid("request.method == 'get' or response.status == 200")
            assertValid("request.method == 'get' xor response.status == 200")
        }

        @Test
        fun `extra spaces are allowed`() {
            assertValid("   Request.Method    ==    'GET'    ")
        }
    }

    @Nested
    inner class StringSearchUsages {
        @Test
        fun `bare string search is valid`() {
            assertValid("'search term'")
            assertValid("\"search term\"")
        }

        @Test
        fun `empty string search is valid`() {
            assertValid("''")
            assertValid("\"\"")
        }
    }
}
