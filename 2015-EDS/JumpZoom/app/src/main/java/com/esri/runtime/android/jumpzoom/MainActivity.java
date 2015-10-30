package com.esri.runtime.android.jumpzoom;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Envelope;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polygon;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.BasemapType;
import com.esri.arcgisruntime.mapping.Map;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.GeoView;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.mapping.view.NavigationCompletedEvent;
import com.esri.arcgisruntime.mapping.view.NavigationCompletedListener;
import com.esri.arcgisruntime.mapping.view.SpatialReferenceChangedEvent;
import com.esri.arcgisruntime.mapping.view.SpatialReferenceChangedListener;
import com.esri.arcgisruntime.mapping.view.VisibleAreaChangedEvent;
import com.esri.arcgisruntime.mapping.view.VisibleAreaChangedListener;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  private double mZoomScale = 5000;
  private MapView mMapView;
  //private Viewpoint mWorldViewpoint;
  private Viewpoint mWorldViewpoint = new Viewpoint(new Point(0, 0, SpatialReferences.getWebMercator()), 200000000);
      //new Envelope(-19000000, 10000000, 19000000, 10000000, SpatialReferences.getWebMercator()));

  private Viewpoint mViewpoint1 = new Viewpoint(new Point(1493528.253391, 6894813.853409, SpatialReferences.getWebMercator()), mZoomScale);
  //private Viewpoint mViewpoint2 = new Viewpoint(new Point(1485240.980716, 6875701.021195, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint2 = new Viewpoint(new Point(1489222.588445, 6893995.144173, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint3 = new Viewpoint(new Point(1196644.068456, 6033554.266798, SpatialReferences.getWebMercator()), mZoomScale);
  private Viewpoint mViewpoint4 = new Viewpoint(new Point(774593.577598, 6610915.197602, SpatialReferences.getWebMercator()), mZoomScale);

  private LogCenterAndScale navCompletedListener;

  private class LogCenterAndScale implements NavigationCompletedListener
  {
    @Override
    public void navigationCompleted(NavigationCompletedEvent navigationCompletedEvent) {
      if (navigationCompletedEvent != null) {
        GeoView source = navigationCompletedEvent.getSource();
        if (source instanceof MapView) {
          MapView mapView = (MapView) source;
          Point pt = mapView.getVisibleArea().getExtent().getCenter();
          Log.i(TAG, String.format("CenterPoint: X:%.6f, Y:%.6f", pt.getX(), pt.getY()));
          Log.i(TAG, "Current scale: " + mapView.getMapScale());
        }
      }

    }
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    mMapView = (MapView) findViewById(R.id.mapView);
    Map map = new Map(BasemapType.IMAGERY_WITH_LABELS, 0, 0, 1);
    mMapView.setMap(map);

    //Viewpoint mapInitialViewpoint = mMapView.getMap().getInitialViewpoint();

//    mMapView.addVisibleAreaChangedListener(new VisibleAreaChangedListener() {
//      @Override
//      public void visibleAreaChanged(VisibleAreaChangedEvent visibleAreaChangedEvent) {
//        //Point pt = mMapView.getVisibleArea().getExtent().getCenter();
//        //Log.i(TAG, String.format("Point: X:%.6f, Y:%.6f", pt.getX(), pt.getY()));
//        if (mWorldViewpoint == null) {
//          if ((mMapView.getSpatialReference() != null) && (mMapView.getVisibleArea() != null)) {
//            mWorldViewpoint = new Viewpoint(mMapView.getVisibleArea().getExtent());
//          }
//        }
//      }
//    });

//    mMapView.addNavigationCompletedListener(new NavigationCompletedListener() {
//      @Override
//      public void navigationCompleted(NavigationCompletedEvent navigationCompletedEvent) {
//        Point pt = mMapView.getVisibleArea().getExtent().getCenter();
//        Log.i(TAG, String.format("CenterPoint: X:%.6f, Y:%.6f", pt.getX(), pt.getY()));
//      }
//    });
    navCompletedListener = new LogCenterAndScale();
    mMapView.addNavigationCompletedListener(navCompletedListener);

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        //TODO - do we actually need a FAB?
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
      }
    });

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.setDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }


  @Override
  public void onBackPressed() {
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.main, menu);
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

  @SuppressWarnings("StatementWithEmptyBody")
  @Override
  public boolean onNavigationItemSelected(MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    Geometry selectedGeometry = null;
    Viewpoint selectedViewpoint = null;
    if (id == R.id.nav_world_location1) {
      selectedViewpoint = mViewpoint1;
    }
    else if (id == R.id.nav_world_location2) {
      selectedViewpoint = mViewpoint2;
    }
    else if (id == R.id.nav_world_location3) {
      selectedViewpoint = mViewpoint3;
    }
    else { //if (id == R.id.nav_world_location4) {
      selectedViewpoint = mViewpoint4;
    }


//    else if (id == R.id.nav_union_location1) {
//    }
//    else if (id == R.id.nav_union_location2) {
//    }
//    else if (id == R.id.nav_union_location3) {
//    }
//    else if (id == R.id.nav_union_location4) {
//    }

    // If new target already inside the current extent, then zoom directly to it.
    if (GeometryEngine.intersects(mMapView.getVisibleArea(), selectedViewpoint.getTargetGeometry())) {
      mMapView.setViewpointWithDurationAsync(selectedViewpoint, 3);
    }
    else {
      // If target is outside of current extent, zoom out first to see both extents, then zoom back in.
      Geometry union = GeometryEngine.union(mMapView.getVisibleArea().getExtent().getCenter(), selectedViewpoint.getTargetGeometry());
      if ((union != null) && (!union.isEmpty())) {
        Log.i(TAG, "Union GeometryType:" + union.getGeometryType().name());
        Viewpoint unionViewpoint = new Viewpoint(union.getExtent());
        jumpZoom(unionViewpoint, selectedViewpoint);
      }
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  private void jumpZoom(Viewpoint firstViewpoint, final Viewpoint secondViewpoint) {
    //Log.i(TAG, "Calling first setViewpoint");
    final ListenableFuture<Boolean> booleanListenableFuture = mMapView.setViewpointWithDurationAsync(firstViewpoint, 3);
    booleanListenableFuture.addDoneListener(new Runnable() {
      @Override
      public void run() {
        //Log.i(TAG, "In AddDoneListener Run");
        try {
          if (booleanListenableFuture.get()) {
            //Log.i(TAG, "AddDoneListener Run get=true");
            // First navigation is complete.
            mMapView.setViewpointWithDurationAsync(secondViewpoint, 3);
          }
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
    });
  }

  @Override
  protected void onPause() {
    super.onPause();
    mMapView.pause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mMapView.resume();
  }
}
