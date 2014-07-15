package com.esri.arcgis.sample;

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;

/**
 * A DialogFragment useful for adjusting time windows.
 * The inflated view will have two TimePickers corresponding 
 * to the start and end time windows respectively.
 */
public class TimeWindowFragment extends DialogFragment {
  
  /**
   * Simple callback for time window changes.
   */
  public interface TimeWindowCallback {
    
    /**
     * Invoked when time windows have been updated and accepted. This will
     * not be invoked if the dialog is dismissed without hitting the positive button.
     * 
     * @param timeWindowStart The time window start (milliseconds past the Unix epoch).
     * @param timeWindowEnd The time window end (milliseconds past the Unix epoch).
     */
    void onTimeWindowUpdated(Long timeWindowStart, Long timeWindowEnd);
    
  }
  
  private TimeWindowCallback mCallback = null;
  
  /**
   * Default constructor.
   */
  public TimeWindowFragment() {
    
  }
  
  /**
   * Create a new Time Window Fragment.
   * 
   * @return a Time Window Fragment.
   */
  public static TimeWindowFragment newInstance() {
    TimeWindowFragment fragment = new TimeWindowFragment();
    return fragment;
  }
  
  /**
   * Bind a callback to be invoked when time windows are updated successfully.
   * If null is supplied, the current callback will be removed.
   * 
   * @param callback The callback to be invoked.
   * @return this for chaining API calls.
   */
  public TimeWindowFragment bindCallback(TimeWindowCallback callback) {
    mCallback = callback;
    return this;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    // Inflate the primary view.
    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.time_window_dialog, (ViewGroup) null, false);
    
    final TimePicker startPicker = (TimePicker) view.findViewById(R.id.time_window_start_picker);
    final TimePicker endPicker = (TimePicker) view.findViewById(R.id.time_window_end_pciker);
    
    // Build the dialog.
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view)
           .setPositiveButton("Okay", new OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
              
              // Clear focus to ensure typed values are captured.
              startPicker.clearFocus();
              endPicker.clearFocus();
              
              // Get the current time.
              Calendar calendar = Calendar.getInstance();
              
              int year = calendar.get(Calendar.YEAR);
              int month = calendar.get(Calendar.MONTH);
              int day = calendar.get(Calendar.DAY_OF_MONTH);
              
              // Adjust hours and minutes on the start time window.
              calendar.set(year, month, day, startPicker.getCurrentHour(), startPicker.getCurrentMinute());
              long startWindow = calendar.getTimeInMillis();
              
              // Adjust hours and minutes on the end time window.
              calendar.set(year, month, day, endPicker.getCurrentHour(), endPicker.getCurrentMinute());
              long endWindow = calendar.getTimeInMillis();
              
              // Invoke the callback.
              if (mCallback != null)
                mCallback.onTimeWindowUpdated(startWindow, endWindow);              
            }
          });    
    
    return builder.create();
  }  
}
