package com.esri.arcgis.sample;

import java.util.ArrayList;
import java.util.List;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.SpatialReference;

public class BasemapComponent {

  private static final String DEFAULT_STREET_MAP_SERVICE = "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer";
  
  private final GraphicsLayer mDefaultGraphicsLayer = new GraphicsLayer(Utils.WEB_MERCATOR, new Envelope(-100000000, -100000000, 100000000, 100000000));
  
  private final MapView mMapView;
  
  private final List<Layer> mActiveLayers = new ArrayList<Layer>();
  
  /**
   * Constructs a new basemap component..
   * 
   * @param mapView The MapView to bind layers to.
   */
  public BasemapComponent(MapView mapView) {
    mMapView = mapView;
  }
  
  /**
   * Loads the default online service (World Street Map) as the active
   * basemap layer. This will remove any other active basemap layers.
   */
  public void loadDefaultOnlineLayer() {
    removeActiveLayers();
    ArcGISTiledMapServiceLayer tiledLayer = new ArcGISTiledMapServiceLayer(DEFAULT_STREET_MAP_SERVICE);
    mMapView.addLayer(tiledLayer);
    mActiveLayers.add(tiledLayer);
  }
  
  /**
   * Load a "blank" layer (all gray tiles) with a spatial reference of Web Mercator and
   * a full extent of the world.
   */
  public void loadDefaultCanvas() {
    removeActiveLayers();
    mMapView.addLayer(mDefaultGraphicsLayer);
    mActiveLayers.add(mDefaultGraphicsLayer);
  }
  
  /**
   * Load a local tiled layer.
   * 
   * @param path The path to the local tiled layer.
   */
  public void loadLocalTileLayer(String path) {
    removeActiveLayers();
    ArcGISLocalTiledLayer baseMap = new ArcGISLocalTiledLayer(path);
    mMapView.addLayer(baseMap, 0);
    mActiveLayers.add(baseMap);
    
    mMapView.post(new Runnable() {
      
      @Override
      public void run() {
        mMapView.setExtent(mActiveLayers.get(0).getFullExtent(), 0, true);
      }
    });
    
    reloadMap(path, true);
  }
  
  /**
   * Loads a geodatabase to be used as a basemap.
   * 
   * @param filePath The absolute path to the runtime geodatabase.
   * @param useLabels If true, labeling will be enabled.
   */
  public void loadGeodatabaseLayer(String filePath, boolean useLabels) {
    
    // Remove any active layers.
    removeActiveLayers();
    
    try {
      Geodatabase gdb = new Geodatabase(filePath);
      List<GeodatabaseFeatureTable> tables = gdb.getGeodatabaseTables();
      
      for (int i = 0; i < tables.size(); i++) {
        FeatureLayer featureLayer = new FeatureLayer(tables.get(i));
        featureLayer.setEnableLabels(useLabels);
        mActiveLayers.add(0, featureLayer);
        mMapView.addLayer(featureLayer, 0);
      }      
      
      mMapView.post(new Runnable() {
        
        @Override
        public void run() {          
          mMapView.setExtent(mActiveLayers.get(0).getFullExtent(), 0, true);          
        }
      });
      
    } catch (Exception e) {
      
    }
    
    reloadMap(filePath, false);
  }
  
  /**
   * Reload the map if necessary. This is required if the new basemap has a different spatial reference
   * than the current map. If the spatial reference match, then this function does nothing.
   * 
   * @param filePath The path to the new basemap.
   * @param isTiled Pass true if the path is to a local tiled layer.
   */
  private void reloadMap(String filePath, boolean isTiled) {
    
    SpatialReference mapSr = mMapView.getSpatialReference();
    SpatialReference topSr = getActiveLayer().getDefaultSpatialReference();
    
    if (mapSr != null && topSr != null && mapSr.getID() != topSr.getID())
      MapFragment.requestNewInstance(mMapView.getContext(), filePath, isTiled);
  }
  
  /**
   * Removes any active basemap layers. This method can
   * be safely called even if there are no active layers.
   */
  public void removeActiveLayers() {
    for (Layer layer : mActiveLayers) 
      mMapView.removeLayer(layer);
    
    mActiveLayers.clear();
  }
  
  /**
   * Returns the active (top most) basemap layer.
   * 
   * @return The topmost basemap layer.
   */
  public Layer getActiveLayer() {
    return mActiveLayers.isEmpty() ? null : mActiveLayers.get(0);
  }
}
