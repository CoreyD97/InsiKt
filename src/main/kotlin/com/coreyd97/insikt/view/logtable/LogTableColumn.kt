package com.coreyd97.insikt.view.logtable

import com.coreyd97.insikt.logging.logentry.LogEntryField
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*

// To define a structure for table headers
// This will provide a high degree of customisation
// a sample JSON object which will be converted to this object is as follows:
// "{'columnsDefinition':[{'id':'number','visibleName':'#','width':50,'type':'int','readonly':true,'order':1,'visible':true,'description':'Item index number','isRegEx':false,'regExData':{'regExString':'','regExCaseSensitive':false}}]}";

@Serializable(with = LogTableColumn.Companion.LogTableColumnSerializer::class)
class LogTableColumn : javax.swing.table.TableColumn(), Comparable<LogTableColumn> {

    @SerialName("id")
    var identifier: LogEntryField = LogEntryField.UNKNOWN
    var name: String = ""
    var order = 0
    var visibleName: String = ""
    var visible = false
    var readOnly = true
    var description: String = ""
        private set
    var defaultVisibleName: String = ""
        private set

    override fun getIdentifier(): Any {
        return identifier
    }

    override fun getHeaderValue(): Any {
        return visibleName
    }

    override fun compareTo(logTableColumn: LogTableColumn): Int {
        return Integer.compare(this.order, logTableColumn.order)
    }

    override fun toString(): String {
        return "LogTableColumn[$identifier]"
    }

    companion object {
        class LogTableColumnSerializer : KSerializer<LogTableColumn> {
            override val descriptor = buildClassSerialDescriptor("LogTableColumn") {
                element<LogEntryField>("id")
                element<String>("name")
                element<Int>("order")
                element<String>("visibleName")
                element<Boolean>("visible")
                element<Boolean>("readOnly")
                element<String>("description")
                element<String>("defaultVisibleName")
                element<Int>("width")
            }

            override fun serialize(encoder: Encoder, value: LogTableColumn) {
                encoder.encodeStructure(descriptor) {
                    encodeSerializableElement(descriptor, 0, LogEntryField.serializer(), value.identifier)
                    encodeStringElement(descriptor, 1, value.name)
                    encodeIntElement(descriptor, 2, value.order)
                    encodeStringElement(descriptor, 3, value.visibleName)
                    encodeBooleanElement(descriptor, 4, value.visible)
                    encodeBooleanElement(descriptor, 5, value.readOnly)
                    encodeStringElement(descriptor, 6, value.description)
                    encodeStringElement(descriptor, 7, value.defaultVisibleName)
                    encodeIntElement(descriptor, 8, value.width)
                }
            }

            override fun deserialize(decoder: Decoder): LogTableColumn {
                return decoder.decodeStructure(descriptor) {
                    var id = LogEntryField.UNKNOWN
                    var name = ""
                    var order = 0
                    var visibleName = ""
                    var visible = false
                    var readOnly = true
                    var description = ""
                    var defaultVisibleName = ""
                    var width = 75

                    while (true) {
                        when (val index = decodeElementIndex(descriptor)) {
                            0 -> id = decodeSerializableElement(descriptor, 0, LogEntryField.serializer())
                            1 -> name = decodeStringElement(descriptor, 1)
                            2 -> order = decodeIntElement(descriptor, 2)
                            3 -> visibleName = decodeStringElement(descriptor, 3)
                            4 -> visible = decodeBooleanElement(descriptor, 4)
                            5 -> readOnly = decodeBooleanElement(descriptor, 5)
                            6 -> description = decodeStringElement(descriptor, 6)
                            7 -> defaultVisibleName = decodeStringElement(descriptor, 7)
                            8 -> width = decodeIntElement(descriptor, 8)
                            CompositeDecoder.DECODE_DONE -> break
                            else -> error("Unexpected index: $index")
                        }
                    }

                    LogTableColumn().apply {
                        identifier = id
                        this.name = name
                        this.order = order
                        this.visibleName = visibleName
                        this.visible = visible
                        this.readOnly = readOnly
                        this.description = description
                        this.defaultVisibleName = defaultVisibleName
                        this.width = width
                        this.preferredWidth = width
                    }
                }
            }
        }
    }


}
