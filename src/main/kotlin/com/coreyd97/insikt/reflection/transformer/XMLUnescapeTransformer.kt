package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.text.StringEscapeUtils

class XMLUnescapeTransformer :
    ParameterValueTransformer("XML Unescape") {

    override fun transform(value: String): String {
        return StringEscapeUtils.unescapeXml(value)
    }
}
