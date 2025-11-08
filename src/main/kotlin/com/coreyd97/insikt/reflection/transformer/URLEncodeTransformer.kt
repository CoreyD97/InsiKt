package com.coreyd97.insikt.reflection.transformer

import java.io.UnsupportedEncodingException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class URLEncodeTransformer :
    ParameterValueTransformer("URL Encode") {

    @Throws(UnsupportedEncodingException::class)
    override fun transform(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
    }
}
