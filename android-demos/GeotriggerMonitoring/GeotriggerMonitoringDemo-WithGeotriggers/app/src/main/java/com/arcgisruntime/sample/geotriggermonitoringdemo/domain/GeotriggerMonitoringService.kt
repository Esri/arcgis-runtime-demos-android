package com.arcgisruntime.sample.geotriggermonitoringdemo.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.getService
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.asLiveData
import com.arcgisruntime.sample.geotriggermonitoringdemo.R
import com.arcgisruntime.sample.geotriggermonitoringdemo.view.MainActivity
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitor
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorNotificationEvent
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorNotificationEventListener
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorStatusChangedEvent
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorStatusChangedEventListener
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorWarningChangedEvent
import com.esri.arcgisruntime.geotriggers.GeotriggerMonitorWarningChangedEventListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class GeotriggerMonitoringService : Service(), GeotriggerMonitorNotificationEventListener,
    GeotriggerMonitorWarningChangedEventListener, GeotriggerMonitorStatusChangedEventListener {

    @Inject
    lateinit var geotriggerInteractor: GeotriggerInteractor

    private lateinit var monitors: MutableList<GeotriggerMonitor>

    override fun onCreate() {
        super.onCreate()
        monitors = geotriggerInteractor.monitors
        geotriggerInteractor.shouldMonitor.asLiveData().observeForever { shouldMonitor ->
            if (shouldMonitor) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                "GeotriggerMonitor",
                "Geotrigger Monitor",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lightColor = Color.BLUE
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                    this
                )
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (ACTION_STOP_SERVICE == intent?.action) {
            Log.d("Geotriggers", "called to cancel service")
            geotriggerInteractor.setMonitoring(false)
        }

        return super.onStartCommand(intent, flags, startId)
    }


    override fun onBind(intent: Intent?): IBinder? {
        // we don't need binding, so we disable it
        return null
    }

    private fun startMonitoring() {
        setupForeground()
        monitors.forEach { geotriggerMonitor ->
            geotriggerMonitor.addGeotriggerMonitorNotificationEventListener(this)
            geotriggerMonitor.addGeotriggerMonitorWarningChangedEventListener(this)
            geotriggerMonitor.addGeotriggerMonitorStatusChangedEventListener(this)
            geotriggerMonitor.startAsync()
        }
    }

    private fun stopMonitoring() {
        monitors.forEach { geotriggerMonitor ->
            geotriggerMonitor.removeGeotriggerMonitorNotificationEventListener(this)
            geotriggerMonitor.removeGeotriggerMonitorWarningChangedEventListener(this)
            geotriggerMonitor.removeGeotriggerMonitorWarningChangedEventListener(this)
            geotriggerMonitor.stop()
        }
        stopForeground(true)
    }

    private var ACTION_STOP_SERVICE = "STOP"
    private val NOTIFICATION = 1

    private fun setupForeground() {
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0 or FLAG_IMMUTABLE)
            }
        val stopSelf = Intent(this, GeotriggerMonitoringService::class.java)
        stopSelf.action = this.ACTION_STOP_SERVICE
        val pStopSelf =
            getService(
                this,
                0,
                stopSelf,
                FLAG_CANCEL_CURRENT or FLAG_IMMUTABLE
            )

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "GeotriggerMonitor")
                .setContentTitle("Monitoring Geotrigger")
                .setContentText("Monitoring geotriggers using your location")
                .setSmallIcon(R.drawable.arcgisruntime_location_display_compass_symbol)
                .setContentIntent(pendingIntent)
                .setTicker("Monitoring")
                .addAction(
                    Notification.Action.Builder(
                        Icon.createWithResource(
                            applicationContext,
                            R.drawable.arcgisruntime_location_display_default_symbol
                        ), "Stop", pStopSelf
                    ).build()
                )
                .build()
        } else {
            NotificationCompat.Builder(this, "GeotriggerMonitor")
                .setContentTitle("Monitoring Geotrigger")
                .setContentText("Monitoring geotriggers using your location")
                .setSmallIcon(R.drawable.arcgisruntime_location_display_compass_symbol)
                .setContentIntent(pendingIntent)
                .setTicker("Monitoring")
                .addAction(
                    NotificationCompat.Action.Builder(
                        IconCompat.createWithResource(
                            applicationContext,
                            R.drawable.arcgisruntime_location_display_default_symbol
                        ), "Stop", pStopSelf
                    ).build()
                )
                .build()
        }

        startForeground(NOTIFICATION, notification)
    }

    override fun onDestroy() {
        geotriggerInteractor.setMonitoring(false)
        super.onDestroy()
    }

    override fun onGeotriggerMonitorNotification(geotriggerMonitorNotificationEvent: GeotriggerMonitorNotificationEvent) {
        geotriggerInteractor.onNotificationEvent(geotriggerMonitorNotificationEvent)
    }

    override fun onGeotriggerMonitoringWarningChanged(geotriggerMonitorWarningChangedEvent: GeotriggerMonitorWarningChangedEvent) {
        geotriggerInteractor.onWarningEvent(geotriggerMonitorWarningChangedEvent)
    }

    override fun onGeotriggerMonitorStatusChanged(geotriggerMonitorStatusChangedEvent: GeotriggerMonitorStatusChangedEvent) {
        geotriggerInteractor.onMonitorStatuschanged(geotriggerMonitorStatusChangedEvent)
    }
}