package org.wycliffeassociates.translationrecorder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wycliffeassociates.translationrecorder.database.IProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.database.ProjectDatabaseHelper
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideProjectDatabaseHelper(
        @ApplicationContext context: Context,
        directoryProvider: IDirectoryProvider
    ): IProjectDatabaseHelper {
        return ProjectDatabaseHelper(context, directoryProvider)
    }
}