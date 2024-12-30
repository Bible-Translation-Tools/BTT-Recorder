package org.wycliffeassociates.translationrecorder.recordingapp

import android.content.Context
import android.content.Intent
import androidx.test.rule.ActivityTestRule
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.project.Project

/**
 * Created by sarabiaj on 9/20/2017.
 */
object ProjectMockingUtil {

    fun createBibleTestProject(
        activityTestRule: ActivityTestRule<*>,
        directoryProvider: IDirectoryProvider
    ): Project {
        activityTestRule.launchActivity(Intent())
        val ctx: Context = activityTestRule.activity
        val db = ProjectDatabaseHelper(ctx, directoryProvider)

        val anthology = db.getAnthology(db.getAnthologyId("ot"))
        val project = Project(
            db.getLanguage(db.getLanguageId("en")),
            anthology,
            db.getBook(db.getBookId("gen")),
            db.getVersion(db.getVersionId("ulb")),
            db.getMode(db.getModeId("verse", anthology.slug)),
        )

        db.close()
        activityTestRule.activity.finish()
        return project
    }

    fun createNotesTestProject(
        activityTestRule: ActivityTestRule<*>,
        directoryProvider: IDirectoryProvider
    ): Project {
        activityTestRule.launchActivity(Intent())
        val ctx: Context = activityTestRule.activity
        val db = ProjectDatabaseHelper(ctx, directoryProvider)

        val anthology = db.getAnthology(db.getAnthologyId("tn"))
        val project = Project(
            db.getLanguage(db.getLanguageId("en")),
            anthology,
            db.getBook(db.getBookId("gen-ch-1")),
            db.getVersion(db.getVersionId("ulb")),
            db.getMode(db.getModeId("note", anthology.slug))
        )

        db.close()
        activityTestRule.activity.finish()
        return project
    }
}
