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

package runtime.esri.com.trafficapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.esri.core.map.Graphic;

import java.util.ArrayList;

/**
 * Created by will6489 on 3/4/15.
 * <p/>
 * Credit to : http://www.myandroidsolutions.com/2015/01/01/android-floating-action-button-fab-tutorial/
 */
public class IncidentAdapter extends ArrayAdapter<Graphic> {


    private ArrayList<Graphic> mListItems;
    private LayoutInflater mLayoutInflater;


    public IncidentAdapter(Context context, ArrayList<Graphic> items) {
        super(context, 0, items);
        mListItems = items;

        //get the layout inflater
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        // create a ViewHolder reference
        ViewHolder holder;

        //check to see if the reused view is null or not, if is not null then reuse it
        if (view == null) {
            holder = new ViewHolder();

            view = mLayoutInflater.inflate(R.layout.list_item, null);
            holder.textView = (TextView) view.findViewById(R.id.list_item_text_view);

            // the setTag is used to store the data within this view
            view.setTag(holder);
        } else {
            // the getTag returns the viewHolder object set as a tag to the view
            holder = (ViewHolder) view.getTag();
        }

        //get the string item from the position "position" from array list to put it on the TextView
        //TODO add different icon depending on incident type and severity:

/*
Typical incident result is:
"features": [
  {
   "attributes": {
    "objectid": 1495,
    "severity": "critical",
    "incidenttype": "CONSTRUCTION",
    "location": "Angeles Crest Hwy SOUTHBOUND",
    "description": "Closed at CA-39/N San Gabriel Canyon Rd - Closed due to roadwork.",
    "fulldescription": "Closed at CA-39/N San Gabriel Canyon Rd - Closed due to roadwork. due to winter weather conditions",
    "start_localtime": 1424946407000,
    "end_localtime": 1433048400000,
    "lastupdated_localtime": 1424946407000,
    "start_utctime": 1425004007000,
    "end_utctime": 1433098800000,
    "lastupdated_utctime": 1424975207000
   },
   "geometry": {
    "x": -13120065,
    "y": 4076709
   }
  }
 ]
 */

        String description =  (String) mListItems.get(position).getAttributeValue("description");

        if (description != null) {
            if (holder.textView != null) {
                holder.textView.setText(description);
            }
        }

        //this method must return the view corresponding to the data at the specified position.
        return view;
    }


    /**
     * Static class used to avoid the calling of "findViewById" every time the getView() method is called,
     * because this can impact to your application performance when your list is too big. The class is static so it
     * cache all the things inside once it's created.
     */
    private static class ViewHolder {

        protected TextView textView;

    }
}
