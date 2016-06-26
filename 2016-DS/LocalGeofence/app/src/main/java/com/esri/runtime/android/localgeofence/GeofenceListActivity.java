/* Copyright 2015 Esri
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

package com.esri.runtime.android.localgeofence;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.tasks.query.QueryParameters;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * A ListActivity to show the display field names of all the features in the
 * opened GeodatabaseFeatureTable, and return as an Activity result the
 * object ID of the selected feature.
 */
public class GeofenceListActivity extends ListActivity {

  private static final String TAG = GeofenceListActivity.class.getSimpleName();

  String[] mFeatureNames = null;

  // A sorted TreeMap sorts features by name on the client.
  TreeMap<Long,String> mFeatures = new TreeMap<Long,String>();


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_list);

    mFeatureNames = getFeatures();
  }

  public String[] getFeatures() {

    // Prepare a query to get features from the geodatabase.
    QueryParameters queryParams = new QueryParameters();
    queryParams.setOutFields(new String[]{MainActivity.FENCE_NAME_FIELD,
        MainActivity.FENCE_OBJECTID_FIELD});
    queryParams.setWhere("1 > 0"); // Select all features.

    // Perform the query.
    Future<FeatureResult> featureTableFuture = MainActivity.mGdbFeatureTable.
        queryFeatures(queryParams, null);
    FeatureResult result = null;
    try {
      result = featureTableFuture.get();
      if ( (result != null) && (result.featureCount() > 1) ) {
        // Iterate the results.
        for (Object objFeature : result) {
          Feature feature = (Feature) objFeature;
          // Get display field values to display in the list.
          String fName = feature.getAttributeValue(MainActivity.FENCE_NAME_FIELD).toString();
          Long fOid = feature.getId();
          mFeatures.put(fOid, fName);
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }

    // Get all values and keys from the map.
    String[] valuesAsArray = new String[mFeatures.size()];
    mFeatures.values().toArray(valuesAsArray);
    Long[] keysAsArray = new Long[mFeatures.size()];
    mFeatures.keySet().toArray(keysAsArray);

    // Set the array adapter to show the values.
    ArrayAdapter<String> arrayAdapter =
        new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, valuesAsArray);
    setListAdapter(arrayAdapter);

    return valuesAsArray;

  }

  @Override
  protected void onListItemClick(ListView l, View v, int position, long id) {
    // Get the tapped item.
    String selectedItem = l.getItemAtPosition(position).toString();

    // Get ObjectID key for the string value...
    Set<Long> keys = new HashSet<Long>();
    Long objectId = -1L;
    if (mFeatures.containsValue(selectedItem)) {

      for (Map.Entry<Long, String> entry : mFeatures.entrySet()) {
        if (entry.getValue().equals(selectedItem)) {
          keys.add(entry.getKey());
        }
      }
      for (Long key : keys) {
        objectId = key;
      }
    }

    // Return the selected feature ID as an activity result.
    if (objectId > -1) {
      Intent resultIntent = new Intent();
      resultIntent.putExtra(MainActivity.GEOFENCE_FEATURE_OBJECTID_EXTRA_ID, objectId);
      setResult(RESULT_OK, resultIntent);
      finish();
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_list, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    if (id == R.id.cancel_menu_item) {
      return true;
    }

    return super.onOptionsItemSelected(item);
  }


}
