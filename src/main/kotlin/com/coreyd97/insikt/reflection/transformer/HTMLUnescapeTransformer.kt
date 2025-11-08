package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class HTMLUnescapeTransformer :
    ParameterValueTransformer("HTML Unescape") {
    override fun transform(value: String): String {
        return StringEscapeUtils.unescapeHtml4(value)
    }
}
