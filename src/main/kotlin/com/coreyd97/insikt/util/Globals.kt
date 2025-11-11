package com.coreyd97.insikt.util

import com.coreyd97.insikt.logging.logentry.LogEntryField
import org.apache.commons.text.StringEscapeUtils
import java.awt.Color
import java.util.regex.Pattern


const val APP_NAME: String = "InsiKt"
const val VERSION: String = "1.1.3"

//Preferences Keys
const val PREF_LOG_TABLE_SETTINGS: String = "tabledetailsjson"
const val PREF_LOG_LEVEL: String = "logLevel"
const val PREF_ENABLED: String = "enabled"
const val PREF_RESTRICT_TO_SCOPE: String = "restricttoscope"
const val PREF_DO_NOT_LOG_IF_MATCH_STRING: String = "donotlogifmatch_string"
const val PREF_DO_NOT_LOG_IF_MATCH: String = "donotlogifmatchfilter_filter"
const val PREF_LOG_ALL: String = "logglobal"
const val PREF_LOG_PROXY: String = "logproxy"
const val PREF_LOG_SPIDER: String = "logspider"
const val PREF_LOG_INTRUDER: String = "logintruder"
const val PREF_LOG_SCANNER: String = "logscanner"
const val PREF_LOG_REPEATER: String = "logrepeater"
const val PREF_LOG_SEQUENCER: String = "logsequencer"
const val PREF_LOG_EXTENSIONS: String = "logextender"
const val PREF_LOG_TARGET_TAB: String = "logtargettab"
const val PREF_LOG_RECORDED_LOGINS: String = "logrecordedlogins"
const val PREF_LOG_SUITE: String = "logsuite"
const val PREF_ENABLE_REFLECTIONS: String = "checkreflections"
const val PREF_FILTER_STRING: String = "filterstring"
const val PREF_COLOR_FILTERS: String = "colorfilters"
const val PREF_TAG_FILTERS: String = "tagfilters"
const val PREF_SAVED_FILTERS: String = "savedfilters"
const val PREF_SORT_COLUMN: String = "sortcolumn"
const val PREF_SORT_ORDER: String = "sortorder"
const val PREF_RESPONSE_TIMEOUT: String = "responsetimeout"
const val PREF_MAXIMUM_ENTRIES: String = "maximumentries"
const val PREF_LAYOUT: String = "layout"
const val PREF_MESSAGE_VIEW_LAYOUT: String = "msgviewlayout"
const val PREF_SEARCH_THREADS: String = "searchthreads"
const val PREF_AUTO_GREP: String = "autogrep"
const val PREF_AUTO_IMPORT_PROXY_HISTORY: String = "autoimportproxyhistory"
const val PREF_ELASTIC_ADDRESS: String = "esAddress"
const val PREF_ELASTIC_PORT: String = "esPort"
const val PREF_ELASTIC_PROTOCOL: String = "esProto"
const val PREF_ELASTIC_CLUSTER_NAME: String = "esClusterName"
const val PREF_ELASTIC_AUTH: String = "esAuth"
const val PREF_ELASTIC_API_KEY_ID: String = "esApiKeyId"
const val PREF_ELASTIC_API_KEY_SECRET: String = "esApiKeySecret"
const val PREF_ELASTIC_USERNAME: String = "esUsername"
const val PREF_ELASTIC_PASSWORD: String = "esPassword"
const val PREF_ELASTIC_INDEX: String = "esIndex"
const val PREF_ELASTIC_DELAY: String = "esDelay"
const val PREF_ELASTIC_FILTER: String = "esFilter"
const val PREF_ELASTIC_FILTER_PROJECT_PREVIOUS: String = "esFilterProjectPrevious"
const val PREF_ELASTIC_FILTER_HISTORY: String = "esFilterHistory"
const val PREF_ELASTIC_AUTOSTART_GLOBAL: String = "elasticAutostartGlobal"
const val PREF_ELASTIC_AUTOSTART_PROJECT: String = "elasticAutostartProject"
const val PREF_FILTER_HISTORY: String = "filterHistory"
const val PREF_AUTO_SCROLL: String = "autoScroll"
const val PREF_STICK_TO_TOP: String = "autoScrollTop"
const val PREF_GREP_HISTORY: String = "grepHistory"
const val PREF_ELASTIC_FIELDS: String = "elasticExportFields"
const val PREF_ELASTIC_FIELDS_PROJ: String = "elasticExportFieldsProject"
const val PREF_SAVED_FIELD_SELECTIONS: String = "savedFieldSelections"
const val PREF_COLUMNS_VERSION: String = "columnsVersion"
const val PREF_MAX_RESP_SIZE: String = "maxRespBodySize"
const val PREF_TABLE_PILL_STYLE: String = "tagsStyle"
val DEFAULT_COLOR_FILTERS_JSON: String =
    ("{\"2add8ace-b652-416a-af08-4d78c5d22bc7\":{\"uid\":\"2add8ace-b652-416a-af08-4d78c5d22bc7\","
            +
            "\"filter\":\"Request.Complete == False\",\"filterString\":\"Request.Complete == False\",\"backgroundColor\":{\"value\":-16777216,\"falpha\":0.0},"
            +
            "\"foregroundColor\":{\"value\":-65536,\"falpha\":0.0},\"enabled\":true,\"modified\":false,\"shouldRetest\":true,\"priority\":1}}")
