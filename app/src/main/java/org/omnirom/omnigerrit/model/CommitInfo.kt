package org.omnirom.omnigerrit.model

import androidx.annotation.Keep

@Keep
data class CommitInfo(val commit: String = "", val subject: String = "", val message: String = "") {
    fun trimmedMessage(): String {
        var strippedMessage = ""
        message.replace("\n\n", "\n").trim().split("\n")
            .filter { line -> !line.startsWith("Change-Id") }
            .forEach { line -> strippedMessage += line + "\n" }
        return strippedMessage.trim()
    }
}
