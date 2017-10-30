package com.esri.arcgisruntime.demos.webtiledlayerkt

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log
import android.view.Menu

import com.esri.arcgisruntime.ArcGISRuntimeEnvironment
import com.esri.arcgisruntime.concurrent.ListenableFuture
import com.esri.arcgisruntime.geometry.GeometryEngine
import com.esri.arcgisruntime.geometry.Point
import com.esri.arcgisruntime.geometry.SpatialReferences
import com.esri.arcgisruntime.layers.ArcGISTiledLayer
import com.esri.arcgisruntime.layers.WebTiledLayer
import com.esri.arcgisruntime.mapping.ArcGISMap
import com.esri.arcgisruntime.mapping.Basemap
import com.esri.arcgisruntime.mapping.Viewpoint
import com.esri.arcgisruntime.mapping.view.MapView
import com.esri.arcgisruntime.mapping.view.NavigationChangedEvent
import com.esri.arcgisruntime.mapping.view.NavigationChangedListener


class MainActivity : AppCompatActivity() {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var mapView: MapView
    private lateinit var map: ArcGISMap

    private lateinit var osmFranceLayer: WebTiledLayer
    private lateinit var osmSwissLayer: WebTiledLayer
    private lateinit var gsiJapanLayer: WebTiledLayer

    private enum class LayerChoice {
        DEFAULT,
        OSM_FRANCE,
        OSM_SWISS,
        GSI_JAPAN
    }


    private fun createOsmFranceLayer() {
        // 1 - Define the set of subdomains available for this service
        val subDomainsOsmFr = arrayListOf("a", "b", "c")

        // 2 - Define the URL template, including placeholders for the subdomains, and the
        // zoom level (Z), column (X) and row (Y) values that uniquely identify each tile
        val templateUriOsmFr = "http://{subDomain}.tile.openstreetmap.fr/osmfr/{level}/{col}/{row}.png"

        // 3- Create a WebTiledLayer with these values.
        osmFranceLayer = WebTiledLayer(templateUriOsmFr, subDomainsOsmFr)

        // 4 - Set appropriate data attribution for this layer.
        osmFranceLayer.attribution = "OpenStreetMap France | © Donnes les contributeurs OpenStreetMap"

        // Add the layer to the map,  Not visible by default
        osmFranceLayer.isVisible = false
        map.operationalLayers.add(osmFranceLayer)
    }



    private fun createOsmChLayer() {
        val subDomainsOsmCh = arrayListOf("tile")
        val templateUriOsmCh = "http://{subDomain}.osm.ch/osm-swiss-style/{level}/{col}/{row}.png"
        osmSwissLayer = WebTiledLayer(templateUriOsmCh, subDomainsOsmCh)
        osmSwissLayer.attribution = "Swiss OpenStreetMap | © Donnes les contributeurs OpenStreetMap"
        osmSwissLayer.isVisible = false
        map.operationalLayers.add(osmSwissLayer)
    }

    private fun createGsiJpLayer() {
        val subDomainsGsiJp = arrayListOf("cyberjapandata")
        val templateUriCycleMap = "http://{subDomain}.gsi.go.jp/xyz/std/{level}/{col}/{row}.png"
        gsiJapanLayer = WebTiledLayer(templateUriCycleMap, subDomainsGsiJp)
        gsiJapanLayer.attribution = "© Geospatial Information Authority of Japan"
        gsiJapanLayer.isVisible = false
        map.operationalLayers.add(gsiJapanLayer)
    }

    private var useJumpZoom = false

    private val LICENSE_STRING = BuildConfig.LICENSE_STRING

    // var keyword indicates not final
    // ? indicates nullable type
    private var navCompletedListener: LogCenterAndScale? = null

