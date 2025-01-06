package org.wycliffeassociates.translationrecorder.project.components

import android.annotation.SuppressLint
import java.io.File

/**
 * Created by sarabiaj on 5/1/2018.
 */
class User(
    val audio: File? = null,
    val hash: String? = null,
    var id: Int = 0
) {
    @SuppressLint("DefaultLocale")
    override fun toString(): String {
        return String.format("{\"id\":%d, \"hash\":\"%s\", \"audio\":\"%s\"}", id, hash, audio)
    }
}