const val CURRENT_COLUMN_VERSION: Int = 2
val LOG_ENTRY_ID_PATTERN: Pattern = Pattern.compile("\\\$I:(\\d+)\\$")

val goodFilterBackground = Color(76, 255, 155)
val badFilterBackground = Color(221, 70, 57)

private var colOrder = 0

val DEFAULT_LOG_TABLE_COLUMNS_JSON = """
    [
        {"id":"${LogEntryField.NUMBER}","name":"Number","order":${colOrder++},"visibleName":"#","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.NUMBER.description)}","defaultVisibleName":"#","width":45},
		{"id":"${LogEntryField.TAGS}","name":"Tags","order":${colOrder++},"visibleName":"Tags","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.TAGS.description)}","defaultVisibleName":"Tags","width":141},
		{"id":"${LogEntryField.INSCOPE}","name":"In Scope","order":${colOrder++},"visibleName":"In Scope","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.INSCOPE.description)}","defaultVisibleName":"In Scope","width":141},
		{"id":"${LogEntryField.COMPLETE}","name":"Complete","order":${colOrder++},"visibleName":"Complete","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.COMPLETE.description)}","defaultVisibleName":"Complete","width":58},
		{"id":"${LogEntryField.REQUEST_TOOL}","name":"Tool","order":${colOrder++},"visibleName":"Tool","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_TOOL.description)}","defaultVisibleName":"Tool","width":62},
		{"id":"${LogEntryField.ISSSL}","name":"IsSSL","order":${colOrder++},"visibleName":"SSL","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.ISSSL.description)}","defaultVisibleName":"SSL","width":75},
		{"id":"${LogEntryField.METHOD}","name":"Method","order":${colOrder++},"visibleName":"Method","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.METHOD.description)}","defaultVisibleName":"Method","width":69},
		{"id":"${LogEntryField.HOST}","name":"Host","order":${colOrder++},"visibleName":"Host","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HOST.description)}","defaultVisibleName":"Host","width":194},
		{"id":"${LogEntryField.PROTOCOL}","name":"Protocol","order":${colOrder++},"visibleName":"Protocol","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.PROTOCOL.description)}","defaultVisibleName":"Protocol","width":75},
		{"id":"${LogEntryField.PATH}","name":"Path","order":${colOrder++},"visibleName":"Path","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.PATH.description)}","defaultVisibleName":"Path","width":614},
		{"id":"${LogEntryField.HOSTNAME}","name":"Hostname","order":${colOrder++},"visibleName":"Host Name","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HOSTNAME.description)}","defaultVisibleName":"Host Name","width":75},
		{"id":"${LogEntryField.QUERY}","name":"Query","order":${colOrder++},"visibleName":"Query","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.QUERY.description)}","defaultVisibleName":"Query","width":280},
		{"id":"${LogEntryField.PORT}","name":"TargetPort","order":${colOrder++},"visibleName":"Port","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.PORT.description)}","defaultVisibleName":"Port","width":75},
		{"id":"${LogEntryField.STATUS}","name":"Status","order":${colOrder++},"visibleName":"Status","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.STATUS.description)}","defaultVisibleName":"Status","width":40},
		{"id":"${LogEntryField.RESPONSE_LENGTH}","name":"ResponseLength","order":${colOrder++},"visibleName":"Response Length","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.RESPONSE_LENGTH.description)}","defaultVisibleName":"Response Length","width":131},
		{"id":"${LogEntryField.INFERRED_TYPE}","name":"InferredType","order":${colOrder++},"visibleName":"Inferred Type","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.INFERRED_TYPE.description)}","defaultVisibleName":"Inferred Type","width":143},
		{"id":"${LogEntryField.EXTENSION}","name":"UrlExtension","order":${colOrder++},"visibleName":"Extension","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.EXTENSION.description)}","defaultVisibleName":"Extension","width":75},
		{"id":"${LogEntryField.PARAMETER_COUNT}","name":"ParameterCount","order":${colOrder++},"visibleName":"Parameter Count","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.PARAMETER_COUNT.description)}","defaultVisibleName":"Parameter Count","width":123},
		{"id":"${LogEntryField.REFLECTED_PARAMS}","name":"ReflectedParams","order":${colOrder++},"visibleName":"Reflected Params","visible":true,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REFLECTED_PARAMS.description)}","defaultVisibleName":"Reflected Params","width":141},
		{"id":"${LogEntryField.URL}","name":"Url","order":${colOrder++},"visibleName":"URL","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.URL.description)}","defaultVisibleName":"URL","width":75},
		{"id":"${LogEntryField.NEW_COOKIES}","name":"NewCookies","order":${colOrder++},"visibleName":"New Cookies","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.NEW_COOKIES.description)}","defaultVisibleName":"New Cookies","width":114},
		{"id":"${LogEntryField.HAS_PARAMS}","name":"Has Params","order":${colOrder++},"visibleName":"Has Params","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HAS_PARAMS.description)}","defaultVisibleName":"Has Params","width":75},
		{"id":"${LogEntryField.REQUEST_TIME}","name":"RequestTime","order":${colOrder++},"visibleName":"Request Time","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_TIME.description)}","defaultVisibleName":"Request Time","width":90},
		{"id":"${LogEntryField.RESPONSE_TIME}","name":"ResponseTime","order":${colOrder++},"visibleName":"Response Time","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.RESPONSE_TIME.description)}","defaultVisibleName":"Response Time","width":95},
		{"id":"${LogEntryField.REQUEST_LENGTH}","name":"RequestLength","order":${colOrder++},"visibleName":"Request Length","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_LENGTH.description)}","defaultVisibleName":"Request Length","width":75},
		{"id":"${LogEntryField.RTT}","name":"RTT","order":${colOrder++},"visibleName":"RTT (ms)","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.RTT.description)}","defaultVisibleName":"RTT (ms)","width":101},
		{"id":"${LogEntryField.LISTENER_INTERFACE}","name":"ListenerInterface","order":${colOrder++},"visibleName":"Proxy Listener Interface","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.LISTENER_INTERFACE.description)}","defaultVisibleName":"Proxy Listener Interface","width":106},
		{"id":"${LogEntryField.PARAMETERS}","name":"Parameters","order":${colOrder++},"visibleName":"Parameters","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.PARAMETERS.description)}","defaultVisibleName":"Parameters","width":75},
		{"id":"${LogEntryField.REFLECTION_COUNT}","name":"ReflectionCount","order":${colOrder++},"visibleName":"Reflection Count","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REFLECTION_COUNT.description)}","defaultVisibleName":"Reflection Count","width":75},
		{"id":"${LogEntryField.ORIGIN}","name":"origin","order":${colOrder++},"visibleName":"Origin header","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.ORIGIN.description)}","defaultVisibleName":"Origin header","width":75},
		{"id":"${LogEntryField.MIME_TYPE}","name":"MimeType","order":${colOrder++},"visibleName":"MIME type","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.MIME_TYPE.description)}","defaultVisibleName":"MIME type","width":75},
		{"id":"${LogEntryField.RESPONSE_CONTENT_TYPE}","name":"ResponseContentType","order":${colOrder++},"visibleName":"Response Content-Type","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.RESPONSE_CONTENT_TYPE.description)}","defaultVisibleName":"Response Content-Type","width":75},
		{"id":"${LogEntryField.HAS_GET_PARAM}","name":"HasQueryStringParam","order":${colOrder++},"visibleName":"Query String?","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HAS_GET_PARAM.description)}","defaultVisibleName":"Query String?","width":75},
		{"id":"${LogEntryField.HAS_POST_PARAM}","name":"HasBodyParam","order":${colOrder++},"visibleName":"Body Params?","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HAS_POST_PARAM.description)}","defaultVisibleName":"Body Params?","width":75},
		{"id":"${LogEntryField.HAS_COOKIE_PARAM}","name":"HasCookieParam","order":${colOrder++},"visibleName":"Sent Cookie?","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HAS_COOKIE_PARAM.description)}","defaultVisibleName":"Sent Cookie?","width":75},
		{"id":"${LogEntryField.SENT_COOKIES}","name":"SentCookies","order":${colOrder++},"visibleName":"Sent Cookies","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.SENT_COOKIES.description)}","defaultVisibleName":"Sent Cookies","width":75},
		{"id":"${LogEntryField.REQUEST_CONTENT_TYPE}","name":"RequestContentType","order":${colOrder++},"visibleName":"Request Type","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_CONTENT_TYPE.description)}","defaultVisibleName":"Request Content Type","width":75},
		{"id":"${LogEntryField.REFERRER}","name":"Referrer","order":${colOrder++},"visibleName":"Referrer","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REFERRER.description)}","defaultVisibleName":"Referrer","width":75},
		{"id":"${LogEntryField.REDIRECT_URL}","name":"Redirect","order":${colOrder++},"visibleName":"Redirect","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REDIRECT_URL.description)}","defaultVisibleName":"Redirect","width":75},
		{"id":"${LogEntryField.HAS_SET_COOKIES}","name":"HasSetCookies","order":${colOrder++},"visibleName":"Set-Cookie?","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.HAS_SET_COOKIES.description)}","defaultVisibleName":"Set-Cookie?","width":75},
		{"id":"${LogEntryField.REQUEST_BODY_LENGTH}","name":"RequestBodyLength","order":${colOrder++},"visibleName":"Request Body Length","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_BODY_LENGTH.description)}","defaultVisibleName":"Request Body Length","width":75},
		{"id":"${LogEntryField.REQUEST_HEADERS}","name":"RequestHeaders","order":${colOrder++},"visibleName":"Request Headers","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_HEADERS.description)}","defaultVisibleName":"Request Headers","width":75},
		{"id":"${LogEntryField.REQUEST_BODY_LENGTH}","name":"ResponseBodyLength","order":${colOrder++},"visibleName":"Response Body Length","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.REQUEST_BODY_LENGTH.description)}","defaultVisibleName":"Response Body Length","width":75},
		{"id":"${LogEntryField.RESPONSE_HEADERS}","name":"ResponseHeaders","order":${colOrder++},"visibleName":"Response Headers","visible":false,"readOnly":true,"description":"${StringEscapeUtils.escapeJson(LogEntryField.RESPONSE_HEADERS.description)}","defaultVisibleName":"Response Headers","width":75}
    ]
    """.trimIndent()

