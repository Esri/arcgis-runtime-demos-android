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

package com.esri.runtime.android.materialbasemaps.model;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class BasemapAdapter extends RecyclerView.Adapter<BasemapViewHolder> {

    // Copy of all BasemapItems
    private final ArrayList<BasemapItem> items;
    // custom interface to handle item clicks
    private BasemapClickListener clickListener;

    private final int rowLayout;

    public BasemapAdapter(ArrayList<BasemapItem> portalItems){
        this.items = portalItems;
        this.rowLayout = com.esri.runtime.android.materialbasemaps.R.layout.row_basemap;
    }

    /**
     * set the listener when image is clicked
     *
     * @param listener
     */
    public void setOnBaseMapClickListener(BasemapClickListener listener){ clickListener = listener; }

    @Override
    public BasemapViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        // create a new view
        View view = LayoutInflater.from(parent.getContext()).inflate(rowLayout, parent, false);

        return new BasemapViewHolder(view);

    }

    /**
     * Fill the view with data from our adapter
     *
     * @param viewHolder
     * @param position
     */
    @Override
    public void onBindViewHolder(BasemapViewHolder viewHolder, final int position){
        final BasemapItem item = items.get(position);
        // get the element from your dataset at this position
        // replace the contents of the view with the element
        viewHolder.image.setImageBitmap(items.get(position).getImage());
        viewHolder.title.setText(items.get(position).getItem().getTitle());

        viewHolder.title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(item != null){
                    clickListener.onImageClick(items.indexOf(item), item.getItem().getItemId(), item.getItem().getTitle());
                }
            }
        });

        viewHolder.itemView.setTag(item);
    }

    @Override
    public int getItemCount(){
        return items == null ? 0 : items.size();
    }



}
