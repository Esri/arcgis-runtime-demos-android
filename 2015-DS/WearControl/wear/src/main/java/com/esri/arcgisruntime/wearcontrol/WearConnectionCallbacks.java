package com.esri.arcgisruntime.wearcontrol;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;

public class WearConnectionCallbacks implements GoogleApiClient.ConnectionCallbacks {
  @Override
  public void onConnected(Bundle bundle) {
    Log.d("test", "onConnected");
  }

  @Override
  public void onConnectionSuspended(int i) {

  }
}
