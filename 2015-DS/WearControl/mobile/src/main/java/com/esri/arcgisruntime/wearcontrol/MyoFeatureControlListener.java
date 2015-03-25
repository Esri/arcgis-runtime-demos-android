package com.esri.arcgisruntime.wearcontrol;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.popup.Popup;
import com.esri.android.map.popup.PopupContainer;
import com.esri.core.geometry.Point;
import com.esri.core.map.Graphic;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;


public class MyoFeatureControlListener extends MyoControlListener {
  private ArcGISFeatureLayer mFeatureLayer;
  PopupDialog mPopupDialog;
  private int[] mCurGraphics = null;
  private int mCurGraphicIndex = 0;
  private int mCurGraphicId = -1;

  public MyoFeatureControlListener(MainActivity activity, MapView mapView, ArcGISFeatureLayer featureLayer) {
    super(activity, mapView);
    mFeatureLayer = featureLayer;
  }

  // onPose() is called whenever a Myo provides a new pose.
  @Override
  public void onPose(Myo myo, long timestamp, Pose pose) {
    if(!myo.isUnlocked() && pose != Pose.DOUBLE_TAP) { return; }
    // Swap wave poses if the Myo is on the left arm. Allows user to "wave" right or left
    // regardless of the Myo arm and have the swipes be in the appropriate direction.
    if (mArm == Arm.LEFT) {
      if (pose == Pose.WAVE_IN) {
        pose = Pose.WAVE_OUT;
      } else if (pose == Pose.WAVE_OUT) {
        pose = Pose.WAVE_IN;
      }
    }

    // Dispatch touch pad events for the standard navigation controls based on the
    // current pose.
    switch (pose) {
      case DOUBLE_TAP:
        if(myo.isUnlocked()) {
          lock(myo);
        } else {
          unlock(myo);
        }
        break;
      case FIST:
        if(mPopupDialog == null && mCurGraphicId != -1) {
          Graphic graphic = mFeatureLayer.getGraphic(mCurGraphicId);
          Popup popup = mFeatureLayer.createPopup(mMapView, 0, graphic);
          PopupContainer popupContainer = new PopupContainer(mMapView);
          popupContainer.addPopup(popup);
          mPopupDialog = new PopupDialog(mMapView.getContext(), popupContainer);
          mPopupDialog.show();
        }
        else if(mPopupDialog != null) {
           if(mPopupDialog.isShowing()) {
            mPopupDialog.dismiss();
          }
          mPopupDialog = null;
        }
        break;
      case FINGERS_SPREAD:
        if(mCurGraphics == null) {
          Point center = mMapView.toScreenPoint(mMapView.getCenter());
          mCurGraphics = mFeatureLayer.getGraphicIDs((float) center.getX(), (float) center.getY(), (mMapView.getWidth()/2));
          mCurGraphicIndex = 0;
          if(mCurGraphics != null && mCurGraphics.length > 0) {
            mCurGraphicId = mCurGraphics[mCurGraphicIndex];
            mFeatureLayer.setSelectedGraphics(new int[]{mCurGraphicId}, true);
          } else {
            Log.d("TEST", "no graphics found");
          }
        } else {
          mCurGraphics = null;
          mCurGraphicId = -1;
          mFeatureLayer.clearSelection();
        }
        break;
      case WAVE_IN:
        if(mCurGraphics != null && mCurGraphics.length > 0) {
          if(mCurGraphicIndex == 0) {
            mCurGraphicIndex = mCurGraphics.length;
          }
          if(mCurGraphicIndex > 0) {
            mFeatureLayer.clearSelection();
            mCurGraphicId = mCurGraphics[--mCurGraphicIndex];
            mFeatureLayer.setSelectedGraphics(new int[] {mCurGraphicId}, true);
          }
        }
        break;
      case WAVE_OUT:
        if(mCurGraphics != null && mCurGraphics.length > 0) {
          if(mCurGraphicIndex == (mCurGraphics.length - 1)) {
            mCurGraphicIndex = - 1;
          }
          if (mCurGraphicIndex < (mCurGraphics.length - 1)) {
            mFeatureLayer.clearSelection();
            mCurGraphicId = mCurGraphics[++mCurGraphicIndex];
            mFeatureLayer.setSelectedGraphics(new int[] {mCurGraphicId}, true);
          }
        }
        break;
      case REST:
        break;
    }
  }

  // A customize full screen dialog.
  private class PopupDialog extends Dialog {
    private PopupContainer popupContainer;

    public PopupDialog(Context context, PopupContainer popupContainer) {
      super(context, android.R.style.Theme);
      this.popupContainer = popupContainer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
      LinearLayout layout = new LinearLayout(getContext());
      layout.addView(popupContainer.getPopupContainerView(), LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
      setContentView(layout, params);
    }

  }
}
