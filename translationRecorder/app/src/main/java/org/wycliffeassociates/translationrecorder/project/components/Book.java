package org.wycliffeassociates.translationrecorder.project.components;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import org.wycliffeassociates.translationrecorder.Utils;
import org.wycliffeassociates.translationrecorder.utilities.ResourceUtility;

import java.util.ArrayList;

/**
 * Created by sarabiaj on 1/15/2016.
 */
public class Book extends ProjectComponent implements Parcelable {

    public static class Chunk {
        public int chapterId;
        public int chunkId;
        public int startVerse;
        public int endVerse;
    }

    private int mNumChapters;
    private String mAnthology;
    private ArrayList<ArrayList<Chunk>> mChunks;
    private int mOrder;

    public Book(String slug, String name, String anthology, int order){
        super(slug, name);
        mNumChapters = 0;
        mAnthology = anthology;
        mChunks = null;
        mOrder = order;
        mName = name;
    }

    public int getNumChapters() {
        return mNumChapters;
    }

    public String getAnthology() {
        return mAnthology;
    }

    public ArrayList<ArrayList<Chunk>> getChunks() {
        return mChunks;
    }

    public int getOrder() {
        return mOrder;
    }

    @Override
    public String getLabel(Context context) {
        StringBuilder label = new StringBuilder();
        String[] resourceLabels = mName.split(" ");
        for(String part: resourceLabels) {
            label
                    .append(" ")
                    .append(Utils.capitalizeFirstLetter(part));
        }
        return label.toString();
    }

    @Override
    public int compareTo(Object another) {
        return Integer.compare(mOrder, ((Book) another).getOrder());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSlug);
        dest.writeString(mName);
        dest.writeString(mAnthology);
        dest.writeInt(mOrder);
    }

    public static final Parcelable.Creator<Book> CREATOR = new Parcelable.Creator<>() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    public Book(Parcel in) {
        super(in);
        mAnthology = in.readString();
        mOrder = in.readInt();
    }

    public static String getLocalizedName(Context context, String slug, String name, String anthology) {
        String localizationSlug = (anthology.equals("obs")) ? "obs_book_" : "book_";
        String localizedName = ResourceUtility.getStringByName(
                localizationSlug + slug,
                context
        );

        return localizedName != null ? localizedName : name;
    }
}
