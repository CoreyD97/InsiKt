package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class JsonEscapeTransformer :
    ParameterValueTransformer("Json Escape") {

    override fun transform(value: String): String {
        return StringEscapeUtils.escapeJson(value)
    }
}
