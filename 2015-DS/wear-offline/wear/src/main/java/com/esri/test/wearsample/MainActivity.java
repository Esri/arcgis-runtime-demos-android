/* Copyright 2015 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * A copy of the license is available in the repository's
 * https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt
 *
 * For information about licensing your deployed app, see
 * https://developers.arcgis.com/android/guide/license-your-app.htm
 *
 */

package com.esri.test.wearsample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.view.DismissOverlayView;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapOnTouchListener;
import com.esri.android.map.MapView;
import com.esri.android.map.TiledLayer;
import com.esri.android.map.ags.ArcGISLocalTiledLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.runtime.LicenseLevel;
import com.esri.core.runtime.LicenseResult;
import com.esri.core.symbol.MarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorReverseGeocodeResult;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;
import com.esri.android.map.GraphicsLayer.RenderingMode;
import com.esri.core.symbol.SimpleMarkerSymbol.STYLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {

  private TextView mTextView;

  private static final String TAG = "**** Watch demo";

  Locator mLocator = null;
  View mCallout = null;
  Spinner dSpinner;

    final String extern = Environment.getExternalStorageDirectory().getPath();
    final String tpkPath = "/ArcGIS/SoCalBasemap.tpk";
    TiledLayer mTileLayer = new ArcGISLocalTiledLayer(extern + tpkPath);
    GraphicsLayer mGraphicsLayer = new GraphicsLayer(RenderingMode.DYNAMIC);

    RouteTask mRouteTask = null;
    NAFeaturesAsFeature mStops = new NAFeaturesAsFeature();

    MapView mMapView;
    private DismissOverlayView mDismissOverlay;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
      // Set license level for the demo. Override "license_client_id" and
      // "license_standard_string" in strings.xml to use Standard license level.
      // Otherwise comment out the following line to use Developer license level.
      // @see <a href="https://developers.arcgis.com/android/guide/license-your-app.htm">License your app</a>
      setLicense();
      setContentView(R.layout.activity_main);

      WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
      stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
          @Override public void onLayoutInflated(WatchViewStub stub) {
              // Now you can access your views
              mMapView = (MapView) findViewById(R.id.mapView);

              mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
              mDismissOverlay.setIntroText("Bye Bye");
              mDismissOverlay.showIntroIfNecessary();

              // Set the tiled map service layer and add a graphics layer
              mMapView.addLayer(mTileLayer);
              mMapView.addLayer(mGraphicsLayer);

              initializeRoutingAndGeocoding();
              mMapView.setOnTouchListener(new TouchListener(MainActivity.this, mMapView));

              final LocationDisplayManager locationManager = mMapView.getLocationDisplayManager();
              locationManager.setAllowNetworkLocation(false);
              locationManager.start();


              if (locationManager.isStarted()){
                  // Add the location as a stop
                  MarkerSymbol locationSymbol = null;
                  try {
                      locationSymbol = (locationManager.getDefaultSymbol());
                  } catch (Exception e) {
                      e.printStackTrace();
                  }
                  Graphic graphic = new Graphic(locationManager.getPoint(), locationSymbol);
                  StopGraphic stop = new StopGraphic(graphic);
                  stop.setName("gps location");
                  mStops.addFeature(stop);
              }
          }
      });


