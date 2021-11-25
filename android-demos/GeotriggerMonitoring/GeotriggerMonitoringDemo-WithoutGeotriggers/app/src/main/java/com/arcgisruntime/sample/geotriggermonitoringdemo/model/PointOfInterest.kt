package com.arcgisruntime.sample.geotriggermonitoringdemo.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PointOfInterest(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val pointX: Double,
    val pointY: Double
)
