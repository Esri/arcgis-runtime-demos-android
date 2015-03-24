package main.java.com.esri.test.glasssample;

import android.app.Activity;
import android.widget.Toast;

import com.esri.android.map.MapView;
import com.esri.core.geometry.Point;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.Arm;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.XDirection;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by mich6984 on 2/23/15.
 */
public class MyoMapListener extends AbstractDeviceListener {

  // The arm that Myo is on is unknown until the arm recognized event is received.
  private Arm mArm = Arm.UNKNOWN;

  private Activity mActivity;
  private MapView mMapView;

  private double mLastYawDelta;
  private double mLastRollDelta;
  private boolean mPanningEnabled = false;
  private Pose mCurrentPose;
  private XDirection mXDirection;
  private double mCurrentRoll;
  private double mCurrentPitch;
  private double mCurrentYaw;
  private double mLastYaw = Double.NaN;
  private double mLastRoll = Double.NaN;
  private double mStartingRoll;
  private double mStartingPitch;
  private double mStartingYaw;
  private final double DIV = 100.0;
  private final double THRESHOLD = 5.0 / DIV;
  private final double DELTA_TRIGGER = 5;
  private double factorBase = 0.0;
  private double xFactor = factorBase;
  private double yFactor = factorBase;
  // Base zoom factor, so that worst case, no zoom occurs
  private float mZoomoutFactor = 0.98f;
  private float mZoominFactor = 1.02f;
  private boolean mZoomingEnabled = false;

  private Timer mPanTimer;
  private Timer mZoomTimer;

  private class PanTimerTask extends TimerTask {
    private final double PAN_AMOUNT = 100.0;
    private final double SCALE_DIV = 10000;
    @Override
    public void run() {
      // zoom the map
      Point center = mMapView.getCenter();
      double newX = center.getX() + (PAN_AMOUNT * (mMapView.getScale()/SCALE_DIV) * xFactor);
      double newY = center.getY() + (PAN_AMOUNT * (mMapView.getScale()/SCALE_DIV) * yFactor);
      mMapView.centerAt(new Point(newX, newY), false);
    }
  }

  private class ZoomTimerTask extends TimerTask {
    private float mZoomFactor;

    public ZoomTimerTask(float zoomFactor) {
      mZoomFactor = zoomFactor;
    }

    @Override
    public void run() {
      // zoom the map
      mMapView.doubleTapZoom(mZoomFactor);
    }
  }

  public MyoMapListener(Activity activity, MapView mapView) {
    mActivity = activity;
    mMapView = mapView;
  }

