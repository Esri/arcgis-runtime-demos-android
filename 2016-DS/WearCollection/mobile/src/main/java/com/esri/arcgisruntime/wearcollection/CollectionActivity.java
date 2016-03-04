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

package com.esri.arcgisruntime.wearcollection;

import java.util.ArrayList;
import java.util.Collection;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.geometry.Point;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureEditResult;
import com.esri.core.map.FeatureType;
import com.esri.core.map.Graphic;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.wearable.Wearable;

/**
 * Allows collection of features through a mobile application.
 */
public class CollectionActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private GoogleApiClient mGoogleApiClient;
  private ArcGISFeatureLayer mArcGISFeatureLayer;
  private View lastSelectedView = null;
  private int lastSelectedType = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_collection);
    // Initialize the Google API client
    initGoogleApiClient();

    // Get the name of the layer we're loading and put it as the toolbar title
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    Bundle b = getIntent().getExtras();
    toolbar.setTitle(b.getString("name"));

    // Get the URL of the layer to show
    String url = b.getString("url");

    // Create a MapView and add a Topographic basemap and the feature layer previously chosen
    com.esri.android.map.MapView mv = (com.esri.android.map.MapView)findViewById(R.id.mapview);
    mv.addLayer(new ArcGISTiledMapServiceLayer("http://server.arcgisonline.com/arcgis/rest/services/World_Topo_Map/MapServer/"));
    mArcGISFeatureLayer = new ArcGISFeatureLayer(url, ArcGISFeatureLayer.MODE.ONDEMAND);
    mv.addLayer(mArcGISFeatureLayer);

    // Set an action for clicking the floating action button
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // Create a dialog of a listview which will display FeatureTypes for the currently feature layer
        final ListView dialogView = new ListView(CollectionActivity.this);
        final ArrayList<String> featureTypes = FeatureLayerUtil.getFeatureTypes(mArcGISFeatureLayer);
        ListAdapter adapter = new ListAdapter(CollectionActivity.this, featureTypes);
        dialogView.setAdapter(adapter);
        // Set the current selected view to null
        lastSelectedView = null;
        // Set the current selection to 0 (so that clicking "Add" without selecting will still have
        // a valid index)
        lastSelectedType = 0;

        // Set a listener for clicking on items
        dialogView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // If we had previously selected an item, change its background back to white
            if (lastSelectedView != null) {
              lastSelectedView.setBackgroundColor(Color.WHITE);
            }
            // Set the now selected item to a light gray background
            view.setBackgroundColor(Color.LTGRAY);
            // Note the view and position selected for later use
            lastSelectedView = view;
            lastSelectedType = position;
          }
        });

        // Set a listener for clicking the dialog buttons
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            switch (which) {
              // Add will collect a feature of the specified type at the current location
              case DialogInterface.BUTTON_POSITIVE:
                collectFeature(featureTypes.get(lastSelectedType));
              // Cancel will simply dismiss the dialog without adding a feature
              case DialogInterface.BUTTON_NEGATIVE:
              default:
                break;
            }
          }
        };

        // Apply the button texts and show the dialog
        final AlertDialog dialog = new AlertDialog.Builder(CollectionActivity.this)
                .setPositiveButton(R.string.add, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .setView(dialogView)
                .show();
      }
    });
  }

  /**
   * Collects a feature of the specified type at the current device location.
   *
   * @param featureTypeName the type of feature to collect
   */
  private void collectFeature(final String featureTypeName) {
    try {
      // Request a single high precision location update
      LocationRequest request = LocationRequest.create()
              .setNumUpdates(1)
              .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
      LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
          // When we've got a location, get the FeatureType that matches the specified name
          FeatureType type = null;
          for (FeatureType ft : mArcGISFeatureLayer.getTypes()) {
            if (ft.getName().equals(featureTypeName)) {
              type = ft;
              break;
            }
          }
          // Only proceed if we found a matching type (which we always should)
          if (type != null) {
            // Create a new feature of the specified type at the current location
            Graphic g = mArcGISFeatureLayer.createFeatureWithType(type, new Point(location.getLongitude(), location.getLatitude()));
            // Apply the edit to the service
            mArcGISFeatureLayer.applyEdits(new Graphic[]{g}, null, null, new CallbackListener<FeatureEditResult[][]>() {
              @Override
              public void onCallback(FeatureEditResult[][] featureEditResults) {
                // Check that we have a success and report success
                if (featureEditResults[0].length > 0) {
                  FeatureEditResult fer = featureEditResults[0][0];
                  if (fer.isSuccess()) {
                    Log.i("Test", "Successfully added feature: " + fer.getObjectId());
                    Toast.makeText(CollectionActivity.this, "Successful collection!", Toast.LENGTH_SHORT).show();
                  } else {
                    Log.e("Test", "Failed to add feature: " + fer.getError().getDescription());
                  }
                }
              }

              @Override
              public void onError(Throwable throwable) {
                Log.e("Test", "Failed to add new graphic");
              }
            });
          } else {
            // If we don't have a matching feature type (which should never happen), log an error
            Log.e("Test", "Could not determine type");
          }
        }
      });
    } catch (SecurityException se) {
      // If we caught an exception trying to get the location, likelihood is that the location
      // permission has not been granted by the user
      Log.e("Test", "Could not access location. Check permissions.");
    }
  }

  /**
   * Initialize the Google API client which will be used to request the location.
   */
  private void initGoogleApiClient() {
    mGoogleApiClient = new GoogleApiClient.Builder(CollectionActivity.this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop() {
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_collection, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onConnected(Bundle bundle) {
    Log.i("Test", "Connected to Google Api Service");
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e("Test", "Failed to connect to Google Api Service");
  }
}

