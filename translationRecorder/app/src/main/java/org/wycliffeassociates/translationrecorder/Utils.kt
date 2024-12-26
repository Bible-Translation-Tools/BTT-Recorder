package org.wycliffeassociates.translationrecorder

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import com.door43.tools.reporting.Logger
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import java.io.BufferedInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Created by sarabiaj on 7/1/2016.
 */
object Utils {
    fun swapViews(toShow: Array<View>, toHide: Array<View>) {
        for (v in toShow) {
            v.visibility = View.VISIBLE
        }
        for (v in toHide) {
            v.visibility = View.INVISIBLE
        }
    }

    fun closeKeyboard(ctx: Activity) {
        val inputManager = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (ctx.currentFocus != null) {
            inputManager.hideSoftInputFromWindow(
                ctx.currentFocus!!.windowToken,
                InputMethodManager.HIDE_NOT_ALWAYS
            )
        }
    }

    //http://stackoverflow.com/questions/13410949/how-to-delete-folder-from-internal-storage-in-android
    @JvmStatic
    fun deleteRecursive(fileOrDirectory: File) {
        if (fileOrDirectory.isDirectory) {
            val files = fileOrDirectory.listFiles()
            if (files != null) {
                for (child in files) {
                    deleteRecursive(child)
                }
            }
        }
        fileOrDirectory.delete()
    }

    // http://stackoverflow.com/questions/5725892/how-to-capitalize-the-first-letter-of-word-in-a-string-using-java
    @JvmStatic
    fun capitalizeFirstLetter(string: String): String {
        return string.replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(Locale.getDefault())
            } else it.toString()
        }
    }

    fun showView(view: View?) {
        if (view == null) {
            Logger.i("Utils.showView()", "A null view is trying to be shown")
            return
        }
        view.visibility = View.VISIBLE
    }

    fun showView(views: Array<View?>) {
        for (v in views) {
            showView(v)
        }
    }

    fun hideView(view: View?) {
        if (view == null) {
            Logger.i("Utils.hideView()", "A null view is trying to be hid")
            return
        }
        view.visibility = View.GONE
    }

    fun hideView(views: Array<View?>) {
        for (v in views) {
            hideView(v)
        }
    }

    fun getUriDisplayName(context: Context, uri: Uri): String {
        val defaultName = "unnamed.file"

        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(
                    uri,
                    null,
                    null,
                    null,
                    null
                ).use { returnCursor ->
                    return returnCursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)?.let { nameIndex ->
                        returnCursor.moveToFirst()
                        returnCursor.getString(nameIndex)
                    } ?: defaultName
                }
            }
            "file" -> uri.lastPathSegment ?: defaultName
            else -> defaultName
        }
    }

    fun copySourceAudio(
        context: Context,
        directoryProvider: IDirectoryProvider,
        uri: Uri
    ): File? {
        val filename = getUriDisplayName(context, uri)
        try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                inputStream?.let {
                    BufferedInputStream(inputStream).use { bufferedStream ->
                        bufferedStream.mark(bufferedStream.available())

                        val magicNumber = ByteArray(4)
                        bufferedStream.read(magicNumber)
                        val header = String(magicNumber, StandardCharsets.US_ASCII)
                        bufferedStream.reset()

                        //aoc was an accident in a previous version
                        if (header == "aoh!" || header == "aoc!") {
                            val target = File(directoryProvider.sourceAudioDir, filename)
                            target.outputStream().use { targetStream ->
                                bufferedStream.copyTo(targetStream)
                            }
                            if (target.exists() && target.length() > 0) {
                                return target
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.e("Utils.copySourceAudio()", "Error copying source audio", e)
        }

        return null
    }
}
