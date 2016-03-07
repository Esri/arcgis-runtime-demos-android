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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import android.util.Log;

/**
 * Provides convenience methods for saving and loading the previous list of layers
 * that were used in the app.
 */
public class FileUtil {

  // The name of the file to save/load
  private static final String FILE_NAME = "layers";
  private static final String FILE_EXTENSION = ".ser";
  private static File sSerFile;

  // The HashMap of layer names to their URLs
  private static HashMap<String, String> sLayers = null;

  /**
   * Gets the HashMap of layer names to their URLs.
   *
   * @return the HashMap of layer names to URLs
   */
  public static HashMap<String, String> getLayerMap() {
    // If the layer list is null, load them from the file
    if (sLayers == null) {
      load();
      if (sLayers.size() == 0) {
        sLayers.put("Recreation", "http://sampleserver6.arcgisonline.com/arcgis/rest/services/LocalGovernment/Recreation/FeatureServer/0");
      }
    }
    return sLayers;
  }

  /**
   * Adds a new layer to the HashMap.
   *
   * @param layerName the name of the layer to add
   * @param url the URL of the layer to add
   */
  public static void addLayer(String layerName, String url) {
    sLayers.put(layerName, url);
  }

  /**
   * Removes a layer from the HashMap.
   *
   * @param layerName the name of the layer to remove
   */
  public static void removeLayer(String layerName) {
    sLayers.remove(layerName);
  }

  /**
   * Saves the current state of the HashMap to storage or loading on the next app session.
   */
  protected static void save() {
    FileOutputStream fos = null;
    ObjectOutputStream oos = null;
    try {
      // Try to open a file output stream to the file and serialize the HashMap to it.
      fos = new FileOutputStream(sSerFile);
      oos = new ObjectOutputStream(fos);
      oos.writeObject(sLayers);
    } catch (Exception e) {
      Log.e("Test", "Could no save feature layer list: " + e.getMessage());
    } finally {
      // Make sure the streams get closed
      try {
        if (fos != null) {
          fos.close();
        }
        if (oos != null) {
          oos.close();
        }
      } catch (Exception e) {
        // Tried to close files
      }
    }
  }

  /**
   * Loads the previous state of the HashMap from storage.
   */
  private static void load() {
    FileInputStream fis = null;
    ObjectInputStream ois = null;
    try {
      // Check if the file already exists
      sSerFile = File.createTempFile(FILE_NAME, FILE_EXTENSION);
      if (sSerFile.exists()) {
        // If it does, read the file and deserialize the HashMap
        fis = new FileInputStream(sSerFile);
        ois = new ObjectInputStream(fis);
        sLayers = (HashMap) ois.readObject();
      } else {
        // Otherise create a new blank HashMap
        sLayers = new HashMap<>();
      }
    } catch (Exception ie) {
      // If there's an exception, just create a blank HashMap
      sLayers = new HashMap<>();
    } finally {
      // Make sure the streams get closed
      try {
        if (fis != null) {
          fis.close();
        }
        if (ois != null) {
          ois.close();
        }
      } catch (Exception e) {
        // Tried to close files
      }
    }
  }
}
