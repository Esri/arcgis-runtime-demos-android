package com.arcgisruntime.sample.geotriggermonitoringdemo.domain

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.arcgisruntime.sample.geotriggermonitoringdemo.model.MapRepository
import com.esri.arcgisruntime.arcade.ArcadeExpression
import com.esri.arcgisruntime.geotriggers.FenceGeotrigger
import com.esri.arcgisruntime.geotriggers.FenceRuleType
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitor
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorNotificationEvent
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorStatusChangedEvent
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorWarningChangedEvent
import com.esri.arcgisruntime.geotriggers.GraphicsOverlayFenceParameters
import com.esri.arcgisruntime.geotriggers.LocationGeotriggerFeed
import com.esri.arcgisruntime.location.AndroidLocationDataSource
import com.esri.arcgisruntime.location.LocationDataSource
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeotriggerInteractor @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {

    val monitors = mutableListOf<GeotriggerMonitor>()

    private val _shouldMonitor = MutableStateFlow(false)
    val shouldMonitor: StateFlow<Boolean> = _shouldMonitor

    val lds: LocationDataSource = AndroidLocationDataSource(applicationContext)

    fun createMonitors(graphicsOverlay: GraphicsOverlay, bufferDistance: Double) {
        val feed = LocationGeotriggerFeed(lds)
        val ruleType = FenceRuleType.ENTER_OR_EXIT
        val fenceParameters = GraphicsOverlayFenceParameters(graphicsOverlay, bufferDistance)
        val arcadeExpression =
            ArcadeExpression("return {message:\$fencenotificationtype + ' a fence(' + \$fencefeature.name + ') with a course of ' + \$feedfeature.course, extra_info:456}")
        val geotrigger =
            FenceGeotrigger(
                feed,
                ruleType,
                fenceParameters,
                arcadeExpression,
                "Graphics Overlay Geotrigger"
            )
        monitors.add(GeotriggerMonitor(geotrigger))
    }

    fun onNotificationEvent(geotriggerMonitorNotificationEvent: GeotriggerMonitorNotificationEvent) {
        val message = geotriggerMonitorNotificationEvent.geotriggerNotificationInfo.message
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun onWarningEvent(geotriggerMonitorWarningChangedEvent: GeotriggerMonitorWarningChangedEvent) {
        val warning = geotriggerMonitorWarningChangedEvent.warning?.additionalMessage
        warning?.let {
            Toast.makeText(applicationContext, it, Toast.LENGTH_LONG).show()
            Log.e("GeotriggerInteractor", warning)
        }
    }

    fun onMonitorStatuschanged(geotriggerMonitorStatusChangedEvent: GeotriggerMonitorStatusChangedEvent) {
        val status = geotriggerMonitorStatusChangedEvent.status
        Toast.makeText(applicationContext, status.name, Toast.LENGTH_SHORT).show()
    }

    fun setMonitoring(start: Boolean) {
        // if starting, start the location data source first
        if (start) {
            lds.startAsync()
            lds.addStatusChangedListener { statusChangedEvent ->
                if (statusChangedEvent.status != LocationDataSource.Status.STARTED) {
                    lds.error?.message?.let { errorMessage ->
                        Log.e("MapViewModel", "Location data source not started: $errorMessage")
                    }
                } else {
                    _shouldMonitor.value = true
                }
            }
        }
        else {
            _shouldMonitor.value = false
            lds.stop()
        }
    }
}