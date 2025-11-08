package com.coreyd97.insikt.filter

import kotlinx.serialization.Serializable
import com.coreyd97.insikt.logging.logentry.FieldGroup
import com.coreyd97.insikt.logging.logentry.LogEntryField
import com.google.inject.Inject
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.serializer
import java.util.*
import kotlin.text.replace

/**
 * Created by corey on 19/07/17.
 */
val validateFilterString: (String) -> Boolean = { filter: String ->
    FilterExpression.fromString(filter).valid()
}

data class FilterExpression(
    @Contextual val astExpression: ASTExpression,
    val snippetDependencies: Set<String>,
    val requiredContexts: Set<FieldGroup>,
) {

    companion object {
        // Statically injected via Guice
        private lateinit var parserService: ParserService

        @Inject
        @JvmStatic
        fun setParserService(service: ParserService) {
            parserService = service
        }

        fun fromString(filterString: String): ParseResult {
            // Delegate parsing to the service so aliases/dependencies can be resolved via DI
            return parserService.parse(filterString)
        }

        fun fromStringIgnoringDependencies(filterString: String): ParseResult {
            return parserService.parse(filterString, false)
        }
    }


    override fun toString(): String {
        return astExpression.filterString
    }
}

@Serializable(with = FilterRule.Serializer::class)
open class FilterRule internal constructor(
    expression: FilterExpression,
    val uuid: UUID = UUID.randomUUID(),
    name: String? = null
){
    var name: String? = name?.replace("[^a-zA-Z0-9_.]".toRegex(), "_")
        set(value) {
            field = value?.replace("[^a-zA-Z0-9_.]".toRegex(), "_")
        }
    var expression = expression
        set(value) {
            field = value
            shouldRetest = true
        }
    val isValid: Boolean
        get() = expression != null

    var shouldRetest: Boolean = false
    var errors: List<String> = emptyList()
        private set


    fun withAddedCondition(
        logicalOperator: LogicalOperator, field: LogEntryField,
        comparison: ComparisonOperator?, value: String?
    ): FilterRule {
        val existing =
            if (expression.astExpression.logicalOperator != null && expression.astExpression.logicalOperator != logicalOperator) {
                "(" + expression.astExpression.filterString + ")"
            } else {
                expression.astExpression.filterString
            }
        val result = FilterExpression.fromString(
            String.format(
                "%s %s %s %s %s",
                existing,
                logicalOperator.toString(),
                field.toString(),
                comparison,
                value
            )
        )
        require(result.valid()) {"Computed expression is invalid"}
        return FilterRule(result.expression)
    }

    fun withAddedCondition(
        logicalOperator: LogicalOperator, filterRule: FilterRule
    ): FilterRule {
        require(filterRule.isValid) {"Additional filter condition is not valid"}
        val existing =
            if (expression.astExpression.logicalOperator != null && expression.astExpression.logicalOperator != logicalOperator) {
                "(" + expression.astExpression.filterString + ")"
            } else {
                expression.astExpression.filterString
            }
        val result = FilterExpression.fromString(
            String.format("%s %s %s", existing, logicalOperator.toString(), filterRule.expression.toString())
        )
        require(result.valid()) {"Computed expression is invalid"}
        return FilterRule(
            result.expression
        )
    }

//    fun matches(entry: LogEntry?): Boolean {
//        if (expression == null) {
//            return false
//        }
//        val visitor = FilterEvaluationVisitor(Insikt.INSTANCE!!.libraryController)
//        return visitor.visit(expression!!.astExpression, entry)
//    }

    override fun toString(): String {
        return expression.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FilterRule

        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    companion object {
        fun fromString(filter: String): FilterRule{
            val res = FilterExpression.fromString(filter)
            require(res.valid()) {"Computed expression is invalid"}
            return FilterRule(res.expression)
        }
    }


    @OptIn(ExperimentalSerializationApi::class)
    class Serializer : KSerializer<FilterRule> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("FilterRule"){
            element<String>("uuid")
            element<String?>("name")
            element<String>("filterString")
            element<Boolean>("shouldRetest")
        }

        override fun serialize(encoder: Encoder, value: FilterRule) {
            var index = 0
            encoder.encodeStructure(descriptor) {
                encodeSerializableElement(descriptor, index++, serializer<String>(), value.uuid.toString())
                encodeNullableSerializableElement(descriptor, index++, serializer<String>(), value.name)
                //Not filterString, as that may not be accurate if we updated expression directly
                encodeStringElement(descriptor, index++, value.toString())
                encodeBooleanElement(descriptor, index++, value.shouldRetest)
            }
        }
        override fun deserialize(decoder: Decoder): FilterRule {
            return decoder.decodeStructure(descriptor) {
                lateinit var uuid: String
                lateinit var filterString: String
                var name: String? = null
                var shouldRetest: Boolean = false
                loop@ while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        CompositeDecoder.DECODE_DONE -> break@loop
                        0 -> uuid = decodeSerializableElement(descriptor, 0, serializer<String>())
                        1 -> name = decodeNullableSerializableElement(descriptor, 1, serializer<String>())
                        2 -> filterString = decodeStringElement(descriptor, 2)
                        3 -> shouldRetest = decodeBooleanElement(descriptor, 3)
                        else -> error("Unexpected index: $index")
                    }
                }

                val expression = FilterExpression.fromStringIgnoringDependencies(filterString)

                FilterRule(uuid = UUID.fromString(uuid), name = name, expression = expression.expression).also {
                    it.shouldRetest = shouldRetest
                }
            }
        }
    }
}