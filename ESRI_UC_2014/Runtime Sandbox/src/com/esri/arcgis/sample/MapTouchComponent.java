package com.esri.arcgis.sample;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.arcgis.sample.GraphicComponent.DataSource;
import com.esri.arcgis.sample.TimeWindowFragment.TimeWindowCallback;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;

public class MapTouchComponent extends MapOnTouchListener {

  private final MapView mMapView;
  
  private RouteComponent mRouteComponent;
  
  private GeocodeComponent mGeocodeComponent;
  
  private GraphicComponent mGraphicComponent;
  
  private SeekBar mTimeBar;
  
  private static final int HIT_TOLERANCE = 20;
  
  private MultiPath mDrawMultipath = null;
  
  private int mDragId = Utils.INVALID_ID;
  
  private TouchMode mDrawMode = TouchMode.NONE;
  
  private final View mCallout;

  public enum TouchMode {
    POLYGON,
    POLYLINE,
    DRAG,
    STREAM_REVERSE_GEOCODE,
    TIME_SLIDE,
    NONE
  }
  
  public MapTouchComponent(Context context, MapView view) {
    super(context, view);   
    mMapView = view;
    LayoutInflater inflater = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    mCallout = inflater.inflate(R.layout.callout, (ViewGroup)null);
  }
  
  /**
   * Bind a RouteComponent for touch enabled routing.
   * 
   * @param routeComponent A routing component to be used for solves.
   * @return this for chaining API calls.
   */
  public MapTouchComponent bindComponent(RouteComponent routeComponent) {
    mRouteComponent = routeComponent;
    return this;
  }
  
  /**
   * Bind a GraphicComponent to enable hit testing and adding graphics on touch events.
   * 
   * @param graphicComponent A graphic component to be used for adding/updating graphics.
   * @return this for chaining API calls.
   */
  public MapTouchComponent bindComponent(GraphicComponent graphicComponent) {
    mGraphicComponent = graphicComponent;
    return this;
  }
  
  /**
   * Bind a GeocodeComponent to enable reverse geocode callout display on touch.
   * 
   * @param geocodeComponent A geocoding component for reverse search operations.
   * @return this for chaining API calls.
   */
  public MapTouchComponent bindComponent(GeocodeComponent geocodeComponent) {
    mGeocodeComponent = geocodeComponent;  
    return this;
  }
  
