package org.wycliffeassociates.translationrecorder.recordingapp.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import org.wycliffeassociates.translationrecorder.project.ProjectSlugs
import org.wycliffeassociates.translationrecorder.project.TakeInfo

/**
 * Created by sarabiaj on 4/25/2017.
 */
@RunWith(AndroidJUnit4::class)
class ProjectPatternMatcherTest {
    @Test
    fun testProjectPatternMatcher() {
        val regexBible =
            "([a-zA-Z]{2,3}[-[\\d\\w]+]*)_([a-zA-Z]{3})_b([\\d]{2})_([1-3]*[a-zA-Z]+)_c([\\d]{2,3})_v([\\d]{2,3})(-([\\d]{2,3}))?(_t([\\d]{2}))?(.wav)?"
        //lang, version booknum book chapter start end take
        val groupsBible = "1 2 3 4 5 6 8 10"

        val ppmOT = ProjectPatternMatcher(regexBible, groupsBible)
        val ppmNT = ProjectPatternMatcher(regexBible, groupsBible)

        val validOTChunks =
            listOf(
                "en_ulb_b01_gen_c01_v01-02_t02.wav"
            )
        val validOTChunkSlugs =
            listOf(
                ProjectSlugs("en", "ulb", 1, "gen")
            )
        val validOTChunkInfo =
            listOf(
                TakeInfo(validOTChunkSlugs[0], 1, 1, 2, 2)
            )

        val validOTVerses =
            listOf(
                "en_ulb_b01_gen_c03_v16_t02.wav"
            )
        val validOTVerseSlugs =
            listOf(
                ProjectSlugs("en", "ulb", 1, "gen")
            )
        val validOTVerseInfo =
            listOf(
                TakeInfo(validOTVerseSlugs[0], 3, 16, -1, 2)
            )

        val invalidOTChunks =
            listOf(
                "en_ulb_gen_c01_v01-02_t02.wav",
                "en_ulb_b01_gen_c01_v01-02-t02.wav"
            )
        val invalidOTVerses =
            listOf(
                "en_ulb_b01_gen_c01_v0102_t02.wav",
                "en_ulb_b01_gen_c01_v01-02-t02.wav",
                "en_ulb_gen_c01_v01-02_t02.wav"
            )

        matchStrings(ppmOT, validOTChunks, true, validOTChunkSlugs, validOTChunkInfo)
        matchStrings(ppmOT, validOTVerses, true, validOTVerseSlugs, validOTVerseInfo)
        matchStrings(ppmOT, invalidOTChunks, false, listOf(), listOf())
        matchStrings(ppmOT, invalidOTVerses, false, listOf(), listOf())

        matchStrings(ppmNT, validOTChunks, true, validOTChunkSlugs, validOTChunkInfo)
        matchStrings(ppmNT, validOTVerses, true, validOTVerseSlugs, validOTVerseInfo)
        matchStrings(ppmNT, invalidOTChunks, false, listOf(), listOf())
        matchStrings(ppmNT, invalidOTVerses, false, listOf(), listOf())
    }

    private fun matchStrings(
        ppm: ProjectPatternMatcher,
        strings: List<String>,
        shouldMatch: Boolean,
        slugs: List<ProjectSlugs>,
        takeInfos: List<TakeInfo>
    ) {
        for (i in strings.indices) {
            ppm.match(strings[i])
            Assert.assertEquals(strings[i], ppm.matched(), shouldMatch)
            if (ppm.matched()) {
                compareSlugs(slugs[i], ppm.projectSlugs!!)
                compareTakeInfo(takeInfos[i], ppm.takeInfo!!)
            }
        }
    }

    private fun compareTakeInfo(expected: TakeInfo, actual: TakeInfo) {
        //assertEquals(expected.getNameWithoutTake(), actual.getNameWithoutTake());
        Assert.assertEquals(expected.chapter.toLong(), actual.chapter.toLong())
        Assert.assertEquals(expected.startVerse.toLong(), actual.startVerse.toLong())
        Assert.assertEquals(expected.endVerse.toLong(), actual.endVerse.toLong())
        Assert.assertEquals(expected.take.toLong(), actual.take.toLong())
    }

    private fun compareSlugs(expected: ProjectSlugs, actual: ProjectSlugs) {
        Assert.assertEquals(expected.book, actual.book)
        Assert.assertEquals(expected.language, actual.language)
        Assert.assertEquals(expected.version, actual.version)
        Assert.assertEquals(expected.bookNumber.toLong(), actual.bookNumber.toLong())
    }
}
