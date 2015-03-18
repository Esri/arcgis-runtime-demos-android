/* Copyright 2015 Esri
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

package com.esri.runtime.android.localgeofence;


import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.geometry.Unit;

import java.io.File;

/**
 * This activity allows the user to monitor where they currently are, using
 * Navigation map auto-pan mode.
 * As an enhancement, could add ability to show the geofence layer, and highlight
 * the currently selected geofence.
 */
public class CurrentLocationActivity extends Activity {

  private static final String TAG = CurrentLocationActivity.class.getSimpleName();

  MapView mMapView = null;
  LocationDisplayManager mLocDispMgr = null;

   @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_current_location);

    mMapView = (MapView)findViewById(R.id.map);
    mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
      @Override
      public void onStatusChanged(Object o, STATUS status) {
        if (status == STATUS.INITIALIZED) {
          // When the map is initialized, start the LocationDisplayManager.
          mLocDispMgr.setLocationListener(mLocationListener);
          mLocDispMgr.start();
        }
      }
    });
    mMapView.setAllowRotationByPinch(true);

    if (isOffline()) {
      String tpkPath = getResources().getString(R.string.tpk_path);
      String path = Environment.getExternalStorageDirectory().getPath() + tpkPath;
      File f = new File(path);
      if (f.exists()) {
        ArcGISLocalTiledLayer localTiledLayer = new ArcGISLocalTiledLayer(path);
        mMapView.addLayer(localTiledLayer);
        mMapView.setExtent(localTiledLayer.getExtent());

        FeatureLayer fenceFeatureLayer = new FeatureLayer(MainActivity.mGdbFeatureTable);
        mMapView.addLayer(fenceFeatureLayer);
      }
      else
      {
        Toast.makeText(this, getResources().getString(
            R.string.noBasemapMessage), Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Failed to find tpk: " + tpkPath);
      }
    }
    else
    {
      // If online, use an online basemap.
      String url = getResources().getString(R.string.basemap_url);
      ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(url);
      mMapView.addLayer(tiledLayer);
      mMapView.setExtent(tiledLayer.getExtent(), 0, false);
    }

    mLocDispMgr = mMapView.getLocationDisplayManager();
    mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);

  }

  private boolean isOffline() {
    ConnectivityManager cm =
        (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    if (activeNetwork == null) return true;
    return !activeNetwork.isConnectedOrConnecting();
  }

  public void onClick_NavButton(View view) {
    if (mLocDispMgr != null) {
      // Re-enable the navigation mode.
      mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
    }
  }

  final LocationListener mLocationListener = new LocationListener() {

    boolean locationChanged = false;

    @Override
    public void onLocationChanged(Location location) {
      if (!locationChanged) {
        // For first fix location, convert to map spatial reference.
        Point currentPt = new Point(location.getLongitude(), location.getLatitude());
        Point currentMapPt = (Point) GeometryEngine.project(currentPt,
            SpatialReference.create(4326), mMapView.getSpatialReference());

        // Use a suitable accuracy value for the typical app usage, if no accuracy
        // value is available.
        float accuracy = 100;
        if (location.hasAccuracy()) {
          accuracy = location.getAccuracy();
        }

        // Convert the accuracy to units of the map, and apply a suitable zoom
        // factor for the app.
        Unit mapUnits = mMapView.getSpatialReference().getUnit();
        double zoomToWidth = 500 * Unit.convertUnits(accuracy,
            Unit.create(LinearUnit.Code.METER), mapUnits);
        Envelope zoomExtent = new Envelope(currentMapPt, zoomToWidth, zoomToWidth);

        // Make sure that the initial zoom is done WITHOUT animation, or it may
        // interfere with autopan.
        mMapView.setExtent(zoomExtent, 0, false);

        // Dont run this again.
        locationChanged = true;

        // Now start the navigation mode.
        mLocDispMgr.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
      }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) { }

    @Override
    public void onProviderEnabled(String provider) { }

    @Override
    public void onProviderDisabled(String provider) { }
  };

}
