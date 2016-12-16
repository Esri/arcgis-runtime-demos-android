/* Copyright 2016 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * A copy of the license is available in the repository's
 * https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt
 *
 * For information about licensing your deployed app, see
 * https://developers.arcgis.com/android/guide/license-your-app.htm
 *
 */

package com.esri.runtime.android.materialbasemaps.ui;


import java.util.concurrent.Callable;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.runtime.android.materialbasemaps.R;
import com.esri.runtime.android.materialbasemaps.util.TaskExecutor;

/**
 * Activity for Map and floating action bar button.
 */
public class MapActivity extends Activity{

    private MapView mMapView;

    // GPS location tracking
    private boolean mIsLocationTracking;
//    private Point mLocation = null;

    // The circle area specified by search_radius and input lat/lon serves
    // searching purpose.
    // It is also used to construct the extent which map zooms to after the first
    // GPS fix is retrieved.
    private final static double SEARCH_RADIUS = 10;

    private static final String KEY_IS_LOCATION_TRACKING = "IsLocationTracking";

    private RelativeLayout relativeMapLayout;
    private ImageButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        // layout to add MapView to
        relativeMapLayout = (RelativeLayout) findViewById(R.id.relative);
        // receive portal id and title of the basemap to add to the map
        Intent intent = getIntent();
        String portalUrl = intent.getExtras().getString("portalUrl");
        String itemId = intent.getExtras().getString("portalId");
        String title = intent.getExtras().getString("title");

        // adds back button to action bar
        ActionBar actionBar = getActionBar();
        assert actionBar != null;
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(title);
        // load the basemap on a background thread
//        String portalUrl = getResources().getString(R.string.portal_url);
        loadPortalItemIntoMapView(itemId, portalUrl);

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
            mMapView.getLocationDisplay().stop();
            mIsLocationTracking = false;
            fab.setImageResource(R.mipmap.ic_action_location_off);
        } else {
//            startLocationTracking();
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
    private void loadPortalItemIntoMapView(final String portalItemId, final String portalUrl){
        TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                final Portal portal = new Portal(portalUrl);
                portal.loadAsync();

                // create a PortalItem from the Item ID
                PortalItem portalItem = new PortalItem(portal, portalItemId);
                // create an ArcGISMap from portal item
                final ArcGISMap portalMap = new ArcGISMap(portalItem);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        portal.addDoneLoadingListener(new Runnable() {
                            @Override
                            public void run() {
                                if(portal.getLoadStatus() == LoadStatus.LOADED){
                                    // create a PortalItem from the Item ID
                                    PortalItem portalItem = new PortalItem(portal, portalItemId);
                                    // create an ArcGISMap from portal item
                                    final ArcGISMap portalMap = new ArcGISMap(portalItem);
                                    //
                                    // update the MapView with the basemap
                                    mMapView = new MapView(getApplicationContext());
                                    mMapView.setMap(portalMap);

                                    Log.d("MapView", "Scale: " + mMapView.getMapScale());
                                    Point initialPoint = new Point(-122.3238046, 47.5972201, SpatialReferences.getWgs84());
                                    mMapView.setViewpointCenterAsync(initialPoint);

                                    // Layout Parameters for MapView
                                    ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT);

                                    mMapView.setLayoutParams(lp);
                                    // add MapView to layout
                                    relativeMapLayout.addView(mMapView);
                                }
                            }
                        });
                    }
                });
                return null;
            }
        });
    }

    /**
     * Starts tracking GPS location.
     */
//    private void startLocationTracking() {
//        LocationDisplayManager locDispMgr = mMapView.getLocationDisplayManager();
//        locDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);
//        locDispMgr.setAllowNetworkLocation(true);
//        locDispMgr.setLocationListener(new LocationListener() {
//
//            boolean locationChanged = false;
//
//            // Zooms to the current location when first GPS fix arrives
//            @Override
//            public void onLocationChanged(Location loc) {
//                Point wgspoint = new Point(loc.getLongitude(), loc.getLatitude());
//                mLocation = (Point) GeometryEngine.project(wgspoint, SpatialReference.create(4326),
//                        mMapView.getSpatialReference());
//                if (!locationChanged) {
//                    locationChanged = true;
//                    Unit mapUnit = mMapView.getSpatialReference().getUnit();
//                    double zoomRadius = Unit.convertUnits(SEARCH_RADIUS, Unit.create(LinearUnit.Code.MILE_US), mapUnit);
//                    Envelope zoomExtent = new Envelope(mLocation, zoomRadius, zoomRadius);
//                    mMapView.setExtent(zoomExtent);
//                }
//            }
//
//            @Override
//            public void onProviderDisabled(String arg0) {
//            }
//
//            @Override
//            public void onProviderEnabled(String arg0) {
//            }
//
//            @Override
//            public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
//            }
//        });
//        locDispMgr.start();
//        mIsLocationTracking = true;
//    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(KEY_IS_LOCATION_TRACKING, mIsLocationTracking);
    }

}
