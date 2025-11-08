package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class XMLEscapeTransformer :
    ParameterValueTransformer("XML Escape") {

    override fun transform(value: String): String {
        return StringEscapeUtils.escapeXml11(value)
    }
}
