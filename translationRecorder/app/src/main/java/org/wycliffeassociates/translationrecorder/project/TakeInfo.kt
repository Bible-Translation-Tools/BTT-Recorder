package org.wycliffeassociates.translationrecorder.project

import android.os.Parcel
import android.os.Parcelable

/**
 * Created by sarabiaj on 4/17/2017.
 */
class TakeInfo(
    val projectSlugs: ProjectSlugs,
    val chapter: Int = 0,
    val startVerse: Int,
    val mEndVerse: Int = 0,
    val take: Int = 0
) : Parcelable {

    constructor(
        slugs: ProjectSlugs,
        chapter: String?,
        startVerse: String,
        endVerse: String?,
        take: String?
    ) : this(
        slugs,
        if (!chapter.isNullOrEmpty()) chapter.toInt() else 1,
        startVerse.toInt(),
        endVerse?.toInt() ?: -1,
        take?.toInt() ?: 0
    )

    val endVerse: Int
        get() {
            //if there is no end verse, there is no verse range, so the end verse is the start verse
            if (mEndVerse == -1) {
                return startVerse
            }
            return mEndVerse
        }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(projectSlugs, flags)
        dest.writeInt(chapter)
        dest.writeInt(startVerse)
        dest.writeInt(mEndVerse)
        dest.writeInt(take)
    }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable(ProjectSlugs::class.java.classLoader)!!,
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt()
    )

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        return if (other !is TakeInfo) {
            false
        } else {
            (projectSlugs == other.projectSlugs
                    && chapter == other.chapter
                    && startVerse == other.startVerse
                    && endVerse == other.endVerse)
        }
    }

    override fun hashCode(): Int {
        var result = projectSlugs.hashCode()
        result = 31 * result + chapter
        result = 31 * result + startVerse
        result = 31 * result + mEndVerse
        result = 31 * result + take
        return result
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<TakeInfo?> = object : Parcelable.Creator<TakeInfo?> {
            override fun createFromParcel(parcel: Parcel): TakeInfo {
                return TakeInfo(parcel)
            }
            override fun newArray(size: Int): Array<TakeInfo?> {
                return arrayOfNulls(size)
            }
        }
    }
}
