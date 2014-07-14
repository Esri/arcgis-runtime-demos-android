package com.esri.arcgis.sample;

import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteManeuverType;

public class DirectionsComponent extends Fragment {

  public static final String TAG = DirectionsComponent.class.getSimpleName();
  
  public interface DirectionsCallback {
    
    void onDirectionSelected(RouteDirection direction);
    
  }

  private DirectionsCallback mCallback;

  private ActionBarDrawerToggle mDrawerToggle;

  private DrawerLayout mDrawerLayout;
  
  private ListView mDrawerListView;

  public DirectionsComponent() {
  }
  
  /**
   * Bind a callback to listen for directions selection events.
   * If null, will clear the current callback;
   * 
   * @param callback The callback to bind.
   * @return this for chaining API calls.
   */
  public DirectionsComponent bindCallback(DirectionsCallback callback) {
    mCallback = callback;
    return this;
  }
  
  /**
   * Set the directions to be displayed.
   * 
   * @param directions A list of directions to be displayed.
   */
  public void setDirections(List<RouteDirection> directions) {
    if (mDrawerListView != null)
      mDrawerListView.setAdapter(new DirectionsAdapter(mDrawerListView.getContext(), directions));
    
    toggleDrawer(directions == null || directions.isEmpty());
  }
  
  /**
   * Toggle the directions drawer. On lock, we change the action
   * bar and lock the DrawerLayout.
   * 
   * @param lock If true, the directions slide out pane will no longer be accessible
   * through home button or touch events.
   */
  private void toggleDrawer(boolean lock) {
    
    if (getActivity() != null)
      getActivity().getActionBar().setDisplayHomeAsUpEnabled(!lock);
    
    if (mDrawerToggle != null)
      mDrawerToggle.setDrawerIndicatorEnabled(!lock);
    
    if (mDrawerLayout != null)
      mDrawerLayout.setDrawerLockMode(lock ? DrawerLayout.LOCK_MODE_LOCKED_CLOSED : DrawerLayout.LOCK_MODE_UNLOCKED);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    setHasOptionsMenu(true);
  }
  
  /**
   * Must be called before any calls to {@link #setDirections(List)}. This will capture necessary views
   * and should be called after the main view hierarchy is established.
   * 
   * @return this for chaining API calls.
   */
  public DirectionsComponent initialize() {   
    
    // Find the drawer layout.
    mDrawerLayout = (DrawerLayout) getActivity().findViewById(R.id.drawer_layout);
    mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    // Initialize the drawer toggle.
    mDrawerToggle = new ActionBarDrawerToggle(getActivity(), mDrawerLayout, R.drawable.ic_drawer, R.string.navigation_drawer_open, R.string.navigation_drawer_close) {
      
      @Override
      public void onDrawerClosed(View drawerView) {
        super.onDrawerClosed(drawerView);
        getActivity().invalidateOptionsMenu();
      }

      @Override
      public void onDrawerOpened(View drawerView) {
        super.onDrawerOpened(drawerView);
        getActivity().invalidateOptionsMenu(); 
      }
    };

    // Sync the state of the drawer toggle.
    mDrawerLayout.post(new Runnable() {
      @Override
      public void run() {
        mDrawerToggle.syncState();
      }
    });    
    
    // Set the drawer listener and lock the directions component.
    mDrawerLayout.setDrawerListener(mDrawerToggle);
    toggleDrawer(true);
    return this;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {    
    
    mDrawerListView = (ListView) inflater.inflate(R.layout.fragment_navigation_drawer, container, false);
    mDrawerListView
        .setOnItemClickListener(new AdapterView.OnItemClickListener() {
          @Override
          public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            if (mCallback != null) {
              RouteDirection direction = (RouteDirection) parent.getItemAtPosition(position);
              mCallback.onDirectionSelected(direction);
            }            
          }
        });
    
    return mDrawerListView;
  }

