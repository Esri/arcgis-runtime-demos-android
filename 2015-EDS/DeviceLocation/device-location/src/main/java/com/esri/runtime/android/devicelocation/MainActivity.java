package com.esri.runtime.android.devicelocation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;


public class MainActivity extends AppCompatActivity {

  private MapView mMapView;
  private LocationDisplayManager mLocationDisplayManager;
  private FloatingActionButton mFab;
  private int requestCode = 2;
  String[] reqPermissions = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mMapView = (MapView) findViewById(R.id.mapView);
    mLocationDisplayManager = mMapView.getLocationDisplayManager();

    mFab = (FloatingActionButton) findViewById(R.id.fab);
    mFab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        // If locationManager already started, then stop it.
        if (mLocationDisplayManager.isStarted()) {
          mLocationDisplayManager.stop();
          mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.colorAccentOff)));
        } else {
          // If not started, check permissions before starting.
          if ((ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[0]) == PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(MainActivity.this, reqPermissions[1]) == PackageManager.PERMISSION_GRANTED)) {
            startLocationDisplay();
          } else {
            // Ask for permissions - in this case, do not show rationale.
            ActivityCompat.requestPermissions(MainActivity.this, reqPermissions, requestCode);
          }
        }
      }
    });

    //    // Can listen for location updates if required.
    //    mLocationDisplayManager.setLocationListener(new LocationListener() {
    //      @Override
    //      public void onLocationChanged(Location location) {
    //        if (location != null) {
    //          if (location.hasAccuracy()) {
    //            // Do something.
    //          }
    //        }
    //      }
    //
    //      @Override
    //      public void onStatusChanged(String s, int i, Bundle bundle) {
    //
    //      }
    //
    //      @Override
    //      public void onProviderEnabled(String s) {
    //
    //      }
    //
    //      @Override
    //      public void onProviderDisabled(String s) {
    //
    //      }
    //    });
  }

  private void startLocationDisplay() {
    mMapView.setRotationAngle(0);
    mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
    mLocationDisplayManager.start();
    mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R.color.colorAccentOn)));
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // If request is cancelled, the result arrays are empty.
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // permission was granted - can start the LocationDisplayManager safely now.
      startLocationDisplay();
    }
    else {
      // If permission was denied, user will see the request permission UX again, and will be able to indicate request
      // should never be shown again. Alternative would be to disable functionality so request is not shown again.
      Toast.makeText(MainActivity.this, getResources().getString(R.string.location_permission_denied),
          Toast.LENGTH_SHORT).show();
    }

    return;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    int id = item.getItemId();

    if (id == R.id.autopan_location) {
      mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
      mMapView.setRotationAngle(0);
      return true;
    }
    else if (id == R.id.autopan_compass) {
      mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.COMPASS);
      return true;
    }
    else if (id == R.id.autopan_navigation) {
      mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onPause() {
    super.onPause();
    mMapView.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mMapView.unpause();
  }
}
