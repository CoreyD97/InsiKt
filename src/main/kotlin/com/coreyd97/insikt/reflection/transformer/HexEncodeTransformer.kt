package com.coreyd97.insikt.reflection.transformer

import org.apache.commons.codec.binary.Hex
import java.io.UnsupportedEncodingException

class HexEncodeTransformer :
    ParameterValueTransformer("Hex Encode") {

    @Throws(UnsupportedEncodingException::class)
    override fun transform(value: String): String {
        return Hex.encodeHexString(value.toByteArray())
    }
}
