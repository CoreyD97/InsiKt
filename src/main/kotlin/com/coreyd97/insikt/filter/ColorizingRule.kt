package com.coreyd97.insikt.filter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import java.awt.Color
import java.util.UUID


@Serializable(with = ColorizingRule.Serializer::class)
class ColorizingRule(name: String?, expression: FilterExpression, uuid: UUID = UUID.randomUUID()) :
    FilterRule(expression, name=name, uuid = uuid), Comparable<ColorizingRule> {
    var backgroundColor: Color? = null
    var foregroundColor: Color? = null

    var priority: Int = 0
    var enabled: Boolean = false

    override fun compareTo(other: ColorizingRule): Int {
        return priority.compareTo(other.priority)
    }

    companion object {
        fun fromString(name: String, filter: String): ColorizingRule {
            val res = FilterExpression.fromString(filter)
            require(res.valid()) { "Computed expression is invalid." }
            return ColorizingRule(name, res.expression).also {
                it.enabled = true
            }
        }
    }

    class Serializer : KSerializer<ColorizingRule> {
        override val descriptor = buildClassSerialDescriptor("ColorizingFilterRule") {
            element<String?>("name")
            element<String>("filterString")
            element<Int?>("backgroundColor")
            element<Int?>("foregroundColor")
            element<Int>("priority")
            element<Boolean>("enabled")
            element<String>("uuid")
            element<String>("shouldRetest")
        }

        override fun serialize(encoder: Encoder, value: ColorizingRule) {
            val composite = encoder.beginStructure(descriptor)
            var index = 0
            composite.encodeNullableSerializableElement(descriptor, index++, serializer<String>(), value.name)
            composite.encodeStringElement(descriptor, index++, value.expression.toString())
            composite.encodeNullableSerializableElement(descriptor, index++, serializer<Int>(), value.backgroundColor?.rgb)
            composite.encodeNullableSerializableElement(descriptor, index++, serializer<Int>(), value.foregroundColor?.rgb)
            composite.encodeIntElement(descriptor, index++, value.priority)
            composite.encodeBooleanElement(descriptor, index++, value.enabled)
            composite.encodeStringElement(descriptor, index++, value.uuid.toString())
            composite.encodeBooleanElement(descriptor, index++, value.shouldRetest)
            composite.endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): ColorizingRule {
            val dec = decoder.beginStructure(descriptor)
            var uuid: UUID? = null
            var name: String? = null
            var filterString: String = ""
            var backgroundColor: Color? = null
            var foregroundColor: Color? = null
            var priority: Int = 0
            var enabled: Boolean = false
            var shouldRetest: Boolean = false

            loop@ while (true) {
                when (val index = dec.decodeElementIndex(descriptor)) {
                    0 -> name = dec.decodeNullableSerializableElement(descriptor, index, serializer<String>())
                    1 -> filterString = dec.decodeStringElement(descriptor, index)
                    2 -> backgroundColor = dec.decodeNullableSerializableElement(descriptor, index, serializer<Int>())?.let { Color(it) }
                    3 -> foregroundColor = dec.decodeNullableSerializableElement(descriptor, index, serializer<Int>())?.let { Color(it) }
                    4 -> priority = dec.decodeIntElement(descriptor, index)
                    5 -> enabled = dec.decodeBooleanElement(descriptor, index)
                    6 -> uuid = UUID.fromString(dec.decodeStringElement(descriptor, index))
                    7 -> shouldRetest = dec.decodeBooleanElement(descriptor, index)
                    CompositeDecoder.DECODE_DONE -> break@loop
                    else -> throw SerializationException("Unknown index $index")
                }
            }
            dec.endStructure(descriptor)

            val result = FilterExpression.fromString(filterString)
            require(result.valid()) { "Parsed filter is invalid." }

            return ColorizingRule(name, result.expression, uuid ?: UUID.randomUUID()).apply {
                this.enabled = enabled
                //Must be below enabled change so it doesn't force shouldRetest on
                this.shouldRetest = shouldRetest
                this.backgroundColor = backgroundColor
                this.foregroundColor = foregroundColor
                this.priority = priority
            }
        }
    }
}

interface ColorizingRuleListener {
    fun onExpressionChange(rule: ColorizingRule)
    fun onAttributeChange(rule: ColorizingRule)
    fun onAdd(rule: ColorizingRule)
    fun onRemove(rule: ColorizingRule)
}