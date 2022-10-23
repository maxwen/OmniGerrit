package org.omnirom.omnigerrit.model

import androidx.annotation.Keep

/*"id": "demo~master~Idaf5e098d70898b7119f6f4af5a6c13343d64b57",
"project": "demo",
"branch": "master",
"change_id": "Idaf5e098d70898b7119f6f4af5a6c13343d64b57",
"subject": "One change",
"status": "NEW",
"created": "2012-07-17 07:18:30.854000000",
"updated": "2012-07-17 07:19:27.766000000",*/

@Keep
data class Change(
    val id: String = "",
    val project: String = "",
    val branch: String = "",
    val change_id: String = "",
    val subject: String = "",
    val status: String = "",
    val created: String = "",
    val updated: String = "",
    val _number: String = ""
) {
    constructor(buildImage: BuildImage) : this(
        subject = buildImage.filename,
        updated = ChangeFilter.gerritDataFormat.format(buildImage.getBuildDateInMillis())
    )

    val updatedInMillis by lazy {
        ChangeFilter.gerritDataFormat.parse(updated).time
    }
}
