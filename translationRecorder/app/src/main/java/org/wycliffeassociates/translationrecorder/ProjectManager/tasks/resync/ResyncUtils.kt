package org.wycliffeassociates.translationrecorder.ProjectManager.tasks.resync

import java.io.File
import java.util.LinkedList

/**
 * Created by sarabiaj on 1/23/2017.
 */
object ResyncUtils {
    fun getAllTakes(root: File): List<File> {
        val dirs = root.listFiles()
        val files: MutableList<File> = LinkedList()
        if (dirs != null && dirs.isNotEmpty()) {
            for (f in dirs) {
                files.addAll(getFilesInDirectory(f.listFiles()))
            }
        }
        return files
    }

    fun getFilesInDirectory(files: Array<File>?): List<File> {
        val list: MutableList<File> = LinkedList()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    list.addAll(getFilesInDirectory(f.listFiles()))
                } else {
                    list.add(f)
                }
            }
        }
        return list
    }
}
