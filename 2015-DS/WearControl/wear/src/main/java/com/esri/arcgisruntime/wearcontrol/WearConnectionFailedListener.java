package com.esri.arcgisruntime.wearcontrol;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class WearConnectionFailedListener implements GoogleApiClient.OnConnectionFailedListener {
  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e("test", "Failed to connect to Google API Client");
  }
}
