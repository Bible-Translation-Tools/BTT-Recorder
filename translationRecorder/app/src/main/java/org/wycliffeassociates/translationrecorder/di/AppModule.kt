package org.wycliffeassociates.translationrecorder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wycliffeassociates.translationrecorder.persistance.DirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IDirectoryProvider
import org.wycliffeassociates.translationrecorder.persistance.IPreferenceRepository
import org.wycliffeassociates.translationrecorder.persistance.PreferenceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDirectoryProvider(
        @ApplicationContext context: Context
    ): IDirectoryProvider {
        return DirectoryProvider(context)
    }

    @Provides
    @Singleton
    fun providePreferenceRepository(
        @ApplicationContext context: Context
    ): IPreferenceRepository {
        return PreferenceRepository(context)
    }
}