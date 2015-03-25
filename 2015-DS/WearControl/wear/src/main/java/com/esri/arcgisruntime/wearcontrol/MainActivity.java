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

package com.esri.arcgisruntime.wearcontrol;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

  private GoogleApiClient mGoogleApiClient;
  private DismissOverlayView mDismissOverlayView;
  private GestureDetector mGestureDetector;

  private final static String MESSAGE_PATH = "/messages/test";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);


    initGoogleApiClient();
    mDismissOverlayView = (DismissOverlayView)findViewById(R.id.dismiss);
    mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {

      @Override
      public void onLongPress(MotionEvent e) {
        mDismissOverlayView.show();
      }

//      @Override
//      public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX, final float distanceY) {
//        int pointers = e2.getPointerCount();
//        float x = e2.getX();
//        float y = e2.getY();
////        sendMessage(String.format("Finger count: %d\nPosX: %f\nPosY: %f", pointers, x, y).getBytes());
//        return true;
//      }
    });
  }

  private void sendEvent(final byte[] message) {
    if (mGoogleApiClient.isConnected()) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
          for (Node node : nodes.getNodes()) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), MESSAGE_PATH, message).await();
            if (!result.getStatus().isSuccess()) {
              Log.e("test", "error");
            } else {
              Log.i("test", "success!! sent to: " + node.getDisplayName());
            }
          }
        }
      }).start();
    } else {
      Log.e("test", "google api not connected");
    }
  }

  @Override
  public boolean dispatchTouchEvent(@NonNull MotionEvent event) {
    boolean wasLongPress = mGestureDetector.onTouchEvent(event);
    if(!wasLongPress) {
      Log.i("test", "not a long press");
      if(!mDismissOverlayView.isShown()) {
        Parcel p = Parcel.obtain();
        event.writeToParcel(p, Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
        sendEvent(p.marshall());
      } else {
        return super.dispatchTouchEvent(event);
      }
    } else {
      Log.i("test", "long press");
    }
    return true;
  }

  private void initGoogleApiClient() {
    mGoogleApiClient = new GoogleApiClient.Builder(MainActivity.this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(new WearConnectionCallbacks())
            .addOnConnectionFailedListener(new WearConnectionFailedListener())
            .build();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop() {
    if(mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override
  public void onConnected(Bundle bundle) {
    Log.d("test", "Connected to Google Api Service");
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(ConnectionResult connectionResult) {
    Log.e("test", "Failed to connect to Google Api Service");
  }
}
