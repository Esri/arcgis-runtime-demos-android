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
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

/**
 * Provides a way for users to select an ArcGISFeatureLayer to be displayed and to
 * use for feature collection. Also supported adding new layers to the list and
 * deleting existing layers from the list.
 */
public class SelectionActivity extends AppCompatActivity {

  ListView mListView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_selection);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_selection);
    setSupportActionBar(toolbar);

    // Get the list view and populate it with the list of layers previously used
    mListView = (ListView) findViewById(R.id.listview);
    final HashMap<String, String> layers = FileUtil.getLayerMap();
    final List<String> nameList = new ArrayList<>(layers.keySet());
    final ListAdapter adapter = new ListAdapter(this, nameList);
    mListView.setAdapter(adapter);

    // Set an on click listener for list items, which will launch the CollectionActivity
    // with the selected layer name and URL
    mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // Get the layer name of the selected item
        String layerName = (String)mListView.getItemAtPosition(position);
        // Create a new intent to launch the CollectionActivity and put the
        // layer name and URL as extras
        Intent intent = new Intent(SelectionActivity.this, CollectionActivity.class);
        intent.putExtra("name", layerName);
        intent.putExtra("url", layers.get(layerName));
        startActivity(intent);
      }
    });

    // Set an on long click listener for list items, which will remove the selected layer
    // from the list
    mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
      @Override
      public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // Get the layer name of the selected item
        String layerName = (String)mListView.getItemAtPosition(position);
        // Remove that layer from the stored file
        FileUtil.removeLayer(layerName);
        // Also remove it from the list view and notify the adapter of the change
        nameList.remove(layerName);
        adapter.notifyDataSetChanged();
        return true;
      }
    });

    // Set an on click listener for the floating action button that will allow adding a new
    // layer to the list
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab_selection);
    fab.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

        // Inflate the add layer dialog view
        LayoutInflater inflater = SelectionActivity.this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layer_dialog, null);

        // Get the EditTexts for the layer name and URL
        final EditText layerName = (EditText) dialogView.findViewById(R.id.layer_name);
        final EditText layerUrl = (EditText) dialogView.findViewById(R.id.layer_url);

        // Create an on click listener for the dialog buttons
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            switch (which) {
              // If they click add, add a new layer with the specified name and URL
              case DialogInterface.BUTTON_POSITIVE:
                // Get the name of the new layer
                String name = layerName.getText().toString();
                // Add it to the file with its URL
                FileUtil.addLayer(name, layerUrl.getText().toString());
                // Add the name to the list view and notify the adapter of the change
                nameList.add(name);
                adapter.notifyDataSetChanged();
                break;
              // If the click cancel, just don't add the layer
              case DialogInterface.BUTTON_NEGATIVE:
              default:
                break;
            }
          }
        };

        // Apply the button texts and disable the positive button unless both layer name and URL have text
        final AlertDialog dialog = new AlertDialog.Builder(SelectionActivity.this).setPositiveButton(R.string.add, dialogClickListener)
                .setNegativeButton(R.string.cancel, dialogClickListener)
                .setView(dialogView)
                .show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);

        // Create a text watcher that will enable the Add button if both name and URL have text
        TextWatcher watcher = new TextWatcher() {
          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
          }
          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
          }
          @Override
          public void afterTextChanged(Editable s) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(layerName.getText().length()>0 && layerUrl.getText().length()>0);
          }
        };
        // Add the text changed listener to the EditTexts
        layerName.addTextChangedListener(watcher);
        layerUrl.addTextChangedListener(watcher);
      }
    });
  }

  @Override
  protected void onDestroy() {
    // When exiting, save any changes made to the layer file
    FileUtil.save();
    super.onDestroy();
  }
}
