package com.arcgisruntime.sample.geotriggermonitoringdemo.domain

import com.arcgisruntime.sample.geotriggermonitoringdemo.model.MapRepository
import com.arcgisruntime.sample.geotriggermonitoringdemo.model.PointOfInterest
import com.esri.arcgisruntime.geometry.GeodeticCurveType
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.LinearUnit
import com.esri.arcgisruntime.geometry.LinearUnitId
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReference
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.view.Graphic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Converts stored points of interest to graphics for the Viewmodel, and converts new screen points to
 * projected geographic points to save.
 */
class PointOfInterestInteractor @Inject constructor(
    private val mapRepository: MapRepository
) {

    var graphicsFlow: Flow<List<Graphic>>? = null

    suspend fun createPointOfInterest(mapPoint: Point) {
        var point = mapPoint
        if (mapPoint.spatialReference != SR) {
            point = GeometryEngine.project(mapPoint, SR) as Point
        }
        val poi = PointOfInterest(0, point.x, point.y)
        mapRepository.savePointOfInterest(poi)
        updateGraphicsFlow()
    }

    suspend fun clearPointsOfInterest() {
        mapRepository.clearPointsOfInterest()
        updateGraphicsFlow()
    }

    private suspend fun loadPointsOfInterestGeometries(): Flow<List<Point>> {
        return mapRepository.getAllPointsOfInterest().map { list ->
            list.map {
                Point(it.pointX, it.pointY, SR)
            }
        }
    }

    private suspend fun updateGraphicsFlow() {
        graphicsFlow = getGraphicsForPointsOfInterest()
    }

    suspend fun getGraphicsForPointsOfInterest(): Flow<List<Graphic>> {
        if (graphicsFlow == null) {
            graphicsFlow = loadPointsOfInterestGeometries().map { list ->
                list.map {
                    Graphic(it)
                }
            }
        }
        return graphicsFlow!!
    }

    companion object {
        val SR: SpatialReference = SpatialReferences.getWgs84()
    }

}