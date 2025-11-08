package com.coreyd97.insikt.reflection.transformer

import com.coreyd97.montoyautilities.Preference

abstract class ParameterValueTransformer internal constructor(
    val name: String
) {
    var enabled by Preference("ParamTransformer_${name}_Enabled", true)

    @Throws(Exception::class)
    abstract fun transform(value: String): String
}
