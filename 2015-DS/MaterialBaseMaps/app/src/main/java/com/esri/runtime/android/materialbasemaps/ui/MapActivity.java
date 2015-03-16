package com.esri.runtime.android.materialbasemaps.ui;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;
import com.esri.runtime.android.materialbasemaps.R;
import com.esri.runtime.android.materialbasemaps.model.BasemapItem;
import com.esri.runtime.android.materialbasemaps.util.TaskExecutor;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Activity for Map and floating action bar button.
 */
public class MapActivity extends Activity{

    final private String portalUrl = "http://www.arcgis.com";

    MapView mMapView;
    ArrayList<BasemapItem> mBasemapItem;

    // GPS location tracking
    private boolean mIsLocationTracking;
    private Point mLocation = null;

    // The circle area specified by search_radius and input lat/lon serves
    // searching purpose.
    // It is also used to construct the extent which map zooms to after the first
    // GPS fix is retrieved.
    private final static double SEARCH_RADIUS = 10;

    private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

    RelativeLayout relativeMapLayout;
    ImageButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        // layout to add MapView to
        relativeMapLayout = (RelativeLayout) findViewById(R.id.relative);
        // receive portal id and title of the basemap to add to the map
        Intent intent = getIntent();
        String itemId = intent.getExtras().getString("portalId");
        String title = intent.getExtras().getString("title");

        // adds back button to action bar
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(title);
        // load the basemap on a background thread
        loadWebMapIntoMapView(itemId, portalUrl);

        fab = (ImageButton) findViewById(R.id.fab);

        // floating action bar settings
        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
                outline.setOval(0, 0, size, size);
            }
        };
        fab.setOutlineProvider(viewOutlineProvider);

    }

    public void onClick(View view){
        // Toggle location tracking on or off
        if (mIsLocationTracking) {
            mMapView.getLocationDisplayManager().stop();
            mIsLocationTracking = false;
            fab.setImageResource(R.mipmap.ic_action_location_off);
        } else {
            startLocationTracking();
            fab.setImageResource(R.mipmap.ic_action_location_found);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate action bar menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        NavUtils.navigateUpFromSameTask(this);

        return super.onOptionsItemSelected(item);
    }


    /**
     * Creates a new WebMap on a background thread based on the portal item id of the basemap
     * to be used.  Goes back to UI thread to use the WebMap as a new MapView to display in
     * the ViewGroup layout.  Centers and zooms to default Palm Springs location.
     *
     * @param portalItemId represents the basemap to be used as a new webmap
     * @param portalUrl represents the portal url to look up the portalItemId
     */
    private void loadWebMapIntoMapView(final String portalItemId, final String portalUrl){
        TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                Portal portal = new Portal(portalUrl, null);
                // load a webmap instance from portal item
                final WebMap webmap = WebMap.newInstance(portalItemId, portal);

                if(webmap != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // udpate the MapView with the basemap
                            mMapView = new MapView(getApplicationContext(), webmap, null, null);

                            // Layout Parameters for MapView
                            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT);

                            mMapView.setLayoutParams(lp);
                            // add MapView to layout
                            relativeMapLayout.addView(mMapView);
                            // enable wrap around date line
                            mMapView.enableWrapAround(true);
                            // attribute esri
                            mMapView.setEsriLogoVisible(true);

                            mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
                                @Override
                                public void onStatusChanged(Object source, STATUS status) {
                                    if(mMapView == source && status == STATUS.INITIALIZED){
                                        // zoom in into Palm Springs
                                        mMapView.centerAndZoom(33.829547, -116.515882, 14);

                                    }
                                }
                            });

                        }
                    });
                }


                return null;
            }
        });
    }

    /**
     * Starts tracking GPS location.
     */
    void startLocationTracking() {
        LocationDisplayManager locDispMgr = mMapView.getLocationDisplayManager();
        locDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
        locDispMgr.setAllowNetworkLocation(true);
        locDispMgr.setLocationListener(new LocationListener() {

            boolean locationChanged = false;

            // Zooms to the current location when first GPS fix arrives
            @Override
            public void onLocationChanged(Location loc) {
                Point wgspoint = new Point(loc.getLongitude(), loc.getLatitude());
                mLocation = (Point) GeometryEngine.project(wgspoint, SpatialReference.create(4326),
                        mMapView.getSpatialReference());
                if (!locationChanged) {
                    locationChanged = true;
                    Unit mapUnit = mMapView.getSpatialReference().getUnit();
                    double zoomWidth = Unit.convertUnits(SEARCH_RADIUS, Unit.create(LinearUnit.Code.MILE_US), mapUnit);
                    Envelope zoomExtent = new Envelope(mLocation, zoomWidth, zoomWidth);
                    mMapView.setExtent(zoomExtent);
                }
            }

            @Override
            public void onProviderDisabled(String arg0) {
            }

            @Override
            public void onProviderEnabled(String arg0) {
            }

            @Override
            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
            }
        });
        locDispMgr.start();
        mIsLocationTracking = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
    }


    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

}