  /**
   * Returns true if the directions view pane is open.
   * 
   * @return true if directions are visible.
   */
  public boolean isDrawerOpen() {
    return mDrawerLayout != null
        && mDrawerLayout.isDrawerOpen(Gravity.LEFT);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    mDrawerToggle.onConfigurationChanged(newConfig);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

    if (mDrawerLayout != null && isDrawerOpen()) {
      inflater.inflate(R.menu.global, menu);
    }
    super.onCreateOptionsMenu(menu, inflater);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (mDrawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * A simple adapter class to hold directions information. Each
   * item will have a main text, sub text, and image.
   */
  private static class DirectionsAdapter extends BaseAdapter  {

    private final Context mContext;
    
    private final List<RouteDirection> mDirections;
    
    public DirectionsAdapter(Context context, List<RouteDirection> directions) {
      mContext = context;
      mDirections = directions;      
    }
    
    @Override
    public int getCount() {
      return mDirections.size();
    }

    @Override
    public Object getItem(int position) {
      return mDirections.get(position);
    }

    @Override
    public long getItemId(int position) {
      return mDirections.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      
      // Recycle the view.
      View view = convertView;
      if (view == null) {
        view = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.directions_item, parent, false);
      }
      
      // Get the main and sub text.
      final RouteDirection routeDirection = mDirections.get(position);
      TextView mainText = (TextView) view.findViewById(R.id.directions_item_main_text);
      TextView subText = (TextView) view.findViewById(R.id.directions_item_sub_text);
      
      // Set the maneuver image.
      ImageView maneuverImage = (ImageView) view.findViewById(R.id.directions_item_thumbnail);
      maneuverImage.setImageResource(getResIdForManeuver(routeDirection.getManeuver()));
      
      mainText.setText(routeDirection.getText());
      
      // Truncate the units if necessary.
      TruncationPair timeTruncate = truncateMinutes(routeDirection.getMinutes());
      TruncationPair distTruncate = truncateMiles(routeDirection.getLength());
      
      String timeString = timeTruncate.didTruncate ? "%.0f sec" : "%.1f min";
      String distString = distTruncate.didTruncate ? "%.0f feet" : "%.1f mi";
      
      subText.setText(String.format(distString + " " + timeString, distTruncate.value, timeTruncate.value));
      
      return view;
    }        
    
    /**
     * Simple class to hold the results of a unit truncation.     *
     */
    private static class TruncationPair {
      boolean didTruncate = false;
      double value = 0.0;
    }
    
    /**
     * Truncates a value in minutes. If the value is less than one minute, the
     * value is converted to seconds.
     * 
     * @param minutes The minutes to test.
     * @return A truncation pair indicating if the value was truncated the value to display.
     */
    private static TruncationPair truncateMinutes(double minutes) {    
      TruncationPair result = new TruncationPair();
      result.didTruncate = minutes <= 1.0;
      result.value = result.didTruncate ? minutes * 60.0 : minutes;
      return result;
    }
    
    /**
     * Truncates a value in miles. If the value is less than 1000 feet, the value
     * will be converted to feet.
     * 
     * @param miles The miles value to test.
     * @return A truncation pair indicate if the miles were truncated to feet, and the value.
     */
    private static TruncationPair truncateMiles(double miles) {
      TruncationPair result = new TruncationPair();
      result.didTruncate = miles <= 0.18;
      result.value = result.didTruncate ? miles * 5280.0 : miles;
      return result;  
    }
  }
  
  /**
   * Returns a resource Id for a particular directions maneuver type.
   * 
   * @param maneuverType The direction maneuver type (turn left, go straight, etc).
   * @return The resource id.
   */
  private static int getResIdForManeuver(RouteManeuverType maneuverType) {
    
    switch (maneuverType) {
    
    case BEAR_LEFT:
      return R.drawable.ic_routing_bear_left_dark;
    case BEAR_RIGHT:
      return R.drawable.ic_routing_bear_right_dark;
    case DEPART:
      return R.drawable.ic_action_add_dark;
    case DOOR_PASSAGE:
      return R.drawable.ic_routing_straight_arrow_dark;
    case ELEVATOR:
      return R.drawable.ic_routing_straight_arrow_dark;
    case END_OF_FERRY:
      return R.drawable.ic_routing_get_off_ferry_dark;
    case ESCALATOR:
      return R.drawable.ic_routing_straight_arrow_dark;
    case FERRY:
      return R.drawable.ic_routing_take_ferry_dark;
    case FORK_CENTER:
      return R.drawable.ic_routing_take_center_fork_dark;
    case FORK_LEFT:
      return R.drawable.ic_routing_take_fork_left_dark;
    case FORK_RIGHT:
      return R.drawable.ic_routing_take_fork_right_dark;
    case HIGHWAY_CHANGE:
      return R.drawable.ic_routing_highway_change_dark;
    case HIGHWAY_EXIT:
      return R.drawable.ic_routing_take_exit_dark;
    case HIGHWAY_MERGE:
      return R.drawable.ic_routing_merge_onto_highway_dark;
    case PEDESTRIAN_RAMP:
      return R.drawable.ic_routing_straight_arrow_dark;
    case RAMP_LEFT:
      return R.drawable.ic_routing_take_ramp_left_dark;
    case RAMP_RIGHT:
      return R.drawable.ic_routing_take_ramp_right_dark;
    case ROUNDABOUT:
      return R.drawable.ic_routing_get_on_roundabout_dark;
    case SHARP_LEFT:
      return R.drawable.ic_routing_turn_sharp_left_dark;
    case SHARP_RIGHT:
      return R.drawable.ic_routing_turn_sharp_right_dark;
    case STAIRS:
      return R.drawable.ic_routing_straight_arrow_dark;
    case STOP:
      return R.drawable.ic_action_add_dark;
    case STRAIGHT:
      return R.drawable.ic_routing_straight_arrow_dark;
    case TRIP_ITEM:
      return R.drawable.ic_action_add_dark;
    case TURN_LEFT:
      return R.drawable.ic_routing_turn_left_dark;
    case TURN_LEFT_LEFT:
      return R.drawable.ic_routing_left_left_dark;
    case TURN_LEFT_RIGHT:
      return R.drawable.ic_routing_left_right_dark;
    case TURN_RIGHT:
      return R.drawable.ic_routing_turn_right_dark;
    case TURN_RIGHT_LEFT:
      return R.drawable.ic_routing_right_left_dark;
    case TURN_RIGHT_RIGHT:
      return R.drawable.ic_routing_right_right_dark;
    case U_TURN:
      return R.drawable.ic_routing_u_turn_dark;
    case UNKNOWN:
      return R.drawable.ic_routing_straight_arrow_dark;
    default:
      return R.drawable.ic_routing_straight_arrow_dark;    
    }    
  }
}
