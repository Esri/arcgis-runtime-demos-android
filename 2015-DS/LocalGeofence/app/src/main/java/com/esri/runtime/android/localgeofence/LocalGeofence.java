package com.esri.runtime.android.localgeofence;

import android.util.Log;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.LinearUnit;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Proximity2DResult;
import com.esri.core.geometry.SpatialReference;

public class LocalGeofence {

  // Location status relative to geofence
  public enum Status {
    OUTSIDE,
    CLOSE,
    INSIDE
  }

  // How location status changed
  public enum Change {
    ENTERED,
    REMAINED_IN,
    EXITED,
    REMAINED_OUT
  }

  // Indicates if a chage is required to the update speed.s
  public enum UpdateChange {
    NO_CHANGE,
    FASTER,
    SLOWER
  }


  public static class FenceInformation {
    public Status status;
    public Change change;
    public UpdateChange updateChange;
  }

  private static final String TAG = LocalGeofence.class.getSimpleName();

  // The geofence geometry, and its spatial reference.
  protected static Polygon mFence = null;
  protected static SpatialReference mFenceSr = null;

  // Fence geometry projected to WGS84.
  protected static Polygon mFenceWgs84 = null;

  // Proximity calculation units, and the distance that is considered 'CLOSE'.
  // Set for demo purposes, should be adjusted for specific usage.
  protected static LinearUnit mProximityUnits = new LinearUnit(LinearUnit.Code.METER);
  protected static double IS_CLOSE_DISTANCE_METERS = 400;

  // Location updates are always geographic coordinates.
  protected static SpatialReference mWgs84Sr = SpatialReference.create(SpatialReference.WKID_WGS84);

  // The feature name, object ID, and caption relating to this geofence.
  protected static String mFeatureName = null;
  protected static String mFenceSubtitle = null;
  protected static Long mFeatureObjectId = -1L;

  // Information about the last location update.
  protected static Status mLastStatus = Status.OUTSIDE;
  protected static Change mLastChange = Change.REMAINED_OUT;
  protected static UpdateChange mLastUpdateChange = UpdateChange.NO_CHANGE;

  public static Polygon getFence() {
    return mFence;
  }

  public static String getFeatureName() {
    return mFeatureName;
  }

  public static void setFeatureName(String newFeatureName) {
    mFeatureName = newFeatureName;
  }

  public static Long getFeatureOid() {
    return mFeatureObjectId;
  }

  public static void setFeatureOid(Long newFeatureOid) {
    mFeatureObjectId = newFeatureOid;
  }

  public static String getSubtitle() {
    return mFenceSubtitle;
  }

  public static void setSubtitle(String newSubtitle) {
    mFenceSubtitle = newSubtitle;
  }

  public static SpatialReference getSpatialReference() {
    return mFenceSr;
  }

  // Store the geofence feature.
  public static void setFence(Polygon newFence, SpatialReference fenceSpatialReference) {

    // Keep the original geometries.
    mFenceSr = fenceSpatialReference;
    mFence = newFence;

    // Work with the fence in WGS84, as that's what the location updates will be in.
    // Note that transformations could be used here to increase accuracy.
    if ( mFenceSr.getID() != mWgs84Sr.getID() ) {
      Geometry densified = GeometryEngine.geodesicDensifyGeometry(mFence,
          mFenceSr, 20, null);
      mFenceWgs84 = (Polygon)GeometryEngine.project(densified, mFenceSr, mWgs84Sr);
    }
    else {
      mFenceWgs84 = mFence;
    }
  }

  /**
   * For the latest location update, calculate fence status and change.
   *
   * @param latestLocation  the latest location update
   * @return  FenceInformation about the change relative to the fence
   */
  public static FenceInformation latestLocation(Point latestLocation) {
    if ( (latestLocation == null) || (mFenceWgs84 == null) ) {
      return null;
    }

    Status newStatus = null;
    Change newChange = null;
    UpdateChange newUpdateChange = null;

    // If point is inside fence, we don't need to know if its close.
    if (isWithinFence(latestLocation)) {
      newStatus = Status.INSIDE;
    }
    else {
      // If not inside, is it close? If not close, it's outside.
      boolean isClose = closeToFence(latestLocation);
      newStatus = (isClose) ? Status.CLOSE : Status.OUTSIDE;
    }

    // How has this state changed from previous status.
    if ( (newStatus == Status.INSIDE) && (mLastStatus == Status.INSIDE) ) {
      // INSIDE -> INSIDE
      newChange = Change.REMAINED_IN;
    }
    else if (newStatus == Status.INSIDE) {
      // OUTSIDE -> INSIDE, CLOSE -> INSIDE
      newChange = Change.ENTERED;
    }
    else if (mLastStatus == Status.INSIDE) {
      // INSIDE -> CLOSE, INSIDE -> OUTSIDE
      newChange = Change.EXITED;
    }

    if (newChange == null) {
      // CLOSE -> CLOSE, OUTSIDE -> OUTSIDE, CLOSE -> OUTSIDE, OUTSIDE -> CLOSE.
      newChange = Change.REMAINED_OUT;
    }

    // Work out if GPS frequency needs to be increased or decreased.
    if (Status.OUTSIDE == mLastStatus) {
      if ( (Status.CLOSE == newStatus) || (Status.INSIDE == newStatus) ) {
        // Need to increase GPS frequency when:
        // OUTSIDE -> INSIDE, or OUTSIDE -> CLOSE
        newUpdateChange = UpdateChange.FASTER;
      }
    }
    else if (Status.OUTSIDE == newStatus) { // LastStatus Must be INSIDE or CLOSE
      // Decrease frequency when:
      // INSIDE -> OUTSIDE, or CLOSE -> OUTSIDE
      newUpdateChange = UpdateChange.SLOWER;
    }

    FenceInformation info = new FenceInformation();
    info.status = newStatus;
    info.change = newChange;
    info.updateChange = newUpdateChange;

    // The information we return becomes the 'previous' information that we store.
    mLastStatus = newStatus;
    mLastChange = newChange;
    mLastUpdateChange = newUpdateChange;
    return info;
  }

  /**
   * Calculate if location is within tolerance of the geofence boundary.
   * @param location location to compare with geofence
   * @return true if the location is within tolerance distance, otherwise false.
   */
  private static boolean closeToFence(Point location) {
    // Work out proximity to the fence by getting the nearest coordinate to the
    // fence boundary and working out distance between that and current location.
    Proximity2DResult proximity =  GeometryEngine.getNearestCoordinate(
        mFenceWgs84, location, true);
    double distanceGeodesic =  GeometryEngine.geodesicDistance(location,
        proximity.getCoordinate(), mWgs84Sr, mProximityUnits);

    Log.i(TAG, String.format("GeometryEngine.geodesicDistance: %.6f", distanceGeodesic));
    return (distanceGeodesic < IS_CLOSE_DISTANCE_METERS);
  }

  /**
   * Find out if location is within geofence.
   * @param location location to compare with geofence
   * @return true if location is within geofence, otherwise false.
   */
  public static boolean isWithinFence(Point location) {
    return GeometryEngine.within(location, mFenceWgs84, mWgs84Sr);
  }

}
