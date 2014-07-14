package com.esri.arcgis.sample;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.SearchView.OnSuggestionListener;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.arcgis.sample.DirectionsComponent.DirectionsCallback;
import com.esri.arcgis.sample.FileFragment.DataType;
import com.esri.arcgis.sample.GeocodeComponent.GeocodeCallback;
import com.esri.arcgis.sample.GeocodeComponent.GeocodeSuggestionAdapter;
import com.esri.arcgis.sample.RouteComponent.RouteCallback;
import com.esri.arcgis.sample.ServiceFragment.ServiceCallback;
import com.esri.arcgis.sample.ServiceFragment.ServiceType;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;
import com.esri.core.tasks.na.RouteDirection;

public class MapFragment extends Fragment {

  public static final String TAG = MapFragment.class.getSimpleName();
  
  private MapView mMapView;
  
  private MapTouchComponent mMapTouchComponent;
  
  private BasemapComponent mBasemapComponent;
  
  private RouteComponent mRouteComponent;
  
  private GeocodeComponent mGeocodeComponent;
  
  private GraphicComponent mGraphicComponent;
  
  private ProgressBar mMapProgressBar;
  
  private SearchView mSearchView;
  
  private View mGeocodeCallout;
  
  private TextView mInfoView;
  
  private DirectionsComponent mDirectionsComponent;
  
  private static final String BASEMAP_PATH_KEY = "basemap_path";
  
  private static final String BASEMAP_TYPE_KEY = "basemap_type";
  
  public MapFragment() {
    
  }
  
  public static MapFragment newInstance() {
    MapFragment fragment = new MapFragment();
    return fragment;
  }
  
  public static void requestNewInstance(Context context, String newBasemapPath, int dataType) {
    
    MapFragment fragment = new MapFragment();
    Bundle args = new Bundle();
    args.putString(BASEMAP_PATH_KEY, newBasemapPath);
    args.putInt(BASEMAP_TYPE_KEY, dataType);
    fragment.setArguments(args);
    
    FragmentManager fm = ((Activity) context).getFragmentManager();
    fm.beginTransaction()
      .replace(R.id.container, fragment, TAG)
      .commit();
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
  }
  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    
    if (mDirectionsComponent != null && mDirectionsComponent.isDrawerOpen())
      return;
    
    inflater.inflate(R.menu.main, menu);    
    mSearchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
    mSearchView.setOnQueryTextListener(new OnQueryTextListener() {
      
      @Override
      public boolean onQueryTextSubmit(String query) {
        
        if (query == null || query.isEmpty())
          return false;
        
        mGeocodeComponent.submitForwardGeocode(query, mMapView.getSpatialReference());
        return true;
      }
      
      @Override
      public boolean onQueryTextChange(String newText) {
        
        if(newText == null || newText.isEmpty())
          return false;
        
        mGeocodeComponent.submitForwardGeocode(newText, mMapView.getSpatialReference());
        return true;
      }
    });
    
