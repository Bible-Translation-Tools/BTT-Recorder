package org.wycliffeassociates.translationrecorder.project.components

import android.os.Parcel
import android.os.Parcelable
import org.wycliffeassociates.translationrecorder.Utils

class Language : ProjectComponent, Parcelable {
    constructor(slug: String?, name: String?) : super(slug, name)

    override fun getLabel(): String {
        return Utils.capitalizeFirstLetter(mName)
    }

    override fun displayItemIcon(): Boolean {
        return false
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(mSlug)
        dest.writeString(mName)
    }

    constructor(parcel: Parcel) : super(parcel)

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<Language> = object : Parcelable.Creator<Language> {
            override fun createFromParcel(parcel: Parcel): Language {
                return Language(parcel)
            }

            override fun newArray(size: Int): Array<Language?> {
                return arrayOfNulls(size)
            }
        }
    }
}