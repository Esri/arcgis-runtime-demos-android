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
