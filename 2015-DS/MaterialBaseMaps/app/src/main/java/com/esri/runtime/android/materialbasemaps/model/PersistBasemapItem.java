package com.esri.runtime.android.materialbasemaps.model;


import java.util.ArrayList;
import java.util.HashMap;

public class PersistBasemapItem {

    private static PersistBasemapItem ourInstance = new PersistBasemapItem();

    public static PersistBasemapItem getInstance(){
        return ourInstance;
    }

    private PersistBasemapItem(){}

    public HashMap<String, ArrayList<BasemapItem>> storage = new HashMap<String, ArrayList<BasemapItem>>();

}
