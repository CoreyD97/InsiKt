package com.coreyd97.insikt.reflection.filter

import burp.api.montoya.http.message.params.HttpParameter
import com.coreyd97.montoyautilities.Preference

abstract class ParameterFilter internal constructor(
    val name: String
) {
   var enabled by Preference("ParamFilter_${name}_Enabled", true)

    abstract fun isFiltered(parameter: HttpParameter): Boolean

    abstract fun showConfigDialog()
}
