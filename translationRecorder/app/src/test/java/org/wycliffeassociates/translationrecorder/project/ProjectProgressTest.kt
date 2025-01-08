import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.wycliffeassociates.translationrecorder.chunkplugin.Chapter
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.ProjectProgress
import kotlin.math.ceil

class ProjectProgressTest {

    private lateinit var chapters: List<Chapter>
    private lateinit var projectProgress: ProjectProgress

    @MockK lateinit var db: IProjectDatabaseHelper
    @MockK lateinit var project: Project

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        every { db.getProjectId(project) } returns 1

        chapters = listOf(
            mockk<Chapter> {
                every { number } returns 1
                every { chunks } returns List(5) { mockk() }
            },
            mockk<Chapter> {
                every { number } returns 2
                every { chunks } returns List(10) { mockk() }
            },
            mockk<Chapter> {
                every { number } returns 3
                every { chunks } returns List(3) { mockk() }
            }
        )
        projectProgress = ProjectProgress(project, db, chapters)
    }

    @Test
    fun `updateProjectProgress with no started units`() {
        every { db.getNumStartedUnitsInProject(project) } returns emptyMap()
        every { db.getProjectId(project) } returns 1
        every { db.setProjectProgress(any(), any()) } just Runs

        projectProgress.updateProjectProgress()

        verify { db.setProjectProgress(1, 0) }
    }

    @Test
    fun `updateChaptersProgress with no started units`() {
        every { db.getNumStartedUnitsInProject(project) } returns emptyMap()
        every { db.getChapterId(project, any()) } returns 1
        every { db.setChapterProgress(any(), any()) } just Runs

        projectProgress.updateChaptersProgress()

        verify { db.setChapterProgress(1, 0) }
    }

    @Test
    fun calculateProjectProgress() {
        var progress = 0

        every { db.getNumStartedUnitsInProject(project) } returns mapOf(1 to 3, 2 to 5, 3 to 2)
        every { db.setProjectProgress(any(), any()) }.answers {
            progress = secondArg()
        }

        val expectedProgress = ceil((60f + 50f + 67f) / 3).toInt()

        projectProgress.updateProjectProgress()

        assertEquals(expectedProgress, progress)
    }

    @Test
    fun updateChaptersProgress() {
        every { db.getNumStartedUnitsInProject(project) } returns mapOf(1 to 4, 2 to 4, 3 to 1)
        every { db.getChapterId(project, any()) } returns 1
        every { db.setChapterProgress(any(), any()) } just Runs

        projectProgress.updateChaptersProgress()

        verify { db.setChapterProgress(1, 80) }
        verify { db.setChapterProgress(1, 40) }
        verify { db.setChapterProgress(1, 33) }
    }

    @Test
    fun updateProjectProgress() {
        every { db.getNumStartedUnitsInProject(project) } returns mapOf(1 to 4, 2 to 4, 3 to 1)
        every { db.getProjectId(project) } returns 1
        every { db.setProjectProgress(any(), any()) } just Runs

        projectProgress.updateProjectProgress()

        verify { db.setProjectProgress(1, 51) }
    }
}