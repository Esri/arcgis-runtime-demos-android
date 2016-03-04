/*
 * COPYRIGHT 1995-2016 ESRI
 *
 * TRADE SECRETS: ESRI PROPRIETARY AND CONFIDENTIAL
 * Unpublished material - all rights reserved under the
 * Copyright Laws of the United States.
 *
 * For additional information, contact:
 * Environmental Systems Research Institute, Inc.
 * Attn: Contracts Dept
 * 380 New York Street
 * Redlands, California, USA 92373
 *
 * email: contracts@esri.com
 */

package com.esri.arcgisruntime.wearcollection;

import java.util.ArrayList;

import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.core.map.FeatureType;

/**
 * Provides a utility method for ArcGISFeatureLayers.
 */
public class FeatureLayerUtil {

  /**
   * Gets the display names of the FeatureTypes contained in the specified ArcGISFeatureLayer.
   *
   * @param layer the layer from which to get the FeatureType names
   * @return the list of FeatureType display names
   */
  public static ArrayList<String> getFeatureTypes(ArcGISFeatureLayer layer) {
    FeatureType[] types = layer.getTypes();
    ArrayList<String> featureTypeNames = new ArrayList<>(types.length);
    for (FeatureType type : types) {
      featureTypeNames.add(type.getName());
    }
    return featureTypeNames;
  }
}
