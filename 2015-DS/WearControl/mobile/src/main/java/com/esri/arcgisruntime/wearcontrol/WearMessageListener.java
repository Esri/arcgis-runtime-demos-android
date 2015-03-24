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
