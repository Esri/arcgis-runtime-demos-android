package com.esri.arcgisruntime.geometrydemo;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.esri.arcgisruntime.mapping.BasemapType;
import com.esri.arcgisruntime.mapping.Map;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.RgbColor;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
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
        Map mMap = new Map(BasemapType.TOPOGRAPHIC, 34.056295, -117.195800, 16);
        // set the map to be displayed in this view
        mMapView.setMap(mMap);

        // create color and symbols for drawing graphics
        RgbColor blue = new RgbColor(0, 0, 255, 255);
        SimpleMarkerSymbol markerSymbol = new SimpleMarkerSymbol(blue, 14, SimpleMarkerSymbol.Style.TRIANGLE);

        GraphicsOverlay overlay = new GraphicsOverlay();
        Graphic pntGraphic = new Graphic(createPoint().getX(), createPoint().getY());
        pntGraphic.setSymbol(markerSymbol);
        mMapView.getGraphicsOverlays().add(overlay);
        overlay.getGraphics().add(pntGraphic);


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

    private Point createPoint(){
        Point point = new Point(34.056295, -117.195800);
        return point;
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
