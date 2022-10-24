package org.omnirom.omnigerrit.model

import androidx.annotation.Keep

@Keep
data class CommitInfo(val commit: String = "", val subject: String = "", val message: String = "") {
    fun trimmedMessage(): String {
        return message.replace("\n\n", "\n").trim()
    }
}