  // onArmRecognized() is called whenever Myo has recognized a setup gesture after someone has put it on their
  // arm. This lets Myo know which arm it's on and which way it's facing.
  @Override
  public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection) {
    // Save the arm the Myo is on so that we can use it in the pose events.
    mArm = arm;
    mXDirection = xDirection;
  }

  // onArmLost() is called whenever Myo has detected that it was moved from a stable position on a person's arm after
  // it recognized the arm. Typically this happens when someone takes Myo off of their arm, but it can also happen
  // when Myo is moved around on the arm.
  @Override
  public void onArmUnsync(Myo myo, long timestamp) {
    mArm = Arm.UNKNOWN;
  }

  @Override
  public void onOrientationData(Myo myo, long timestamp, Quaternion rotation) {
    updateValues(rotation);
    if(mPanningEnabled) {
      //X Factor
      normalizeYaw();
      double yawDiff = ((mCurrentYaw - mStartingYaw) * -1.0) / DIV;
      if(Math.abs(yawDiff) >= THRESHOLD) {
        yawDiff = (yawDiff < 0) ? yawDiff*yawDiff*-1.0 : yawDiff*yawDiff;
        xFactor = factorBase + yawDiff;
      } else {
        xFactor = factorBase;
      }

      //Y Factor
      double pitchDiff = ((mCurrentPitch - mStartingPitch) * -1.0) / DIV;
      if(Math.abs(pitchDiff) >= THRESHOLD) {
        pitchDiff = (pitchDiff < 0) ? pitchDiff*pitchDiff*-1.0 : pitchDiff*pitchDiff;
        yFactor = factorBase + pitchDiff;
      } else {
        yFactor = factorBase;
      }
      if(mPanTimer == null) {
        mPanTimer = new Timer();
        mPanTimer.schedule(new PanTimerTask(), 0, 16 /*16ms ensure 60Hz refresh rate*/);
      }
    }
    if(myo.isUnlocked() && mCurrentPose == Pose.FIST) {
      normalizeRoll();
      double rollDiff = mCurrentRoll - mStartingRoll;
      if(Math.abs(rollDiff) >= 5.0) {
        double newRoll = mMapView.getRotationAngle() + rollDiff;
        mMapView.setRotationAngle(newRoll);
      }
    }
  }

  private void normalizeYaw() {
    if(mLastYaw != Double.NaN) {
      double delta = mCurrentYaw - mLastYaw;
      if (Math.abs(delta) >= DELTA_TRIGGER) {
        if (mLastYawDelta > 0 && delta < 0) {
          mStartingYaw -= 360;
        } else if (mLastYawDelta < 0 && delta > 0) {
          mStartingYaw += 360;
        }
      }
      mLastYawDelta = delta;
    }
    mLastYaw = mCurrentYaw;
  }

  private void normalizeRoll() {
    if(mLastRoll != Double.NaN) {
      double delta = mCurrentRoll - mLastRoll;
      if (Math.abs(delta) >= DELTA_TRIGGER) {
        if (mLastRollDelta > 0 && delta < 0) {
          mStartingRoll -= 360;
        } else if (mLastRollDelta < 0 && delta > 0) {
          mStartingRoll += 360;
        }
      }
      mLastRollDelta = delta;
    }
    mLastRoll = mCurrentRoll;
  }

  private void updateValues(Quaternion rotation) {
    // Calculate Euler angles (roll, pitch, and yaw) from the quaternion.
    float roll = (float) Math.toDegrees(Quaternion.roll(rotation));
    float pitch = (float) Math.toDegrees(Quaternion.pitch(rotation));
    float yaw = (float) Math.toDegrees(Quaternion.yaw(rotation));
    // Adjust roll and pitch for the orientation of the Myo on the arm.
    if (mXDirection == XDirection.TOWARD_ELBOW) {
      roll *= -1;
      pitch *= -1;
    }
    roll += 180;
    yaw += 180;
    mCurrentRoll = roll;
    mCurrentPitch = pitch;
    mCurrentYaw = yaw;
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
        mStartingRoll = mCurrentRoll;
        break;
      case FINGERS_SPREAD:
        if(mPanningEnabled) {
          stopPanning(myo);
        } else {
          startPanning(myo);
        }
        break;
      case WAVE_IN:
        if(!mZoomingEnabled) {
          startZooming(mZoomoutFactor);
        }
        break;
      case WAVE_OUT:
        if(!mZoomingEnabled) {
          startZooming(mZoominFactor);
        }
        break;
      case REST:
        switch(mCurrentPose) {
          case WAVE_IN:
          case WAVE_OUT:
            stopZooming();
            break;
        }
        break;
    }
    mCurrentPose = pose;
  }

  private void lock(final Myo myo) {
    if (mPanningEnabled) {
      stopPanning(myo);
    }
    showToast("Control locked!");
    myo.lock();
  }

  private void unlock(final Myo myo) {
    showToast("Control unlocked!");
    myo.unlock(Myo.UnlockType.HOLD);
  }

  private void stopPanning(Myo myo) {
    mPanningEnabled = false;
    stopPanTimer();
    resetPanValues();
    showToast("Panning disabled!");
    myo.vibrate(Myo.VibrationType.MEDIUM);
  }

  private void startPanning(Myo myo) {
    mPanningEnabled = true;
    mStartingPitch = mCurrentPitch;
    mStartingYaw = mCurrentYaw;
    showToast("Panning enabled!");
    myo.vibrate(Myo.VibrationType.MEDIUM);
  }

  private void stopPanTimer() {
    if (mPanTimer != null) {
      mPanTimer.cancel();
      mPanTimer = null;
    }
  }

  private void resetPanValues() {
    xFactor = factorBase;
    yFactor = factorBase;
    mLastYaw = Double.NaN;
    mLastYawDelta = 0.0;
    mLastRoll = Double.NaN;
    mLastRollDelta = 0.0;
  }

  private void startZooming(float zoomFactor) {
    mZoomingEnabled = true;
    if(mZoomTimer == null) {
      mZoomTimer = new Timer();
      mZoomTimer.schedule(new ZoomTimerTask(zoomFactor), 0, 16);
    }
  }

  private void stopZooming() {
    mZoomingEnabled = false;
    if (mZoomTimer != null) {
      mZoomTimer.cancel();
      mZoomTimer = null;
    }
  }

  private void showToast(final String message) {
    mActivity.runOnUiThread(new Runnable() {
      public void run() {
        Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
      }
    });
  }
}
