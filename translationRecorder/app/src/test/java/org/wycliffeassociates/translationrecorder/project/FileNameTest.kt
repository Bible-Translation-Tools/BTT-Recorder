import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import org.wycliffeassociates.translationrecorder.project.FileName
import org.wycliffeassociates.translationrecorder.project.components.Anthology
import org.wycliffeassociates.translationrecorder.project.components.Book
import org.wycliffeassociates.translationrecorder.project.components.Language
import org.wycliffeassociates.translationrecorder.project.components.Version

class FileNameTest {

    @Test
    fun `getFileName with chapter and single verse`() {
        val language = mockk<Language>()
        every { language.slug } returns "en"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "1111111111"
        every { anthology.resource } returns "text"
        every { anthology.slug } returns "nt"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 4
        every { book.slug } returns "mat"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1, 1)

        assertEquals("en_text_nt_ulb_b04_mat_c01_v01", result)
    }

    @Test
    fun `getFileName with chapter and verse range`() {
        val language = mockk<Language>()
        every { language.slug } returns "en"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "1111111111"
        every { anthology.resource } returns "text"
        every { anthology.slug } returns "nt"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 4
        every { book.slug } returns "mat"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1, 1, 5)

        assertEquals("en_text_nt_ulb_b04_mat_c01_v01-05", result)
    }

    @Test
    fun `getFileName with chapter and same start and end verse`() {
        val language = mockk<Language>()
        every { language.slug } returns "en"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "1111111111"
        every { anthology.resource } returns "text"
        every { anthology.slug } returns "nt"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 4
        every { book.slug } returns "mat"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1, 1, 1)

        assertEquals("en_text_nt_ulb_b04_mat_c01_v01", result)
    }

    @Test
    fun `getFileName with chapter and end verse -1`() {
        val language = mockk<Language>()
        every { language.slug } returns "en"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "1111111111"
        every { anthology.resource } returns "text"
        every { anthology.slug } returns "nt"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 4
        every { book.slug } returns "mat"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1, 1, -1)

        assertEquals("en_text_nt_ulb_b04_mat_c01_v01", result)
    }

    @Test
    fun `computeFileNameFormat with partial mask`() {
        val language = mockk<Language>()
        every { language.slug } returns "en"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "0000001111"
        every { anthology.resource } returns "text"
        every { anthology.slug } returns "nt"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 4
        every { book.slug } returns "mat"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1,1)

        assertEquals("c01_v01", result)
    }

    @Test
    fun `computeFileNameFormat with different mask`() {
        val language = mockk<Language>()
        every { language.slug } returns "de"
        val anthology = mockk<Anthology>()
        every { anthology.mask } returns "1011011100"
        every { anthology.resource } returns "Old Testament"
        every { anthology.slug } returns "ot"
        val version = mockk<Version>()
        every { version.slug } returns "ulb"
        val book = mockk<Book>()
        every { book.order } returns 19
        every { book.slug } returns "psa"

        val fileName = FileName(language, anthology, version, book)
        val result = fileName.getFileName(1,1)

        assertEquals("de_ot_ulb_psa_c01_v01", result)
    }
}