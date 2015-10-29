package com.esri.runtime.android.devicelocation;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

//TODO - change theme accent color

public class MainActivity extends AppCompatActivity {

  private MapView mMapView;
  private LocationDisplayManager mLocationDisplayManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mMapView = (MapView) findViewById(R.id.mapView);

    mLocationDisplayManager = mMapView.getLocationDisplayManager();
    mLocationDisplayManager.setLocationListener(new LocationListener() {

      boolean locationChanged = false;

      // Zooms to the current location when first GPS fix arrives.
      @Override
      public void onLocationChanged(Location loc) {
        if (!locationChanged) {
          locationChanged = true;
          mLocationDisplayManager.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
        }
      }

      @Override
      public void onProviderDisabled(String arg0) {
      }

      @Override
      public void onProviderEnabled(String arg0) {
      }

      @Override
      public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
      }
    });

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        //TODO - replace with starting LocationDisplayManager
        if (mLocationDisplayManager != null) {
          if (mLocationDisplayManager.isStarted()) {
            mLocationDisplayManager.stop();
          }
          else {
            mLocationDisplayManager.start();
          }

        }
        //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
      }
    });
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

    //TODO - could replace with pop-up FAB
    if (id == R.id.autopan_location) {
      mLocationDisplayManager.setAutopanMode(LocationDisplayManager.AutoPanMode.LOCATION);
      return true;
    }
    else if (id == R.id.autopan_compass) {
      mLocationDisplayManager.setAutopanMode(LocationDisplayManager.AutoPanMode.COMPASS);
      return true;
    }
    else if (id == R.id.autopan_navigation) {
      mLocationDisplayManager.setAutopanMode(LocationDisplayManager.AutoPanMode.NAVIGATION);
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
