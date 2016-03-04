/* Copyright 2016 Esri
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
