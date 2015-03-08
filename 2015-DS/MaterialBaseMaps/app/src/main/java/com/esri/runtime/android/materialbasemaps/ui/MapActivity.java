package com.esri.runtime.android.materialbasemaps.ui;


import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.RelativeLayout;

import com.esri.android.map.MapView;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;
import com.esri.runtime.android.materialbasemaps.R;
import com.esri.runtime.android.materialbasemaps.util.TaskExecutor;

import java.util.concurrent.Callable;

public class MapActivity extends Activity{

    final private String portalUrl = "http://www.arcgis.com";

    MapView mMapView;
    RelativeLayout relativeMapLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);

        relativeMapLayout = (RelativeLayout) findViewById(R.id.relative);

        Intent intent = getIntent();
        String itemId = intent.getExtras().getString("portalId");

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        loadWebMapIntoMapView(itemId, portalUrl);

        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int size = getResources().getDimensionPixelSize(R.dimen.fab_size);
                outline.setOval(0, 0, size, size);
            }
        };
        findViewById(R.id.fab).setOutlineProvider(viewOutlineProvider);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate action bar menu
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivityForResult(intent, 0);

        return super.onOptionsItemSelected(item);
    }

    private void loadWebMapIntoMapView(final String portalItemId, final String portalUrl){
        TaskExecutor.getInstance().getThreadPool().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {

                Portal portal = new Portal(portalUrl, null);
                // load a webmap instance from portal item
                final WebMap webmap = WebMap.newInstance(portalItemId, portal);

                if(webmap != null){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMapView = new MapView(getApplicationContext(), webmap, null, null);

                            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                            mMapView.setLayoutParams(lp);
                            relativeMapLayout.addView(mMapView);

                            mMapView.enableWrapAround(true);
                            mMapView.setEsriLogoVisible(true);

                            mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
                                @Override
                                public void onStatusChanged(Object source, STATUS status) {
                                    if(mMapView == source && status == STATUS.INITIALIZED){
//                                        mMapView.addView(findViewById(R.id.fab));
                                    }
                                }
                            });

                        }
                    });
                }


                return null;
            }
        });
    }

    @Override
    protected void onResume() {

        super.onResume();
    }
}
