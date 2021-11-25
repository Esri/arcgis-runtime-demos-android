package com.arcgisruntime.sample.geotriggermonitoringdemo.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PointOfInterestDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(point: PointOfInterest)

    @Query("SELECT * FROM pointofinterest WHERE id = :id")
    fun load(id: Int): Flow<PointOfInterest>

    @Delete
    fun delete(point: PointOfInterest)

    @Query("DELETE FROM pointofinterest")
    fun clear()

    @Query("SELECT * FROM pointofinterest")
    fun getAll(): Flow<List<PointOfInterest>>
}
