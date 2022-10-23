package org.omnirom.omnigerrit.model

import java.text.SimpleDateFormat
import java.util.*

object ChangeFilter {
    var defaultBranch: String = ""
    val gerritDataFormat by lazy {
        initDateFormat()
    }

    private fun initDateFormat(): SimpleDateFormat {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format
    }

    enum class Status {
        Merged, Open
    }

    fun createQueryString(
        branch: String = "",
        project: String = "",
        status: Status = Status.Merged
    ): String {
        val q = mutableListOf<String>()
        if (branch.isNotEmpty()) {
            q.add("branch:$branch")
        } else {
            q.add("branch:$defaultBranch")
        }
        if (project.isNotEmpty()) {
            q.add("project:$project")
        }
        when (status) {
            Status.Merged -> q.add("status:merged")
            Status.Open -> q.add("status:open")
        }
        return q.joinToString(separator = "+")
    }

}