package com.coreyd97.insikt.reflection.filter

import burp.api.montoya.http.message.params.HttpParameter
import com.coreyd97.montoyautilities.Preference
import com.coreyd97.montoyautilities.panelBuilder
import javax.swing.*

class LengthFilter : ParameterFilter("Value Length Range") {
    private var minLength by Preference("LENGTH_MIN_PREF", 3)
    private var maxLength by Preference("LENGTH_MAX_PREF", 999)

    override fun isFiltered(parameter: HttpParameter): Boolean {
        val len = parameter.value().length
        return len < minLength || len > maxLength
    }

    override fun showConfigDialog() {
        val minLengthSpinner = JSpinner(SpinnerNumberModel(minLength, 0, 99999, 1))
        val maxLengthSpinner = JSpinner(SpinnerNumberModel(maxLength, 0, 99999, 1))
        val panel = panelBuilder {
            grid(0, 2) {
                gbc.gridwidth = 2
                label("Enter parameter value length range:")
                gbc.gridwidth = 1
                label("Minimum: ")
                add(minLengthSpinner)
                label("Maximum: ")
                add(maxLengthSpinner)
            }
        }

        val result = JOptionPane.showConfirmDialog(
            null, panel, "Reflection Value Length Filter",
            JOptionPane.OK_CANCEL_OPTION
        )
        if (result == JOptionPane.OK_OPTION) {
            minLength = minLengthSpinner.value as Int
            maxLength = maxLengthSpinner.value as Int
        }
    }

    companion object {
        private const val LENGTH_MIN_PREF = "lengthMinFilter"
        private const val LENGTH_MAX_PREF = "lengthMaxFilter"
    }
}
