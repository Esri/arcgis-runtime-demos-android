package com.esri.runtime.android.localgeofence;


import android.graphics.Bitmap;

/**
 * Defines the AlertItems and its properties in the cardview
 */
public class GeofenceAlertItem {
  String title;
  String featureName;
  String featureId;
  Bitmap thumbnail;

  boolean fetchingLocationUpdates = false;

  public GeofenceAlertItem(String title, String featureName, String featureId,Bitmap thumbnail,boolean fetchingLocationUpdates){
    this.title = title;
    this.featureName =  featureName;
    this.featureId = featureId;
    this.thumbnail = thumbnail;
    this.fetchingLocationUpdates = fetchingLocationUpdates;
  }


  public String getTitle() {
    return title;
  }

  public String getFeatureName() {
    return featureName;
  }

  public String getFeatureId() {
    return featureId;
  }

  public boolean isFetchingLocationUpdates() {
    return fetchingLocationUpdates;
  }
  public void setFetchingLocationUpdates(boolean fetchingLocationUpdates) {
    this.fetchingLocationUpdates = fetchingLocationUpdates;
  }



}
