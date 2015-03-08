package com.esri.runtime.android.materialbasemaps.model;


/**
 * custom interface to be used in RecyclerView adapter
 */
public interface BasemapClickListener {
    /**
     * Callback when the view is clicked
     *
     * @param position position of the clicked basemap
     * @param itemID String portal item id representing basemap
     * @param title String representation of the basemap title
     */
    public void onImageClick(int position, String itemID, String title);
}
