package com.esri.android.devsummit;

import android.content.Context;
import android.graphics.Color;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup.LayoutParams;
import android.widget.SeekBar;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.RasterLayer;
import com.esri.android.map.event.OnZoomListener;
import com.esri.core.analysis.LineOfSight;
import com.esri.core.analysis.Viewshed;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.esri.core.raster.FileRasterSource;
import com.esri.core.raster.FunctionRasterSource;
import com.esri.core.renderer.BlendRenderer;
import com.esri.core.renderer.Colormap;
import com.esri.core.renderer.ColormapRenderer;
import com.esri.core.renderer.RGBRenderer;
import com.esri.core.renderer.StretchParameters;
import com.esri.core.renderer.UniqueValueRenderer;
import com.esri.core.renderer.UniqueValue;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by xuem6538 on 2/26/15.
 */
public class RasterAnalysisHelper {
  private MapView mMapView;
  private String mPathLayer;
  private String mPathTask;
  private LineOfSight mLineOfSight;
  private Layer mLosLayer;
  private RasterLayer mRasterLayer, mViewShedLayer;
  private Viewshed mViewshed;
  private GraphicsLayer mGraphicsLayer;

  public RasterAnalysisHelper(MapView mapView, String pathLayer, String pathTask) {
    mMapView = mapView;
    mPathLayer = pathLayer;
    mPathTask = pathTask;
  }

  /*
   * Line of Sight Analysis
   */
  public void performLOS(MenuItem item) {
    if ((mMapView == null) || (!mMapView.isLoaded())
        || (mPathTask == null) || (mPathTask.isEmpty())) {
      return;
    }

    // clear any analysis layers showing
    turnOffFunctionLayers();

    try {
      mLineOfSight = new LineOfSight(mPathTask);
    } catch (FileNotFoundException | RuntimeException e) {
      e.printStackTrace();
      return;
    }

    if (mLineOfSight != null) {
      mLosLayer = mLineOfSight.getOutputLayer();
      mMapView.addLayer(mLosLayer);
      // set observer features
      mLineOfSight.setObserver(mMapView.getCenter());
      setRenderer4LOS(mLineOfSight);

      // Set gesture used to change the position of the observer and target.
      // When the position of a target is changed, the task will be executed and
      // the result will be rendered on the map view.
      mMapView.setOnTouchListener(new OnTouchListenerLOS(mMapView
          .getContext(), mMapView, mLineOfSight));
      // Reset the observer to center of map on map zoom
      mMapView.setOnZoomListener(new OnZoomListener() {
        private static final long serialVersionUID = 1L;

        @Override
        public void preAction(float pivotX, float pivotY, double factor) {
        }

        @Override
        public void postAction(float pivotX, float pivotY, double factor) {
          // set the observer to the center of the map
          if (mLineOfSight != null) {
            mLineOfSight.setObserver(mMapView.getCenter());
          }
        }
      });
    }

  }

  /*
   * Load a raster layer as basemap
   */
  public void loadRasterAsBasemap() {
    if (mMapView == null) {
      return;
    }

    try {
      mMapView.removeAll();
      // add raster layer
      FileRasterSource rasterSource = new FileRasterSource(mPathLayer);
      mRasterLayer = new RasterLayer(rasterSource);
      // allow to zoom in after the largest LOD for the Landsat 8 image
      mRasterLayer.setMaxScale(200000);
      mMapView.addLayer(mRasterLayer);
      applyRGBRenderer(true);

      Envelope initialExtent = new Envelope(-157.4368965374797, 20.516069728186316, -155.81463794462434, 21.298471528698848);
      mMapView.setExtent(initialExtent);

      // add graphics layer
      mGraphicsLayer = new GraphicsLayer();
      mMapView.addLayer(mGraphicsLayer);

    } catch (FileNotFoundException | RuntimeException e) {
      e.printStackTrace();
    }

  }

