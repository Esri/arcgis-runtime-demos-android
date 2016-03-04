/*
 * COPYRIGHT 1995-2016 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.esri.arcgisruntime.wearcollection;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.geometry.GeometryEngine;
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
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Provides a background service that listens for requests from the Wear device for displaying
 * collection options and collecting a new feature.
 */
public class RequestListenerService extends WearableListenerService {

  // Specify the paths for the various request and response types
  private static final String LAYER_REQUEST = "/request/layers";
  private static final String FEATURE_TYPE_REQUEST = "/request/feature_types";
  private static final String LAYER_RESPONSE = "/response/layers";
  private static final String FEATURE_TYPE_RESPONSE = "/response/feature_types";
  private static final String STATUS_RESPONSE = "/response/status";
  // ArcGISFeatureLayer for fetching FeatureTypes and collecting a new feature
  private static ArcGISFeatureLayer sArcGISFeatureLayer;

  // HashMap of layer names to their URLs
  private static final HashMap<String, String> sLayerMap = FileUtil.getLayerMap();


  @Override
  public void onMessageReceived(final MessageEvent event) {
    Log.i("Test", "Received message!");
    // When a message is received, build a Google API client and connect it
    // The Wearable API is used for communicating with the Wear device, and the
    // Location API is used when collecting a new feature
    final GoogleApiClient client = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .addApi(LocationServices.API)
            .build();
    ConnectionResult connectionResult = client.blockingConnect(30, TimeUnit.SECONDS);
    if (!connectionResult.isSuccess()) {
      Log.e("Test", "Failed to connect to GoogleApiClient");
    }
    Log.i("Test", "Successfully connected to Google Api Service");
    // Get the path of the message and handle it appropriately
    String path = event.getPath();
    if (LAYER_REQUEST.equals(path)) {
      handleLayerRequest(event, client);
    } else if (FEATURE_TYPE_REQUEST.equals(path)) {
      handleFeatureTypeRequest(event, client);
    } else if (FEATURE_TYPE_RESPONSE.equals(path)) {
      handleFeatureTypeResponse(event, client);
    }
  }

  /**
   * Handles issuing a response to a layer request. A layer request indicates
   * that the Wear device wants a list of previously used layers to display to
   * the user.
   *
   * @param event the MessageEvent from the Wear device
   * @param client the Google API client used to communicate
   */
  private void handleLayerRequest(MessageEvent event, GoogleApiClient client) {
    Log.i("Test", "Received Layer request, sending layer list");
    // Create a PutDataMapRequest with the Layer response path
    PutDataMapRequest req = PutDataMapRequest.create(LAYER_RESPONSE);
    DataMap dm = req.getDataMap();
    // Put an array list of layer names into the data map
    dm.putStringArray("layers", sLayerMap.keySet().toArray(new String[sLayerMap.size()]));
    // Put the current time into the data map, which forces an onDataChanged event (this event
    // only occurs when data actually changes, so putting the time ensures something always changes)
    dm.putLong("Time", System.currentTimeMillis());
    // Put the DataItem into the Data API stream
    Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
  }

  /**
   * Handles issuing a response to a FeatureType request. A FeatureType request
   * indicates that the Wear devices wants a list of available FeatureTypes for
   * the selected layer to display to the user.
   *
   * @param event the MessageEvent from the Wear device
   * @param client the Google API client used to communicate
   */
  private void handleFeatureTypeRequest(MessageEvent event, GoogleApiClient client) {
    // Get the name and URL of the layer that was selected
    String layerName = new String(event.getData());
    String url = sLayerMap.get(layerName);
    // Create an ArcGISFeatureLayer with the specified URL
    sArcGISFeatureLayer = new ArcGISFeatureLayer(url, ArcGISFeatureLayer.MODE.SNAPSHOT);

    // While this isn't good practice, there is no way to be notified that an
    // ArcGISFeatureLayer has loaded its LayerServiceInfo. The OnStatusChangedListener
    // seems to be more relevant when the layer is actually being added to a MapView.
    // As such, we simply do a quick sleep until the LayerServiceInfo has been loaded
    try {
      while (sArcGISFeatureLayer.getLayerServiceInfo() == null) {
        Thread.sleep(500);
      }
    } catch (Exception e) {
      //
    }
    // Create a PutDataMapRequest with the FeatureType response path
    PutDataMapRequest req = PutDataMapRequest.create(FEATURE_TYPE_RESPONSE);
    DataMap dm = req.getDataMap();
    // Put an array list of the FeatureType names into the data map
    dm.putStringArrayList("featureTypes", FeatureLayerUtil.getFeatureTypes(sArcGISFeatureLayer));
    // Put the current time into the data map, which forces an onDataChanged event (this event
    // only occurs when data actually changes, so putting the time ensures something always changes)
    dm.putLong("Time", System.currentTimeMillis());
    // Put the DataItem into the Data API stream
    Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
  }

