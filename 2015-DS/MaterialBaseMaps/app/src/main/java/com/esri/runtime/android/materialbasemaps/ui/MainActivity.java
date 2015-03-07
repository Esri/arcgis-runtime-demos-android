package com.esri.runtime.android.materialbasemaps.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalGroup;
import com.esri.core.portal.PortalInfo;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalItemType;
import com.esri.core.portal.PortalQueryParams;
import com.esri.core.portal.PortalQueryResultSet;
import com.esri.runtime.android.materialbasemaps.R;
import com.esri.runtime.android.materialbasemaps.model.BasemapAdapter;
import com.esri.runtime.android.materialbasemaps.model.BasemapClickListener;
import com.esri.runtime.android.materialbasemaps.model.BasemapItem;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity {

    @InjectView(R.id.list) RecyclerView mRecyclerView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;

    // create a portal object with null credentials
    Portal portal;

    private BasemapAdapter mBasemapAdapter;
    private ArrayList<BasemapItem> mBasemapList;
    private BasemapSearchAsyncTask mBasemapSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        mBasemapList = new ArrayList<BasemapItem>();

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // create an instance of adapter
        mBasemapAdapter = new BasemapAdapter( mBasemapList , R.layout.row_basemap, this);

        mBasemapAdapter.setOnBaseMapClickListener(new BasemapClickListener() {

            @Override
            public void onImageClick(int position, String itemId) {
                Context context = getApplicationContext();
                sendPortalId(context, itemId);

            }
        });

        mRecyclerView.setAdapter(mBasemapAdapter);

        // execute async task to fetch basemaps
        mBasemapSearch = new BasemapSearchAsyncTask();
        mBasemapSearch.execute();
    }

    public void sendPortalId(Context context, String portalId){
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra("portalId", portalId);
        startActivity(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(mBasemapList.size() == 0){
            mBasemapSearch = new BasemapSearchAsyncTask();
            mBasemapSearch.execute();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

    }


    private class BasemapSearchAsyncTask extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... params) {
            try{
                fetchBasemapItems();
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            // success
            mProgressBar.setVisibility(View.INVISIBLE);
            mBasemapAdapter.notifyDataSetChanged();

        }
    }

    /**
     * Fetches basemaps from arcgis.com portal
     *
     * @throws Exception
     */
    private void fetchBasemapItems() throws Exception {
        // create a new instance of portal
        portal = new Portal("http://www.arcgis.com", null);
        // get the information provided by portal
        PortalInfo portalInfo = portal.fetchPortalInfo();
        // get query to determine which basemap gallery group should be used in client
        String basemapGalleryGroupQuery = portalInfo.getBasemapGalleryGroupQuery();
        // create a PortalQueryParams from the basemap query
        PortalQueryParams portalQueryParams = new PortalQueryParams(basemapGalleryGroupQuery);
        // allow public search for basemaps
        portalQueryParams.setCanSearchPublic(true);
        // find groups for basemaps
        PortalQueryResultSet<PortalGroup> results = portal.findGroups(portalQueryParams);
        // get the basemap results
        List<PortalGroup> groupResults = results.getResults();

        List<PortalItem> queryResults = null;
        if (groupResults != null && groupResults.size() > 0) {
            PortalQueryParams queryParams = new PortalQueryParams();
            queryParams.setCanSearchPublic(true);
            queryParams.setLimit(12);
            String groupID = groupResults.get(0).getGroupId();
            queryParams.setQuery(PortalItemType.WEBMAP, groupID, null);
            queryParams.setSortField("name").setSortOrder(PortalQueryParams.PortalQuerySortOrder.ASCENDING);
            PortalQueryResultSet<PortalItem> queryResultSet = portal.findItems(queryParams);
            queryResults = queryResultSet.getResults();

            // loop through query results
            for (PortalItem item : queryResults) {
                // fetch the item from the server
                byte[] data = item.fetchThumbnail();
                if (data != null) {
                    // decode thumbnail and add this to list for display
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    BasemapItem portalItemData = new BasemapItem(item, bitmap);
                    mBasemapList.add(portalItemData);
                }
            }
        }
    }
}
