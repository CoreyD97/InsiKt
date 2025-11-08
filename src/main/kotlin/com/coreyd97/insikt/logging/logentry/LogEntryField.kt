package com.coreyd97.insikt.logging.logentry

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

@Serializable(with = LogEntryField.Serializer::class)
enum class LogEntryField(
  val fieldGroup: FieldGroup,
  val type: Class<*>,
  val description: String,
  vararg val labels: String
) {
    //Proxy
    NUMBER(
        FieldGroup.ENTRY,
        Int::class.javaObjectType,
        "Item table number. Not valid for filter use.",
        "Number"
    ),
    PROXY_TOOL(
        FieldGroup.ENTRY, String::class.javaObjectType,
        "Originating tool name. Extension generated requests will be displayed as \"Extender\".",
        "Tool"
    ),
    TAGS(
        FieldGroup.ENTRY,
        String::class.javaObjectType, "The configured tags for which this entry match.", "Tags"
    ),

    ENTRY_INSCOPE(FieldGroup.ENTRY, Boolean::class.javaObjectType, "If the URL is in scope", "InScope"),
    LISTENER_INTERFACE(
        FieldGroup.ENTRY, String::class.javaObjectType,
        "The interface the proxied message was delivered to.", "ListenInterface", "Interface"
    ),  //  CLIENT_IP(FieldGroup.ENTRY, String.class, "The requesting client IP address.", "ClientIP",
    //      "ClientAddress"),

    //Request,
    INSCOPE(FieldGroup.REQUEST, Boolean::class.javaObjectType, "If the URL is in scope", "InScope"),

    //  BASE64_REQUEST(FieldGroup.REQUEST, String.class, "The entire request encoded in Base64",
    //      "AsBase64"),
    REQUEST_HEADERS(
        FieldGroup.REQUEST, String::class.javaObjectType, "The request line and associated headers.",
        "Headers", "Header"
    ),
    REQUEST_BODY(FieldGroup.REQUEST, String::class.javaObjectType, "The request body.", "Body"),
    REQUEST_BODY_LENGTH(
        FieldGroup.REQUEST,
        String::class.javaObjectType, "The request body's length.", "BodyLength"
    ),
    REQUEST_TIME(
        FieldGroup.REQUEST, Date::class.javaObjectType,
        "Date and time of inital request (as received by L++).", "Time"
    ),
    REQUEST_LENGTH(
        FieldGroup.REQUEST, Int::class.javaObjectType, "The length of the received request.",
        "Length"
    ),
    REQUEST_TOOL(
        FieldGroup.REQUEST, String::class.javaObjectType, "The tool used to initiate the request.",
        "Tool"
    ),  //Alias for proxy.tool,
    COMMENT(FieldGroup.REQUEST, String::class.javaObjectType, "Comments set on the entry.", "Comment"),
    COMPLETE(
        FieldGroup.REQUEST, Boolean::class.javaObjectType, "Has a response been received?", "Complete",
        "isComplete"
    ),
    URL(FieldGroup.REQUEST, String::class.javaObjectType, "The entire URL of the request.", "URL", "URI"),
    METHOD(FieldGroup.REQUEST, String::class.javaObjectType, "The request method used.", "Method"),
    PATH(
        FieldGroup.REQUEST,
        String::class.javaObjectType, "The path component of the requested URL.", "Path"
    ),
    QUERY(
        FieldGroup.REQUEST,
        String::class.javaObjectType, "The query parameters of the requested URL.", "Query",
        "GetParams", "QueryParams"
    ),
    PROTOCOL(
        FieldGroup.REQUEST, String::class.javaObjectType, "The protocol component of the requested URL.",
        "Protocol"
    ),
    ISSSL(FieldGroup.REQUEST, Boolean::class.javaObjectType, "Did the request use SSL?", "IsSSL", "ssl"),

    //  USES_COOKIE_JAR(FieldGroup.REQUEST, String.class,
    //      "Compares the cookies with the cookie jar to see if any of them are in use.", "UsesCookieJar",
    //      "CookieJar"),
    HOSTNAME(
        FieldGroup.REQUEST, String::class.javaObjectType, "The hostname component of the requested URL.",
        "Hostname"
    ),
    HOST(
        FieldGroup.REQUEST,
        String::class.javaObjectType, "The protocol and hostname of the requested URL.", "Host"
    ),
    PORT(FieldGroup.REQUEST, Short::class.javaObjectType, "The port the request was sent to.", "Port"),
    REQUEST_CONTENT_TYPE(
        FieldGroup.REQUEST, String::class.javaObjectType,
        "The content-type header sent to the server.", "ContentType", "Content_Type"
    ),
    REQUEST_HTTP_VERSION(
        FieldGroup.REQUEST, String::class.javaObjectType, "The HTTP version sent in the request.",
        "RequestHttpVersion", "RequestHttpVersion"
    ),
    EXTENSION(
        FieldGroup.REQUEST, String::class.javaObjectType, "The URL extension used in the request.",
        "Extension"
    ),
    REFERRER(
        FieldGroup.REQUEST, String::class.javaObjectType, "The referrer header value of the request.",
        "Referrer"
    ),
    HAS_PARAMS(
        FieldGroup.REQUEST,
        Boolean::class.javaObjectType,
        "Did the request contain parameters?",
        "HasParams"
    ),
    HAS_GET_PARAM(
        FieldGroup.REQUEST, Boolean::class.javaObjectType, "Did the request contain get parameters?",
        "HasGetParam", "HasGetParams", "HasQueryString"
    ),
    HAS_POST_PARAM(
        FieldGroup.REQUEST, Boolean::class.javaObjectType, "Did the request contain post parameters?",
        "HasPostParam", "HasPayload", "Payload"
    ),
    HAS_COOKIE_PARAM(
        FieldGroup.REQUEST, Boolean::class.javaObjectType, "Did the request contain cookies?",
        "HasSentCookies", "HasCookies"
    ),
    SENT_COOKIES(
        FieldGroup.REQUEST, Boolean::class.javaObjectType,
        "The value of the cookies header sent to the server.", "CookieString", "SentCookies",
        "Cookies"
    ),
    PARAMETER_COUNT(
        FieldGroup.REQUEST, Int::class.javaObjectType, "The number of parameters in the request.",
        "ParameterCount", "ParamCount"
    ),
    PARAMETERS(
        FieldGroup.REQUEST, String::class.javaObjectType, "The parameters in the request.", "Parameters",
        "Params"
    ),
    ORIGIN(FieldGroup.REQUEST, String::class.javaObjectType, "The Origin header", "Origin"),

    //Response
    RESPONSE_HEADERS(
        FieldGroup.RESPONSE, String::class.javaObjectType, "The status line and associated headers.",
        "Headers", "Header"
    ),
    RESPONSE_BODY(FieldGroup.RESPONSE, String::class.javaObjectType, "The response body.", "Body"),
    RESPONSE_BODY_LENGTH(
        FieldGroup.RESPONSE, String::class.javaObjectType, "The response body's length.",
        "BodyLength"
    ),
    RESPONSE_HASH(
        FieldGroup.RESPONSE,
        String::class.javaObjectType, "SHA1 Hash of the response", "hash", "sha1"
    ),
    RESPONSE_TIME(
        FieldGroup.RESPONSE, Date::class.javaObjectType,
        "Date and time of receiving the response (as received by L++).", "Time"
    ),
    RESPONSE_LENGTH(
        FieldGroup.RESPONSE, Int::class.javaObjectType, "The length of the received response.",
        "Length", "Size"
    ),
    REDIRECT_URL(
        FieldGroup.RESPONSE,
        URL::class.javaObjectType, "The URL the response redirects to.", "Redirect",
        "RedirectURL"
    ),
    STATUS(
        FieldGroup.RESPONSE,
        Short::class.javaObjectType,
        "The status code received in the response.",
        "Status",
        "StatusCode"
    ),
    STATUS_TEXT(
        FieldGroup.RESPONSE, Short::class.javaObjectType, "The status text received in the response.",
        "StatusText", "StatusText"
    ),
    RESPONSE_HTTP_VERSION(
        FieldGroup.RESPONSE, Short::class.javaObjectType,
        "The HTTP version received in the response.", "ResponseHttpVersion", "ResponseHttpVersion"
    ),
    RTT(
        FieldGroup.RESPONSE, Int::class.javaObjectType,
        "The round trip time (as calculated by L++, not 100% accurate).", "RTT", "TimeTaken"
    ),
    TITLE(FieldGroup.RESPONSE, String::class.javaObjectType, "The HTTP response title.", "Title"),
    RESPONSE_CONTENT_TYPE(
        FieldGroup.RESPONSE, String::class.javaObjectType,
        "The content-type header sent by the server.", "ContentType", "Content_Type"
    ),
    INFERRED_TYPE(
        FieldGroup.RESPONSE, String::class.javaObjectType, "The type inferred by the response content.",
        "InferredType", "Inferred_Type"
    ),
    MIME_TYPE(
        FieldGroup.RESPONSE,
        String::class.javaObjectType, "The mime-type stated by the server.", "MimeType",
        "Mime"
    ),
    HAS_SET_COOKIES(
        FieldGroup.RESPONSE, Boolean::class.javaObjectType, "Did the response set cookies?",
        "HasSetCookies", "DidSetCookies"
    ),
    NEW_COOKIES(
        FieldGroup.RESPONSE,
        String::class.javaObjectType, "The new cookies sent by the server", "Cookies",
        "NewCookies", "New_Cookies", "SetCookies"
    ),
    REFLECTED_PARAMS(
        FieldGroup.RESPONSE, String::class.javaObjectType, "Values reflected in the response",
        "ReflectedParams", "ReflectedParameters"
    ),
    REFLECTION_COUNT(
        FieldGroup.RESPONSE, Int::class.javaObjectType, "Number of values reflected in the response",
        "Reflections", "ReflectionCount", "ReflectedCount"
    ),
    UNKNOWN(FieldGroup.ENTRY, Any::class.javaObjectType, "", "Unknown");

    fun fullLabel(label: String): String {
        return fieldGroup.label + "." + label
    }

    val fullLabel: String
        get() = fieldGroup.label + "." + labels[0]

    val descriptiveMessage: String
        get() = String.format(
            "Field: <b>%s</b>\nType: %s\nDescription: %s", java.lang.String.join(", ", *labels),
            type.simpleName, description
        )

    override fun toString(): String {
        //TODO Better output for alternatives in error messages
        return fullLabel
    }

    companion object {
        private val completeGroupFieldMap = HashMap<FieldGroup, HashMap<String, LogEntryField>>()
        private val shortGroupFieldMap = HashMap<FieldGroup, HashMap<String, LogEntryField>>()

        init {
            for (fieldGroup in FieldGroup.entries) {
                completeGroupFieldMap[fieldGroup] =
                    LinkedHashMap()
                shortGroupFieldMap[fieldGroup] =
                    LinkedHashMap()
            }

            for (field in entries) {
                shortGroupFieldMap[field.fieldGroup]!![field.labels[0]] = field
                for (label in field.labels) {
                    completeGroupFieldMap[field.fieldGroup]!![label.lowercase(
                        Locale.getDefault()
                    )] =
                        field
                }
            }
        }


        @JvmStatic
        fun getByLabel(fieldGroup: FieldGroup, searchLabel: String): LogEntryField? {
            val groupFields = completeGroupFieldMap[fieldGroup]
            return groupFields?.get(searchLabel.lowercase(Locale.getDefault()))
        }

        @JvmStatic
        fun getFieldsInGroup(fieldGroup: FieldGroup): Collection<LogEntryField> {
            return shortGroupFieldMap[fieldGroup]!!.values
        }

        @JvmStatic
        fun getByFullyQualifiedName(fqn: String): LogEntryField? {
            val split = fqn.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val group = FieldGroup.findByLabel(split[0])
            if(group == null) return null
            return getByLabel(group, split[1])
        }
    }

    class Serializer: KSerializer<LogEntryField> {
        val log = org.apache.logging.log4j.LogManager.getLogger(Serializer::class.java)

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LogEntryField", PrimitiveKind.STRING)

        // Serialize the LogEntryField instance
        override fun serialize(encoder: Encoder, value: LogEntryField) {
            encoder.encodeString(value.fullLabel)
        }

        // Deserialize and reconstruct a LogEntryField instance
        override fun deserialize(decoder: Decoder): LogEntryField {
            val str=decoder.decodeString()
            try {

                log.trace("Deserializing LogEntryField from: $str")
                return LogEntryField.getByFullyQualifiedName(str)
                    ?: throw IllegalArgumentException("Unknown LogEntryField $str")
            }catch (e: Exception){
                throw IllegalArgumentException("Unknown LogEntryField $str", e)
            }
        }
    }
}