val DEFAULT_LIBRARY_ENTRIES = """
    [
        {"uuid":"1e763a2f-82f6-45e7-a2c8-9f9fa144ccac","name":"AttachmentDisposition","filterString":"Response.Headers MATCHES \".*Content-Disposition:\\s*attachment.*\"","shouldRetest":true},
        {"uuid":"5719b014-2a7f-4319-8605-c7fb6a7ab60e","name":"ContentTypeSniffing","filterString":"Response.MimeType == \"HTML\" AND Response.InferredType != \"HTML\" AND Response.Length > 0","shouldRetest":true},
        {"uuid":"b9c2bfd9-5ba7-454e-bc69-d935fde1d762","name":"NoSniff","filterString":"!(Response.Headers MATCHES \".*X-Content-Type-Options:\\\\s*nosniff.*\")","shouldRetest":true},
        {"uuid":"b9c9e48b-9c18-488f-a601-bc2df07253b9","name":"ContentTypeMismatch","filterString":"Response.InferredType IN [\"HTML\", \"SCRIPT\", \"JSON\", \"PLAIN_TEXT\"] AND (Response.InferredType != Response.MimeType) AND !(#NoSniff OR #AttachmentDisposition)","shouldRetest":true},
        {"uuid":"6ccfac17-5618-4dc2-8afa-b4548cb9b359","name":"PotentialCSRF","filterString":"Request.Method == \"POST\" AND !(Request.Parameters MATCHES \".*(csrf|xsrf|authenticity_token|__requestverificationtoken).*\") AND !(Request.Headers CONTAINS \"Authorization:\") AND !(Request.Headers MATCHES \".*(XSRF|CSRF).*\")","shouldRetest":true},
        {"uuid":"833363aa-1a14-4b30-8e76-a0deb39acdbf","name":"HasPathTraversalPayload","filterString":"(Request.Path MATCHES \".*(\\.|%(25)?2e){2}(/|%(25)?2f).*\" OR Request.Parameters MATCHES \".*(\\.|%(25){2}2e)+(/|%(25)?2f).*\")","shouldRetest":true},
        {"uuid":"79a36dc3-25c2-4449-aac5-548baee8b392","name":"DirListing","filterString":"Response.InferredType == \"HTML\" AND Response.Body MATCHES \".*(Index of\\s|Directory listing for|Parent Directory).*\"","shouldRetest":true},
        {"uuid":"287d43f6-bbd1-482b-bfdc-5590ed0ef64d","name":"ACAO","filterString":"Response.Headers MATCHES \".*Access-Control-Allow-Credentials:\\s*true.*\" AND Response.Headers MATCHES \".*Access-Control-Allow-Origin:\\s*\\*.*\"","shouldRetest":true},
        {"uuid":"a7166326-1f78-4b38-9763-d6b4c1404875","name":"ExternalRedirect","filterString":"Response.Status IN [301, 302, 307, 308] AND Response.Redirect MATCHES \"https?://.*\" AND !(Response.Redirect CONTAINS Request.Hostname)","shouldRetest":true},
        {"uuid":"64e23a0d-c082-4b96-987b-a45659d0ef2c","name":"HasUrlParam","filterString":"Request.Parameters MATCHES \".*=https?(%3A%2F%2F|://).+\" AND Response.Redirect == \"\"","shouldRetest":true},
        {"uuid":"a32f77eb-1630-40eb-802a-567bbdbfad58","name":"MissingCSP","filterString":"Response.InferredType == \"HTML\" AND !(Response.Headers MATCHES \".*Content-Security-Policy:.*\")","shouldRetest":true},
        {"uuid":"d27de0fe-2678-4218-9a72-44001d006c23","name":"CachedSensitive","filterString":"(Request.Path MATCHES \".*(login|account|user|profile|checkout|cart|settings).*\") AND (Response.Headers MATCHES \".*Cache-Control:\\\\s*(public|max-age=([0-9]{2,})).*\")","shouldRetest":true},
        {"uuid":"d87832c2-7032-4ce2-9acc-525bc8121f80","name":"Navigate","filterString":"Request.Headers CONTAINS \"Sec-Fetch-Mode: navigate\"","shouldRetest":true}
    ]
""".trimIndent()

enum class ElasticAuthType {
    ApiKey, Basic, None
}

enum class Protocol {
    HTTP, HTTPS
}
