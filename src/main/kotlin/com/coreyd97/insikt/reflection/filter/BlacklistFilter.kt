package com.coreyd97.insikt.reflection.filter

import burp.api.montoya.http.message.params.HttpParameter
import com.coreyd97.insikt.util.MoreHelp
import com.coreyd97.montoyautilities.Preference
import org.apache.commons.lang3.StringUtils

class BlacklistFilter : ParameterFilter("Value Blacklist") {
    private val blacklist by Preference(BLACKLIST_PREF, mutableSetOf("0", "1", "true", "false"))

    override fun isFiltered(parameter: HttpParameter): Boolean {
        return blacklist.contains(parameter.value())
    }

    override fun showConfigDialog() {
        val valueString = MoreHelp.showPlainInputMessage(
            "Enter comma separated blacklist values:",
            "Parameter Value Blacklist", StringUtils.join(blacklist, ",")
        )!!

        val values = valueString.split(",").dropLastWhile { it.isEmpty() }.toTypedArray()
        blacklist.clear()
        blacklist.addAll(values.filter { it.isNotEmpty() })
    }

    companion object {
        private const val BLACKLIST_PREF = "parameterValueBlacklist"
    }
}
