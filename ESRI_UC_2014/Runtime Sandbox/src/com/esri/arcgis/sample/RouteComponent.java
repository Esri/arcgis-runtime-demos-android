package com.esri.arcgis.sample;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Graphic;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.NetworkDescription;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;

public class RouteComponent implements OnSharedPreferenceChangeListener {
  
  /**
   * Interface invoked for initialization/solve 
   * failures and updates.
   */
  public interface RouteCallback {
    
    /**
     * Invoked when a new Route shape is ready.
     * 
     * @param routeShape The new route geometry.
     */
    void onRouteShapeReady(Geometry routeShape);
    
    /**
     * Invoked when there was an initialization error.
     * 
     * @param e The exception thrown during initialization.
     */
    void onInitializationError(Exception e);
    
    /**
     * Invoked when a solve fails.
     * 
     * @param e The exception thrown during the solve.
     */
    void onSolveFailed(Exception e);
    
    /**
     * Invoked when new directions are ready. Will never
     * be null, but may be empty.
     * 
     * @param directions The new turn by turn directions.
     */
    void onDirectionsReady(List<RouteDirection> directions);
    
    /**
     * Invoked when new route totals (miles and minutes) are ready.
     * 
     * @param totalMiles The total miles of the solved route.
     * @param totalMinutes The total minutes of the solved route.
     */
    void onTotalsChanged(double totalMiles, double totalMinutes);
  }
  
  private final ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
  
  Future<?> mInitialzed = null;
  
  Future<?> mLastSolve = null;
  
  RouteTask mRouteTask = null;
  
  RouteParameters mRouteParameters = null;
  
  NetworkDescription mNetworkDescription = null;
  
  RouteCallback mCallback = null;
  
  ProgressCallback mProgressCallback = null;
  
  /**
   * Default constructor.
   */
  public RouteComponent() {
    
  }
  
  /**
   * Bind a callback to listen for routing events.
   * If null is passed in, the current callback will be
   * removed.
   * 
   * @param callback The callback to bind.
   * @return this for chaining API calls.
   */
  public RouteComponent bindCallback(RouteCallback callback) {
    mCallback = callback;
    return this;
  }
  
  /**
   * Bind a progress callback to be invoked when initialization  and solves
   * start/end. Passing null will remove the current callback.
   * 
   * @param callback The callback to bind.
   * @return this for chaining API calls.
   */
  public RouteComponent bindCallback(ProgressCallback callback) {
    mProgressCallback = callback;
    return this;
  }
  
  /**
   * Get the active network description. Warning, not synchronized, so can
   * be invalid between calls to initialize.
   * 
   * @return The network description of the currently initialized RouteTask.
   */
  public NetworkDescription getNetworkDescription() {
    
    return mNetworkDescription;
  }
  
  /**
   * Get the active route parameters. Warning, not synchronized, so can be invalid
   * between calls to initialize.
   * 
   * @return The route parameters as a reflection of the initialized RouteTask and 
   * any global settings changes.
   */
  public RouteParameters getRouteParameters() {
    return mRouteParameters;
  }  
  
  /**
   * Cleans up any resources that may be held. Can safely be called
   * multiple times.
   */
  public void cleanup() {
    
    if (mRouteTask != null) {
      mRouteTask.dispose();
      mRouteTask = null; 
    }    
    
    if (mRouteParameters != null) {
      mRouteParameters.dispose();
      mRouteParameters = null;
    } 
    
    mNetworkDescription = null;
  }
  
  /**
   * Asynchronously initialize the routing engine. The action will be queued on
   * a serialized thread pool.
   * 
   * @param url A service URL or absolute file path to a geodatabase.
   * @param credentials The credentials to use (service only).
   * @return A future to track/cancel the initialization.
   */
  public Future<?> initialize(final String url, final UserCredentials credentials) {
    
    return mThreadPool.submit(new Runnable() {

      @Override
      public void run() {
        
        if(mProgressCallback != null)
          mProgressCallback.toggleIndeterminateProgress(true);
        
        try {
          
          // Cleanup any old RouteTasks.
          cleanup();
          
          // Attempt to pull the name of the transportation network (local only).
          String transportationNetwork = Utils.findTransportationNetwork(url);
          
          mRouteTask = transportationNetwork == null
                     ? RouteTask.createOnlineRouteTask(url, credentials)
                     : RouteTask.createLocalRouteTask(url, transportationNetwork);
          
          mRouteParameters = mRouteTask.retrieveDefaultRouteTaskParameters();
          mNetworkDescription = mRouteTask.getNetworkDescription();
          
        } catch (Exception e) {
          
          if (mCallback != null)
            mCallback.onInitializationError(e); 
          
        } finally {
          
          if (mProgressCallback != null)
            mProgressCallback.toggleIndeterminateProgress(false);
          
        }
      }     
      
    });
  }
    
