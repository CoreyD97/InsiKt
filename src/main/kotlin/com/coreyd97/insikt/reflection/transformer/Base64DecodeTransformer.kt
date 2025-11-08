package com.coreyd97.insikt.reflection.transformer

import java.io.UnsupportedEncodingException
import java.util.*

class Base64DecodeTransformer :
    ParameterValueTransformer("Base64 Decode") {

    @Throws(UnsupportedEncodingException::class)
    override fun transform(value: String): String {
        return String(Base64.getDecoder().decode(value.toByteArray()))
    }
}