    mSearchView.setOnSuggestionListener(new OnSuggestionListener() {
      
      @Override
      public boolean onSuggestionSelect(int position) {
        return false;
      }
      
      /**
       * When clicking a geocoding suggestions, we simply show 
       * the callout at that location.
       */
      @Override
      public boolean onSuggestionClick(int position) {
        Cursor cursor = (Cursor) mSearchView.getSuggestionsAdapter().getItem(position);
        Point point = GeocodeSuggestionAdapter.getPointFromCursor(cursor, mMapView.getSpatialReference());
        String address = GeocodeSuggestionAdapter.getAddressFromCursor(cursor);
        mMapView.setExtent(point, 0, true);
        showCalloutWithText(address, point);
        return false;
      }
    });
  }
  
  /**
   * On pause, pause the map and unregister the routing component
   * as a listener for preference changes.
   */
  @Override
  public void onPause() {
    PreferenceManager.getDefaultSharedPreferences(getActivity()).unregisterOnSharedPreferenceChangeListener(mRouteComponent);
    mMapView.pause();
    super.onPause();
  }
  
  /**
   * On resume, unpause the map and register the routing component 
   * as a listener for preference changes.
   */
  @Override
  public void onResume() {
    PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(mRouteComponent);
    mMapView.unpause();
    super.onResume();
  }
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    
    // Inflate the layout.
    View rootView = inflater.inflate(R.layout.fragment_main, container, false);
    
    // Inflate the callout.
    mGeocodeCallout = inflater.inflate(R.layout.geocode_callout, container, false);
    mGeocodeCallout.setOnClickListener(new OnClickListener() {
      
      @Override
      public void onClick(View v) {
        mMapView.getCallout().hide();        
      }
    });
    
    // Find the map.
    mMapView = (MapView) rootView.findViewById(R.id.map_view);
    mMapView.setAllowRotationByPinch(true);
    mMapProgressBar = (ProgressBar) rootView.findViewById(R.id.map_progress_bar);        
    mInfoView = (TextView) rootView.findViewById(R.id.map_info);
    
    //////////////////////////////////
    // Initialize components.
    //////////////////////////////////
    
    // Basemap.
    mBasemapComponent = new BasemapComponent(mMapView);
    
    // In the event that the MapFragment was reloaded with a specific basemap
    // request (change in spatial reference), we load it appropriately here.
    if (getArguments() != null) {
    
      String dataPath = getArguments().getString(BASEMAP_PATH_KEY, null);
      int dataType = getArguments().getInt(BASEMAP_TYPE_KEY, -1);
      
      if (dataType != -1 && dataPath != null) {
        
        BasemapComponent.DataType enumValue = BasemapComponent.DataType.values()[dataType];      
      
        if (enumValue == BasemapComponent.DataType.GEODATABASE)
          mBasemapComponent.loadGeodatabaseLayer(dataPath, true);
        else if (enumValue == BasemapComponent.DataType.LOCAL_TILED_LAYER)
          mBasemapComponent.loadLocalTileLayer(dataPath);
        else if (enumValue == BasemapComponent.DataType.TILED_SERVICE_LAYER)
          mBasemapComponent.loadOnlineLayer(dataPath);
      }

    } else {
      
      mBasemapComponent.loadDefaultCanvas();
    }
    
    // Route.
    mRouteComponent = new RouteComponent()
                      .bindCallback(mRouteCallback)
                      .bindCallback(mProgressCallback);
    
    // Directions.
    mDirectionsComponent = (DirectionsComponent) getFragmentManager().findFragmentById(R.id.navigation_drawer);
    mDirectionsComponent.initialize()
                        .bindCallback(mDirectionsCallback);
    
    // Geocode.
    mGeocodeComponent = new GeocodeComponent()
                        .bindCallback(mProgressCallback)
                        .bindCallback(mGeocodeCallback);
    
    // Graphics.
    mGraphicComponent = new GraphicComponent(mMapView);
    
    // Touch.
    mMapTouchComponent = new MapTouchComponent(getActivity(), mMapView)
                             .bindComponent(mGraphicComponent)
                             .bindComponent(mRouteComponent)
                             .bindComponent(mGeocodeComponent);
    
    // Attach our status listener.
    mMapView.setOnStatusChangedListener(mMapInitListener);
    mMapView.setOnTouchListener(mMapTouchComponent);
    
    return rootView;
  }
  
  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    
    
    switch (item.getItemId()) {
    
    case R.id.action_load_network:
      
      // When a request to load a transportation network is made,
      // re-initialize the Route component.
      FileFragment.newInstance(DataType.TRANSPORTAION_NETWORK)
        .bindCallback(new FileFragment.FileCallback() {
          
          @Override
          public void onFileSelected(String absolutePath) {
            mRouteComponent.initialize(absolutePath, null);            
          }
        }).show(getFragmentManager(), null);
      return true;
    
    case R.id.action_load_locator:
      
      // When a request to load a locator is made re-initialize the Geocode component.
      FileFragment.newInstance(DataType.LOCATOR)
        .bindCallback(new FileFragment.FileCallback() {
          
          @Override
          public void onFileSelected(String absolutePath) {
            mGeocodeComponent.initialize(absolutePath, null);            
          }
        }).show(getFragmentManager(), null);
      return true;
      
    case R.id.action_load_geodatabase:
      
      // When a request to load a locator is made re-initialize the Basemap component.
      FileFragment.newInstance(DataType.GEODATABASE)
        .bindCallback(new FileFragment.FileCallback() {
          
          @Override
          public void onFileSelected(String absolutePath) {
            mBasemapComponent.loadGeodatabaseLayer(absolutePath, true);            
          }
        }).show(getFragmentManager(), null);
      return true;
      
    case R.id.action_load_local_tiled_layer:
      
      // When a request to load a locator is made re-initialize the Geocode component.
      FileFragment.newInstance(DataType.LOCAL_TILED_LAYER)
        .bindCallback(new FileFragment.FileCallback() {
          
          @Override
          public void onFileSelected(String absolutePath) {
            mBasemapComponent.loadLocalTileLayer(absolutePath);          
          }
        }).show(getFragmentManager(), null);
      return true;
      
    case R.id.action_clear:
      mGraphicComponent.removeAll();
      postDirectionsChange(new ArrayList<RouteDirection>());
      postShowInfo("", false);
      return true;
      
    case R.id.action_add_polygon_barrier:
      mMapTouchComponent.startDrawingPolygon();
      return true;
      
    case R.id.action_add_polyline_barrier:
      mMapTouchComponent.startDrawingPolyline();
      return true;
      
    case R.id.action_settings: 
      
      Bundle args = SettingsComponent.createBundle(mRouteComponent.getNetworkDescription(), mRouteComponent.getRouteParameters());
      SettingsComponent settings = SettingsComponent.newInstance(args);
      getFragmentManager().beginTransaction().add(R.id.container, settings).addToBackStack(null).commit();
      return true;
      
    case R.id.action_load_service:
      
      ServiceFragment.newInstance()
                     .bindCallback(new ServiceCallback() {
                      
                      @Override
                      public void onServiceSelected(ServiceType type, String url, String user,
                          String password) {
                        
                        UserCredentials credentials = null;
                        if (!user.isEmpty()) {
                          credentials = new UserCredentials();
                          credentials.setUserAccount(user, password);
                        }
                        
                        if (type == ServiceType.GEOCODING)
                          mGeocodeComponent.initialize(url, credentials);
                        else if (type == ServiceType.ROUTING)
                          mRouteComponent.initialize(url, credentials);
                        else if (type == ServiceType.MAP_SERVER)
                          mBasemapComponent.loadOnlineLayer(url);
                      }
                    }).show(getFragmentManager(), null);
      return true;
      
    case R.id.action_navigate:
      
      final String sender = "Runtime Sandbox";
      final String URI_FORMAT = "google.navigation:q=%f,%f&current=%f,%f&sender=%s";
      final SpatialReference WGS84 = SpatialReference.create(4326);
      Graphic[] stops = mGraphicComponent.getStops();
      
      if (stops != null && stops.length == 2) {      
      
        Point current = (Point) GeometryEngine.project(stops[0].getGeometry(), mMapView.getSpatialReference(), WGS84);
        Point destination = (Point) GeometryEngine.project(stops[1].getGeometry(), mMapView.getSpatialReference(), WGS84);
        
        String uri = String.format(Locale.ENGLISH, URI_FORMAT,  destination.getY(), destination.getX(), current.getY(), current.getX(), sender);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        startActivity(intent);
      }
      return true;
      
    default:
      break;
    }
    
    return super.onOptionsItemSelected(item);    
  }
  
  ProgressCallback mProgressCallback = new ProgressCallback() {
    
    @Override
    public void toggleIndeterminateProgress(final boolean show) {
      mMapProgressBar.post(new Runnable() {
        
        @Override
        public void run() {
          mMapProgressBar.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
          mMapProgressBar.bringToFront();
          mMapProgressBar.invalidate();
        }
      });      
    }
  };
  
  DirectionsCallback mDirectionsCallback = new DirectionsCallback() {

    @Override
    public void onDirectionSelected(RouteDirection direction) {
      
      postExtentChange(direction.getGeometry());      
    }    
  };
  
  RouteCallback mRouteCallback = new RouteCallback() {
    
    @Override
    public void onSolveFailed(Exception e) {
      logError(e, true);      
    }
    
    @Override
    public void onRouteShapeReady(Geometry routeShape) {
      
      if (mGraphicComponent != null) {
        mGraphicComponent.updateTrackedRoute(routeShape);
        
        if (mMapTouchComponent != null && !mMapTouchComponent.isDragging())
          postExtentChange(routeShape);
      }
    }
    
    @Override
    public void onInitializationError(Exception e) {
      logError(e, true);      
    }

    @Override
    public void onDirectionsReady(List<RouteDirection> directions) {      
      postDirectionsChange(directions);      
    }

    @Override
    public void onTotalsChanged(double totalMiles, double totalMinutes) {
      postShowInfo(String.format(Locale.ENGLISH, "Total Minutes: %.1f\nTotal Miles: %.1f", totalMinutes, totalMiles), true);
    }
  };
  
  private void postShowInfo(final String message, final boolean show) {
    
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        mInfoView.setText(message);
        mInfoView.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
      }
    });    
  }
  
  private void postDirectionsChange(final List<RouteDirection> directions) {
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        mDirectionsComponent.setDirections(directions);        
      }
    });
  }
  
  private void postExtentChange(final Geometry newExtent) {
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        mMapView.setExtent(newExtent, 0, true);        
      }
    });
  }
  
  private void showCalloutWithText(final String text, final Point location) {
    
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        
        TextView geocodeText = (TextView) mGeocodeCallout.findViewById(R.id.geocode_callout_main_text);
        geocodeText.setText(text);
        
        View addStopButton = mGeocodeCallout.findViewById(R.id.geocode_callout_add_stop);
        addStopButton.setOnClickListener(new OnClickListener() {
          
          @Override
          public void onClick(View v) {              
            if (mGraphicComponent != null) {
              mGraphicComponent.addAndTrackStop(location);
            }              
            
            mMapView.getCallout().hide();
          }
        });
        
        mMapView.getCallout().setStyle(R.xml.geocode_callout_style);
        mMapView.getCallout().show(location, mGeocodeCallout);
      }
    });         
  }
  
  private GeocodeCallback mGeocodeCallback = new GeocodeCallback() {
    
    private static final String NO_ADDRESS = "No Address Found";
    
    @Override
    public void onReverseGeocodeResultsReady(final LocatorReverseGeocodeResult result) {
      
      showCalloutWithText(GeocodeComponent.getAddress(result.getAddressFields()), result.getLocation());   
    }
    
    @Override
    public void onReverseGeocodeFailed(Exception exception, Point attemptedLocation) {

      showCalloutWithText(NO_ADDRESS, attemptedLocation); 
    }
    
    @Override
    public void onInitializationError(Exception exception) {
      
      logError(exception, true);
    }

    @Override
    public void onForwardGeocodeResultsReady(final List<LocatorGeocodeResult> results) {
      
      mSearchView.post(new Runnable() {
        
        @Override
        public void run() {
          mSearchView.setSuggestionsAdapter(GeocodeSuggestionAdapter.create(getActivity(), results));
        }
      });
    }

    @Override
    public void onForwardGeocodeFailed(Exception exception) {
     
      logError(exception, true);
    }
  };
  
  private void logError(final Exception e, final boolean show) {
    
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        Log.e("Exception", e.getMessage());
        if (show)
          Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();        
      }
    });
    
  }
  
  OnStatusChangedListener mMapInitListener = new OnStatusChangedListener() {

    private static final long serialVersionUID = -7371431461950096308L;

    private static final String MAP_TAG = "Map";
    
    private static final String BASEMAP_TAG = "Basemap";
    
    private static final String INIT_FAILED = "Initialization Failed";
    
    private static final String INIT_SUCCESS = "Initialized";
    
    private static final String LOAD_FAILED = "Load Failed";
    
    private static final String LOAD_SUCCESS = "Loaded";
    
    @Override
    public void onStatusChanged(Object source, STATUS status) {
      
      if (source == mMapView) {
        
        // Log useful information about our map status.
        switch (status) {
        case INITIALIZATION_FAILED:
          Log.e(MAP_TAG, INIT_FAILED);
          break;
        case INITIALIZED:
          Log.v(MAP_TAG, INIT_SUCCESS);
          break;
        default:
          break;
        }        
      } else if (source == mBasemapComponent.getActiveLayer()) {
        
        // Log useful information about our basemap status.
        switch (status) {
        case INITIALIZATION_FAILED:
          Log.e(BASEMAP_TAG, INIT_FAILED);
          break;
        case INITIALIZED:
          Log.v(BASEMAP_TAG, INIT_SUCCESS);
          break;
        case LAYER_LOADED:
          Log.v(BASEMAP_TAG, LOAD_SUCCESS);
          break;
        case LAYER_LOADING_FAILED:
          Log.e(BASEMAP_TAG, LOAD_FAILED);
          break;
        }
          
        mMapView.setExtent(mBasemapComponent.getActiveLayer().getFullExtent(), 0, true);
      }
    }    
  };
}
