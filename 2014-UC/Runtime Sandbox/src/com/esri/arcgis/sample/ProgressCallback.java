package com.esri.arcgis.sample;

/**
 * Simple interface for communicating progress updates.
 */
public interface ProgressCallback {

  void toggleIndeterminateProgress(boolean show);
  
}
