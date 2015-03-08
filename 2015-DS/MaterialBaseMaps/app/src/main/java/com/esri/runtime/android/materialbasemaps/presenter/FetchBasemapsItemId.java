package com.esri.runtime.android.materialbasemaps.presenter;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.esri.core.portal.Portal;
import com.esri.core.portal.PortalGroup;
import com.esri.core.portal.PortalInfo;
import com.esri.core.portal.PortalItem;
import com.esri.core.portal.PortalItemType;
import com.esri.core.portal.PortalQueryParams;
import com.esri.core.portal.PortalQueryResultSet;
import com.esri.runtime.android.materialbasemaps.model.BasemapItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Fetches the item id of a PortalItem.  Handles task cancellation by
 * checking for the threads interrupted state
 */
public class FetchBasemapsItemId implements Callable<Void>{

    // callback interface
    public OnTaskCompleted delegate = null;
    // create a portal object with null credentials
    Portal portal;
    private ArrayList<BasemapItem> mBasemapList;

    Activity activity;

    /**
     * Constructor to assign callback
     *
     * @param response assign callback
     */
    public FetchBasemapsItemId(Activity activity, OnTaskCompleted response){
        this.activity = activity;
        // assign callback interface through constructor
        delegate = response;
    }

    @Override
    public Void call() throws Exception {
        final ArrayList<BasemapItem> mBasemapList = new ArrayList<BasemapItem>(12);

        try {
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

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        delegate.processResults(mBasemapList);
                    }
                });


            }

        } catch (Exception e) {
            Log.d("TAG", e.getMessage());
        }


        return null;
    }
}
