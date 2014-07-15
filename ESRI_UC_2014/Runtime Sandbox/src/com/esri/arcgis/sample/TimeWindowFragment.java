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

public class TimeWindowFragment extends DialogFragment {
  
  public interface TimeWindowCallback {
    
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
   * Bind a callback to be invoked when a service is selected.
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
              
              startPicker.clearFocus();
              endPicker.clearFocus();
              
              Calendar calendar = Calendar.getInstance();
              
              int year = calendar.get(Calendar.YEAR);
              int month = calendar.get(Calendar.MONTH);
              int day = calendar.get(Calendar.DAY_OF_MONTH);
              
              calendar.set(year, month, day, startPicker.getCurrentHour(), startPicker.getCurrentMinute());
              long startWindow = calendar.getTimeInMillis();
              
              calendar.set(year, month, day, endPicker.getCurrentHour(), endPicker.getCurrentMinute());
              long endWindow = calendar.getTimeInMillis();
              
              if (mCallback != null)
                mCallback.onTimeWindowUpdated(startWindow, endWindow);              
            }
          });    
    
    return builder.create();
  }  
}
