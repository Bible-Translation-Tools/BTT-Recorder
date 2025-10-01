package org.wycliffeassociates.translationrecorder.project

import java.io.File
import java.util.regex.Pattern

/**
 * Created by Joe on 3/31/2017.
 */
class ProjectPatternMatcher(var regex: String, var groups: String) {

    private var mPattern: Pattern = Pattern.compile(regex)
    private lateinit var locations: IntArray

    private var mName: String? = null
    private var mMatched: Boolean = false

    var projectSlugs: ProjectSlugs? = null
        private set
    var takeInfo: TakeInfo? = null
        private set

    init {
        parseLocations()
    }

    fun matched(): Boolean {
        return mMatched
    }

    private fun parseLocations() {
        val groups = groups.split(" ")
        locations = IntArray(groups.size)
        for (i in locations.indices) {
            locations[i] = groups[i].toInt()
        }
    }

    fun match(file: File): Boolean {
        return match(file.name)
    }

    fun match(file: String): Boolean {
        if (file != mName) {
            mName = file
            mPattern.matcher(file).apply {
                find()
                if (matches()) {
                    try {
                        val values = arrayOfNulls<String>(locations.size)
                        for (i in locations.indices) {
                            if (locations[i] != -1) {
                                values[i] = group(locations[i])
                            } else {
                                values[i] = ""
                            }
                        }
                        projectSlugs = ProjectSlugs(
                            values[0]!!,
                            values[1]!!,
                            values[2]!!.toInt(),
                            values[3]!!
                        )

                        takeInfo = TakeInfo(
                            projectSlugs!!,
                            values[4],
                            values[5]!!,
                            values[6],
                            values[7]
                        )
                        mMatched = true
                    } catch (e: Exception) {
                        mMatched = false
                        mName = null
                    }
                } else {
                    mMatched = false
                    mName = null
                }
            }
        }
        return mMatched
    }
}
