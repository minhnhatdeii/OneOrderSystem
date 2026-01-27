package com.example.oneorder_sm.di

import android.content.Context
import androidx.room.Room
import com.example.oneorder_sm.data.database.OneOrderDatabase
import com.example.oneorder_sm.data.database.StatisticsDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): OneOrderDatabase {
        return Room.databaseBuilder(
            context,
            OneOrderDatabase::class.java,
            "oneorder_sm.db"
        )
        .fallbackToDestructiveMigration() // For development simplicity
        .build()
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(database: OneOrderDatabase): StatisticsDao {
        return database.statisticsDao()
    }
}
