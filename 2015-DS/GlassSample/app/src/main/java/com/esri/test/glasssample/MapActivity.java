package main.java.com.esri.test.glasssample;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mich6984 on 9/29/14.
 */
public class MapActivity extends Activity implements SensorEventListener {
  GestureDetector mGestureDetector;

  SensorManager mSensorManager;
  Sensor mRotationSensor;
  boolean mMotionControlEnabled = false;
  private float[] mLastRotation = null;
  private float[] mCurRotation = null;
  private float zValLast;
  private float zValCur;
  private int count = 1;

  private boolean mDying = false;

  private MyoMapListener mListener;
  private MapView mMapView;

  // Called when the activity is first created.
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mMapView = MapService.getMap(this);
    Log.d("ArcGIS", "Entering MapActivity");
    initGestureDetector(this);
    initSensors(this);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(mMapView);
    Hub hub = Hub.getInstance();
    if (!hub.init(this, getPackageName())) {
      // We can't do anything with the Myo device if the Hub can't be initialized, so exit.
      Toast.makeText(this, "Couldn't initialize Hub", Toast.LENGTH_SHORT).show();
      finish();
      return;
    }
    mListener = new MyoMapListener(this, mMapView);
    hub.addListener(mListener);
    hub.setLockingPolicy(Hub.LockingPolicy.NONE);
    hub.attachToAdjacentMyo();
  }

  @Override
  public void onSensorChanged(SensorEvent e) {
    if(mLastRotation == null) {
      mLastRotation = new float[9];
      SensorManager.getRotationMatrixFromVector(mLastRotation, e.values);
      zValLast = mLastRotation[8];
    } else {
      mCurRotation = new float[9];
      SensorManager.getRotationMatrixFromVector(mCurRotation, e.values);
      zValCur = mCurRotation[8];
      float deltaZ = zValCur - zValLast;
      if(Math.abs(deltaZ) > 0.2) {
        Point mapPoint = mMapView.getCenter();
        double dif = Math.abs(mapPoint.getY() * 0.01);
        Point newMapPoint;
        if(zValCur > zValLast) {
          //Pan down
           newMapPoint = new Point(mapPoint.getX(), mapPoint.getY()-dif);

        } else {
          //Pan up
          newMapPoint = new Point(mapPoint.getX(), mapPoint.getY()+dif);
        }
        mMapView.centerAt(newMapPoint, false);
      }
    }
    if(count % 50 == 0) {
      count = 0;
    }
    count++;
  }

  @Override
  public void onAccuracyChanged(Sensor s, int acc) {

  }

  @Override
  protected void onPause() {
    Log.d("ArcGIS", "Pausing MapActivity");
    mMapView.pause();
    super.onPause();

  }

  @Override
  protected void onResume() {
    Log.d("ArcGIS", "Resuming MapActivity");
    mMapView.unpause();
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    Log.d("ArcGIS", "Exiting MapActivity, removing view");
    mSensorManager.unregisterListener(this);
    ((ViewGroup)mMapView.getParent()).removeView(mMapView);
    Hub.getInstance().removeListener(mListener);
    if (isFinishing()) {
      // The Activity is finishing, so shutdown the Hub. This will disconnect from the Myo.
      Hub.getInstance().shutdown();
    }
    if(!mDying)
      MapService.returnToLiveCard();
    super.onDestroy();
  }

  private void initGestureDetector(Context context) {
    mGestureDetector = new GestureDetector(context);
    mGestureDetector.setBaseListener(new GestureDetector.BaseListener() {
      @Override
    public boolean onGesture(Gesture gesture) {
        if(gesture == Gesture.TAP) {
          startActivityForResult(new Intent(MapActivity.this, MenuActivity.class), 1);
          return true;
        } else if(gesture == Gesture.TWO_TAP) {

          return true;
        } else if(gesture == Gesture.SWIPE_RIGHT) {

          return true;
        } else if(gesture == Gesture.SWIPE_LEFT) {

          return true;
        } else if(gesture == Gesture.TWO_LONG_PRESS) {
//          toggleMotionControl();
//          enableMyo();
        }
//        else if(gesture == Gesture.SWIPE_DOWN) {
//          Log.d("ArcGIS", "Received swipe down, about to finish");
//          finish();
//          return true;
//        }
        return false;
      }
    });

    mGestureDetector.setFingerListener(new GestureDetector.FingerListener() {
      @Override
    public void onFingerCountChanged(int previousCount, int currentCount) {

      }
    });

    mGestureDetector.setTwoFingerScrollListener( new GestureDetector.TwoFingerScrollListener() {
      @Override
      public boolean onTwoFingerScroll(float displacement, float delta, float velocity) {
        if((displacement > 0 && delta < 0) || (displacement < 0 && delta > 0)) { delta = -delta;}
        float factor = 1.0f + (delta/100.0f);
//        mMapView.zoomTo(mMapView.getCenter(), factor);
//        mMapView.doubleTapZoom(factor);
        return true;
      }
    });
  }

  private void initSensors(Context context) {
    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
  }

  private void toggleMotionControl() {
    String message = "";
    if(mMotionControlEnabled) {
      mSensorManager.unregisterListener(this);
      mLastRotation = null;
      message = "Motion control disabled";
    } else {
      mSensorManager.registerListener(this, mRotationSensor, 20);
      message = "Motion control enabled";
    }
    mMotionControlEnabled = !mMotionControlEnabled;
    Toast.makeText(MapActivity.this, message, Toast.LENGTH_SHORT).show();
  }

  @Override
  public boolean onGenericMotionEvent(MotionEvent e) {
    if(mGestureDetector != null) {
      return mGestureDetector.onMotionEvent(e);
    }
    return false;
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    mDying = true;
    finish();
  }

}
