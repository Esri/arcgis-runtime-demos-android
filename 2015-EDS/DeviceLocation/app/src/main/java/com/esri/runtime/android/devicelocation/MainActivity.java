package com.esri.runtime.android.devicelocation;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;

public class MainActivity extends AppCompatActivity {

  private MapView mMapView;
  private LocationDisplayManager mLocationDisplayManager;
  private FloatingActionButton mFab;

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
        if (mLocationDisplayManager != null) {
          if (mLocationDisplayManager.isStarted()) {
            mLocationDisplayManager.stop();
            mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this,
                R.color.colorAccentOff)));
          }
          else {
            mMapView.setRotationAngle(0);
            mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
            mLocationDisplayManager.start();
            mFab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(MainActivity.this, R
                .color.colorAccentOn)));
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
