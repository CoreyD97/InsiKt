package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class JsonUnescapeTransformer :
    ParameterValueTransformer("Json Unescape") {

    override fun transform(value: String): String {
        return StringEscapeUtils.unescapeJson(value)
    }
}
