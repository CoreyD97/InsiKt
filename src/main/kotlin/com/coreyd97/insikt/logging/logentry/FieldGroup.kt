package com.coreyd97.insikt.logging.logentry

import java.util.*
import java.util.function.Consumer

enum class FieldGroup(val label: String, vararg labels: String?) {
    ENTRY("Entry", "Log", "Proxy"),
    REQUEST("Request"),
    RESPONSE("Response");

    private val additionalLabels: MutableList<String?>

    init {
        additionalLabels = Arrays.asList<String?>(*labels)
    }

    companion object {
        private val groupLabelMap = HashMap<String?, FieldGroup?>()

        init {
            for (fieldGroup in entries) {
                groupLabelMap.put(
                    fieldGroup.label.uppercase(Locale.getDefault()),
                    fieldGroup
                )
                fieldGroup.additionalLabels.forEach(
                    Consumer { label: String? ->
                        groupLabelMap.put(
                            label!!.uppercase(
                                Locale.getDefault()
                            ), fieldGroup
                        )
                    })
            }
        }

        @JvmStatic
        fun findByLabel(label: String): FieldGroup? {
            return groupLabelMap.get(label.uppercase(Locale.getDefault()))
        }
    }
}
