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

package com.esri.runtime.android.materialbasemaps.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ProgressBar;

import com.esri.runtime.android.materialbasemaps.R;
import com.esri.runtime.android.materialbasemaps.model.BasemapAdapter;
import com.esri.runtime.android.materialbasemaps.model.BasemapClickListener;
import com.esri.runtime.android.materialbasemaps.model.BasemapItem;
import com.esri.runtime.android.materialbasemaps.model.PersistBasemapItem;
import com.esri.runtime.android.materialbasemaps.presenter.FetchBasemapsItemId;
import com.esri.runtime.android.materialbasemaps.presenter.OnTaskCompleted;
import com.esri.runtime.android.materialbasemaps.util.TaskExecutor;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity{

    @InjectView(R.id.list) RecyclerView mRecyclerView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;

    private BasemapAdapter mBasemapAdapter;
    private ArrayList<BasemapItem> mBasemapList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // inject our progress bar and recycler view
        ButterKnife.inject(this);
        // array of basemap items to have available to load as basemaps
        mBasemapList = new ArrayList<>();

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // create an instance of adapter
        mBasemapAdapter = new BasemapAdapter( mBasemapList);

        // click listener to send portal id to MapActivity
        mBasemapAdapter.setOnBaseMapClickListener(new BasemapClickListener() {

            @Override
            public void onImageClick(int position, String itemId, String title) {
                Context context = getApplicationContext();
                sendBasemapItemInfo(context, itemId, title);
            }
        });

        mRecyclerView.setAdapter(mBasemapAdapter);

        // If basemap item is persisted do not got out to service to fetch them again
        if(PersistBasemapItem.getInstance().storage.get("basemap-items") != null){
            // populate basemapItems with persited BasemapItems
            ArrayList<BasemapItem> basemapItems = PersistBasemapItem.getInstance().storage.get("basemap-items");
            mBasemapList.clear();
            mBasemapList.addAll(basemapItems);
            mBasemapAdapter.notifyDataSetChanged();
        }else {
            // turn on progress bar while searching basemaps
            mProgressBar.setVisibility(View.VISIBLE);
            // search and collect basemap portal ids on background thread
            fetchBasemaps();
        }

    }

    /**
     * Retrieve basemaps portal item id to sent to MapActivity
     */
    private void fetchBasemaps(){
        TaskExecutor.getInstance().getThreadPool().submit(new FetchBasemapsItemId(this, new OnTaskCompleted() {
            @Override
            public void processResults(ArrayList<BasemapItem> basemapItems) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mBasemapList.clear();
                mBasemapList.addAll(basemapItems);
                mBasemapAdapter.notifyDataSetChanged();
                PersistBasemapItem.getInstance().storage.put("basemap-items", basemapItems);
            }
        }));

    }

    /**
     * Intent to send to MapActivity
     *
     * @param context application context
     * @param portalId portal id representing the basemap to open
     * @param title basemap title
     */
    private void sendBasemapItemInfo(Context context, String portalId, String title){
        Intent intent = new Intent(context, MapActivity.class);
        intent.putExtra("portalId", portalId);
        intent.putExtra("title", title);
        // create activity animation
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this, mRecyclerView, "title_transition");
        startActivity(intent, options.toBundle());
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if(mBasemapList.size() == 0){
            fetchBasemaps();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

    }

}
