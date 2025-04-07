package com.door43.sysutils

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.core.net.toFile
import androidx.documentfile.provider.DocumentFile
import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * This class provides some utility methods for handling files
 */
object FileUtilities {
    /**
     * Converts an input stream into a string
     * @param is
     * @return
     * @throws Exception
     */
    @Throws(IOException::class)
    fun readStreamToString(`is`: InputStream?): String {
        val reader = BufferedReader(InputStreamReader(`is`))
        val sb = StringBuilder()
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            sb.append(line).append("\n")
        }
        return sb.toString()
    }

    /**
     * Returns the contents of a file as a string
     * @param file
     * @return
     * @throws Exception
     */
    @Throws(IOException::class)
    fun readFileToString(file: File?): String {
        var fis: FileInputStream? = null
        try {
            fis = FileInputStream(file)
            val contents = readStreamToString(fis)
            fis.close()
            return contents
        } finally {
            fis?.close()
        }
    }

    /**
     * Writes a string to a file
     * @param file
     * @param contents
     * @throws IOException
     */
    @Throws(IOException::class)
    fun writeStringToFile(file: File, contents: String) {
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(file.absolutePath)
            fos.write(contents.toByteArray())
        } finally {
            fos?.close()
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copyDirectory(srcDir: File, destDir: File, filter: FileFilter?) {
        if (!srcDir.exists()) {
            throw FileNotFoundException("Source \'$srcDir\' does not exist")
        } else if (!srcDir.isDirectory) {
            throw IOException("Source \'$srcDir\' exists but is not a directory")
        } else if (srcDir.canonicalPath == destDir.canonicalPath) {
            throw IOException("Source \'$srcDir\' and destination \'$destDir\' are the same")
        } else {
            val exclusionList = arrayListOf<String>()
            if (destDir.canonicalPath.startsWith(srcDir.canonicalPath)) {
                val srcFiles = if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter)
                if (srcFiles != null && srcFiles.isNotEmpty()) {
                    val len = srcFiles.size
                    for (i in 0 until len) {
                        val srcFile = srcFiles[i]
                        val copiedFile = File(destDir, srcFile.name)
                        exclusionList.add(copiedFile.canonicalPath)
                    }
                }
            }

            doCopyDirectory(srcDir, destDir, filter, exclusionList)
        }
    }

    @Throws(IOException::class)
    private fun doCopyDirectory(
        srcDir: File,
        destDir: File,
        filter: FileFilter?,
        exclusionList: List<String>?
    ) {
        val srcFiles = if (filter == null) srcDir.listFiles() else srcDir.listFiles(filter)
        if (srcFiles == null) {
            throw IOException("Failed to list contents of $srcDir")
        } else {
            if (destDir.exists()) {
                if (!destDir.isDirectory) {
                    throw IOException("Destination \'$destDir\' exists but is not a directory")
                }
            } else if (!destDir.mkdirs() && !destDir.isDirectory) {
                throw IOException("Destination \'$destDir\' directory cannot be created")
            }

            if (!destDir.canWrite()) {
                throw IOException("Destination \'$destDir\' cannot be written to")
            } else {
                val len = srcFiles.size
                for (i in 0 until len) {
                    val srcFile = srcFiles[i]
                    val dstFile = File(destDir, srcFile.name)
                    if (exclusionList == null || !exclusionList.contains(srcFile.canonicalPath)) {
                        if (srcFile.isDirectory) {
                            doCopyDirectory(srcFile, dstFile, filter, exclusionList)
                        } else {
                            doCopyFile(srcFile, dstFile)
                        }
                    }
                }

                // reserve date
                destDir.setLastModified(srcDir.lastModified())
            }
        }
    }

    /**
     * Copies directory uri to a new directory
     */
    @JvmStatic
    fun copyDirectory(context: Context, sourceDir: Uri, destDir: File) {
        when (sourceDir.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val rootDocumentFile = DocumentFile.fromTreeUri(context, sourceDir)
                if (rootDocumentFile != null && rootDocumentFile.isDirectory) {
                    rootDocumentFile.listFiles().forEach { file ->
                        copyFile(context, file, destDir)
                    }
                }
            }
            ContentResolver.SCHEME_FILE -> {
                if (sourceDir.toFile().isDirectory) {
                    copyDirectory(File(sourceDir.path!!), destDir, null)
                }
            }
        }
    }

    /**
     * Copies directory uri that is equals to dirName to a new directory
     * @param context App context
     * @param sourceDir Directory uri
     * @param destDir Destination directory
     * @param dirName Filter to directory name
     */
    @JvmStatic
    fun copyDirectory(context: Context, sourceDir: Uri, destDir: File, dirName: String) {
        when (sourceDir.scheme) {
            ContentResolver.SCHEME_CONTENT -> {
                val rootDocumentFile = DocumentFile.fromTreeUri(context, sourceDir)
                if (rootDocumentFile != null && rootDocumentFile.isDirectory) {
                    rootDocumentFile.listFiles().forEach { file ->
                        if (file.name == dirName) {
                            file.listFiles().forEach { subFile ->
                                copyFile(context, subFile, destDir)
                            }
                        }
                    }
                }
            }
            ContentResolver.SCHEME_FILE -> {
                val rootDir = sourceDir.toFile()
                if (rootDir.isDirectory) {
                    rootDir.listFiles()?.forEach { file ->
                        if (file.isDirectory && file.name == dirName) {
                            copyDirectory(file, destDir, null)
                        }
                    }
                }
            }
        }
    }

    @JvmStatic
    fun copyFile(context: Context, file: DocumentFile, targetDir: File) {
        if (file.isDirectory) {
            // Create a corresponding directory in the cache
            val newDir = File(targetDir, file.name ?: "unnamed")
            if (!newDir.exists()) {
                newDir.mkdirs()
            }

            // Recursively copy contents
            file.listFiles().forEach { subFile ->
                copyFile(context, subFile, newDir)
            }
        } else if (file.isFile) {
            // Copy the file to the target directory
            val targetFile = File(targetDir, file.name ?: "unnamed_file")
            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    /**
     * Copies a file or directory
     * @param srcFile
     * @param destFile
     */
    @JvmStatic
    @Throws(IOException::class)
    fun copyFile(srcFile: File, destFile: File) {
        if (!srcFile.exists()) {
            throw FileNotFoundException("Source \'$srcFile\' does not exist")
        } else if (srcFile.isDirectory) {
            throw IOException("Source \'$srcFile\' exists but is a directory")
        } else if (srcFile.canonicalPath == destFile.canonicalPath) {
            throw IOException("Source \'$srcFile\' and destination \'$destFile\' are the same")
        } else {
            val parentFile = destFile.parentFile
            if (parentFile != null && !parentFile.mkdirs() && !parentFile.isDirectory) {
                throw IOException("Destination \'$parentFile\' directory cannot be created")
            } else if (destFile.exists() && !destFile.canWrite()) {
                throw IOException("Destination \'$destFile\' exists but is read-only")
            } else {
                doCopyFile(srcFile, destFile)
            }
        }
    }

    @Throws(IOException::class)
    private fun doCopyFile(srcFile: File, destFile: File) {
        if (destFile.exists() && destFile.isDirectory) {
            throw IOException("Destination \'$destFile\' exists but is a directory")
        } else {
            FileInputStream(srcFile).use { fis ->
                FileOutputStream(destFile).use { fos ->
                    val input = fis.channel
                    val output = fos.channel
                    val size = input.size()
                    var pos = 0L
                    var count = 0L

                    while (pos < size) {
                        count = if (size - pos > 31457280L) 31457280L else size - pos
                        pos += output.transferFrom(input, pos, count)
                    }
                }
            }

            if (srcFile.length() != destFile.length()) {
                throw IOException("Failed to copy full contents from \'$srcFile\' to \'$destFile\'")
            } else {
                // preserve date
                destFile.setLastModified(srcFile.lastModified())
            }
        }
    }

    /**
     * Recursively deletes a directory or just deletes the file
     * @param fileOrDirectory
     */
    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            for (child in fileOrDirectory.listFiles()) {
                deleteRecursive(child)
            }
        }
        fileOrDirectory.delete()
    }
}
