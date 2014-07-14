package com.esri.arcgis.sample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.arcgis.sample.GraphicComponent.DataSource;
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
    NONE
  }
  
  public MapTouchComponent(Context context, MapView view) {
    super(context, view);   
    mMapView = view;
    LayoutInflater inflater = ((LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE));
    mCallout = inflater.inflate(R.layout.callout, (ViewGroup)null);
  }
  
  public MapTouchComponent bindComponent(RouteComponent routeComponent) {
    mRouteComponent = routeComponent;
    return this;
  }
  
  public MapTouchComponent bindComponent(GraphicComponent graphicComponent) {
    mGraphicComponent = graphicComponent;
    return this;
  }
  
  public MapTouchComponent bindComponent(GeocodeComponent geocodeComponent) {
    mGeocodeComponent = geocodeComponent;  
    return this;
  }
  
  public void startDrawingPolygon() {
    mDrawMode = TouchMode.POLYGON;
    mDrawMultipath = null;
  }
  
  public void startDrawingPolyline() {
    mDrawMode = TouchMode.POLYLINE;
    mDrawMultipath = null;
  }
  
  public void startDragging() {
    mDrawMode = TouchMode.DRAG;
  }
  
  public boolean isDragging() {
    return mDrawMode == TouchMode.DRAG;
  }
  
  @Override
  public boolean onDragPointerMove(MotionEvent from, MotionEvent to) {
    
    if (mDrawMode == TouchMode.POLYGON || mDrawMode == TouchMode.POLYLINE) {
      
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
      
      if (mGraphicComponent != null) {
        
        Point mapPoint = mMapView.toMapPoint(to.getX(), to.getY());
        mGraphicComponent.updateTrackedStop(mDragId, mapPoint);   
        submitSolve(false);
      }      
      
      return true;
      
    } else if (mDrawMode == TouchMode.STREAM_REVERSE_GEOCODE) {
      
      reverseGeocode(to.getX(), to.getY());
      return true;
    }
    
    return super.onDragPointerMove(from, to);
  }
  
  @Override
  public void onLongPress(MotionEvent point) {
    
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
    
    if (submitSolve(true))
      return true;
      
    return super.onDoubleTap(point);
  }
  
  private boolean hitTest(float x, float y) {
    
    if (mGraphicComponent == null)
      return false;
    
    Point mapPoint = mMapView.toMapPoint(x, y);
    
    // Attempt a hit test on our stops graphics layer. If no stop graphic
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
  
  private boolean reverseGeocode(float x, float y) {
    
    if (mGeocodeComponent == null)
      return false;
    
    Point mapPoint = mMapView.toMapPoint(x, y);
    mGeocodeComponent.submitReverseGeocode(mapPoint, mMapView.getSpatialReference(), mMapView.getSpatialReference());
    return true;
  }

  private boolean submitSolve(boolean enqueue) {
    
    if (mGraphicComponent == null)
      return false;
    
    boolean didSubmit = false;
    Graphic[] stops = mGraphicComponent.getStops();
    Graphic[] polygonBarriers = mGraphicComponent.getPolygonBarriers();
    Graphic[] polylineBarriers = mGraphicComponent.getPolylineBarriers();
    if (stops.length > 1 && mRouteComponent != null) {
      
      mRouteComponent.submitSolve(
          stops,
          polygonBarriers,
          polylineBarriers,
          mMapView.getSpatialReference(),
          mMapView.getSpatialReference(),
          null,
          enqueue);
      didSubmit = true;
    }
    
    return didSubmit;
  }
}
