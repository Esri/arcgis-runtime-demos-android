package com.esri.runtime.android.localgeofence;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.esri.core.geometry.Point;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.Random;


/**
 * An IntentService to receive updates from the Google Fusion API at a frequent
 * rate of updates. Also responsible for stopping the normal update rate
 * service when
 */
public class GeofenceServiceFast extends IntentService  {

  private static final String TAG = GeofenceServiceFast.class.getSimpleName();

  GoogleApiClient mGoogleApiClient = null;

  public GeofenceServiceFast() {
    super(TAG);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    if (intent != null) {
      final String action = intent.getAction();

      if (MainActivity.ACTION_CHECK_LOCATION.equals(action)) {
        final Location location = intent.getParcelableExtra(
            FusedLocationProviderApi.KEY_LOCATION_CHANGED);
        handleActionCheckLocation(location);
      }
      else if (MainActivity.ACTION_START_NORMAL_UPDATES.equals(action)) {
        handleActionChangeToNormalUpdates();
      }
      else if (MainActivity.ACTION_START_FAST_UPDATES.equals(action)) {
        handleActionStartFastUpdates();
      }
    }
  }

  /**
   * Start receiving location updates, by connecting to the Google API client,
   * and registering to receive location updates at a more frequent rate,
   * using high power and accuracy location settings.
   * If this is called, then we assume that the Normal updates service is not
   * running already.
   */
  private void handleActionStartFastUpdates() {

    // Connect to the Google API client.
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

          @Override
          public void onConnected(Bundle bundle) {

            // Define settings for Google Fusion API location updates. Use high power
            // and accuracy, and ensure updates are not requested frequently, also allow
            // updates to be sent when other apps request data updates.
            LocationRequest accurateRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setFastestInterval(MainActivity.FAST_FASTEST_UPDATE_INTERVAL)
                .setInterval(MainActivity.FAST_UPDATE_INTERVAL)
                .setSmallestDisplacement(MainActivity.FAST_MIN_DISPLACEMENT);

            // Create a PendingIntent for this service, and send this to the
            // requestLocationUpdates method.
            Intent serviceIntent = new Intent(MainActivity.ACTION_CHECK_LOCATION,
                null, getApplicationContext(), GeofenceServiceFast.class);

            PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(), 0, serviceIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

            // Start the request for location updates.
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, accurateRequest, pendingIntent);

            // Disconnect the client, not required until next change.
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
          }

          @Override
          public void onConnectionSuspended(int i) {

          }
        })
        .build();

    mGoogleApiClient.connect();
  }

  /**
   * Stop this service, and register the other normal update service to receive
   * location updates instead.
   */
  private void handleActionChangeToNormalUpdates() {
    // Connect to the Google API client.
    mGoogleApiClient = new GoogleApiClient.Builder(this)
        .addApi(LocationServices.API)
        .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {

          @Override
          public void onConnected(Bundle bundle) {
            Intent selfIntent = new Intent(MainActivity.ACTION_CHECK_LOCATION,
                null, getApplicationContext(), GeofenceServiceFast.class);
            PendingIntent selfPendingIntent = PendingIntent.getService(
                getApplicationContext(), 0, selfIntent, 0);
            final PendingResult<Status> cancelFastResult =
                LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, selfPendingIntent);
            cancelFastResult.setResultCallback(new ResultCallback<Status>() {
              @Override
              public void onResult(Status status) {
                Log.i(TAG, "    handleActionChangeToNormalUpdates PendingResult Status:" + status);
              }
            });

            LocationRequest balancedRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setFastestInterval(MainActivity.NORMAL_FASTEST_UPDATE_INTERVAL)
                .setInterval(MainActivity.NORMAL_UPDATE_INTERVAL)
                .setSmallestDisplacement(MainActivity.NORMAL_MIN_DISPLACEMENT);

            // Create a PendingIntent to start the GeofenceServiceNormal.
            Intent serviceIntent = new Intent(MainActivity.ACTION_CHECK_LOCATION,
                null, getApplicationContext(), GeofenceServiceNormal.class);

            PendingIntent pendingIntent = PendingIntent.getService(
                getApplicationContext(), 0, serviceIntent, 0);

            // Start the request for location updates.
            LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, balancedRequest, pendingIntent);

            // Disconnect the client, not required until next change.
            mGoogleApiClient.disconnect();
            mGoogleApiClient = null;
          }

          @Override
          public void onConnectionSuspended(int i) {

          }
        })
        .build();

    mGoogleApiClient.connect();
  }



  /**
   * This action is handled in the provided background thread. It is called
   * when this service receives a location update from Google fusion API;
   * therefore no connection to the Google API client is required.
   */
  private void handleActionCheckLocation(Location newLocation) {

    if(newLocation !=null) {
      // The incoming location is the current device location, in geographic coordinates.
      Point locationPoint = new Point(newLocation.getLongitude(), newLocation.getLatitude());
      LocalGeofence.FenceInformation info = LocalGeofence.latestLocation(locationPoint);
      Log.i(TAG, String.format("GeofenceServiceFast Status: %s, UpdateChange: %s, Change: %s", info.status, info.updateChange, info.change));

      if (info.change == LocalGeofence.Change.ENTERED) {
        sendNotification(String.format("Alert! Entered %s", LocalGeofence.getFeatureName()));
      }
      else if (info.change == LocalGeofence.Change.EXITED) {
        sendNotification(String.format("Exited %s", LocalGeofence.getFeatureName()));
      }

      if (LocalGeofence.UpdateChange.FASTER == info.updateChange) {
        // Ensure we are receiving updates frequently.
        handleActionStartFastUpdates();
      }
      else if (LocalGeofence.UpdateChange.SLOWER == info.updateChange) {
        // Ensure we are receiving updates less frequently.
        handleActionChangeToNormalUpdates();
      }
    }
  }

  public void sendNotification(String title) {
    try {
      NotificationManager notificationManager = null;
      Context ctx = getApplicationContext();
      if (ctx != null)
      {
        notificationManager = (NotificationManager) ctx.getSystemService(NOTIFICATION_SERVICE);
      }
      else {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
      }

      // Build a notification.
      Notification.Builder notificationBuilder = new Notification.Builder(this);
      notificationBuilder.setContentTitle(title);
      notificationBuilder.setContentText(LocalGeofence.getSubtitle());
      notificationBuilder.setSmallIcon(R.drawable.ic_fence_simple);
      Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_geofence_bright);
      notificationBuilder.setLargeIcon(largeIcon);
      if (Build.VERSION.SDK_INT >= 21) {
        // API 21 and over -
        notificationBuilder.setColor(getResources().getColor(R.color.material_blue_700));
      }

      // Notification API was introduced at v11, but there were some additional changes from v16.
      // We only need to deal with changes since v14.
      Random r = new Random();
      int id = r.nextInt();
      if (Build.VERSION.SDK_INT < 16) {
        notificationManager.notify(id, notificationBuilder.getNotification());
      } else {
        notificationManager.notify(id, notificationBuilder.build());
      }
    }
    catch (Exception ex) {
      Log.i(TAG, ex.getMessage());
    }
  }

}
