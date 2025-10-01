package org.wycliffeassociates.translationrecorder

import android.app.Activity
import android.content.pm.ActivityInfo
import android.graphics.Point
import android.view.Display
import android.view.Surface

/*
* Copyright (c) delight.im <info@delight.im>
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/** Utilities for working with screen sizes and orientations  */
object Screen {
    /**
     * Locks the screen's orientation to the current setting
     *
     * @param activity an `Activity` reference
     */
    fun lockOrientation(activity: Activity, forceLandscape: Boolean = false) {
        val isNewApi = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R

        if (forceLandscape) {
            activity.requestedOrientation = Orientation.LANDSCAPE
            return
        }

        val display: Display
        val width: Int
        val height: Int

        if (isNewApi) {
            display = activity.display
            val metrics = activity.windowManager.currentWindowMetrics
            val bounds = metrics.bounds
            width = bounds.width()
            height = bounds.height()
        } else {
            display = activity.windowManager.defaultDisplay
            val size = Point()
            display.getSize(size)
            width = size.x
            height = size.y
        }

        val rotation = display.rotation

        when (rotation) {
            Surface.ROTATION_90 -> if (width > height) {
                activity.requestedOrientation = Orientation.LANDSCAPE
            } else {
                activity.requestedOrientation = Orientation.REVERSE_PORTRAIT
            }

            Surface.ROTATION_180 -> if (height > width) {
                activity.requestedOrientation = Orientation.REVERSE_PORTRAIT
            } else {
                activity.requestedOrientation = Orientation.REVERSE_LANDSCAPE
            }

            Surface.ROTATION_270 -> if (width > height) {
                activity.requestedOrientation = Orientation.REVERSE_LANDSCAPE
            } else {
                activity.requestedOrientation = Orientation.PORTRAIT
            }

            else -> if (height > width) {
                activity.requestedOrientation = Orientation.PORTRAIT
            } else {
                activity.requestedOrientation = Orientation.LANDSCAPE
            }
        }
    }

    /**
     * Unlocks the screen's orientation in case it has been locked before
     *
     * @param activity an `Activity` reference
     */
    fun unlockOrientation(activity: Activity) {
        activity.requestedOrientation = Orientation.UNSPECIFIED
    }

    private object Orientation {
        const val LANDSCAPE: Int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        const val PORTRAIT: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        const val REVERSE_LANDSCAPE: Int = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        const val REVERSE_PORTRAIT: Int = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
        const val UNSPECIFIED: Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
}
