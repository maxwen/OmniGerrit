package org.omnirom.omnigerrit.model

import java.text.SimpleDateFormat
import java.util.*

object ChangeFilter {
    var defaultBranch: String = ""
    val gerritDateTimeFormat by lazy {
        initDateFormat()
    }

    private val hideProjectList = listOf("android_device_", "android_hardware_", "android_kernel_")

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
        status: Status = Status.Merged,
        message: String = ""
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
        if (message.isNotEmpty()) {
            q.add("message:$message")
        }
        when (status) {
            Status.Merged -> q.add("status:merged")
            Status.Open -> q.add("status:open")
        }
        return q.joinToString(separator = "+")
    }


    /*if (c.project.startsWith("android_device_")) hidden = true
    if (c.project.startsWith("android_hardware_")) hidden = true
    if (c.project.startsWith("android_kernel_")) hidden = true
    if (mUseProjectsList.contains(c.project)) hidden = false*/

    fun showProject(project: String): Boolean {
        return hideProjectList.none() { project.startsWith(it) }
    }
}