    private inner class LogCenterAndScale : NavigationChangedListener {
        override fun navigationChanged(navigationCompletedEvent: NavigationChangedEvent?) {
            if (navigationCompletedEvent != null) {
                val source = navigationCompletedEvent.source
                if (source is MapView) {
                    val pt = source.visibleArea.extent.center
                    Log.i(TAG, String.format("CenterPoint: X:%.6f, Y:%.6f", pt.x, pt.y))
                    Log.i(TAG, "Current scale: " + mapView.mapScale)
                }
            }
        }
    }

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_osm_france -> {
                switchTo(LayerChoice.OSM_FRANCE)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_osm_swiss -> {
                switchTo(LayerChoice.OSM_SWISS)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_gsi_japan -> {
                switchTo(LayerChoice.GSI_JAPAN)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_home -> {
                switchTo(LayerChoice.DEFAULT)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        mapView = findViewById<MapView>(R.id.mapView)
        setupMap()

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private val mEuropeViewpoint = Viewpoint(Point(703684.664586, 6284550.416175,
            SpatialReferences.getWebMercator()), 4.2E7)

    private fun setupMap() {

        val canvasBasemap = Basemap(ArcGISTiledLayer("https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer"))
        map = ArcGISMap(canvasBasemap)
        map.initialViewpoint = mEuropeViewpoint
        mapView.map = map
        navCompletedListener = LogCenterAndScale()

        // Uncomment to report map center and scale if required.
        //mapView.addNavigationChangedListener(navCompletedListener)

        createOsmFranceLayer()

        createOsmChLayer()

        createGsiJpLayer()
    }

    // Switching between tabs, change visible layer and Viewpoint.

    private val osmFranceView = Viewpoint(Point(263979.116397, 6248712.789973,
            SpatialReferences.getWebMercator()), 17805.951741089462)
    private val osmSwissView = Viewpoint(Point(817942.120229, 5891465.587225,
            SpatialReferences.getWebMercator()), 1532338.910874782)
    private val gsiJapanView = Viewpoint(Point(15466920.067368, 4318755.787271,
            SpatialReferences.getWebMercator()), 4411065.683299846)

    private fun switchTo(newLayer: LayerChoice) {
        // Switch visibility of existing layers
        osmFranceLayer.isVisible = newLayer === LayerChoice.OSM_FRANCE
        osmSwissLayer.isVisible = newLayer === LayerChoice.OSM_SWISS
        gsiJapanLayer.isVisible = newLayer === LayerChoice.GSI_JAPAN

        // zoom to appropriate location?? Swiss map does not cover world (same for Japan example)
        var selectedViewpoint: Viewpoint? = null
        when (newLayer) {
            LayerChoice.OSM_FRANCE -> selectedViewpoint = osmFranceView
            LayerChoice.OSM_SWISS -> selectedViewpoint = osmSwissView
            LayerChoice.GSI_JAPAN -> selectedViewpoint = gsiJapanView
        }
        if (selectedViewpoint == null) return

        if (useJumpZoom) {
            // If new target already inside the current extent, then zoom directly to it, no jump-out required.
            if (GeometryEngine.intersects(mapView.visibleArea, selectedViewpoint.targetGeometry)) {
                jumpZoom(selectedViewpoint, null)
            } else {
                // If target is outside of current extent, zoom out first to see both extents, then zoom back in.
                val union = GeometryEngine.union(mapView.visibleArea.extent.center, selectedViewpoint
                        .targetGeometry)
                if (union != null && !union.isEmpty) {
                    jumpZoom(Viewpoint(union.extent), selectedViewpoint)
                }
            }
        } else {
            mapView.setViewpoint(selectedViewpoint)
        }
    }

    // If enabled, zoom out to extent of both current and new viewpoint, before zooming back in.

    private lateinit var booleanListenableFuture: ListenableFuture<Boolean>

    private fun jumpZoom(firstViewpoint: Viewpoint?, secondViewpoint: Viewpoint?) {
        if (firstViewpoint == null) return

        booleanListenableFuture = mapView.setViewpointAsync(firstViewpoint, 3.0f)

        if (secondViewpoint == null) return

        booleanListenableFuture.addDoneListener {
            try {
                if (booleanListenableFuture.get()) {
                    // First navigation is complete, was not interrupted by the user or another navigation.
                    mapView.setViewpointAsync(secondViewpoint, 3.0f)
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val item = menu.add("Jump-zoom")
        item.isCheckable = true
        item.isChecked = false
        item.setOnMenuItemClickListener {
            nameLocal -> //Names the MenuItem name to a local variable for use
            nameLocal.isChecked = !useJumpZoom
            useJumpZoom = nameLocal.isChecked
            true
        }
        return true
    }

}
