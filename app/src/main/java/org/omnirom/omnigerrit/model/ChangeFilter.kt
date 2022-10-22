package org.omnirom.omnigerrit.model

object ChangeFilter {
    var defaultBranch: String = ""

    enum class Status {
        Merged, Open
    }

    fun createQueryString(branch: String = "", project: String = "", status: Status = Status.Merged): String {
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