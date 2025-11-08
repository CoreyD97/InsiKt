package com.coreyd97.insikt.logging.logentry

//import com.coreyd97.insikt.logentry.LogEntryField.Companion.getByFullyQualifiedName
//import com.google.gson.*
//import java.lang.reflect.Type

//class LogEntryFieldSerializer : JsonSerializer<LogEntryField?>, JsonDeserializer<LogEntryField?> {
//    @Throws(JsonParseException::class)
//    override fun deserialize(
//        json: JsonElement, typeOfT: Type?,
//        context: JsonDeserializationContext?
//    ): LogEntryField? {
//        return getByFullyQualifiedName(json.asString)
//    }
//
//    override fun serialize(
//        src: LogEntryField, typeOfSrc: Type?,
//        context: JsonSerializationContext
//    ): JsonElement? {
//        return context.serialize(src.fullLabel)
//    }
//}
