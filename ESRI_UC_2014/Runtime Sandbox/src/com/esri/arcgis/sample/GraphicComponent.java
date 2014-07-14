package com.esri.arcgis.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.GraphicsLayer.RenderingMode;
import com.esri.android.map.MapView;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;

public class GraphicComponent {
  
  private final MapView mMapView;

  private final GraphicsLayer mStopGraphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC); 
  
  private final GraphicsLayer mRouteGraphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC);
  
  private final GraphicsLayer mBarrierGraphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC);
  
  private final List<Graphic> mStopGraphics = new ArrayList<Graphic>();
  
  private final List<Graphic> mPolygonBarriers = new ArrayList<Graphic>();
  
  private final List<Graphic> mPolylineBarriers = new ArrayList<Graphic>();
  
  private final PictureMarkerSymbol mDefaultStopSymbol;
  
  private static final SimpleLineSymbol sDefaultRouteSymbol = new SimpleLineSymbol(0x990000EE, 3);
  
  private static final SimpleFillSymbol sDefaultPolygonSymbol = new SimpleFillSymbol(0x999C1AED);
  
  private static final SimpleLineSymbol sDefaultPolylineSymbol = new SimpleLineSymbol(0x99ED951A, 3);
  
  private int mTrackedRouteID = Utils.INVALID_ID;
  
  private int mTrackedPolygonBarrierID = Utils.INVALID_ID;
  
  private int mTrackedPolylineBarrierID = Utils.INVALID_ID;
  
  private static final String STABLE_ID_KEY = "Stable ID";
  
  public enum DataSource {
    STOP_GRAPHICS,
    POLYGON_GRAPHICS,
    POLYLINE_GRAPHICS
  }
  
  public GraphicComponent(MapView mapView) {
    mMapView = mapView;
    
    // Load the default stop symbol
    mDefaultStopSymbol = new PictureMarkerSymbol(mMapView.getContext(), mMapView.getContext().getResources().getDrawable(R.drawable.ic_action_add));
    mDefaultStopSymbol.setOffsetY(15.0f);
    
    // Bind the graphics layers.
    mMapView.addLayer(mStopGraphicsLayer);
    mMapView.addLayer(mRouteGraphicsLayer);
    mMapView.addLayer(mBarrierGraphicsLayer);
  }
  
  /**
   * Add a route geometry and track the graphic Id. Only one route
   * will be tracked at a time, calling this method will remove any
   * previous routes.
   * 
   * @param routeGeometry The route geometry to add.
   */
  public void addAndTrackRoute(Geometry routeGeometry) {
    
    if (mTrackedRouteID != Utils.INVALID_ID)
      mRouteGraphicsLayer.removeGraphic(mTrackedRouteID);
    
    mTrackedRouteID = mRouteGraphicsLayer.addGraphic(new Graphic(routeGeometry, sDefaultRouteSymbol));
  }
  
  /**
   * Update the geometry of a tracked route. If a route is not currently being tracked,
   * the route will be added first.
   * 
   * @param newGeometry The new route geometry.
   */
  public void updateTrackedRoute(Geometry newGeometry) {
    
    if (mTrackedRouteID != Utils.INVALID_ID)
      mRouteGraphicsLayer.updateGraphic(mTrackedRouteID, newGeometry);    
    else
      addAndTrackRoute(newGeometry);
  }  
  
  /**
   * Add and track a stop.
   * 
   * @param stopGeometry The geometry to add as a stop.
   */
  public void addAndTrackStop(Point stopGeometry) {
    
    Graphic stopGraphic = new Graphic(stopGeometry, mDefaultStopSymbol);
    int stopId = mStopGraphicsLayer.addGraphic(stopGraphic); 
    mStopGraphics.add(createStableGraphic(stopGraphic, stopId));
  }
  
  /**
   * Update the geometry of a tracked stop.
   * 
   * @param id The id of the stop.
   * @param newGeometry The new geometry of the stop.
   */
  public void updateTrackedStop(int id, Point newGeometry) {
    mStopGraphicsLayer.updateGraphic(id, newGeometry);
    
    for (int i = 0; i < mStopGraphics.size(); i++) {
      
      Graphic stop = mStopGraphics.get(i);
      Integer stableId = (Integer) stop.getAttributeValue(STABLE_ID_KEY);
      if (stableId == id) {
        mStopGraphics.set(i, new Graphic(newGeometry, stop.getSymbol(), stop.getAttributes()));
        break;
      }
    }    
  }
  
  /**
   * Add and track a polygon barrier.
   * 
   * @param barrierGeometry The polygon geometry to add as a barrier.
   */
  public void addAndTrackBarrier(MultiPath barrierGeometry) {
    
    if (barrierGeometry instanceof Polygon) {
      
      Graphic barrierGraphic = new Graphic(barrierGeometry, sDefaultPolygonSymbol);
      mTrackedPolygonBarrierID = mBarrierGraphicsLayer.addGraphic(barrierGraphic); 
      mPolygonBarriers.add(createStableGraphic(barrierGraphic, mTrackedPolygonBarrierID));
      
    } else if (barrierGeometry instanceof Polyline) {
      
      Graphic barrierGraphic = new Graphic(barrierGeometry, sDefaultPolylineSymbol);
      mTrackedPolylineBarrierID = mBarrierGraphicsLayer.addGraphic(barrierGraphic);
      mPolylineBarriers.add(createStableGraphic(barrierGraphic, mTrackedPolylineBarrierID));
    }
  }
  
  /**
   * Update the geometry of a tracked polygon barrier. This method will only update
   * the geometry of the most recently added barrier. If a polygon barrier is not
   * currently being tracked, one will be added.
   * 
   * @param newGeometry The geometry to update as the polygon barrier with.
   */
  public void updateTrackedBarrier(MultiPath newGeometry) {

    if (newGeometry instanceof Polygon && mTrackedPolygonBarrierID != Utils.INVALID_ID)
      mBarrierGraphicsLayer.updateGraphic(mTrackedPolygonBarrierID, newGeometry);
    else if (newGeometry instanceof Polyline && mTrackedPolylineBarrierID != Utils.INVALID_ID)
      mBarrierGraphicsLayer.updateGraphic(mTrackedPolylineBarrierID, newGeometry);
    else
      addAndTrackBarrier(newGeometry);
  }
  
  /**
   * Get the list of actively tracked stops.
   * 
   * @return An array of stops.
   */
  public Graphic[] getStops() {
    return mStopGraphics.toArray(new Graphic[mStopGraphics.size()]);
  }
  
  /**
   * Get the list of actively tracked polygon barriers.
   * 
   * @return An array of polygon barriers.
   */
  public Graphic[] getPolygonBarriers() {
    return mPolygonBarriers.toArray(new Graphic[mPolygonBarriers.size()]);
  }
  
  /**
   * Get the list of actively tracked polyline barriers.
   * 
   * @return An array of polyline barriers.
   */
  public Graphic[] getPolylineBarriers() {
    return mPolylineBarriers.toArray(new Graphic[mPolylineBarriers.size()]);
  }
  
  /**
   * Remove all stops and barriers. Also, clear any tracking status.
   */
  public void removeAll() {
    mStopGraphicsLayer.removeAll();   
    mRouteGraphicsLayer.removeAll();
    mBarrierGraphicsLayer.removeAll();
    mStopGraphics.clear();
    mPolygonBarriers.clear();
    mTrackedRouteID = Utils.INVALID_ID;
    mTrackedPolygonBarrierID = Utils.INVALID_ID;
    mTrackedPolylineBarrierID = Utils.INVALID_ID;
  }
  
  /**
   * Remove a stop with the given id.
   * 
   * @param id The id of the stop to remove.
   */
  public void removeStop(int id) {    
    removeStableGraphic(mStopGraphics, id);    
    mStopGraphicsLayer.removeGraphic(id);
  }
  
  /**
   * Remove a barrier with the given id.
   * 
   * @param id The id of the barrier to remove.
   */
  public void removeBarrier(int id) {
    removeStableGraphic(mPolygonBarriers, id);
    removeStableGraphic(mPolylineBarriers, id);
    mBarrierGraphicsLayer.removeGraphic(id);
  }
  
  /**
   * Perform a hit test on the given data source.
   * 
   * @param dataSource The data source to test.
   * @param screenX The screen x coordinate.
   * @param screenY The screen y coordinate.
   * @param tolerance The tolerance of the hit test.
   * @return A list of IDs which can be used in other methods in the {@link GraphicComponent}.
   */
  public int[] hitTest(DataSource dataSource, float screenX, float screenY, int tolerance) {
    
    switch (dataSource) {
    case STOP_GRAPHICS:
      return mStopGraphicsLayer.getGraphicIDs(screenX, screenY, tolerance);
    case POLYGON_GRAPHICS:
    case POLYLINE_GRAPHICS:
      return mBarrierGraphicsLayer.getGraphicIDs(screenX, screenY, tolerance);
    default:
      break;
    }
    
    return new int[] { };
  }
  
  private Graphic createStableGraphic(Graphic graphic, int id) {
    
    Map<String,Object> attributes = new HashMap<String,Object>();
    if (graphic.getAttributes() != null)
      attributes.putAll(graphic.getAttributes());
    attributes.put(STABLE_ID_KEY, id);
    return new Graphic(graphic.getGeometry(), graphic.getSymbol(), attributes);
  }
  
  private void removeStableGraphic(List<Graphic> graphics, int id) {
    
    int index = Utils.INVALID_ID;
    for (int i = 0; i < graphics.size(); i++) {
      Graphic graphic = graphics.get(i);
      Integer stableId = (Integer) graphic.getAttributeValue(STABLE_ID_KEY);
      if (stableId != null && stableId == id) {
        index = i;
        break;
      }
    }
    
    if (index != Utils.INVALID_ID)
      graphics.remove(index);    
  }
  
}
