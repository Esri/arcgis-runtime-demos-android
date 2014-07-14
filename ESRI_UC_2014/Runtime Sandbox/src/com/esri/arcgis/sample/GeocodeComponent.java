package com.esri.arcgis.sample;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.io.UserCredentials;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;

public class GeocodeComponent {

  public interface GeocodeCallback {
    
    void onInitializationError(Exception exception);
    
    void onReverseGeocodeResultsReady(LocatorReverseGeocodeResult result);
    
    void onReverseGeocodeFailed(Exception exception, Point attemptedLocation);
    
    void onForwardGeocodeResultsReady(List<LocatorGeocodeResult> results);
    
    void onForwardGeocodeFailed(Exception exception);
  }
  
  private static final double REVERSE_TOLERANCE = 150.0;
  
  private final ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
  
  Future<?> mInitialzed = null;
  
  private Locator mLocator = null;
  
  private ProgressCallback mProgressCallback;
  
  private GeocodeCallback mCallback;
  
  public void cleanup() {
    
    if (mLocator != null) {
      mLocator.dispose();
      mLocator = null;
    }
  }
  
  public GeocodeComponent bindCallback(ProgressCallback callback) {
    mProgressCallback = callback;
    return this;
  }
  
  public GeocodeComponent bindCallback(GeocodeCallback callback) {
    mCallback = callback;
    return this;
  }
  
