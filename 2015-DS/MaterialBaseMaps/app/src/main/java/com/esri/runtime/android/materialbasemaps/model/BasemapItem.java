package com.esri.runtime.android.materialbasemaps.model;


import android.app.Application;
import android.graphics.Bitmap;

import com.esri.core.portal.PortalItem;

public class BasemapItem {

    private PortalItem item;
    private Bitmap image;

    public BasemapItem(PortalItem item, Bitmap image){
        this.item = item;
        this.image = image;
    }

    public PortalItem getItem(){ return item; }

    public Bitmap getImage(){ return image; }

}
