package main.java.com.esri.test.glasssample;

/* Copyright 2012 ESRI
 *
 * All rights reserved under the copyright laws of the United States
 * and applicable international laws, treaties, and conventions.
 *
 * You may freely redistribute and use this sample code, with or
 * without modification, provided you include the original copyright
 * notice and use restrictions.
 *
 * See the Sample code usage restrictions document for further information.
 *
 */


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import java.util.ArrayList;
import java.util.List;


/**
 * The HelloWorld app is the most basic Map app for the ArcGIS Runtime SDK for Android. It shows how to define a MapView
 * in the layout XML of the activity. Within the XML definition of the MapView, MapOptions attributes are used to
 * populate that MapView with a basemap layer showing streets, and also the initial extent and zoom level are set. By
 * default, this map supports basic zooming and panning operations. This sample also demonstrates calling the MapView
 * pause and unpause methods from the Activity onPause and onResume methods, which suspend and resume map rendering
 * threads. A reference to the MapView is set within the onCreate method of the Activity which can be used at the
 * starting point for further coding.
 */

public class HelloWorld extends Activity {

  private final String TAG = "ArcGIS";
  private CardScrollAdapter mAdapter;
  private CardScrollView mCardScroller;

  // Called when the activity is first created.
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    startService(new Intent(HelloWorld.this, MapService.class));
//    mAdapter = new CardAdapter(createCards(this));
//    mCardScroller = new CardScrollView(this);
//    mCardScroller.setAdapter(mAdapter);
//    setContentView(mCardScroller);
//    setCardScrollerListener();
  }

  /**
   * Create list of API demo cards.
   */
  private List<View> createCards(Context context) {
    ArrayList<View> cards = new ArrayList<View>();
    cards.add(0, new CardBuilder(context, CardBuilder.Layout.TEXT)
            .setText(R.string.map_card).getView());
    return cards;
  }

  @Override
  protected void onPause() {
//    mCardScroller.deactivate();
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
//    mCardScroller.activate();
  }

  /**
   * Different type of activities can be shown, when tapped on a card.
   */
  private void setCardScrollerListener() {
    mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d(TAG, "Clicked view at position " + position + ", row-id " + id);
        int soundEffect = Sounds.TAP;
        switch (position) {

          case 0:
            startService(new Intent(HelloWorld.this, MapService.class));
            break;

          default:
            soundEffect = Sounds.ERROR;
            Log.d(TAG, "Don't show anything");
        }

        // Play sound.
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        am.playSoundEffect(soundEffect);
      }
    });
  }
}
