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

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

/**
 * Implements a ListAdapter that uses an ArrayList of Strings.
 */
public class ListAdapter extends BaseAdapter {

  private Activity mContext;
  private List<String> mList;

  /**
   * Creates a new ListAdapter with the specified list of items.
   *
   * @param context the context used to create the item views
   * @param list the list of items to display
   */
  public ListAdapter(Activity context, List<String> list) {
    mContext = context;
    mList = list;
  }


  @Override
  public int getCount() {
    return mList.size();
  }

  @Override
  public Object getItem(int pos) {
    return mList.get(pos);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    ListViewItem viewHolder;
    // If the view is null, inflate a new item and set its text
    if (convertView == null) {
      LayoutInflater li = (LayoutInflater) mContext
              .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = li.inflate(R.layout.list_item, null);
      viewHolder = new ListViewItem(v);
      v.setTag(viewHolder);
    } else {
      viewHolder = (ListViewItem) v.getTag();
    }
    viewHolder.getItem().setText(mList.get(position));
    return v;
  }

  /**
   * Represents a ListView item that contains the TextView in which to display the item name.
   */
  private class ListViewItem {

    private TextView mItem;

    /**
     * Creates a new ListViewItem.
     *
     * @param base the base view which holds the TextView
     */
    public ListViewItem(View base) {
      mItem = (TextView) base.findViewById(R.id.item_name);
    }

    /**
     * Gets the TextView in which to display the item name.
     *
     * @return the TextView in which to display the item name
     */
    protected TextView getItem() {
      return mItem;
    }
  }
}
