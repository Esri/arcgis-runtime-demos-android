package com.esri.runtime.android.materialbasemaps.model;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.esri.runtime.android.materialbasemaps.R;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * ViewHolder for BasemapAdapter required by RecyclerView for caching views
 */
public class BasemapViewHolder extends RecyclerView.ViewHolder {

    @InjectView(R.id.basemapName) TextView title;
    @InjectView(R.id.basemapImage) ImageView image;



    public BasemapViewHolder(View itemView) {
        super(itemView);

        ButterKnife.inject(this, itemView);

    }


}
