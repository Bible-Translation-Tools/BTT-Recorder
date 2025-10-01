package org.wycliffeassociates.translationrecorder.persistance

import java.io.InputStream

interface AssetsProvider {
    fun open(path: String): InputStream
    fun list(path: String): Array<String>?
}