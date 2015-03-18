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
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geometry.SpatialReference;

import java.io.File;

public class GeofenceMapActivity extends Activity {


  public static final String TAG = "GeofenceMapActivity";

  private final int SELECT_TOLERANCE = 10;

  MapView mMapView;
  FeatureLayer mGeofenceFeatureLayer = null;
  SpatialReference mGeofenceSpatialReference = null;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_geofence_map);

    mMapView = (MapView)findViewById(R.id.map);
    mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
      @Override
      public void onStatusChanged(Object o, STATUS status) {
        if (status == STATUS.INITIALIZED) {
          // When map is initialized, allow user to select a feature to use
          // as the geofence.
          mMapView.setOnSingleTapListener(mapSingleTapListener);
        }
      }
    });

    // If the app is offline, use a local TPK as a basemap. If online, then
    // use online global basemap instead.
    if (isOffline()) {
      String tpkPath = getResources().getString(R.string.tpk_path);
      String path = Environment.getExternalStorageDirectory().getPath() + tpkPath;
      File f = new File(path);
      if (f.exists()) {
        ArcGISLocalTiledLayer localTiledLayer = new ArcGISLocalTiledLayer(path);
        mMapView.addLayer(localTiledLayer);
        mMapView.setExtent(localTiledLayer.getExtent());
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

    addGeofenceFeatureLayer();
  }


  private void addGeofenceFeatureLayer() {
    if (MainActivity.mGdbFeatureTable != null) {
      mGeofenceFeatureLayer = new FeatureLayer(MainActivity.mGdbFeatureTable);
      mGeofenceSpatialReference = mGeofenceFeatureLayer.getSpatialReference();
      mMapView.addLayer(mGeofenceFeatureLayer);
      mMapView.setExtent(mGeofenceFeatureLayer.getExtent());
    }
    else
    {
      Toast.makeText(this, "Cannot display geofence features", Toast.LENGTH_LONG).show();
    }
  }

  private boolean isOffline() {
    ConnectivityManager cm =
        (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    if (activeNetwork == null) return true;
    return !activeNetwork.isConnectedOrConnecting();
  }

  /**
   * Map SingleTapListener identifies the tapped feature and gets the
   * Object ID of the feature.
   */
  final OnSingleTapListener mapSingleTapListener = new OnSingleTapListener() {
    @Override
    public void onSingleTap(float x, float y) {

      if (mGeofenceSpatialReference == null) {
        mGeofenceSpatialReference = mGeofenceFeatureLayer.getSpatialReference();
      }

      // Select the tapped water feature to use as the geofence.
      long[] featureIds = mGeofenceFeatureLayer.getFeatureIDs(x, y, SELECT_TOLERANCE);
      if (featureIds.length > 0) {

        // Show features selected on map.
        mGeofenceFeatureLayer.selectFeatures(featureIds, false);

        // Get the feature object ID.
        Long objectId = -1L;
        if (featureIds.length == 1) {
          objectId = featureIds[0];
        }

        // Return the object ID of the feature as an Activity result.
        Intent resultIntent = new Intent();
        resultIntent.putExtra(MainActivity.GEOFENCE_FEATURE_OBJECTID_EXTRA_ID, objectId);
        setResult(RESULT_OK, resultIntent);
      }
    }
  };

}