  public Future<?> initialize(final String url, final UserCredentials credentials) {
    
    return mThreadPool.submit(new Runnable() {

      @Override
      public void run() {
        
        if(mProgressCallback != null)
          mProgressCallback.toggleIndeterminateProgress(true);
        
        try {
          
          // Cleanup any oldLocators.
          cleanup();
          
          mLocator = url == null ? Locator.createOnlineLocator() :
                     Utils.isLocator(new File(url)) ? Locator.createLocalLocator(url) :
                     Locator.createOnlineLocator(url, credentials);
          
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
  
  public Future<?> submitForwardGeocode(final String address, final SpatialReference outSR) {
    return mThreadPool.submit(new Runnable() {

      @Override
      public void run() {
        
        if (mProgressCallback != null)
          mProgressCallback.toggleIndeterminateProgress(true);
        
        try {
          
          if (mLocator == null)
            throw new Exception("Locator not initialized");
          
          if (address == null)
            throw new IllegalArgumentException("Address required");
          
          LocatorFindParameters lfp = new LocatorFindParameters(address);
          lfp.setOutSR(outSR);
          List<LocatorGeocodeResult> results = mLocator.find(lfp);
          if (mCallback != null)
            mCallback.onForwardGeocodeResultsReady(results);         
          
        } catch (Exception e) {
          
          if (mCallback != null) {
            mCallback.onForwardGeocodeFailed(e);  
          }
          
        } finally {
          
          if (mProgressCallback != null)
            mProgressCallback.toggleIndeterminateProgress(false);
        }
      }      
    });
  }
  
  public Future<?> submitReverseGeocode(final Point point, final SpatialReference inSR, final SpatialReference outSR) {
    return mThreadPool.submit(new Runnable() {

      @Override
      public void run() {
        
        if (mProgressCallback != null)
          mProgressCallback.toggleIndeterminateProgress(true);
        
        try {
          
          if (mLocator == null)
            throw new Exception("Locator not initialized");
          
          if (point == null || inSR == null)
            throw new IllegalArgumentException("Point and spatial reference required");
          
          LocatorReverseGeocodeResult result = mLocator.reverseGeocode(point, REVERSE_TOLERANCE, inSR, outSR);
          if (mCallback != null)
            mCallback.onReverseGeocodeResultsReady(result);         
          
        } catch (Exception e) {
          
          if (mCallback != null) {
            Point attemptedLocation = (Point) GeometryEngine.project(point, inSR, outSR);
            mCallback.onReverseGeocodeFailed(e, attemptedLocation);             
          }
          
        } finally {
          
          if (mProgressCallback != null)
            mProgressCallback.toggleIndeterminateProgress(false);
        }
      }      
    });
  }
  
  private static final String[] STREET_KEYS = new String[] { "street", "address" };
  
  private static final String[] STATE_KEYS = new String[] { "state", "region" };
  
  private static final String[] ZIP_KEYS = new String[] { "zip", "postal" };
  
  private static final String[] CITY_KEYS = new String[] { "city" };
  
  private static final String[] SINGLE_LINE_KEYS = new String[] { "singlekey", "singlelinefield" };
  
  public static String getAddress(Map<String,String> addressFields) {
    
    String street = null, state = null, city = null, zip = null, singleLine = null;
    
    for (Entry<String,String> entry : addressFields.entrySet()) {
      
      String key = entry.getKey();
      String value = entry.getValue();
      
      if (Utils.containsCaseIgnore(STREET_KEYS, key))
        street = value;
      else if (Utils.containsCaseIgnore(CITY_KEYS, key))
        city = value;
      else if (Utils.containsCaseIgnore(STATE_KEYS, key))
        state = value;
      else if (Utils.containsCaseIgnore(ZIP_KEYS, key))
        zip = value;      
      else if (Utils.containsCaseIgnore(SINGLE_LINE_KEYS, key))
        singleLine = value;
    }
    
    StringBuilder formattedAddress = new StringBuilder();
    
    if (singleLine != null){
      
      formattedAddress.append(singleLine);
      
    } else {
      
      formattedAddress.append(street == null ? "" : street)
                      .append(street == null ? "" : "\n")
                      .append(city == null ? "" : city)
                      .append(city == null ? "" : " ")
                      .append(state == null ? "" : state)
                      .append(state == null ? "" : " ")
                      .append(zip == null ? "" : zip);
    }
    
    return formattedAddress.toString();
  }
  
  public static class GeocodeSuggestionAdapter extends CursorAdapter {
    
    private static final String MAIN_TEXT_COLUMN = "main_text";
    
    private static final String SUB_TEXT_COLUMN = "sub_text";
    
    private static final String X_COLUMN = "x";
    
    private static final String Y_COLUMN = "y";
    
    private static final String SRID_COLUMN = "srid";
    
    private static final String ID_COLUMN = "_id";
    
    private static String[] extractText(LocatorGeocodeResult result) {
      
      if (result == null || result.getAddress() == null)
        return new String[] { "", "" };
      
      String address = result.getAddress();
      int firstDelim = address.indexOf(',');
      if (firstDelim == -1)
        firstDelim = address.indexOf(' ');
      
      if (firstDelim == -1)
        return new String[] { address, "" };
      
      return new String[] { address.substring(0, firstDelim), address.substring(firstDelim + 1) };
    }
    
    public static Point getPointFromCursor(Cursor cursor, SpatialReference outSR) {
      
      double x = cursor.getDouble(cursor.getColumnIndex(X_COLUMN));
      double y = cursor.getDouble(cursor.getColumnIndex(Y_COLUMN));
      int wkid = cursor.getInt(cursor.getColumnIndex(SRID_COLUMN));
      
      return (Point) GeometryEngine.project(new Point(x,y), SpatialReference.create(wkid), outSR);      
    }
    
    public static String getAddressFromCursor(Cursor cursor) {
      
      String mainText = cursor.getString(cursor.getColumnIndex(MAIN_TEXT_COLUMN));
      String subText = cursor.getString(cursor.getColumnIndex(SUB_TEXT_COLUMN));
      
      return mainText + "\n" + subText;      
    }
    
    public static GeocodeSuggestionAdapter create(Context context, List<LocatorGeocodeResult> results) {
      
      MatrixCursor matrixCursor = new MatrixCursor(
          new String[] { ID_COLUMN, MAIN_TEXT_COLUMN, SUB_TEXT_COLUMN , X_COLUMN, Y_COLUMN, SRID_COLUMN});
      for (int i = 0; i < results.size(); i++) {
        
        LocatorGeocodeResult result = results.get(i);
        Point point = result.getLocation();
        int wkid = result.getSpatialreference().getID();
        String[] mainSub = extractText(result);
        matrixCursor.addRow(new Object[] { i, mainSub[0], mainSub[1] , point.getX(), point.getY(), wkid});        
      }
      
      return new GeocodeSuggestionAdapter(context, matrixCursor, 0);
    }
    
    public GeocodeSuggestionAdapter(Context context, Cursor c, int flags) {
      super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
      
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View view = inflater.inflate(R.layout.geocode_item, parent, false);
      
      bindView(view, context, cursor);      
      return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {      
      TextView mainText =  (TextView) view.findViewById(R.id.geocode_item_main_text);
      TextView subText = (TextView) view.findViewById(R.id.geocode_item_sub_text);
      
      mainText.setText(cursor.getString(cursor.getColumnIndex(MAIN_TEXT_COLUMN)));
      subText.setText(cursor.getString(cursor.getColumnIndex(SUB_TEXT_COLUMN)));      
    }    
  }
  
}
