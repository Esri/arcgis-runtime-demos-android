package main.java.com.esri.test.glasssample;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.View;
import android.widget.RemoteViews;

import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.google.android.glass.timeline.LiveCard;

/**
 * Created by mich6984 on 9/29/14.
 */
public class MapService extends Service {

  private final String LIVE_CARD_TAG="esriMaps";

  private static LiveCard mLiveCard;

  private static MapView mapView;

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if(mLiveCard == null) {
      mLiveCard = new LiveCard(this, LIVE_CARD_TAG);
      RemoteViews rvs = new RemoteViews(this.getPackageName(), R.layout.livecard_textview);
      mLiveCard.setViews(rvs);
      mapView = getMap(this);
      Intent i = new Intent(this, MapActivity.class);

      mLiveCard.setAction(PendingIntent.getActivity(this, 0, new Intent(this, MapActivity.class), 0));
      mLiveCard.attach(this);
      mLiveCard.publish(LiveCard.PublishMode.REVEAL);

    } else {
      mLiveCard.navigate();
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    if (mLiveCard != null && mLiveCard.isPublished()) {
      mLiveCard.unpublish();
      mLiveCard = null;
    }
    super.onDestroy();
  }

  public static MapView getMap(Context context) {
    if(mapView == null || mapView.isRecycled()) {
//      mapView = new MapView(context, "http://csf.maps.arcgis.com/home/webmap/viewer.html?webmap=e153bcfe4beb48a8bde62c71ea1aef9b", "regtest", "bazinga");
      mapView = new MapView(context);
      mapView.addLayer(new ArcGISTiledMapServiceLayer("http://server.arcgisonline.com/arcgis/rest/services/World_Topo_Map/MapServer"));
      LocationDisplayManager ldm = mapView.getLocationDisplayManager();
      ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.LOCATION);
      ldm.start();
    }
    return mapView;
  }

  public static void returnToLiveCard() {
    if(mLiveCard != null && mLiveCard.isPublished()) {
      mLiveCard.navigate();
    }
  }

}
