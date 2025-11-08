package com.coreyd97.insikt.logging.logentry

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.ToolType
import burp.api.montoya.http.HttpService
import burp.api.montoya.http.message.Cookie
import burp.api.montoya.http.message.HttpHeader
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.params.ParsedHttpParameter
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.http.message.responses.HttpResponse
import burp.api.montoya.proxy.ProxyHttpRequestResponse
import com.coreyd97.insikt.filter.ColorizingRule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.*
import java.util.stream.Collectors

class LogEntry internal constructor(
    val montoya: MontoyaApi,
    val tool: ToolType,
   val request: HttpRequest,
   var requestDateTime: Date,
   var response: HttpResponse? = null,
   var responseDateTime: Date? = null,
    ) {

    var identifier: Int? = null
    var status = Status.AWAITING_RESPONSE

    val matchingColorFilters: MutableSet<ColorizingRule> = sortedSetOf()
    val matchingTags: MutableSet<ColorizingRule> = sortedSetOf()
    val reflectedParameters = mutableSetOf<String>()
    

    val requestBytes: ByteArray
        get() = request.toByteArray().bytes

    val responseBytes: ByteArray?
        get() = response?.toByteArray()?.bytes

    fun isInScope(): Boolean {
        return montoya.scope().isInScope(
            request.url()
        )
    }

    fun getProtocol(): String {
        return if (request.httpService().secure()) "https" else "http"
    }

    fun getHostname(): String {
        return request.httpService().host()
    }

    fun getPort(): Int {
        return request.httpService().port()
    }

    fun getHttpService(): HttpService {
        return request.httpService()
    }

    fun getPath(): String {
        return request.path().let { it.split("?", limit = 2)[0] }
    }

    fun getUrl(): String {
        return request.url()
    }

    fun getValueByKey(columnName: LogEntryField?, forFilterEvaluation: Boolean = false): Any? {
        try {
            return when (columnName) {
                LogEntryField.ENTRY_INSCOPE, LogEntryField.INSCOPE -> isInScope()

                LogEntryField.PROXY_TOOL, LogEntryField.REQUEST_TOOL -> tool.toolName()
                LogEntryField.TAGS -> matchingTags
                    .filter { it.enabled }
                    .sortedBy { it.priority }
                    .map {
                        if(forFilterEvaluation) it.name
                        else it
                }.toList()
                LogEntryField.URL -> getUrl()
                LogEntryField.PATH -> getPath()
                LogEntryField.QUERY -> request.parameters().stream()
                    .filter { p: ParsedHttpParameter -> p.type() == HttpParameterType.URL }
                    .map { p: ParsedHttpParameter -> p.name() + "=" + p.value() }
                    .collect(Collectors.joining("&"))

                LogEntryField.STATUS -> response?.statusCode()
                LogEntryField.RESPONSE_HTTP_VERSION -> response?.httpVersion()
                LogEntryField.PROTOCOL -> getProtocol()
                LogEntryField.HOST -> getProtocol() + "://" + getHostname()
                LogEntryField.HOSTNAME -> getHostname()
                LogEntryField.RESPONSE_CONTENT_TYPE, LogEntryField.MIME_TYPE -> response?.statedMimeType()
                LogEntryField.RESPONSE_LENGTH -> response?.body()?.length()
                LogEntryField.PORT -> request.httpService().port()
                LogEntryField.METHOD -> request.method()
                LogEntryField.REQUEST_TIME -> this.requestDateTime
                LogEntryField.RESPONSE_TIME -> this.responseDateTime
                LogEntryField.REQUEST_CONTENT_TYPE -> request.headers().stream()
                    .filter { h: HttpHeader -> h.name().equals("content-type", ignoreCase = true) }
                    .map { obj: HttpHeader -> obj.value() }.findFirst().orElse("")

                LogEntryField.REQUEST_HTTP_VERSION -> request.httpVersion()
                LogEntryField.EXTENSION -> {
                    request.pathWithoutQuery()
                        .substringAfterLast("/", "")
                        .substringAfterLast(".", "")
                }

                LogEntryField.REFERRER -> request.headers().stream()
                    .filter { h: HttpHeader -> h.name().equals("referer", ignoreCase = true) }
                    .map { obj: HttpHeader -> obj.value() }.findFirst().orElse("")

                LogEntryField.PARAMETERS -> request.parameters().stream()
                    .filter { p: ParsedHttpParameter -> p.type() != HttpParameterType.COOKIE }
                    .map { p: ParsedHttpParameter -> p.name() + "=" + p.value() }
                    .collect(Collectors.joining("&"))

                LogEntryField.PARAMETER_COUNT -> request.parameters().size
                LogEntryField.HAS_GET_PARAM -> request.parameters().stream()
                    .anyMatch { p: ParsedHttpParameter -> p.type() == HttpParameterType.URL }

                LogEntryField.HAS_POST_PARAM -> request.body().length() > 0
                LogEntryField.HAS_COOKIE_PARAM -> request.parameters().stream()
                    .anyMatch { p: ParsedHttpParameter -> p.type() == HttpParameterType.COOKIE }

                LogEntryField.REQUEST_LENGTH -> request.body().length()
                LogEntryField.INFERRED_TYPE -> response?.inferredMimeType()
                LogEntryField.HAS_SET_COOKIES -> response?.cookies()?.isNotEmpty()
                LogEntryField.HAS_PARAMS -> request.parameters().isNotEmpty()
                LogEntryField.ISSSL -> request.httpService().secure()
                LogEntryField.NEW_COOKIES -> response?.cookies()?.stream()
                    ?.map { obj: Cookie -> obj.name() }
                    ?.collect(Collectors.joining(", "))

                LogEntryField.COMPLETE -> this.response != null
                LogEntryField.SENT_COOKIES -> request.parameters().stream()
                    .filter { p: ParsedHttpParameter -> p.type() == HttpParameterType.COOKIE }
                    .map { obj: ParsedHttpParameter -> "${obj.name()}=${obj.value()}" }
                    .collect(Collectors.joining(", "))

                LogEntryField.ORIGIN -> request.headers().stream()
                    .filter { h: HttpHeader -> h.name().equals("origin", ignoreCase = true) }
                    .map { obj: HttpHeader -> obj.value() }.findFirst().orElse("")

                LogEntryField.REFLECTED_PARAMS -> reflectedParameters
                LogEntryField.REFLECTION_COUNT -> reflectedParameters.size
                LogEntryField.REQUEST_BODY -> request.bodyToString()
                LogEntryField.REQUEST_BODY_LENGTH -> request.body()?.length()
                LogEntryField.RESPONSE_BODY -> response?.bodyToString()
                LogEntryField.RESPONSE_BODY_LENGTH -> response?.body()?.length()
                LogEntryField.RTT -> {
                    if (responseDateTime == null) {
                        -1
                    } else (responseDateTime!!.time - requestDateTime.time).toInt()
                }

                LogEntryField.REQUEST_HEADERS -> request.headers().stream()
                    .map { obj: HttpHeader -> obj.toString() }
                    .collect(Collectors.joining("\r\n"))

                LogEntryField.RESPONSE_HEADERS -> response!!.headers().stream()
                    .map { obj: HttpHeader -> obj.toString() }
                    .collect(Collectors.joining("\r\n"))

                LogEntryField.REDIRECT_URL -> response?.headers()?.stream()
                    ?.filter { h: HttpHeader -> h.name().equals("location", ignoreCase = true) }
                    ?.map { obj: HttpHeader -> obj.value() }?.findFirst()?.orElse("")

//            LogEntryField.BASE64_REQUEST -> Base64.getEncoder()
//                .encodeToString(this.requestBytes)
//
//            LogEntryField.BASE64_RESPONSE -> Base64.getEncoder()
//                .encodeToString(this.responseBytes)

                else -> return ""
            }
        }catch (e: Exception) {
            return "ERROR"
        }
    }
}

@Singleton
class LogEntryFactory @Inject constructor(
    val montoyaApi: MontoyaApi,
){
    fun fromRequest(tool: ToolType, request: HttpRequest, sentTime: Date = Date()): LogEntry{
        return LogEntry(montoyaApi, tool, request, sentTime)
    }

    fun fromRequestResponse(tool: ToolType,
                            requestResponse: HttpRequestResponse,
                            sentTime: Date = Date(),
                            responseTime: Date = Date()): LogEntry {
        return LogEntry(
            montoyaApi,
            tool,
            requestResponse.request(),
            sentTime,
            requestResponse.response(),
            responseTime
        )
    }

    fun fromProxyRequestResponse(tool: ToolType,
                                 requestResponse: ProxyHttpRequestResponse,
                                 sentTime: Date = Date(),
                                 responseTime: Date = Date()): LogEntry {
        return LogEntry(
            montoyaApi,
            tool,
            requestResponse.request(),
            sentTime,
            requestResponse.response(),
            responseTime
        )
    }
}
