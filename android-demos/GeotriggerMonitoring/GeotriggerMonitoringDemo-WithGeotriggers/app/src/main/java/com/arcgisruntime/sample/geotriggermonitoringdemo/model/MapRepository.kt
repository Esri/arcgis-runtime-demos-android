package com.arcgisruntime.sample.geotriggermonitoringdemo.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MapRepository @Inject constructor(
    private val pointOfInterestDao: PointOfInterestDao
) {
    suspend fun savePointOfInterest(pointOfInterest: PointOfInterest) =
        withContext(Dispatchers.IO) {
            pointOfInterestDao.save(pointOfInterest)
        }

    suspend fun clearPointsOfInterest() = withContext(Dispatchers.IO) {
        pointOfInterestDao.clear()
    }

    suspend fun getAllPointsOfInterest(): Flow<List<PointOfInterest>> =
        withContext(Dispatchers.IO) { pointOfInterestDao.getAll() }
}