/* Copyright 2016 Esri
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

package com.esri.runtime.android.materialbasemaps.presenter;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalGroup;
import com.esri.arcgisruntime.portal.PortalInfo;
import com.esri.arcgisruntime.portal.PortalItem;
import com.esri.arcgisruntime.portal.PortalQueryParameters;
import com.esri.arcgisruntime.portal.PortalQueryResultSet;
import com.esri.runtime.android.materialbasemaps.model.BasemapItem;

/**
 * Fetches the item id of a PortalItem.  Handles task cancellation by
 * checking for the threads interrupted state
 */
public class FetchBasemapsItemId implements Callable<Void>{

    private static final String TAG = FetchBasemapsItemId.class.getSimpleName();

    // callback interface
    private final OnTaskCompleted taskCompletedDelegate;
    private final Activity activity;
    private final String portalUrl;

    /**
     * Constructor to assign callback
     *
     * @param response assign callback
     */
    public FetchBasemapsItemId(Activity activity, String portalUrl, OnTaskCompleted response){
        this.activity = activity;
        this.portalUrl = portalUrl;
        // assign callback interface through constructor
        taskCompletedDelegate = response;
    }

    @Override
    public Void call() throws Exception {
        final ArrayList<BasemapItem> basemapList = new ArrayList<>(12);

        final Portal portal = new Portal("http://www.arcgis.com");
        portal.loadAsync();
        portal.addDoneLoadingListener(new Runnable() {
            @Override
            public void run() {
                if(portal.getLoadStatus() == LoadStatus.LOADED){
                    // get the information provided by portal
                    PortalInfo portalInfo = portal.getPortalInfo();
                    // get query to determine which basemap gallery group should be used in client
                    String basemapGalleryGroupQuery = portalInfo.getBasemapGalleryGroupQuery();
                    // create a PortalQueryParams from the basemap query
                    PortalQueryParameters portalQueryParams = new PortalQueryParameters(basemapGalleryGroupQuery);
                    // allow public search for basemaps
                    portalQueryParams.setCanSearchPublic(true);
                    // find groups for basemaps
                    final ListenableFuture<PortalQueryResultSet<PortalGroup>> results = portal.findGroupsAsync(portalQueryParams);
                    results.addDoneListener(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // get the basemap results
                                PortalQueryResultSet<PortalGroup> groupResults = results.get();
                                if (groupResults != null && groupResults.getResults().size() > 0) {
                                    PortalQueryParameters queryParams = new PortalQueryParameters();
                                    queryParams.setCanSearchPublic(true);
                                    queryParams.setLimit(12);

                                    List group = groupResults.getResults();
                                    PortalGroup pGroup = (PortalGroup)group.get(0);

                                    String groupID = pGroup.getGroupId();
                                    queryParams.setQuery(PortalItem.Type.WEBMAP, groupID, null);
                                    queryParams.setSortField("name").setSortOrder(PortalQueryParameters.SortOrder.ASCENDING);

                                    final ListenableFuture<PortalQueryResultSet<PortalItem>> queryResultSet = portal.findItemsAsync(queryParams);
                                    queryResultSet.addDoneListener(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                PortalQueryResultSet<PortalItem> queryResults = queryResultSet.get();
                                                // loop through query results
                                                for (final PortalItem item : queryResults.getResults()) {
                                                    // fetch the thumbnail
                                                    final ListenableFuture<byte[]> thumbnailFuture = item.fetchThumbnailAsync();
                                                    thumbnailFuture.addDoneListener(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            // get the thumbnail image data
                                                            byte[] itemThumbnailData;
                                                            try {
                                                                itemThumbnailData = thumbnailFuture.get();
                                                                if ((itemThumbnailData != null) && (itemThumbnailData.length > 0)) {
                                                                    // create a Bitmap to use as required
                                                                    Bitmap bitmap = BitmapFactory.decodeByteArray(itemThumbnailData, 0, itemThumbnailData.length);
                                                                    BasemapItem portalItemData = new BasemapItem(item, bitmap);
                                                                    basemapList.add(portalItemData);
                                                                }
                                                            } catch(InterruptedException | ExecutionException e) {
                                                                Log.d(TAG, e.getMessage());
                                                            }
                                                        }
                                                    });
                                                }
                                            } catch (InterruptedException | ExecutionException e) {
                                                Log.d(TAG, e.getMessage());
                                            }
                                        }
                                    });
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            taskCompletedDelegate.processResults(basemapList);
                                        }
                                    });
                                }
                            } catch (InterruptedException | ExecutionException e) {
                                Log.d(TAG, e.getMessage());
                            }
                        }
                    });
                } else {
                    Log.d(TAG, portal.getLoadError().getCause().getMessage());
                }

            }
        });

        return null;
    }
}
