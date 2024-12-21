package org.wycliffeassociates.translationrecorder.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.wycliffeassociates.translationrecorder.persistance.AssetsProvider
import org.wycliffeassociates.translationrecorder.persistance.MainAssetsProvider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AssetsModule {
    @Provides
    @Singleton
    fun provideAssetsProvider(@ApplicationContext context: Context): AssetsProvider {
        return MainAssetsProvider(context)
    }
}