  /**
   * Submit a solve to the routing engine. If the engine is not initialized, 
   * an appropriate error will be passed to the callback (if bound).
   * 
   * @param stops The stops to solve on (Graphics must have point geometry).
   * @param polygonBarriers The polygon barriers to use, can be null.
   * @param lineBarriers The line barriers to use, can be null.
   * @param inSR The input spatial reference. If null, the default routing engine spatial reference will be assumed.
   * @param outSR The output spatial reference. If null, the default routing engine spatial reference will be assumed.
   * @param optimize Null to use global settings, otherwise true to optimize stops.
   * @param enqueue If true, and there is an active solve, the solve will be enqueued. If false and there
   * is an active solve, this function will return immediately with a handle to the last enqueued solve.
   * @return A handle to the solve if submitted successfully, otherwise an exisitng solve handle (enqueue = false), or null.
   */
  public Future<?> submitSolve(final Graphic[] stops, 
                               final Graphic[] polygonBarriers,
                               final Graphic[] lineBarriers,
                               final SpatialReference inSR,
                               final SpatialReference outSR,
                               final Boolean optimize,
                               final Date startTime,
                               boolean enqueue) {
    
    if (!enqueue && mLastSolve != null && !mLastSolve.isDone())
      return mLastSolve;
    
    mLastSolve = mThreadPool.submit(new Runnable() {

      @Override
      public void run() {
        
        if (mProgressCallback != null)
          mProgressCallback.toggleIndeterminateProgress(true);
        
        try {
          
          if (mRouteTask == null || mRouteParameters == null)
            throw new Exception("Task not initialized");
          
          if (stops == null || stops.length < 2)
            throw new Exception("Invalid stops");          
          
          // Note: when settings features, the online services are very particular
          // and settings "empty" features will throw a service exception. In the case
          // where no feature were specified on the input, we want to set null on the
          // route parameters object.
          
          // Set the stops
          NAFeaturesAsFeature features = new NAFeaturesAsFeature();
          features.setSpatialReference(inSR);
          features.setFeatures(stops);          
          mRouteParameters.setStops(stops.length == 0 ? null : features);
          
          // Set the line barriers
          features = new NAFeaturesAsFeature();
          features.setSpatialReference(inSR);
          features.setFeatures(lineBarriers);
          mRouteParameters.setPolylineBarriers(lineBarriers.length == 0 ? null : features);
          
          // Set the polygon barriers
          features = new NAFeaturesAsFeature();
          features.setSpatialReference(inSR);
          features.setFeatures(polygonBarriers);
          mRouteParameters.setPolygonBarriers(polygonBarriers.length == 0 ? null : features);
          
          // Set Optimize
          if (optimize != null)
            mRouteParameters.setFindBestSequence(optimize);
          
          // Set the start time
          mRouteParameters.setStartTime(startTime);
          
          // Set the output spatial reference
          mRouteParameters.setOutSpatialReference(outSR);
          
          // Solve
          RouteResult result = mRouteTask.solve(mRouteParameters);
          Route route = result.getRoutes().get(0);
          Graphic routeGraphic = route.getRouteGraphic();
          
          // Geometry callback
          if (mCallback != null && routeGraphic != null)
            mCallback.onRouteShapeReady(routeGraphic.getGeometry());
          
          // Directions callback
          if (mCallback != null)
            mCallback.onDirectionsReady(route.getRoutingDirections());
          
          // Total time and distance callbacl
          if (mCallback != null)
            mCallback.onTotalsChanged(route.getTotalMiles(), route.getTotalMinutes());
          
        } catch (Exception e) {
          
          if (mCallback != null)
            mCallback.onSolveFailed(e);  
          
        } finally {
          
          if (mProgressCallback != null)
            mProgressCallback.toggleIndeterminateProgress(false);
        }
      }     
      
    });
    
    return mLastSolve;
  }

  /**
   * The Route component will listen for changes on the global settings;.
   */
  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
   
    if (mRouteParameters == null)
      return;
    
    if (SettingsComponent.IMPEDANCE_PREF_KEY.equals(key)) {
      
      String newImpedance = sharedPreferences.getString(key, null);
      if (newImpedance != null)
        mRouteParameters.setImpedanceAttributeName(newImpedance);
    
    } else if (SettingsComponent.RESTRICTIONS_PREF_KEY.equals(key)) {
      
      Set<String> newRestrictions = sharedPreferences.getStringSet(key, null);
      if (newRestrictions != null)
        mRouteParameters.setRestrictionAttributeNames(newRestrictions.toArray(new String[newRestrictions.size()]));
    
    } else if (SettingsComponent.TSP_PREF_KEY.equals(key)) {
      
      mRouteParameters.setFindBestSequence(sharedPreferences.getBoolean(key, false));
      
    } else if (SettingsComponent.TSP_FIRST_PREF_KEY.equals(key)) {
      
      mRouteParameters.setPreserveFirstStop(sharedPreferences.getBoolean(key, true));
      
    } else if (SettingsComponent.TSP_LAST_PREF_KEY.equals(key)) {
      
      mRouteParameters.setPreserveLastStop(sharedPreferences.getBoolean(key, true));
      
    } else if (SettingsComponent.DIRECTIONS_LANGUAGE_PREF_KEY.equals(key)) {
      
      String newLanguage = sharedPreferences.getString(key, null);
      if (newLanguage != null)
        mRouteParameters.setDirectionsLanguage(newLanguage);
      
    } else if (SettingsComponent.DIRECTIONS_PREF_KEY.equals(key)) {
      
      mRouteParameters.setReturnDirections(sharedPreferences.getBoolean(key, false));
    }
    
  }
}