  public void applyBlendRenderer() {
    if ((mMapView == null) || (!mMapView.isLoaded())
        || (mRasterLayer == null) || (!mRasterLayer.isInitialized())) {
      return;
    }

    clearFunctionLayers();

    BlendRenderer renderer = new BlendRenderer();
    try {
      renderer.setElevationSource(new FileRasterSource(mPathTask));
//      renderer.setGamma(1.5);
      renderer.setAltitude(80);
      mRasterLayer.setRenderer(renderer);
    } catch (FileNotFoundException | RuntimeException e) {
      e.printStackTrace();
    }
  }

  public void applyRGBRenderer(boolean isDefault) {
    if ((mRasterLayer == null) || (!mRasterLayer.isInitialized())) {
      return;
    }

    clearFunctionLayers();

    RGBRenderer renderer = new RGBRenderer();
    if (isDefault) {
      renderer.setBandIds(new int[]{0, 1, 2});
    } else {
      renderer.setBandIds(new int[]{0, 2, 1});
    }
    StretchParameters.MinMaxStretchParameters stretchParameters = new StretchParameters.MinMaxStretchParameters();
    stretchParameters.setGamma(-1);
    renderer.setStretchParameters(stretchParameters);
    mRasterLayer.setRenderer(renderer);
    mRasterLayer.setBrightness(75.0f);
    mRasterLayer.setContrast(75.0f);
    mRasterLayer.setGamma(10.0f);
  }

  /*
   * Viewshed Analysis
   */
  public void calculateViewshed(MenuItem item) {
    if ((mMapView == null) || (!mMapView.isLoaded())
        || (mPathTask == null) || (mPathTask.isEmpty())) {
      return;
    }

    // clear any analysis layers showing
    turnOffFunctionLayers();

    // create a viewshed function
    try {
      mViewshed = new Viewshed(mPathTask);
      mViewshed.setObserverZOffset(50);
    } catch (FileNotFoundException | RuntimeException e) {
      e.printStackTrace();
      return;
    }

    if (mViewshed != null) {
      FunctionRasterSource functionRS = mViewshed.getOutputFunctionRasterSource();
      mViewShedLayer = new RasterLayer(functionRS);
      mMapView.addLayer(mViewShedLayer);
      setRenderer4Viewshed(mViewShedLayer);

      // Set gesture used to change the position of the observer
      mMapView.setOnTouchListener(new OnTouchListenerViewshed(mMapView
          .getContext(), mMapView, mViewshed));
    }

  }

  private void setRenderer4LOS(LineOfSight los) {
    UniqueValue visible = new UniqueValue();
    visible.setValue(new Integer[]{1});
    visible.setLabel("visible");
    visible.setSymbol(new SimpleLineSymbol(Color.GREEN, 6));
    UniqueValue invisible = new UniqueValue();
    invisible.setValue(new Integer[]{0});
    invisible.setLabel("invisible");
    invisible.setSymbol(new SimpleLineSymbol(Color.RED, 6));
    UniqueValue nodata = new UniqueValue();
    nodata.setValue(new Integer[]{-1});
    nodata.setLabel("nodata");
    nodata.setSymbol(new SimpleLineSymbol(Color.YELLOW, 6));
    UniqueValueRenderer lineRenderer = new UniqueValueRenderer();
    lineRenderer.setField1("visibility");
    lineRenderer.addUniqueValue(visible);
    lineRenderer.addUniqueValue(invisible);
    lineRenderer.addUniqueValue(nodata);
    los.setSightRenderers(null, lineRenderer);
  }

  private void setRenderer4Viewshed(RasterLayer rasterLayer) {
    List<Colormap.UniqueValue> values = new ArrayList<Colormap.UniqueValue>();
    values.add(new Colormap.UniqueValue(1, Color.GREEN, "visible"));

    ColormapRenderer renderer = new ColormapRenderer();
    Colormap colormap = new Colormap();
    colormap.setUniqueValues(values);
    renderer.setColormap(colormap);

    rasterLayer.setRenderer(renderer);
  }

