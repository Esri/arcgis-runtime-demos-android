package com.esri.arcgisruntime.demo.geocodeandroute;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SearchView;
import android.widget.Spinner;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class GeocodeRouteActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {


  private final String extern = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();

  // Permissions
  private final int requestCode = 2;
  private final String[] permission = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

  // UI
  private Spinner mSpinner;
  private SearchView mSearchview;
  private MapView mMapView;

  // Map
  private ArcGISMap mMap;
  private GraphicsOverlay graphicsOverlay = new GraphicsOverlay();
  private MobileMapPackage mMobileMapPackage;

  // Geocoding
  private LocatorTask mLocatorTask = null;
  private GeocodeParameters mGeocodeParameters;
  private Graphic mFromAddressResult;
  private Graphic mToAddressResult;
  private PictureMarkerSymbol mFromAddressSymbol;
  private PictureMarkerSymbol mToAddressSymbol;
  private GeocodeResult mGeocodedLocation;

  // Routing
  private RouteTask mRouteTask = null;
  private final SimpleLineSymbol mSolvedRouteSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, Color.GREEN, 4.0f);
  private Graphic mRouteGraphic;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_geocode_route);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // inflate MapView from layout
    mMapView = (MapView) findViewById(R.id.mapView);
    mMapView.getGraphicsOverlays().add(graphicsOverlay);

    // Check permissions required for this app.
    boolean permissionCheck = ContextCompat.checkSelfPermission(GeocodeRouteActivity.this, permission[0]) ==
        PackageManager.PERMISSION_GRANTED;

    if (!permissionCheck) {
      // If permissions are not already granted, request permission from the user.
      ActivityCompat.requestPermissions(GeocodeRouteActivity.this, permission, requestCode);
    } else {
      // if permission was already granted, set up offline map, geocoding and routing functionality
      setupOfflineMap();
    }

    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        solveRoute();
      }
    });

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
        this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
    drawer.addDrawerListener(toggle);
    toggle.syncState();

    NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
    navigationView.setNavigationItemSelectedListener(this);
  }

  // Map functions

  /**
   * Sets up a map by opening a mobile map package, getting the first map, adding a tile package as a basemap layer, and
   * and setting the map into the MapView in the layout. Then calls functions to set up geocoding and routing tasks
   * using same mmpk.
   */
  private void setupOfflineMap() {
    // Create an MMPK from the specified file.
    final String mmpkPath = getMmpkPath();
    if (TextUtils.isEmpty(mmpkPath)) {
      return;
    }
    mMobileMapPackage = new MobileMapPackage(mmpkPath);

    // Load MMPK - When MMPK is loaded the Maps and other content should be accessible.
    mMobileMapPackage.addDoneLoadingListener(new Runnable() {

      @Override
      public void run() {

        if (mMobileMapPackage.getLoadStatus() != LoadStatus.LOADED) {
          Snackbar.make(mMapView, String.format(getString(R.string.object_not_loaded), "MMPK"),
              Snackbar.LENGTH_SHORT).show();
          return;
        }

        // Get the first map.
        if (mMobileMapPackage.getMaps().size() == 0) {
          Snackbar.make(mMapView, String.format(getString(R.string.no_maps_in_mmpk), mmpkPath),
              Snackbar.LENGTH_SHORT).show();
          return;
        }

        mMap = mMobileMapPackage.getMaps().get(0);

        // Set a Basemap from a raster tile cache package
        String tpkPath = getTpkPath();
        if (TextUtils.isEmpty(tpkPath)) {
          return;
        }
        TileCache tileCache = new TileCache(getTpkPath());
        final ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tileCache);
        mMap.setBasemap(new Basemap(tiledLayer));

        // No need to explicitly load the map, just set it into the MapView; that will trigger loading when displayed.
        mMapView.setMap(mMap);
        mMap.addDoneLoadingListener(new Runnable() {
          @Override
          public void run() {

            if (mMobileMapPackage.getLoadStatus() != LoadStatus.LOADED) {
              Snackbar.make(mMapView, String.format(getString(R.string.object_not_loaded), "Map"),
                  Snackbar.LENGTH_SHORT).show();
              return;
            }

            mMapView.setViewpointGeometryAsync(tiledLayer.getFullExtent());

            setUpOfflineMapGeocoding();
            setupOfflineNetwork();
            setupSearchView();

            mMap.removeDoneLoadingListener(this);
          }
        });
      }
    });

    // Load the MMPK.
    mMobileMapPackage.loadAsync();
  }

  /**
   * Gets the local path of the tile cache file to be used for a basemap. Shows message to user if file is not found.
   * @return local absolute path to the .tpk file, or null if the file cannot be found.
   */
  private String getTpkPath() {
    // NOTE: Update this path value to suit your own datasets.
    File tpkFile = new File(extern, getString(R.string.tpk_path));
    if (!tpkFile.exists()) {
      Snackbar.make(mMapView, String.format(getString(R.string.file_not_found), tpkFile.getAbsolutePath()),
          Snackbar.LENGTH_SHORT).show();
      return null;
    }
    else {
      return tpkFile.getAbsolutePath();
    }
  }

  /**
   * Gets the local path of the mobile map package containing operational data, locator, and network dataset. Shows
   * message to user if file is not found.
   * @return local absolute path to the .mmpk file, or null if the file cannot be found.
   */
  private String getMmpkPath() {
    // NOTE: Update this path value to suit your own datasets.
    File mmpkFile = new File(extern, getString(R.string.mmpk_path));
    if (!mmpkFile.exists()) {
      Snackbar.make(mMapView, String.format(getString(R.string.file_not_found), mmpkFile.getAbsolutePath()),
          Snackbar.LENGTH_SHORT).show();
      return null;
    } else {
      return mmpkFile.getAbsolutePath();
    }
  }


  // Geocoding functions

  /**
   * Sets up a LocatorTask from the loaded mobile map package, and graphics to show results from running the task.
   * Shows a message to user if Locator task is not found.
   */
  private void setUpOfflineMapGeocoding() {

    if (mMobileMapPackage.getLocatorTask() == null) {
      Snackbar.make(mMapView, "Current mobile map package has no LocatorTask", Snackbar.LENGTH_SHORT).show();
      return;
    }

    // Get LocatorTask from loaded MobileMapPackage and listen for loading events
    mLocatorTask = mMobileMapPackage.getLocatorTask();
    mLocatorTask.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mLocatorTask.getLoadStatus() != LoadStatus.LOADED) {
          Snackbar.make(mMapView, String.format(getString(R.string.object_not_loaded), "LocatorTask"),
              Snackbar.LENGTH_SHORT).show();
        }
      }
    });
    mLocatorTask.loadAsync();

    // Add a graphics overlay that will be used for geocoding results
    graphicsOverlay = new GraphicsOverlay();
    graphicsOverlay.setSelectionColor(0xFF00FFFF);
    mMapView.getGraphicsOverlays().add(graphicsOverlay);

    // Define the parameters that will be used by the locator task
    mGeocodeParameters = new GeocodeParameters();
    mGeocodeParameters.getResultAttributeNames().add("*");
    mGeocodeParameters.setMaxResults(10); //1);
    mGeocodeParameters.setOutputSpatialReference(mMapView.getSpatialReference());

    //Create picture marker symbols from app resources for geocode results
    BitmapDrawable addressDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.pin_blank_orange);
    mFromAddressSymbol = new PictureMarkerSymbol(addressDrawable);
    mFromAddressSymbol.setHeight(64);
    mFromAddressSymbol.setWidth(64);
    mFromAddressSymbol.loadAsync();
    mFromAddressSymbol.setLeaderOffsetY(32);
    mFromAddressSymbol.setOffsetY(32);

    BitmapDrawable hydrantDrawable = (BitmapDrawable) ContextCompat.getDrawable(this, R.drawable.pin_circle_blue_d);
    mToAddressSymbol = new PictureMarkerSymbol(hydrantDrawable);
    mToAddressSymbol.setHeight(64);
    mToAddressSymbol.setWidth(64);
    mToAddressSymbol.loadAsync();
    mToAddressSymbol.setLeaderOffsetY(32);
    mToAddressSymbol.setOffsetY(32);
  }

  /**
   * Sets up a search view displayed over the MapView, where geocode queries can be entered, or selected from a spinner.
   */
  private void setupSearchView() {
    mSearchview = (SearchView) findViewById(R.id.searchView1);
    mSearchview.setIconifiedByDefault(true);
    mSearchview.setQueryHint(getResources().getString(R.string.search_hint));
    mSearchview.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        hideKeyboard();
        geocodeAddress(query);
        mSearchview.clearFocus();
        return true;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });

    mSpinner = (Spinner) findViewById(R.id.spinner);

    // Create an ArrayAdapter using the string array and a default spinner layout
    final ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_dropdown_item) {
      @NonNull
      @Override
      public View getView(int position, View convertView, @NonNull ViewGroup parent) {

        View v = super.getView(position, convertView, parent);
        if (position == getCount()) {
          mSearchview.clearFocus();
        }

        return v;
      }

      @Override
      public int getCount() {
        return super.getCount() - 1; // dont display last item. It is used as hint.
      }

    };

    // Specify the layout to use when the list of choices appears
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    adapter.addAll(getResources().getStringArray(R.array.suggestion_items));

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      // set vertical offset to spinner dropdown for API less than 21
      mSpinner.setDropDownVerticalOffset(80);
    }

    // Apply the adapter to the spinner
    mSpinner.setAdapter(adapter);
    mSpinner.setSelection(adapter.getCount());

    mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == adapter.getCount()) {
          mSearchview.clearFocus();
        } else {
          hideKeyboard();
          mSearchview.setQuery(getResources().getStringArray(R.array.suggestion_items)[position], false);
          geocodeAddress(getResources().getStringArray(R.array.suggestion_items)[position]);
          mSearchview.setIconified(false);
          mSearchview.clearFocus();
        }
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {
      }
    });

  }

  /**
   * Geocodes the given address/location string using the previously set up LocatorTask.
   * @param address address or hydrant number to geocode
   */
  private void geocodeAddress(final String address) {
    // Null out any previously located result
    mGeocodedLocation = null;

    if (mLocatorTask == null) {
      Snackbar.make(mMapView, getString(R.string.locator_task_not_set), Snackbar.LENGTH_SHORT).show();
      return;
    }
    if ((mFromAddressResult != null) && (mToAddressResult != null)) {
      Snackbar.make(mMapView, getString(R.string.clear_results), Snackbar.LENGTH_SHORT).show();
      return;
    }

    // Call geocodeAsync on LocatorTask, passing in an address
    final ListenableFuture<List<GeocodeResult>> geocodeFuture = mLocatorTask.geocodeAsync(address, mGeocodeParameters);
    geocodeFuture.addDoneListener(new Runnable() {

      @Override
      public void run() {
        try {
          // Get the results of the async operation
          List<GeocodeResult> geocodeResults = geocodeFuture.get();
          if (geocodeResults.size() > 0) {
            // Get the top geocoded location from the result and use it.
            mGeocodedLocation = geocodeResults.get(0);
            displayGeocodeResult();
          }
          else
          {
            Snackbar.make(mMapView, String.format(getString(R.string.location_not_found), address),
                Snackbar.LENGTH_LONG).show();
          }
        }
        catch (InterruptedException | ExecutionException e) {
          // Deal with exception...
          e.printStackTrace();
          Snackbar.make(mMapView, getString(R.string.geo_locate_error), Snackbar.LENGTH_LONG).show();
        }

        // Done processing and can remove this listener.
        geocodeFuture.removeDoneListener(this);
      }
    });
  }

  /**
   * Displays the given geocode result, selecting appropriate symbol based on order of geocoded results found.
   */
  private void displayGeocodeResult() {

    if (mMapView.getCallout().isShowing()) {
      mMapView.getCallout().dismiss();
    }
    if (graphicsOverlay == null) return;

    // Create graphic object for resulting location, depending on which type of location has been found.
    if (mFromAddressResult == null) {
      mFromAddressResult = new Graphic(mGeocodedLocation.getDisplayLocation(), mFromAddressSymbol);
      graphicsOverlay.getGraphics().add(mFromAddressResult);
    }
    else if (mToAddressResult == null) {
      mToAddressResult = new Graphic(mGeocodedLocation.getDisplayLocation(), mToAddressSymbol);
      graphicsOverlay.getGraphics().add(mToAddressResult);
    }

    // Zoom map to geocode result location
    mMapView.setViewpointAsync(new Viewpoint(mGeocodedLocation.getDisplayLocation(), 8000), 3);
  }


  // Routing functions

  /**
   * Sets up a RouteTask from the NetworkDatasets in the current map. Shows a message to user if network dataset is
   * not found.
   */
  private void setupOfflineNetwork() {

    if ((mMap.getTransportationNetworks() == null) || (mMap.getTransportationNetworks().size() < 1)) {
      Snackbar.make(mMapView, getString(R.string.network_dataset_not_found), Snackbar.LENGTH_SHORT).show();
      return;
    }

    // Create the RouteTask from network data set using same map used in display
    mRouteTask = new RouteTask(GeocodeRouteActivity.this, mMap.getTransportationNetworks().get(0));
    mRouteTask.addDoneLoadingListener(new Runnable() {
      @Override
      public void run() {
        if (mRouteTask.getLoadStatus() != LoadStatus.LOADED) {
          Snackbar.make(mMapView, String.format(getString(R.string.object_not_loaded), "RouteTask"),
              Snackbar.LENGTH_SHORT).show();
        }
      }
    });
    mRouteTask.loadAsync();
  }

  /**
   * Solves a route using the existing geocoded address and hydrant locations, and displays a graphic of the route, and
   * message with distance and time. Shows messages to user if locations are not set.
   */
  private void solveRoute() {

    if (mFromAddressResult == null) {
      Snackbar.make(mMapView, getString(R.string.address_from_stop_not_set), Snackbar.LENGTH_SHORT).show();
      return;
    }
    if (mToAddressResult == null) {
      Snackbar.make(mMapView, getString(R.string.address_to_stop_not_set), Snackbar.LENGTH_SHORT).show();
      return;
    }

    if (mRouteTask == null ) {
      Snackbar.make(mMapView, getString(R.string.route_task_not_set), Snackbar.LENGTH_SHORT).show();
      return;
    }

    RouteParameters routeParams;
    try {
      routeParams = mRouteTask.createDefaultParametersAsync().get();

      Stop start = new Stop((Point)mFromAddressResult.getGeometry());
      start.setRouteName(getString(R.string.route_name));
      start.setName(getString(R.string.stop1_name));
      routeParams.getStops().add(start);

      Stop finish = new Stop((Point)mToAddressResult.getGeometry());
      finish.setRouteName(getString(R.string.route_name));
      finish.setName(getString(R.string.stop2_name));
      routeParams.getStops().add(finish);

      final ListenableFuture<RouteResult> routeFuture = mRouteTask.solveRouteAsync(routeParams);
      routeFuture.addDoneListener(new Runnable() {
        @Override
        public void run() {
          // Show results of solved route.
          RouteResult routeResult;
          try {
            routeResult = routeFuture.get();
            if (routeResult.getRoutes().size() > 0) {
              // Add first result to the map as a graphic.
              Route topRoute = routeResult.getRoutes().get(0);
              mRouteGraphic = new Graphic(topRoute.getRouteGeometry(), mSolvedRouteSymbol);
              mRouteGraphic.getAttributes().put("Name", topRoute.getRouteName());
              mRouteGraphic.setSelected(true);
              mRouteGraphic.setZIndex(-1);
              graphicsOverlay.getGraphics().add(mRouteGraphic);

              // Display route distance and time.
              Snackbar.make(mMapView, String.format(getString(R.string.route_result_info),
                  topRoute.getTotalLength(), topRoute.getTotalTime()), Snackbar.LENGTH_SHORT).show();

              mMapView.setViewpointGeometryAsync(topRoute.getRouteGeometry());

            }
          } catch (InterruptedException | ExecutionException e) {
            Snackbar.make(mMapView, String.format(getString(R.string.route_error), e.getMessage()),
                Snackbar.LENGTH_SHORT).show();
          }
        }
      });
    } catch (InterruptedException | ExecutionException e) {
      Snackbar.make(mMapView, String.format(getString(R.string.route_params_error), e.getMessage()),
          Snackbar.LENGTH_SHORT).show();
    }
  }


  // Activity members and UI related functions

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // If request is cancelled, the result arrays are empty.
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // Permission was granted; this would have been triggered in response to finding permissions were not granted, so
      // now try setting up the map based on locally stored files.
      setupOfflineMap();
    }
    else {
      // If permission was denied, show message to inform user what was chosen. Map will not be set, so functionality will
      // not work.
      Snackbar.make(mMapView, getResources().getString(R.string.storage_permission_denied), Snackbar.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onBackPressed() {
    // If back button is pressed while in activity and the navigation drawer is open, then close the navigation drawer.
    // Otherwise, delegate to the activity.
    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    if (drawer.isDrawerOpen(GravityCompat.START)) {
      drawer.closeDrawer(GravityCompat.START);
    } else {
      super.onBackPressed();
    }
  }
  
  @Override
  public boolean onNavigationItemSelected(@NonNull MenuItem item) {
    // Handle navigation view item clicks here.
    int id = item.getItemId();

    if (id == R.id.nav_clear) {
      // clear graphics
      graphicsOverlay.getGraphics().clear();
      mFromAddressResult = null;
      mRouteGraphic = null;
      mToAddressResult = null;
    }

    DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
    drawer.closeDrawer(GravityCompat.START);
    return true;
  }

  /**
   *
   */
  private void hideKeyboard() {
    mSearchview.clearFocus();
    InputMethodManager inputManager = (InputMethodManager) getApplicationContext()
        .getSystemService(Context.INPUT_METHOD_SERVICE);
    inputManager.hideSoftInputFromWindow(mSearchview.getWindowToken(), 0);
  }

}
