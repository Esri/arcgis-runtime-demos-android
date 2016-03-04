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

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.support.wearable.view.WearableListView;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Represents the layout for an item in the WearableListView
 */
public class WearableListItemLayout extends LinearLayout
        implements WearableListView.OnCenterProximityListener {

  // ImageView of a circle that shows to the left of the text
  private ImageView mCircle;
  // TextView of the item to show
  private TextView mName;

  // Values for controlling the look of both the selected and unselected listview items
  private final float mFadedTextAlpha;
  private final int mFadedCircleColor;
  private final int mChosenCircleColor;

  public WearableListItemLayout(Context context) {
    this(context, null);
  }

  public WearableListItemLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  /**
   * Instantiates a WearableListItemLayout and defines the values that affect the look
   * of the selected and unselected list items.
   *
   * @param context
   * @param attrs
   * @param defStyle
   */
  public WearableListItemLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mFadedTextAlpha = getResources().getInteger(R.integer.action_text_faded_alpha) / 100f;
    mFadedCircleColor = getResources().getColor(R.color.grey);
    mChosenCircleColor = getResources().getColor(R.color.blue);
  }

  // Get references to the icon and text in the item layout definition
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    // These are defined in the layout file for list items
    // (see next section)
    mCircle = (ImageView) findViewById(R.id.circle);
    mName = (TextView) findViewById(R.id.name);
  }

  @Override
  public void onCenterPosition(boolean animate) {
    // When an item is in the center (selected), set the text to fully opaque
    // and the circle to the selected color
    mName.setAlpha(1f);
    ((GradientDrawable) mCircle.getDrawable()).setColor(mChosenCircleColor);
  }

  @Override
  public void onNonCenterPosition(boolean animate) {
    // When an item moves out of the center, set the circle color to the
    // unselected color and change the transparency of the text
    ((GradientDrawable) mCircle.getDrawable()).setColor(mFadedCircleColor);
    mName.setAlpha(mFadedTextAlpha);
  }

}
