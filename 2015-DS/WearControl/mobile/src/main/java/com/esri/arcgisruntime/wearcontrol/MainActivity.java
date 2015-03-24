package com.esri.arcgisruntime.wearcontrol;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.thalmic.myo.Hub;


public class MainActivity extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private GoogleApiClient mGoogleApiClient;
  private WearMessageListener mWearMessageListener;
  private MapView mMapView;
  private MyoFeatureControlListener mFeatureControlListener;
  private MyoMapControlListener mMapControlListener;
  private boolean mIsFeatureControl = false;

  private static final int REQUEST_ENABLE_BT = 1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // First, we initialize the Hub singleton
    final Hub hub = Hub.getInstance();
    if (!hub.init(this, getPackageName())) {
      // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
      Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    hub.setLockingPolicy(Hub.LockingPolicy.NONE);
    // Connects to a Myo that is physically touching the device
    hub.attachToAdjacentMyo();
    mMapView = new MapView(this, "http://csf.maps.arcgis.com/home/item.html?id=f065c8fa4e514749aeef57919b3d192e", null, null);
    mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
      @Override
      public void onStatusChanged(Object source, STATUS status) {
        if (source instanceof ArcGISFeatureLayer && status == STATUS.LAYER_LOADED) {
          mFeatureControlListener = new MyoFeatureControlListener(MainActivity.this, mMapView, (ArcGISFeatureLayer) source);
        }
      }
    });
    mMapControlListener = new MyoMapControlListener(this, mMapView);
    hub.addListener(mMapControlListener);
    setContentView(mMapView);
    mWearMessageListener = new WearMessageListener(mMapView);
    initGoogleApiClient();
  }

  private void initGoogleApiClient() {
    mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
  }

  public void switchMyoListener() {
    Hub hub = Hub.getInstance();
    String message;
    if(mIsFeatureControl) {
      message = "Switching to map control";
      hub.removeListener(mFeatureControlListener);
      mMapControlListener.prepareSwitch();
      hub.addListener(mMapControlListener);
    } else {
      message = "Switching to feature control";
      hub.removeListener(mMapControlListener);
      mFeatureControlListener.prepareSwitch();
      hub.addListener(mFeatureControlListener);
    }
    mIsFeatureControl = !mIsFeatureControl;
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();

    // If Bluetooth is not enabled, request to turn it on.
    if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
      Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }
  }

  @Override
  protected void onStop() {
    if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      Wearable.MessageApi.removeListener(mGoogleApiClient, mWearMessageListener);
      mGoogleApiClient.disconnect();
    }
    // We don't want any callbacks when the Activity is gone, so unregister the listener.
    Hub.getInstance().removeListener(mFeatureControlListener);
    Hub.getInstance().removeListener(mMapControlListener);

    if (isFinishing()) {
      // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
      Hub.getInstance().shutdown();
    }
    super.onStop();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
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
    Log.d("test", "Connected to Google Api Service");
    Wearable.MessageApi.addListener(mGoogleApiClient, mWearMessageListener);
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e("test", "Failed to connect to Google Api Service");
  }
}
