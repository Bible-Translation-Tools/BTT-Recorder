import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wycliffeassociates.translationrecorder.project.ProjectPatternMatcher
import java.io.File

class ProjectPatternMatcherTest {

    private val regex = "([a-zA-Z]{2,3}[-[\\d\\w]+]*)_([a-zA-Z]{3})_b([\\d]{2})_([1-3]*[a-zA-Z]+)_c([\\d]{2,3})_v([\\d]{2,3})(-([\\d]{2,3}))?(_t([\\d]{2}))?(.wav)?"
    private val groups = "1 2 3 4 5 6 8 10"

    private val obsRegex = "([a-zA-Z]{2,3}[-[\\d\\w]+]*)_obs_([\\w\\d]*)_b([\\d]{2})_([\\d]+)_v([\\d]{2,3})(-([\\d]{2,3}))?(_t([\\d]{2}))?(.wav)?"
    private val obsGroups = "1 2 3 4 -1 5 6 9"

    @Test
    fun `match with valid file and captures data`() {
        val file = File("en_ulb_b01_gen_c03_v02_t01.wav")
        val matcher = ProjectPatternMatcher(regex, groups)

        assertTrue(matcher.match(file))

        val projectSlugs = matcher.projectSlugs
        assertNotNull(projectSlugs)
        assertEquals("en", projectSlugs?.language)
        assertEquals("ulb", projectSlugs?.version)
        assertEquals(1, projectSlugs?.bookNumber)
        assertEquals("gen", projectSlugs?.book)

        val takeInfo = matcher.takeInfo
        assertNotNull(takeInfo)
        assertEquals(projectSlugs, takeInfo?.projectSlugs)
        assertEquals(3, takeInfo?.chapter)
        assertEquals(2, takeInfo?.startVerse)
        assertEquals(2, takeInfo?.endVerse)
        assertEquals(1, takeInfo?.take)
    }

    @Test
    fun `match with invalid file`() {
        val file = File("invalid_file_name.wav")
        val matcher = ProjectPatternMatcher(regex, groups)

        assertFalse(matcher.match(file))
        assertFalse(matcher.matched())
        assertNull(matcher.projectSlugs)
        assertNull(matcher.takeInfo)
    }

    @Test
    fun `match with missing group in file`() {
        val file = File("projectA_book12_chapter3_take1.txt")
        val matcher = ProjectPatternMatcher(regex, groups)

        assertFalse(matcher.match(file))
        assertFalse(matcher.matched())
        assertNull(matcher.projectSlugs)
        assertNull(matcher.takeInfo)
    }

    @Test
    fun `match with empty capture group`() {
        val file = File("en__b01_gen_c03_v02_t01.wav")
        val matcher = ProjectPatternMatcher(regex, groups)

        assertFalse(matcher.match(file))
        assertNull(matcher.projectSlugs)
        assertNull(matcher.takeInfo)
    }

    @Test
    fun `match with obs file`() {
        val file = File("en_obs_v4_b50_2_v03_t01.wav")
        val matcher = ProjectPatternMatcher(obsRegex, obsGroups)

        assertTrue(matcher.match(file))

        val projectSlugs = matcher.projectSlugs
        assertNotNull(projectSlugs)
        assertEquals("en", projectSlugs?.language)
        assertEquals("v4", projectSlugs?.version)
        assertEquals(50, projectSlugs?.bookNumber)
        assertEquals("2", projectSlugs?.book)

        val takeInfo = matcher.takeInfo
        assertNotNull(takeInfo)
        assertEquals(projectSlugs, takeInfo?.projectSlugs)
        assertEquals(3, takeInfo?.startVerse)
        assertEquals(3, takeInfo?.endVerse)
        assertEquals(1, takeInfo?.take)
    }

}