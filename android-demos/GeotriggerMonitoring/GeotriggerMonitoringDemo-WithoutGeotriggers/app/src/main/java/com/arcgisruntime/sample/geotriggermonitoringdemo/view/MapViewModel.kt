package com.arcgisruntime.sample.geotriggermonitoringdemo.view

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arcgisruntime.sample.geotriggermonitoringdemo.domain.PointOfInterestInteractor
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol
import com.esri.arcgisruntime.symbology.SimpleRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val pointOfInterestInteractor: PointOfInterestInteractor,
) : ViewModel() {

    val map: ArcGISMap = ArcGISMap(Basemap.createLightGrayCanvasVector())

    private val _viewpoint = MutableLiveData<Viewpoint>()
    val viewpoint: LiveData<Viewpoint> = _viewpoint

    private val symbol = SimpleMarkerSymbol(SimpleMarkerSymbol.Style.X, Color.RED, 10.0F)
    val graphicsOverlay = GraphicsOverlay().apply {
        renderer = SimpleRenderer(symbol)
    }

    init {
        // ensures the graphics overlay is always updated with new points of interest
        viewModelScope.launch {
            pointOfInterestInteractor.getGraphicsForPointsOfInterest().collect {
                graphicsOverlay.graphics.clear()
                graphicsOverlay.graphics.addAll(it)
            }
        }
        // set the viewpoint to Edinburgh
        setEdinburgh()
    }

    private fun setViewpoint(x: Double, y: Double, scale: Double) {
        val point = Point(x, y, SpatialReferences.getWgs84())
        _viewpoint.value = Viewpoint(point, scale)
    }

    fun createPointOfInterest(point: Point) {
        viewModelScope.launch { pointOfInterestInteractor.createPointOfInterest(point) }
    }

    fun clearPointsOfInterest() {
        viewModelScope.launch {
            pointOfInterestInteractor.clearPointsOfInterest()
        }
    }

    fun setEdinburgh() {
        setViewpoint(-3.1824314444161477, 55.95994422289002, 50000.0)
    }
}