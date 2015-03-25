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

import android.os.Parcel;
import android.util.Log;
import android.view.MotionEvent;

import com.esri.android.map.MapView;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;

public class WearMessageListener implements MessageApi.MessageListener {

  private MapView mMapView;
  private final static String MESSAGE_PATH = "/messages/test";

  public WearMessageListener(MapView mapView) {
    mMapView = mapView;
  }

  @Override
  public void onMessageReceived(MessageEvent messageEvent) {
    Log.i("test", "onMessageReceived()");
    if(MESSAGE_PATH.equals(messageEvent.getPath())) {
      Parcel p = Parcel.obtain();
      byte[] data = messageEvent.getData();
      p.unmarshall(data, 0, data.length);
      p.setDataPosition(0);
      MotionEvent e = MotionEvent.CREATOR.createFromParcel(p);
      mMapView.onTouchEvent(e);
    }
  }
}
