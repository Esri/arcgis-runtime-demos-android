package com.esri.arcgis.sample;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

public class ServiceFragment extends DialogFragment {

  public enum ServiceType {
    ROUTING,
    GEOCODING,
    MAP_SERVER
  }
  
  /**
   * Interface for listening to service load requests.
   */
  public interface ServiceCallback {
    
    /**
     * Invoked when a service load is requested.
     * 
     * @param type The type of service (Routing, Geocoding, etc).
     * @param url The url of the service.
     * @param user The username (may be empty).
     * @param passoprd The password (may be empty).
     */
    void onServiceSelected(ServiceType type, String url, String user, String passoprd);
    
  }
  
  private static List<ServiceTuple> sOnlineServices = new ArrayList<ServiceTuple>();
  
  static {
    sOnlineServices.add(new ServiceTuple("World Routing Service", "http://route.arcgis.com/arcgis/rest/services/World/Route/NAServer/Route_World", ServiceType.ROUTING));
    sOnlineServices.add(new ServiceTuple("World Geocoding Service", "http://geocode.arcgis.com/arcgis/rest/services/World/GeocodeServer", ServiceType.GEOCODING));
    sOnlineServices.add(new ServiceTuple("World Street Basemap Service", "http://services.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer", ServiceType.MAP_SERVER));
  }
  
  private ServiceCallback mCallback = null;
  
  /**
   * Default constructor.
   */
  public ServiceFragment() {
    
  }
  
  /**
   * Create a new Service Fragment.
   * 
   * @return a Service Fragment.
   */
  public static ServiceFragment newInstance() {
    ServiceFragment fragment = new ServiceFragment();
    return fragment;
  }
  
  /**
   * Bind a callback to be invoked when a service is selected.
   * If null is supplied, the current callback will be removed.
   * 
   * @param callback The callback to be invoked.
   * @return this for chaining API calls.
   */
  public ServiceFragment bindCallback(ServiceCallback callback) {
    mCallback = callback;
    return this;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    // Inflate the primary view.
    LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    View view = inflater.inflate(R.layout.fragment_services, (ViewGroup) null, false);
    
    ListView serviceList = (ListView) view.findViewById(R.id.service_list);
    
    // Load the display names into an ArrayAdapter.
    List<String> display  = new ArrayList<String>();
    for (ServiceTuple service : sOnlineServices)
      display.add(service.displayName);
    
    serviceList.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1, display));
    
    // Grab the credential fields.
    final EditText userName = (EditText) view.findViewById(R.id.username_edit_text);
    final EditText password = (EditText) view.findViewById(R.id.password_edit_text);
    
    // On click, invoke the callback and dismiss the dialog.
    serviceList.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        
        if (mCallback != null) {
          
          ServiceTuple service = sOnlineServices.get(position);
          mCallback.onServiceSelected(service.type, service.url, userName.getText().toString(), password.getText().toString());          
          ServiceFragment.this.dismiss();
        }        
      }
    });
    
    // Build the dialog.
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setView(view)
           .setNegativeButton("Cancel", null);    
    
    return builder.create();
  }
  
  /**
   * Dirt simple class for joining useful service information.
   */
  private static class ServiceTuple {
    
    final String displayName;
    
    final ServiceType type;
    
    final String url;    
    
    public ServiceTuple(String displayName, String url, ServiceType type) {
      this.displayName = displayName;
      this.url = url;
      this.type = type;
    }
  }
  
}
