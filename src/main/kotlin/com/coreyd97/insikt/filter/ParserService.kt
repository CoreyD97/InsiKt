package com.coreyd97.insikt.filter

import com.google.inject.Inject
import com.google.inject.Provider
import com.google.inject.Singleton
import java.io.StringReader
import java.util.*

interface ParserService {
    fun parse(filter: String, checkAliasesExist: Boolean = true): ParseResult
}

@Singleton
class ParserServiceImpl @Inject constructor(
    val filterLibrary: Provider<FilterLibrary>,
): ParserService {

    override fun parse(filter: String, checkAliasesExist: Boolean): ParseResult {
        val filterParser = FilterParser(StringReader(filter))
        val node: ASTExpression?
        try {
            node = filterParser.Filter()
            val dependencies = DependencyVisitor(filterLibrary.get(), checkAliasesExist).visit(node)
            val expression = FilterExpression(node, dependencies.snippets, dependencies.contexts)
            return ParseResult(expression, dependencies.errors)
        } catch (e: ParseException) {
            return ParseResult(null, Arrays.asList<String?>(e.message))
        }
    }
}