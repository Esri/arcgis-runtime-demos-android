package com.esri.runtime.android.materialbasemaps.model;


import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class BasemapAdapter extends RecyclerView.Adapter<BasemapViewHolder> {

    // Context to construct view
    private Context mContext;
    // Copy of all BasemapItems
    private ArrayList<BasemapItem> items;
    // custom interface to handle item clicks
    private BasemapClickListener clickListener;

    private int rowLayout;

    public BasemapAdapter(ArrayList<BasemapItem> portalItems, int rowLayout, Context context){
        this.mContext = context;
        this.items = portalItems;
        this.rowLayout = rowLayout;
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

        BasemapViewHolder vh = new BasemapViewHolder(view);
        return vh;

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
                    clickListener.onImageClick(items.indexOf(item), item.getItem().getItemId());
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