  /**
   * Handles issuing a response to a FeatureType response. A FeatureType response
   * indicates that the Wear devices wants a feature of the specified FeatureType
   * to be collected at the current device location.
   *
   * @param event the MessageEvent from the Wear device
   * @param client the Google API client used to communicate
   */
  private void handleFeatureTypeResponse(final MessageEvent event, final GoogleApiClient client) {
    // Create a PutDataMapRequest with the status response path
    final PutDataMapRequest req = PutDataMapRequest.create(STATUS_RESPONSE);
    final DataMap dm = req.getDataMap();
    // Put the current time into the data map, which forces an onDataChanged event (this event
    // only occurs when data actually changes, so putting the time ensures something always changes)
    dm.putLong("Time", System.currentTimeMillis());
    try {
      // Request a single high precision location update
      LocationRequest request = LocationRequest.create()
              .setNumUpdates(1)
              .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
      LocationServices.FusedLocationApi.requestLocationUpdates(client, request, new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
          // When we've got a location, get the FeatureType that matches the specified name
          Log.i("Test", "Received location");
          String featureTypeName = new String(event.getData());
          FeatureType type = null;
          for (FeatureType ft : sArcGISFeatureLayer.getTypes()) {
            if (ft.getName().equals(featureTypeName)) {
              type = ft;
              break;
            }
          }
          // Only proceed if we found a matching type (which we always should)
          if (type != null) {
            // Create a new feature of the specified type at the current location
            Graphic g = sArcGISFeatureLayer.createFeatureWithType(type, new Point(location.getLongitude(), location.getLatitude()));
            // Apply the edit to the service
            sArcGISFeatureLayer.applyEdits(new Graphic[]{g}, null, null, new CallbackListener<FeatureEditResult[][]>() {
              @Override
              public void onCallback(FeatureEditResult[][] featureEditResults) {
                // Check that we have a success and report success
                if (featureEditResults[0].length > 0) {
                  FeatureEditResult fer = featureEditResults[0][0];
                  if (fer.isSuccess()) {
                    Log.i("Test", "Successfully added feature");
                    // Put a boolean indicating success into the data map
                    dm.putBoolean("success", true);
                  } else {
                    Log.e("Test", "Failed to add feature: " + fer.getError().getDescription());
                    // Put a boolean indicating failure into the data map
                    dm.putBoolean("success", false);
                    // Put a string with the reason for failure into the data map
                    dm.putString("reason", "Error code: " + fer.getError().getCode());
                  }
                }
                // Put the DataItem into the Data API stream
                Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
              }

              @Override
              public void onError(Throwable throwable) {
                Log.d("Test", "Failed to add new graphic");
                // Put a boolean indicating failure into the data map
                dm.putBoolean("success", false);
                // Put a string with the reason for failure into the data map
                dm.putString("reason", throwable.getLocalizedMessage());
                // Put the DataItem into the Data API stream
                Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
              }
            });
          } else {
            // If we don't have a matching feature type (which should never happen), log an error
            Log.e("Test", "Could not determine type");
            // Put a boolean indicating failure into the data map
            dm.putBoolean("success", false);
            // Put a string with the reason for failure into the data map
            dm.putString("reason", "Specified type not found");
            // Put the DataItem into the Data API stream
            Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
          }
        }
      });
    } catch (SecurityException se) {
      // If we caught an exception trying to get the location, likelihood is that the location
      // permission has not been granted by the user
      Log.e("Test", "Could not access location");
      // Put a boolean indicating failure into the data map
      dm.putBoolean("success", false);
      // Put a string with the reason for failure into the data map
      dm.putString("reason", "Could not access location. Check permissions.");
      // Put the DataItem into the Data API stream
      Wearable.DataApi.putDataItem(client, req.asPutDataRequest());
    }
  }
}
