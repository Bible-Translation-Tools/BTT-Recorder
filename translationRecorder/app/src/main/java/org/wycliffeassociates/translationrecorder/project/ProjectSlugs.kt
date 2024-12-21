package org.wycliffeassociates.translationrecorder.project

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by Joe on 3/31/2017.
 */
class ProjectSlugs(
    val language: String,
    val version: String,
    val bookNumber: Int,
    val book: String
) : Parcelable {

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(language)
        dest.writeString(book)
        dest.writeString(version)
        dest.writeInt(bookNumber)
    }

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readInt(),
        parcel.readString()!!
    )

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        return if (other !is ProjectSlugs) {
            false
        } else {
            (version == other.version
                    && book == other.book
                    && bookNumber == other.bookNumber
                    && language == other.language)
        }
    }

    override fun hashCode(): Int {
        var result = language.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + bookNumber
        result = 31 * result + book.hashCode()
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ProjectSlugs?> = object : Parcelable.Creator<ProjectSlugs?> {
            override fun createFromParcel(`in`: Parcel): ProjectSlugs {
                return ProjectSlugs(`in`)
            }
            override fun newArray(size: Int): Array<ProjectSlugs?> {
                return arrayOfNulls(size)
            }
        }
    }
}