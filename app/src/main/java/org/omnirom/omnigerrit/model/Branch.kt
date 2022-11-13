package org.omnirom.omnigerrit.model

import androidx.annotation.Keep

@Keep
data class Branch(val ref: String, val revision: String)
