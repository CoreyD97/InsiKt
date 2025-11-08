package com.coreyd97.insikt.reflection.transformer

import java.io.UnsupportedEncodingException
import java.util.*

class Base64EncodeTransformer :
    ParameterValueTransformer("Base64 Encode") {

    @Throws(UnsupportedEncodingException::class)
    override fun transform(value: String): String {
        return String(Base64.getEncoder().encode(value.toByteArray()))
    }
}
