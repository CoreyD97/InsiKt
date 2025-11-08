package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class HTMLEscapeTransformer :
    ParameterValueTransformer("HTML Escape") {

    override fun transform(value: String): String {
        return StringEscapeUtils.escapeHtml4(value)
    }
}
