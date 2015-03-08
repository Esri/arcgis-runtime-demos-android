package com.esri.runtime.android.materialbasemaps.model;


/**
 * custom interface to be used in RecyclerView adapter
 */
public interface BasemapClickListener {
    /**
     * Callback when the view is clicked
     *
     * @param position position of the clicked basemap
     */
    public void onImageClick(int position, String itemID, String title);
}
