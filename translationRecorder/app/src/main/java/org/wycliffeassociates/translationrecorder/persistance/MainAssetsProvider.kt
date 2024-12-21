package org.wycliffeassociates.translationrecorder.persistance

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream

class MainAssetsProvider(
    @ApplicationContext private val context: Context
) : AssetsProvider {
    override fun open(path: String): InputStream {
        return context.assets.open(path)
    }

    override fun list(path: String): Array<String>? {
        return context.assets.list(path)
    }
}