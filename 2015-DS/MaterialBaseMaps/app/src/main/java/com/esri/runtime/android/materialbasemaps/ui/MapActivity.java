package com.esri.runtime.android.materialbasemaps.ui;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.esri.android.map.MapView;
import com.esri.core.portal.Portal;
import com.esri.core.portal.WebMap;
import com.esri.runtime.android.materialbasemaps.util.TaskExecutor;

import java.util.concurrent.Callable;

public class MapActivity extends Activity{

    final private String portalUrl = "http://www.arcgis.com";

    MapView mMapView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String itemId = intent.getExtras().getString("portalId");

        loadWebMapIntoMapView(itemId, portalUrl);

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
                            setContentView(mMapView);
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