  private void addSlider(final BlendRenderer renderer, final RasterLayer rasterLayer){
    int max = 200;
    SeekBar slider = new SeekBar(mMapView.getContext());
    slider.setMax(max);
    slider.setProgress(50);
    slider.setBackgroundColor(Color.BLUE);
    slider.setLayoutParams(new LayoutParams(300, LayoutParams.WRAP_CONTENT));
    slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        double gamma = progress / 100.0;
        renderer.setGamma(gamma);
        rasterLayer.setRenderer(renderer);
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {

      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {

      }
    });

    mMapView.addView(slider);
  }

  /*
   * Remove any analysis layers on map
   * Dispose of analysis functions
   */
  private void clearFunctionLayers() {
    turnOffFunctionLayers();

    if(mViewshed != null){
      mViewshed.dispose();
      mViewshed = null;
    }

    if(mLineOfSight != null){
      mLineOfSight.dispose();
      mLineOfSight = null;
    }

  }

  private void resetTouchListener() {
    mMapView.setOnTouchListener(new MapOnTouchListener(mMapView
        .getContext(), mMapView));
  }

  /*
   * Remove layer and recycle
   */
  private void turnOffLayer(Layer layer) {
    if (layer != null && !layer.isRecycled()) {
      mMapView.removeLayer(layer);
      layer.recycle();
    }

  }

  /*
   * Remove analysis layers
   * Clear any graphics
   */
  public void turnOffFunctionLayers() {
    // clear analysis layers
    turnOffLayer(mLosLayer);
    turnOffLayer(mViewShedLayer);

    // clear any graphics
    if (mGraphicsLayer != null) {
      mGraphicsLayer.removeAll();
    }

    resetTouchListener();
  }

  /*
   * Override com.esri.android.map.MapOnTouchListener to customize gesture
   * used to change the position of the observer and target.
   */
  private class OnTouchListenerLOS extends MapOnTouchListener {

    MapView mMap;
    LineOfSight mTask;

    public OnTouchListenerLOS(Context context, MapView map, LineOfSight task) {
      super(context, map);
      mMap = map;
      mTask = task;
    }

    @Override
    public boolean onDragPointerMove(MotionEvent from, MotionEvent to) {
      try {
        Point p = mMap.toMapPoint(to.getX(), to.getY());
        mTask.setTarget(p);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }

    @Override
    public boolean onSingleTap(MotionEvent tap) {
      try {
        Point p = mMap.toMapPoint(tap.getX(), tap.getY());
        mTask.setTarget(p);
      } catch (Exception e) {
        e.printStackTrace();
      }

      return true;
    }

    /*
     * Override method to change the observers position in calculating Line of Sight.
     *
     * @see
     * com.esri.android.map.MapOnTouchListener#onLongPress(android.view.MotionEvent)
     */
    @Override
    public void onLongPress(MotionEvent tap) {
      Point p = mMap.toMapPoint(tap.getX(), tap.getY());
      mTask.setObserver(p);

    }
  }

  /*
   * Override com.esri.android.map.MapOnTouchListener to customize gesture
   * used to change the position of the observer.
   */
  private class OnTouchListenerViewshed extends MapOnTouchListener {

    private MapView mMap;
    private Viewshed mTask;

    public OnTouchListenerViewshed(Context context, MapView map,
                                   Viewshed task) {
      super(context, map);
      mMap = map;
      mTask = task;
    }

    @Override
    public boolean onDragPointerMove(MotionEvent from, MotionEvent to) {
      try {
        Point mapPoint = mMap.toMapPoint(to.getX(), to.getY());
        changeObserver(mapPoint);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }

    @Override
    public boolean onSingleTap(MotionEvent tap) {
      try {
        Point mapPoint = mMap.toMapPoint(tap.getX(), tap.getY());
        changeObserver(mapPoint);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return true;
    }

    private void changeObserver(Point mapPoint) {
      // clear any graphics
      mGraphicsLayer.removeAll();
      // create a graphic to represent observer position
      Graphic graphic = new Graphic(mapPoint, new SimpleMarkerSymbol(
          Color.YELLOW, 20, SimpleMarkerSymbol.STYLE.CROSS));
      // add graphic to map
      mGraphicsLayer.addGraphic(graphic);
      //set observer on viewshed
      mTask.setObserver(mapPoint);
    }
  }
}
