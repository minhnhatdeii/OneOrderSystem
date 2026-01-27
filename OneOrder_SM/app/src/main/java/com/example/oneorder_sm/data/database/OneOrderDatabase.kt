package com.example.oneorder_sm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DashboardSummaryEntity::class,
        OrderStatisticEntity::class,
        PopularItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class OneOrderDatabase : RoomDatabase() {
    abstract fun statisticsDao(): StatisticsDao
}
