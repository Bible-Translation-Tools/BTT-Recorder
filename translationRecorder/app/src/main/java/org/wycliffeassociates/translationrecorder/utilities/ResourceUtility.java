package org.wycliffeassociates.translationrecorder.utilities;

import android.content.res.Resources;

public class ResourceUtility {
    public static String getStringByName(String name, Resources resources, String packageName) {
        int resourceId = resources.getIdentifier(name, "string", packageName);
        return resources.getString(resourceId);
    }

    public static String getStringByName(String name, String resourceType,
                                         Resources resources, String packageName) {
        int resourceId = resources.getIdentifier(name, resourceType, packageName);
        return resources.getString(resourceId);
    }
}
