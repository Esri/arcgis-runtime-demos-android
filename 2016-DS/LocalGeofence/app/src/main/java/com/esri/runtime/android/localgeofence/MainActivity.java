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
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Polygon;
import com.esri.core.map.Feature;
import com.esri.core.table.TableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  // Latest Google Play Services must be installed and available on device.
  //http://www.zionsoft.net/2014/02/google-play-services-set-up/
  private static GoogleApiClient mGoogleClient;
  private static Context mContext;
  public static boolean mLocationUpdatesStarted = false;


  // Geodatabase and table containing fence features to choose from.
  public static Geodatabase mGdb = null;
  public static GeodatabaseFeatureTable mGdbFeatureTable = null;

  // Define two sets of update intervals, for normal updates, and for fast updates
  // when device is near the fence. GeofenceService will only be assigned power
  // blame for the interval set by xx_UPDATE_INTERVAL, but can still receive
  // locations triggered by other applications at a rate up to
  // xx_FASTEST_UPDATE_INTERVAL. Note that these are set to suit demo purposes,
  // but should be tailored to the speciifc usage.
  public static final int NORMAL_UPDATE_INTERVAL = 1800000; // 30 minutes
  public static final int NORMAL_FASTEST_UPDATE_INTERVAL = 25000; // 25 seconds
  public static final int NORMAL_MIN_DISPLACEMENT = 150; // Meters
  public static final int FAST_UPDATE_INTERVAL = 15000; // 15 seconds
  public static final int FAST_FASTEST_UPDATE_INTERVAL = 2000; // 2 seconds
  public static final int FAST_MIN_DISPLACEMENT = 25; // Meters

  public static final String ACTION_CHECK_LOCATION = "com.esri.arcgis.android.samples.localgeofence.action.CHECK_LOCATION";
  public static final String ACTION_START_NORMAL_UPDATES = "com.esri.arcgis.android.samples.localgeofence.action.NORMAL_UPDATES";
  public static final String ACTION_START_FAST_UPDATES = "com.esri.arcgis.android.samples.localgeofence.action.FAST_UPDATES";

  // Intent Extra IDs
  public static final String GEOFENCE_FEATURE_OBJECTID_EXTRA_ID = "com.esri.arcgis.android.samples.geometryobjectid";


  // Geodatabase of geofences. Set this to any geodatabase, ensuring that the
  // approproate Layer ID and display field names are also set below.
  public static final String GEODATABASE_FILEPATH = "/ArcGIS/California/Cali2.geodatabase";
  public static final int FENCE_LAYER_ID = 1;
  public static final String FENCE_NAME_FIELD = "NAME";
  public static final String FENCE_OBJECTID_FIELD = "OBJECTID";

  // Keys for shared preferences.
  //public static final String KEY_FENCE_SUBTITLE = "fenceSubtitle";
  //public static final String KEY_FENCE_GEOMETRY_OID = "fenceGeometryOid";

  private RecyclerView mRecyclerView;
  List<GeofenceAlertItem> mGeofenceAlertItemsList = new ArrayList<GeofenceAlertItem>();
  static GeofenceListViewerAdapter mGeofenceListViewerAdapter;
  static CoordinatorLayout coordinatorLayout;
  Bitmap geofenceAlertThumbnail;


  LinearLayoutManager linearLayoutManager;

  String alertString;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main_recyclerview);
    mContext = this;
    coordinatorLayout = (CoordinatorLayout) findViewById(R.id
        .coordinatorLayout);
    // Recycler view to hold list of geofence alert items
    mRecyclerView = (RecyclerView) findViewById(R.id.geofenceAlertListView);
    mRecyclerView.setHasFixedSize(true);

    linearLayoutManager = new LinearLayoutManager(MainActivity.this);
    linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    mRecyclerView.setLayoutManager(linearLayoutManager);

    // Connect to geodatabase
    setupGeodatabase();

    geofenceAlertThumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.ic_notifications_black_48dp);
    alertString = "Alert While Enter and Exit";


    setupAlertItemAdapter();

    FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.addGeofence);
    floatingActionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        selectFromList();
      }
    });


  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.choose_location_map_menu_item) {
      selectOnMap();
      return true;
    } else if (id == R.id.show_location_menu_item) {
      showCurrentLocation();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onClick_SelectFromList(View view) {
    selectFromList();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mGdb != null) {
      mGdb.dispose();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Receive results from either map or list geofence selection - both give
    // exactly the same information.
    if (data == null) return;

    if (resultCode == RESULT_OK) {

      long fenceOid = data.getLongExtra(GEOFENCE_FEATURE_OBJECTID_EXTRA_ID, -1);
      Feature fenceFeature = getFeatureFromGeodatabase(fenceOid);
      if (fenceFeature != null) {
        Polygon fencePolygon = (Polygon) fenceFeature.getGeometry();
        final String fenceName = fenceFeature.getAttributeValue(FENCE_NAME_FIELD).toString();
        LocalGeofence.setFence(fencePolygon, mGdbFeatureTable.getSpatialReference());
        LocalGeofence.setFeatureName(fenceName);
        LocalGeofence.setFeatureOid(fenceOid);
        final GeofenceAlertItem geofenceAlertItem = new GeofenceAlertItem(alertString, fenceName, String.valueOf(fenceOid), geofenceAlertThumbnail, false);
        mGeofenceListViewerAdapter.add(geofenceAlertItem);
        final Snackbar snackbar = Snackbar
            .make(coordinatorLayout, "Added Geofence County: " + fenceName, Snackbar.LENGTH_LONG);
        snackbar.setAction("UNDO", new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mGeofenceListViewerAdapter.remove(geofenceAlertItem);
            Snackbar snackbarRemove = Snackbar
                .make(coordinatorLayout, "Removed Geofence County: " + fenceName, Snackbar.LENGTH_LONG);
            snackbarRemove.show();
          }
        });
        snackbar.show();
      }
    }
  }


  @Override
  protected void onNewIntent(Intent intent) {
    if (intent != null) {
      if (intent.getExtras() != null) {
        if (intent.getExtras().getString("Notification") != null) {
          stopServices();
          int selectedGeofenceAlertItemPosition = mGeofenceListViewerAdapter.getIndexOfSelectedAlertItem();
          mGeofenceListViewerAdapter.getGeofenceAlertItemList().get(selectedGeofenceAlertItemPosition).setFetchingLocationUpdates(false);
          mGeofenceListViewerAdapter.notifyDataSetChanged();
        }
      }
    }
    super.onNewIntent(intent);
  }

  /**
   * Start an activity that allows the user to select a geofence feature from a list.
   */
  private void selectFromList() {
    Intent geofenceListIntent = new Intent(MainActivity.this, GeofenceListActivity.class);

    // If working against multiple layers, use Extras to send layer IDs.
    startActivityForResult(geofenceListIntent, FENCE_LAYER_ID);
  }

  /**
   * Start an activity that allows the user to select a geofence feature on a map.
   */
  private void selectOnMap() {
    Intent geofenceMapIntent = new Intent(MainActivity.this, GeofenceMapActivity.class);
    // If working against multiple layers, use Extras to send layer IDs.
    startActivityForResult(geofenceMapIntent, FENCE_LAYER_ID);
  }

  /**
   * Start an activity that shows the users current location, so they can track
   * progress.
   */
  private void showCurrentLocation() {
    Intent currentLocationIntent = new Intent(MainActivity.this,
        CurrentLocationActivity.class);
    // No result is required as we dont need any data from this activity.
    startActivity(currentLocationIntent);
  }

  /**
   * Get a feature from the geodatabase by its ObjectID.s
   */
  private static Feature getFeatureFromGeodatabase(long featureOid) {
    if (featureOid < 0) {
      return null;
    }
    Feature feature = null;
    try {
      feature = mGdbFeatureTable.getFeature(featureOid);
    } catch (TableException e) {
      e.printStackTrace();
    }
    return feature;
  }

  /**
   * Open the geodatabase of geofence features.
   *
   * @return true if geodatabase was opened, otherwise false.
   */
  private boolean setupGeodatabase() {
    File rootFile = Environment.getExternalStorageDirectory();
    File dataFile = new File(rootFile, GEODATABASE_FILEPATH);
    if (!dataFile.exists()) return false;

    String filePath = dataFile.getAbsolutePath();
    try {
      mGdb = new Geodatabase(filePath);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return false;
    }

    if (mGdb == null) return false;
    mGdbFeatureTable = mGdb.getGeodatabaseFeatureTableByLayerId(FENCE_LAYER_ID);
    return (mGdbFeatureTable != null);
  }

  /**
   * Sets up the adapter for recycler view
   */
  private void setupAlertItemAdapter() {

    mGeofenceListViewerAdapter = new GeofenceListViewerAdapter(mGeofenceAlertItemsList);

    mRecyclerView.setAdapter(mGeofenceListViewerAdapter);
  }

  /**
   * Updates the Local fence polygon based on selection
   * @param position
   */
  public static void updateLocalFence(int position) {
    long fenceOid = Long.valueOf(mGeofenceListViewerAdapter.getGeofenceAlertItemList().get(position).getFeatureId());
    Feature fenceFeature = getFeatureFromGeodatabase(fenceOid);
    if (fenceFeature != null) {
      Polygon fencePolygon = (Polygon) fenceFeature.getGeometry();
      final String fenceName = fenceFeature.getAttributeValue(FENCE_NAME_FIELD).toString();
      LocalGeofence.setFence(fencePolygon, mGdbFeatureTable.getSpatialReference());
      LocalGeofence.setFeatureName(fenceName);
      LocalGeofence.setFeatureOid(fenceOid);
      LocalGeofence.setLastStatus(LocalGeofence.Status.UNKNOWN);
   }

  }


  /**
   * Send an intent to start the geofence service that listens to location
   * updates at a fast/normal rate of frequency.
   */
  public static void startGeofenceService() {
    /*Intent serviceIntent = new Intent(MainActivity.ACTION_START_NORMAL_UPDATES,
        null, this, GeofenceServiceNormal.class);*/
    Intent serviceIntent = new Intent(MainActivity.ACTION_START_FAST_UPDATES,
        null, mContext, GeofenceServiceFast.class);
    PendingIntent pendingIntent = PendingIntent.getService(mContext, 0,
        serviceIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    try {
      pendingIntent.send();
      mLocationUpdatesStarted = true;
      Snackbar snackbar = Snackbar
          .make(coordinatorLayout, "Started Fetching Location Updates  ", Snackbar.LENGTH_LONG);
      snackbar.show();
    } catch (PendingIntent.CanceledException e) {
      Snackbar snackbar = Snackbar
          .make(coordinatorLayout, "Problem starting geofence service", Snackbar.LENGTH_LONG);
      snackbar.show();
      e.printStackTrace();
    }
  }

  /**
   * Unsubscribe both of the geofence services from location updates.
   */
  public static void stopServices() {
    ((Activity) mContext).runOnUiThread(new Runnable() {
      @Override
      public void run() {
        mGoogleClient = new GoogleApiClient.Builder(mContext)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
              @Override
              public void onConnected(Bundle bundle) {
                Log.i(TAG, "MainActivity.stopServiceByConnectAndRemove.onConnected");
                mLocationUpdatesStarted = false;
                // Create a PendingIntent for this GeofenceServiceNormal class
                // and use it to unregister from location updates.
                Intent fastIntent = new Intent(MainActivity.ACTION_CHECK_LOCATION,
                    null, mContext, GeofenceServiceFast.class);
                PendingIntent fastPendingIntent = PendingIntent.getService(
                    mContext, 0, fastIntent, 0);
                final PendingResult<Status> cancelFastResult = LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleClient, fastPendingIntent);

                cancelFastResult.setResultCallback(new ResultCallback<Status>() {
                  @Override
                  public void onResult(Status status) {
                    Log.i(TAG, "    removeLocationUpdates PendingResult Status:" + status);
                    final Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Stopped Fast Location Updates", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                  }
                });

                Intent normalIntent = new Intent(MainActivity.ACTION_CHECK_LOCATION,
                    null, mContext, GeofenceServiceNormal.class);
                PendingIntent normalPendingIntent = PendingIntent.getService(
                    mContext, 0, normalIntent, 0);
                final PendingResult<Status> cancelNormalResult = LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleClient, normalPendingIntent);
                cancelNormalResult.setResultCallback(new ResultCallback<Status>() {
                  @Override
                  public void onResult(Status status) {
                    Log.i(TAG, "    removeLocationUpdates PendingResult Status:" + status);
                    final Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Stopped Normal Location Updates", Snackbar.LENGTH_SHORT);
                    snackbar.show();
                  }
                });
                // Disconnect the client again, no longer required unless user
                // wants to start updates again.
                mGoogleClient.disconnect();
                mGoogleClient = null;
              }

              @Override
              public void onConnectionSuspended(int i) {

              }
            })
            .build();

        mGoogleClient.connect();
      }
    });
  }

}
