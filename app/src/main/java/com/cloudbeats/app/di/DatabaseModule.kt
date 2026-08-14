package com.cloudbeats.app.di

import android.content.Context
import androidx.room.Room
import com.cloudbeats.app.data.local.CloudBeatsDatabase
import com.cloudbeats.app.data.local.dao.PlaylistDao
import com.cloudbeats.app.data.local.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CloudBeatsDatabase {
        return Room.databaseBuilder(
            context,
            CloudBeatsDatabase::class.java,
            "cloudbeats.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSongDao(database: CloudBeatsDatabase): SongDao {
        return database.songDao()
    }

    @Provides
    fun providePlaylistDao(database: CloudBeatsDatabase): PlaylistDao {
        return database.playlistDao()
    }
}