  /**
   * Bind a SeekBar to be used as a time slider. If visible, the value of the time
   * bar will be captured and used for any subsequent solves. Start time is calculated 
   * as a 12 hour buffer on either side of the middle of the seek bar i.e.
   * 
   * 
   *      |------------|-----------|
   * -12 hours  current time   +12 hours
   * 
   * @param timeSlider A SeekBar to be used as a time slider.
   * @return this for chaining API calls.
   */
  public MapTouchComponent bindTimeBar(SeekBar timeSlider) {
    mTimeBar = timeSlider;
    
    // Bind a listener for when the time slider changes.
    if (mTimeBar != null) {
      mTimeBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
        
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
          mDrawMode = TouchMode.NONE;
        }
        
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
          mDrawMode = TouchMode.TIME_SLIDE;
        }
        
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
          submitSolve(false);          
        }
      });
    }
    return this;
  }
  
  /**
   * Signal to start drawing a polygon.
   */
  public void startDrawingPolygon() {
    mDrawMode = TouchMode.POLYGON;
    mDrawMultipath = null;
  }
  
  /**
   * Signal to start drawing a polyline.
   */
  public void startDrawingPolyline() {
    mDrawMode = TouchMode.POLYLINE;
    mDrawMultipath = null;
  }
  
  /**
   * Returns true if the touch component is not actively drawing 
   * or dragging.
   * 
   * @return true if the component is in a stable (motionless) state.
   */
  public boolean isStable() {
    return mDrawMode == TouchMode.NONE;
  }
  
  @Override
  public boolean onDragPointerMove(MotionEvent from, MotionEvent to) {
    
    if (mDrawMode == TouchMode.POLYGON || mDrawMode == TouchMode.POLYLINE) {
      
      // If drawing a multipath add/update the geometry.
      if (mDrawMultipath == null) {
        mDrawMultipath = mDrawMode == TouchMode.POLYGON ? new Polygon() : new Polyline();
        Point startPoint = mMapView.toMapPoint(from.getX(), from.getY());
        mDrawMultipath.startPath(startPoint);          
        
        if (mGraphicComponent != null)
          mGraphicComponent.addAndTrackBarrier(mDrawMultipath);     
      }
      
      Point endPoint = mMapView.toMapPoint(to.getX(), to.getY());
      mDrawMultipath.lineTo(endPoint);
      
      if (mGraphicComponent != null)
        mGraphicComponent.updateTrackedBarrier(mDrawMultipath);
      
      return true;
      
    } else if (mDrawMode == TouchMode.DRAG) {
      
      // If dragging, attempt to submit a solve. We do not queue the solve
      // as we process touch events much faster than we can solve.
      if (mGraphicComponent != null) {
        
        Point mapPoint = mMapView.toMapPoint(to.getX(), to.getY());
        mGraphicComponent.updateTrackedStop(mDragId, mapPoint);   
        submitSolve(false);
      }      
      
      return true;
      
    } else if (mDrawMode == TouchMode.STREAM_REVERSE_GEOCODE) {
      
      // If streaming, reverse geocode every touch event.
      reverseGeocode(to.getX(), to.getY());
      return true;
    }
    
    return super.onDragPointerMove(from, to);
  }
  
  @Override
  public void onLongPress(MotionEvent point) {
    
    // On long press :
    // - if a stop is hit, start drag re-routing
    // - otherwise, enable stream reverse geocoding
    int[] hitGraphics = mGraphicComponent.hitTest(DataSource.STOP_GRAPHICS, point.getX(), point.getY(), HIT_TOLERANCE);
    if (hitGraphics != null && hitGraphics.length > 0) {
      mDragId = hitGraphics[0];
      mDrawMode = TouchMode.DRAG;
      return;
      
    } else {
      mDrawMode = TouchMode.STREAM_REVERSE_GEOCODE;
      reverseGeocode(point.getX(), point.getY());
    }
    
    super.onLongPress(point);
  }
  
  @Override
  public boolean onDragPointerUp(MotionEvent from, MotionEvent to) {
    
    // On up, finish the multipath/end dragging and always reset the 
    // current state.
    
    if (mDrawMode == TouchMode.POLYGON || mDrawMode == TouchMode.POLYLINE) {      
      
      Point endPoint = mMapView.toMapPoint(to.getX(), to.getY());
      mDrawMultipath.lineTo(endPoint);
      
      if (mGraphicComponent != null)
        mGraphicComponent.updateTrackedBarrier(mDrawMultipath);
      
      mDrawMultipath = null;
      
    } else if (mDrawMode == TouchMode.DRAG) {
      
      mDragId = Utils.INVALID_ID;
      submitSolve(false);
    }    
    
    mDrawMode = TouchMode.NONE;    
    return super.onDragPointerUp(from, to);
  }
  
  @Override
  public boolean onSingleTap(MotionEvent point) {
    
    // Filter our hit tests first.
    if (hitTest(point.getX(), point.getY())) {
      return true;
    }
    
    // Next, attempt to reverse geocode.
    if (reverseGeocode(point.getX(), point.getY())) {
      return true;
    }    
    
    return super.onSingleTap(point);
  }
  
  @Override
  public boolean onDoubleTap(MotionEvent point) {
    
    // Try to solve.
    if (submitSolve(true))
      return true;
      
    return super.onDoubleTap(point);
  }
  
  private boolean hitTest(float x, float y) {
    
    if (mGraphicComponent == null)
      return false;
    
    Point mapPoint = mMapView.toMapPoint(x, y);
    
    // Attempt a hit test on our graphics layers. If no graphic
    // was selected, return false (we did not consume the hit).
    
    // First, try to hit our stops layer.
    int[] hitGraphics = mGraphicComponent.hitTest(DataSource.STOP_GRAPHICS, x, y, HIT_TOLERANCE);
    if (hitGraphics != null && hitGraphics.length > 0) {
      final int id = hitGraphics[0];
      mMapView.getCallout().show(mapPoint, mCallout);
      mCallout.findViewById(R.id.callout_delete_button).setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
          if (mGraphicComponent != null)
            mGraphicComponent.removeStop(id);        
          
          mMapView.getCallout().hide();
        }
      });
      
      
      mCallout.findViewById(R.id.callout_add_button).setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
          
          TimeWindowFragment.newInstance()
          .bindCallback(new TimeWindowCallback() {
            
            @Override
            public void onTimeWindowUpdated(Long timeWindowStart, Long timeWindowEnd) {
              
              if (mGraphicComponent != null)
                mGraphicComponent.updateTrackedStop(id, timeWindowStart, timeWindowEnd);
            }
          }).show(((Activity)mMapView.getContext()).getFragmentManager(), null); 
          
          mMapView.getCallout().hide();
        }
      });
      
      return true;
    }
    
    // Next try to hit the polygon barriers.
    hitGraphics = mGraphicComponent.hitTest(DataSource.POLYGON_GRAPHICS, x, y, HIT_TOLERANCE);
    if (hitGraphics != null && hitGraphics.length > 0) {
      final int id = hitGraphics[0];
      mMapView.getCallout().show(mapPoint, mCallout);
      mCallout.findViewById(R.id.callout_delete_button).setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
          if (mGraphicComponent != null)
            mGraphicComponent.removeBarrier(id);       
          
          mMapView.getCallout().hide();
        }
      });      
      return true;
    }
    
    // Next, the line barriers.
    hitGraphics = mGraphicComponent.hitTest(DataSource.POLYLINE_GRAPHICS, x, y, HIT_TOLERANCE);
    if (hitGraphics != null && hitGraphics.length > 0) {
      final int id = hitGraphics[0];
      mMapView.getCallout().show(mapPoint, mCallout);
      mCallout.findViewById(R.id.callout_delete_button).setOnClickListener(new OnClickListener() {
        
        @Override
        public void onClick(View v) {
          if (mGraphicComponent != null)
            mGraphicComponent.removeBarrier(id);   
          
          mMapView.getCallout().hide();
        }
      });      
      return true;
    }
    
    // Otherwise, we don't consume the hit.
    return false;
  }
  
  /**
   * Attempt to reverse geocode.
   * 
   * @param x Screen x coordinate.
   * @param y Screen y coordinate.
   * @return true if the operation was submitted.
   */
  private boolean reverseGeocode(float x, float y) {
    
    if (mGeocodeComponent == null)
      return false;
    
    Point mapPoint = mMapView.toMapPoint(x, y);
    mGeocodeComponent.submitReverseGeocode(mapPoint, mMapView.getSpatialReference(), mMapView.getSpatialReference());
    return true;
  }
  
  /**
   * Calculate the start time if a time slider is bound and visible.
   * See {@link #bindTimeBar(SeekBar)} for more information on how
   * this value is calculated.
   * 
   * @return The start time of the route, or null if a time bar is not bound/visible.
   */
  private Long getStartTime() {
    
    if (mTimeBar == null || mTimeBar.getVisibility() != View.VISIBLE)
      return null;
    
    Calendar now = Calendar.getInstance();
    long currentTime = now.getTimeInMillis();
    
    long twelveHoursMillis = 12 * 3600 * 1000;
    
    double percent = ((double)mTimeBar.getProgress() - 50.0)/50.0;
    double advance = percent * twelveHoursMillis;
    double newTime = currentTime + advance;
    
    return (long)newTime;
  }

  /**
   * Submit a solve to the bound routing component.
   * 
   * @param enqueue If true, the solve will the added to the solve queue.
   * @return false if there are not enough stops or no bound Graphic/Route component.
   */
  private boolean submitSolve(boolean enqueue) {
    
    if (mGraphicComponent == null)
      return false;
    
    boolean didSubmit = false;
    Graphic[] stops = mGraphicComponent.getStops();
    Graphic[] polygonBarriers = mGraphicComponent.getPolygonBarriers();
    Graphic[] polylineBarriers = mGraphicComponent.getPolylineBarriers();
    Date startTime = getStartTime() == null ? null : new Date(getStartTime());
    if (stops.length > 1 && mRouteComponent != null) {
      
      mRouteComponent.submitSolve(
          stops,
          polygonBarriers,
          polylineBarriers,
          mMapView.getSpatialReference(),
          mMapView.getSpatialReference(),
          null,
          startTime,
          enqueue);
      didSubmit = true;
    }
    
    return didSubmit;
  }
}