//      String path = Environment.getExternalStorageDirectory().getPath() + "/ArcGIS/SoCalBasemap.tpk";

  }



    private void setLicense() {
        // Running this activity should result in a Standard license level
        LicenseLevel standardLicense = LicenseLevel.STANDARD;

        // Set clientId to upgrade DEVELOPER to BASIC
        String clientId = getString(R.string.license_client_id);
        LicenseResult licenseResult = ArcGISRuntime.setClientId(clientId);

        // Use a license string to set a license.
        String licenseString = getString(R.string.license_standard_string);
        licenseResult = ArcGISRuntime.License.setLicense(licenseString);
    }


    private void initializeRoutingAndGeocoding() {

        // We will spin off the initialization in a new thread
        new Thread(new Runnable() {

            @Override
            public void run() {

                String locatorPath = "/ArcGIS/samples/palmsprings/locator/CaliforniaNevadaLocator/v101/CaliforniaNevadaLocator.loc";
                String networkPath = "/ArcGIS/samples/palmsprings/data/canv.geodatabase";

//                String networkName = "Streets_ND";

                String networkName = "Routing_ND";

                // Attempt to load the local geocoding and routing data
                try {
                    mLocator = Locator.createLocalLocator(extern + locatorPath);
                    mRouteTask = RouteTask.createLocalRouteTask(extern + networkPath, networkName);
                } catch (Exception e) {
                    Log.d(TAG, "Error while initializing :" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }).start();
    }


    class TouchListener extends MapOnTouchListener {

        private int routeHandle = -1;

        @Override
        public void onLongPress(MotionEvent point) {
            // Our long press will clear the screen
//            mStops.clearFeatures();
//            mGraphicsLayer.removeAll();
//            mMapView.getCallout().hide();
            mDismissOverlay.show();
        }

        @Override
        public boolean onSingleTap(MotionEvent point) {

            if (mLocator == null) {
                Log.d(TAG, "Locator uninitialized : " + true);
                return super.onSingleTap(point);
            }
            // Add a graphic to the screen for the touch event
            Point mapPoint = mMapView.toMapPoint(point.getX(), point.getY());
            Graphic graphic = new Graphic(mapPoint, new SimpleMarkerSymbol(Color.BLUE, 10, STYLE.DIAMOND));
            mGraphicsLayer.addGraphic(graphic);

            String stopAddress = "";
            try {
                // Attempt to reverse geocode the point.
                // Our input and output spatial reference will
                // be the same as the map.
                SpatialReference mapRef = mMapView.getSpatialReference();
                LocatorReverseGeocodeResult result = mLocator.reverseGeocode(mapPoint, 50, mapRef, mapRef);

                // Construct a nicely formatted address from the results
                StringBuilder address = new StringBuilder();
                if (result != null && result.getAddressFields() != null) {
                    Map<String, String> addressFields = result.getAddressFields();
                    address.append(String.format("%s\n%s, %s %s", addressFields.get("Street"), addressFields.get("City"),
                            addressFields.get("State"), addressFields.get("ZIP")));
                }

                // Show the results of the reverse geocoding in
                // the map's callout.
                stopAddress = address.toString();
                showCallout(stopAddress, mapPoint);

            } catch (Exception e) {
                Log.v("Reverse Geocode", e.getMessage());
            }

            // Add the touch event as a stop
            StopGraphic stop = new StopGraphic(graphic);
            stop.setName(stopAddress.toString());
            mStops.addFeature(stop);

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent point) {

            // Return default behavior if we did not initialize properly.
            if (mRouteTask == null) {
                Log.d(TAG,"RouteTask uninitialized : "+"true");
                return super.onDoubleTap(point);
            }

            try {

                // Set the correct input spatial reference on the stops and the
                // desired output spatial reference on the RouteParameters object.
                SpatialReference mapRef = mMapView.getSpatialReference();
                RouteParameters params = mRouteTask.retrieveDefaultRouteTaskParameters();
                params.setOutSpatialReference(mapRef);
                mStops.setSpatialReference(mapRef);

                // Set the stops and since we want driving directions,
                // returnDirections==true
                params.setStops(mStops);
                params.setReturnDirections(true);

                // Perform the solve
                RouteResult results = mRouteTask.solve(params);

                // Grab the results; for offline routing, there will only be one
                // result returned on the output.
                Route result = results.getRoutes().get(0);

                // Remove any previous route Graphics
                if (routeHandle != -1)
                    mGraphicsLayer.removeGraphic(routeHandle);

                // Add the route shape to the graphics layer
                Geometry geom = result.getRouteGraphic().getGeometry();
                routeHandle = mGraphicsLayer.addGraphic(new Graphic(geom, new SimpleLineSymbol(0x99990055, 5)));
                mMapView.getCallout().hide();

                // Get the list of directions from the result
                List<RouteDirection> directions = result.getRoutingDirections();

                // enable spinner to receive directions
//                dSpinner.setEnabled(true);

                // Iterate through all of the individual directions items and
                // create a nicely formatted string for each.
                List<String> formattedDirections = new ArrayList<String>();
                for (int i = 0; i < directions.size(); i++) {
                    RouteDirection direction = directions.get(i);
                    formattedDirections.add(String.format("%s\nGo %.2f %s For %.2f Minutes", direction.getText(),
                            direction.getLength(), params.getDirectionsLengthUnit().name(), direction.getMinutes()));
                }

                // Add a summary String
                formattedDirections.add(0, String.format("Total time: %.2f Mintues", result.getTotalMinutes()));

                // Create a simple array adapter to visualize the directions in
                // the Spinner
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_spinner_item, formattedDirections);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//                dSpinner.setAdapter(adapter);

                // Add a custom OnItemSelectedListener to the spinner to allow
                // panning to each directions item.
//                dSpinner.setOnItemSelectedListener(new DirectionsItemListener(directions));

            } catch (Exception e) {

                e.printStackTrace();
            }
            return true;
        }

        public TouchListener(Context context, MapView view) {
            super(context, view);
        }
    }

    private void showCallout(String text, Point location) {

        // If the callout has never been created, inflate it
        if (mCallout == null) {
            LayoutInflater inflater = (LayoutInflater) getApplication().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mCallout = inflater.inflate(R.layout.callout, null);
        }

        // Show the callout with the given text at the given location
        ((TextView) mCallout.findViewById(R.id.calloutText)).setText(text);
        mMapView.getCallout().show(location, mCallout);
        mMapView.getCallout().setMaxWidth(700);
    }


}
