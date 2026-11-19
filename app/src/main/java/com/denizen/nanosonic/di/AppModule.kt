package com.denizen.nanosonic.di

import android.content.Context
import android.content.SharedPreferences
import com.denizen.nanosonic.data.autoeq.AndroidAssetSource
import com.denizen.nanosonic.data.autoeq.AssetSource
import com.denizen.nanosonic.data.library.LibraryRepository
import com.denizen.nanosonic.data.library.MediaStoreLibraryRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class AppScope
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SettingsPrefs
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class EqProfilePrefs

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Application-lifetime scope on the main thread, for singletons that hold state flows. */
    @Provides @Singleton @AppScope
    fun appScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    // File names and keys are part of the on-device data format. Do not change them.
    @Provides @Singleton @SettingsPrefs
    fun settingsPrefs(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    @Provides @Singleton @EqProfilePrefs
    fun eqProfilePrefs(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("nanosonic_eq_profiles", Context.MODE_PRIVATE)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun assetSource(impl: AndroidAssetSource): AssetSource
    @Binds abstract fun libraryRepository(impl: MediaStoreLibraryRepository): LibraryRepository
}
