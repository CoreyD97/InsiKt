package com.coreyd97.insikt.logging

import burp.api.montoya.core.Annotations
import com.coreyd97.insikt.util.LOG_ENTRY_ID_PATTERN
import org.apache.commons.lang3.StringUtils

object LogProcessorHelper {
    fun addIdentifierInComment(identifier: Int, annotations: Annotations): Annotations {
        var annotations = annotations
        val originalComment = if (annotations.notes() != null) annotations.notes() else ""
        annotations = annotations.withNotes("$originalComment\$I:$identifier$")
        return annotations
    }

    fun extractAndRemoveIdentifierFromRequestResponseComment(annotations: Annotations): ExtractedComment {
        var annotations = annotations
        var identifier: Int? = null
        if (!StringUtils.isEmpty(annotations.notes())) {
            val matcher = LOG_ENTRY_ID_PATTERN.matcher(annotations.notes())
            if (matcher.find()) {
                identifier = matcher.group(1).toInt()
                annotations = annotations.withNotes(matcher.replaceAll(""))
            }
        }

        return ExtractedComment(identifier, annotations)
    }
}

data class ExtractedComment(val identifier: Int?, val annotations: Annotations)
