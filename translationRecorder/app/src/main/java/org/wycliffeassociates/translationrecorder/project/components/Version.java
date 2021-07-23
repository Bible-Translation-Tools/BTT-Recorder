package org.wycliffeassociates.translationrecorder.project.components;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.wycliffeassociates.translationrecorder.TranslationRecorderApp;
import org.wycliffeassociates.translationrecorder.Utils;
import org.wycliffeassociates.translationrecorder.utilities.ResourceUtility;

/**
 * Created by sarabiaj on 3/22/2017.
 */

public class Version extends ProjectComponent implements Parcelable {

    public Version(String slug, String name) {
        super(slug, name);
    }

    @Override
    public String getLabel() {
        String label = "";
        label += mSlug.toUpperCase();
        label += ":";

        Context ctx = TranslationRecorderApp.getContext();
        String name = ResourceUtility.getStringByName(
                mSlug,
                ctx.getResources(),
                ctx.getPackageName()
        );

        name = (name == null)? mName : name;

        String[] resourceLabels = name.split(" ");
        for(String part : resourceLabels) {
            label += " " + Utils.capitalizeFirstLetter(part);
        }
        return label;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSlug);
        dest.writeString(mName);
    }

    public static final Parcelable.Creator<Version> CREATOR = new Parcelable.Creator<Version>() {
        public Version createFromParcel(Parcel in) {
            return new Version(in);
        }

        public Version[] newArray(int size) {
            return new Version[size];
        }
    };

    public Version(Parcel in) {
        super(in);
    }
}
