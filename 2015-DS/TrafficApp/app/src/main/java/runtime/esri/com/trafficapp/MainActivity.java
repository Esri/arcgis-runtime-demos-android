package runtime.esri.com.trafficapp;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geometry.Point;
import com.esri.core.io.UserCredentials;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.map.TimeExtent;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    MapView mMapView;
    ArcGISDynamicMapServiceLayer mTrafficLayer;
    GraphicsLayer mGraphicLayer;
    UserCredentials mUserCredentials;
    ListView mListView;
    ImageButton mFabImageButton;
    boolean isIncidentListVisible = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.trafficmap_activity);

        //TODO YOU MUST SET THIS YOURSELF TO RUN THIS!
        ArcGISRuntime.setClientId(XXXXXXXXXXXXXXXX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.myColorPrimaryDark));
        }

        //set up support toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.traffic_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_action_ic_menu_24px);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        //set up action button
        mFabImageButton = (ImageButton) findViewById(R.id.fab_image_button);
        mFabImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isIncidentListVisible) {
                    showCalendar();
                } else {
                    showIncidentList();
                }
            }
        });


        //create credentials
        //TODO YOU MUST ADD VALID USERNAME AND PASSWORD OF AN ORG USER TO RUN THIS APP
        mUserCredentials = new UserCredentials();
        mUserCredentials.setUserAccount(XXX, XXX);

        //setup map
        FrameLayout mapFrame = (FrameLayout) findViewById(R.id.map_frame);
        String mapUrl = getResources().getString(R.string.webmap_url);
        mMapView = new MapView(getApplicationContext(), mapUrl, mUserCredentials, null, null);
        mapFrame.addView(mMapView);


        //wait for mapview to load then set zoom and query for incidents
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            @Override
            public void onStatusChanged(Object source, STATUS status) {

                //Once map is loaded zoom to location
                if (source == mMapView && status == STATUS.INITIALIZED) {

                    //Zoom to location
                    Point point = new Point(-13046162, 4036352);
                    mMapView.zoomToScale(point, 100000);

                    //assume we do a geocode here for the location, currently hardcoded to redlands
                    //LA incident 34.00, -118.31
                    //Point point = new Point(-13173005, 4030270);
                    //"34.056215, -117.195668" / -13046162, 4036352



                    Log.d("T_APP", "map initialized");

                    AsyncQueryTask ayncQuery = new AsyncQueryTask();
                    ayncQuery.execute();

                    //Get traffic layer
                    //TODO should try and use the layer name or ID here, as if map changes then this will break, current bug prevents this
                    Layer[] layers = mMapView.getLayers();
                    mTrafficLayer = (ArcGISDynamicMapServiceLayer) layers[1];

                    //TODO why have to do this here?
                    mGraphicLayer = new GraphicsLayer();
                    mMapView.addLayer(mGraphicLayer);

                }
            }
        });

        //TODO set up retain instance methods

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_search:
                //TODO implement search for another location
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Query Task executes asynchronously.
     */
    private class AsyncQueryTask extends AsyncTask<String, Void, FeatureResult> {

        @Override
        protected void onPreExecute() {
            //TODO add progress spinner in toolbar
        }


        @Override
        protected FeatureResult doInBackground(String... params) {

            QueryParameters qParameters = new QueryParameters();
            qParameters.setGeometry(mMapView.getExtent());
            qParameters.setInSpatialReference(mMapView.getSpatialReference());
            qParameters.setOutSpatialReference(mMapView.getSpatialReference());
            qParameters.setOutFields(new String[]{"*"});
            qParameters.setReturnGeometry(true);
            qParameters.setSpatialRelationship(SpatialRelationship.INTERSECTS);

            try {

                QueryTask qTask = new QueryTask("http://traffic.arcgis.com/arcgis/rest/services/World/Traffic/MapServer/2", mUserCredentials);

                FeatureResult results = qTask.execute(qParameters);

                Log.d("T_APP", "query executed");

                return results;
            } catch (Exception e) {
                Log.e("T_APP", "nasty", e);
                e.printStackTrace();
            }
            return null;

        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            Log.d("T_APP", "results returned");

            //relate the list view from java to the one created in xml
            mListView = (ListView) findViewById(R.id.incidents_listview);
            final ArrayList<Graphic> list = new ArrayList<>();
            final IncidentAdapter adapter = new IncidentAdapter(MainActivity.this, list);

            if (results != null && results.featureCount() > 0) {
                int size = (int) results.featureCount();

                Log.d("T_APP", "we have " + size + " results!");

                //add the results
                for (Object element : results) {
                    if (element instanceof Feature) {
                        Feature feature = (Feature) element;
                        Graphic graphic = new Graphic(feature.getGeometry(), null, feature.getAttributes());
                        //check for valid incidents which have a full description
                        if (graphic.getAttributeValue("description") != null) {
                            list.add(graphic);
                        }
                    }
                }

            } else {
                Log.d("T_APP", "no incident results");
            }

            //if there are no valid results then add an empty result
            if (list.size() == 0) {
                Map attributes = new HashMap<String, String>();
                attributes.put("description", "No results");
                list.add(new Graphic(null, null, attributes));
            }

            adapter.notifyDataSetChanged();
            mListView.setAdapter(adapter);

            //set the on click listener for the list view
            mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    //clear graphics layer
                    mGraphicLayer.removeAll();

                    //get the graphic
                    Graphic graphic = adapter.getItem(position);

                    //zoom the map to the graphic (they are already in the right SR)
                    mMapView.zoomToScale((Point) graphic.getGeometry(), 50000);
                    Graphic markerGraphic = new Graphic(graphic.getGeometry(), new SimpleMarkerSymbol(Color.CYAN, 15, SimpleMarkerSymbol.STYLE.CROSS));
                    mGraphicLayer.addGraphic(markerGraphic);

                }
            });

        }

    }

    private void showCalendar() {

        isIncidentListVisible = false;

        //clear the context frame layout
        ViewGroup contextFrame = (ViewGroup) findViewById(R.id.context_frame);
        contextFrame.removeAllViews();

        //change the floating action button icon
        mFabImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_ic_history_24px));

        //show the date picker
        View contextLayoutDatePicker = getLayoutInflater().inflate(R.layout.context_layout_datepicker, contextFrame);
        DatePicker datePicker = (DatePicker) contextFrame.findViewById(R.id.date_picker);
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        datePicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Log.d("T_APP", "onDateChanged");
                setMapTimeExtent(year, monthOfYear, dayOfMonth);
            }
        });
    }

    private void showIncidentList() {

        isIncidentListVisible = true;

        //clear graphics layer
        mGraphicLayer.removeAll();

        //clear time on the map
        mTrafficLayer.setTimeInterval(null);

        //clear out the context frame layout
        ViewGroup contextFrame = (ViewGroup) findViewById(R.id.context_frame);
        contextFrame.removeAllViews();

        //change the floating action button icon
        mFabImageButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_ic_grid_on_24px));

        //show the incidents list
        View contextLayoutIncidents = getLayoutInflater().inflate(R.layout.context_layout_incidents, contextFrame);

        //do another query based on the map extent
        AsyncQueryTask ayncQuery = new AsyncQueryTask();
        ayncQuery.execute();


    }

    private void setMapTimeExtent(int year, int monthOfYear, int dayOfMonth) {

        //clear graphics layer
        mGraphicLayer.removeAll();

        Log.d("T_APP", "setMapTimeExtent");


        Calendar newDate = Calendar.getInstance();
        newDate.set(Calendar.YEAR, year);
        newDate.set(Calendar.MONTH, monthOfYear);
        newDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        TimeExtent timeInterval = new TimeExtent(newDate);
        mTrafficLayer.setTimeInterval(timeInterval);

    }

}
