package com.coreyd97.insikt.logging.logentry

//import com.coreyd97.insikt.logentry.LogEntryField.Companion.getFieldsInGroup
//import com.google.gson.JsonElement
//import com.google.gson.JsonObject
//import com.google.gson.JsonSerializationContext
//import com.google.gson.JsonSerializer
//import java.lang.reflect.Type
//import java.util.List

//class LogEntrySerializer : JsonSerializer<LogEntry?> {
//    private val excludedFields: MutableList<LogEntryField?> =
//        List.of<LogEntryField?>(LogEntryField.NUMBER)
//
//    override fun serialize(
//        src: LogEntry,
//        typeOfSrc: Type?,
//        context: JsonSerializationContext
//    ): JsonElement {
//        val entry = JsonObject()
//        for (group in FieldGroup.entries) {
//            val groupEntries = JsonObject()
//            for (fieldInGroup in getFieldsInGroup(group)) {
//                if (excludedFields.contains(fieldInGroup)) {
//                    continue
//                }
//                groupEntries.add(
//                    fieldInGroup.labels[0],
//                    context.serialize(src.getValueByKey(fieldInGroup))
//                )
//            }
//            entry.add(group.getLabel(), groupEntries)
//        }
//        return entry
//    }
//}
