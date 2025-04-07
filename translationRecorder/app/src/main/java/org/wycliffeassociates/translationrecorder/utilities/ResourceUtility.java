package org.wycliffeassociates.translationrecorder.utilities;

import android.content.Context;

public class ResourceUtility {
    public static String getStringByName(
            String name,
            Context context
    ) {
        int resourceId = context.getResources().getIdentifier(
                name,
                "string",
                context.getPackageName()
        );
        if (resourceId == 0) {
            return null;
        }
        return context.getString(resourceId);
    }
}
