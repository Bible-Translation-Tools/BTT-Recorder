package org.wycliffeassociates.translationrecorder.recordingapp

import org.wycliffeassociates.translationrecorder.SettingsPage.SettingsActivity
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.setDefaultPref
import org.wycliffeassociates.translationrecorder.project.Project
import org.wycliffeassociates.translationrecorder.project.components.User
import java.lang.reflect.Field

/**
 * Created by sarabiaj on 9/20/2017.
 */
object TestUtils {

    fun createBibleProject(db: IProjectDatabaseHelper): Project {
        val anthology = db.getAnthology(db.getAnthologyId("ot"))
        val project = Project(
            db.getLanguage(db.getLanguageId("aa")),
            anthology,
            db.getBook(db.getBookId("gen")),
            db.getVersion(db.getVersionId("reg")),
            db.getMode(db.getModeId("verse", anthology.slug)),
        )
        db.addProject(project)
        return project
    }

    fun createBibleProject(
        languageId: String,
        anthologyId: String,
        bookId: String,
        versionId: String,
        modeId: String,
        db: IProjectDatabaseHelper
    ): Project {
        val anthology = db.getAnthology(db.getAnthologyId(anthologyId))
        val project = Project(
            db.getLanguage(db.getLanguageId(languageId)),
            anthology,
            db.getBook(db.getBookId(bookId)),
            db.getVersion(db.getVersionId(versionId)),
            db.getMode(db.getModeId(modeId, anthology.slug)),
        )
        db.addProject(project)
        return project
    }

    fun createTestUser(
        directoryProvider: IDirectoryProvider,
        db: IProjectDatabaseHelper,
        prefs: IPreferenceRepository
    ): User {
        val tempFile = directoryProvider.createTempFile("fake", "")
        val user = User(tempFile, "ff8adece0631821959f443c9d956fc39", 1)
        db.addUser(user)
        prefs.setDefaultPref(SettingsActivity.KEY_PROFILE, user.id)
        return user
    }

    /**
     * Sets a property of an object using reflection
     * @param obj the source object
     * @param fieldName the name of the field
     * @param value the value to set
     */
    fun setPropertyReflection(obj: Any, fieldName: String, value: Any) {
        val cls = obj::class.java
        val field = findField(cls, fieldName)
        field?.isAccessible = true
        field?.set(obj, value)
    }

    /**
     * Finds a field in a class hierarchy
     */
    private fun findField(cls: Class<*>, fieldName: String): Field? {
        var field: Field? = null
        try {
            field = cls.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            if (cls.superclass != null) {
                field = findField(cls.superclass, fieldName)
            }
        }
        return field
    }
}
