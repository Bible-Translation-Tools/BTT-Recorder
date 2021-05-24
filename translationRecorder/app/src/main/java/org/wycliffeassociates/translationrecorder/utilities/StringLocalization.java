package org.wycliffeassociates.translationrecorder.utilities;

import android.content.res.Resources;

public class StringLocalization {
    private Resources resources;

    public StringLocalization(Resources resources) {
        this.resources = resources;
    }

    public String getBookName(String slug, String anthology, String packageName) {
        String localizationSlug = (anthology.equals("obs")) ? "obs_book_" : "book_";
        return ResourceUtility.getStringByName(localizationSlug + slug, resources, packageName);
    }
}
