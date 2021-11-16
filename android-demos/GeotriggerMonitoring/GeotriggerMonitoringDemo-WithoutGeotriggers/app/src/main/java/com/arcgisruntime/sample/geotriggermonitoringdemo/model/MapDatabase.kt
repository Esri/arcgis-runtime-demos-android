package com.arcgisruntime.sample.geotriggermonitoringdemo.model

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PointOfInterest::class],
    version = 3
)
abstract class MapDatabase : RoomDatabase() {
    abstract fun pointOfInterestDao(): PointOfInterestDao
}