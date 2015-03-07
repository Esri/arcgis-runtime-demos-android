package com.esri.runtime.android.materialbasemaps.ui;

import android.app.Activity;
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
import com.esri.runtime.android.materialbasemaps.presenter.BasemapSearchPresenter;
import com.esri.runtime.android.materialbasemaps.presenter.OnTaskCompleted;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;


public class MainActivity extends Activity{

    @InjectView(R.id.list) RecyclerView mRecyclerView;
    @InjectView(R.id.progressBar) ProgressBar mProgressBar;

    private BasemapAdapter mBasemapAdapter;
    private ArrayList<BasemapItem> mBasemapList;
    private BasemapSearchPresenter mBasemapSearch;

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
        mProgressBar.setVisibility(View.VISIBLE);
        fetchBasemaps();

    }

    public void fetchBasemaps(){
        mBasemapSearch = new BasemapSearchPresenter(new OnTaskCompleted() {
            @Override
            public void processResults(ArrayList<BasemapItem> basemapItems) {
                mProgressBar.setVisibility(View.INVISIBLE);
                mBasemapList.clear();
                mBasemapList.addAll(basemapItems);
                mBasemapAdapter.notifyDataSetChanged();
            }
        });
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
            fetchBasemaps();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

    }
}
