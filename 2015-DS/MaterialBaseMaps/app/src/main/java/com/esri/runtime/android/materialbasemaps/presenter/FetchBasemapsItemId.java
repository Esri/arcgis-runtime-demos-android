package com.esri.runtime.android.materialbasemaps.presenter;


import com.esri.runtime.android.materialbasemaps.model.BasemapItem;

import java.util.ArrayList;
import java.util.concurrent.Callable;

/**
 * Fetches the item id of a PortalItem.  Handles task cancellation by
 * checking for the threads interrupted state
 */
public class FetchBasemapsItemId implements Callable<Void>{

    private ArrayList<BasemapItem> mBasemapList;

    @Override
    public Void call() throws Exception {




        return null;
    }
}
