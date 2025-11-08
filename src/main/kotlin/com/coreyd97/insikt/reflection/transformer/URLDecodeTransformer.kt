package com.coreyd97.insikt.reflection.transformer

import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class URLDecodeTransformer :
    ParameterValueTransformer("URL Decode") {

    @Throws(UnsupportedEncodingException::class)
    override fun transform(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8)
    }
}
