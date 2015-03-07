package com.esri.runtime.android.materialbasemaps.presenter;


import com.esri.runtime.android.materialbasemaps.model.BasemapItem;

import java.util.ArrayList;

/**
 * Interface to process response from an operation on a background thread
 */
public interface OnTaskCompleted {
    /**
     * Return BasemapItems
     */
    void processResults(ArrayList<BasemapItem> basemapItems);
}
