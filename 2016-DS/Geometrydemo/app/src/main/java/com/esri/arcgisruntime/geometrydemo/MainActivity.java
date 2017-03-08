package com.esri.arcgisruntime.geometrydemo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.core.geometry.GeoJsonImportFlags;
import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.OperatorImportFromGeoJson;
import com.esri.core.geometry.Point;

public class MainActivity extends AppCompatActivity {

    private MapView mMapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Point created with geometry api", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // inflate MapView from layout
        mMapView = (MapView) findViewById(R.id.mapView);
        // create a map with the BasemapType topographic
        ArcGISMap mMap = new ArcGISMap(Basemap.Type.NAVIGATION_VECTOR, 47.609201, -122.331597, 14);
        // set the map to be displayed in this view
        mMapView.setMap(mMap);

        try{
            // create 6 station points from geojson string
            String station1 = "{\"type\":\"Point\",\"coordinates\":[-122.32799470424652,47.59850568873259],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station1Pnt = createPointFromGeoJson(station1);
            String station2 = "{\"type\":\"Point\",\"coordinates\":[-122.33112752437592,47.602473763740484],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station2Pnt = createPointFromGeoJson(station2);
            String station3 = "{\"type\":\"Point\",\"coordinates\":[-122.33570337295531,47.60749401439728],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station3Pnt = createPointFromGeoJson(station3);
            String station4 = "{\"type\":\"Point\",\"coordinates\":[-122.33618617057799,47.61182666756116],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station4Pnt = createPointFromGeoJson(station4);
            String station5 = "{\"type\":\"Point\",\"coordinates\":[-122.32020020484924,47.61901562056099],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station5Pnt = createPointFromGeoJson(station5);
            String station6 = "{\"type\":\"Point\",\"coordinates\":[-122.30383872985841,47.64981422491055],\"crs\":\"EPSG:4326\"}";
            com.esri.core.geometry.Point station6Pnt = createPointFromGeoJson(station6);
            // create color and symbols for drawing graphics
            SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.TRIANGLE, Color.BLUE, 14);

            // create a graphics overlay to display stations
            GraphicsOverlay overlay = new GraphicsOverlay();
            // create station graphics
            Graphic station1Graphic = new Graphic(station1Pnt.getY(), station1Pnt.getX());
            Graphic station2Graphic = new Graphic(station2Pnt.getY(), station2Pnt.getX());
            Graphic station3Graphic = new Graphic(station3Pnt.getY(), station3Pnt.getX());
            Graphic station4Graphic = new Graphic(station4Pnt.getY(), station4Pnt.getX());
            Graphic station5Graphic = new Graphic(station5Pnt.getY(), station5Pnt.getX());
            Graphic station6Graphic = new Graphic(station6Pnt.getY(), station6Pnt.getX());
            // set the maker symbols
            station1Graphic.setSymbol(markerSymbol);
            station2Graphic.setSymbol(markerSymbol);
            station3Graphic.setSymbol(markerSymbol);
            station4Graphic.setSymbol(markerSymbol);
            station5Graphic.setSymbol(markerSymbol);
            station6Graphic.setSymbol(markerSymbol);
            // add the graphics to the overlay
            mMapView.getGraphicsOverlays().add(overlay);
            overlay.getGraphics().add(station1Graphic);
            overlay.getGraphics().add(station2Graphic);
            overlay.getGraphics().add(station3Graphic);
            overlay.getGraphics().add(station4Graphic);
            overlay.getGraphics().add(station5Graphic);
            overlay.getGraphics().add(station6Graphic);
        }catch(Exception e){
            Log.d("Exception", e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Converts a geojson string to com.esri.core.geometry.Point.
     *
     * @param jsonPoint geoJson string representation of a Point
     * @return com.esri.core.geometry.Point
     * @throws Exception
     */
    static Point createPointFromGeoJson(String jsonPoint) throws Exception {

        MapGeometry mapGeom = OperatorImportFromGeoJson.local().execute(GeoJsonImportFlags.geoJsonImportDefaults,
                Geometry.Type.Point,
                jsonPoint,
                null);

        return (Point) mapGeom.getGeometry();
    }

    @Override
    protected void onPause(){
        super.onPause();
        mMapView.pause();
    }

    @Override
    protected void onResume(){
        super.onResume();
        mMapView.resume();
    }
}
