package com.esri.arcgisruntime.wearcollection;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Provides a workflow for collecting a feature from the Wear device.
 */
public class CollectionActivity extends WearableActivity implements DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        WearableListView.ClickListener {

  // Various message paths used to communicate with the mobile device
  private static final String LAYER_REQUEST = "/request/layers";
  private static final String FEATURE_TYPE_REQUEST = "/request/feature_types";
  private static final String LAYER_RESPONSE = "/response/layers";
  private static final String FEATURE_TYPE_RESPONSE = "/response/feature_types";
  private static final String STATUS_RESPONSE = "/response/status";

  // Lists of the names of the layers and feature types
  private ArrayList<String> layerNames;
  private ArrayList<String> featureTypeNames;

  // Indicates the type of the next response to be made
  private RESPONSE_TYPE mNextResponseType;

  // The Google API client used to communicate with the mobile device
  private GoogleApiClient mGoogleApiClient;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_collection);
    // Initialize by showing text that we are fetching the list of layers
    TextView text = (TextView) findViewById(R.id.text);
    text.setText(R.string.fetching_layers);
    // Initialize the Google API client
    initGoogleApiClient();
  }

  /**
   * Sends a message to the mobile device with the specified path and payload.
   *
   * @param path the path of the message
   * @param payload the payload of the message
   */
  private void sendMessage(final String path, final byte[] payload) {
    Log.i("Test", "About to send message");
    if (mGoogleApiClient.isConnected()) {
      new Thread(new Runnable() {
        @Override
        public void run() {
          // Get any nodes connected to the Wear device (should only be one)
          NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
          Log.i("Test", "Just checked nodes");
          for (Node node : nodes.getNodes()) {
            Log.i("Test", "Here's a node");
            // For each node, send the message and check that it was sent successfully
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, payload).await();
            if (result.getStatus().isSuccess()) {
              Log.i("Test", "Successfully sent message to " + node.getDisplayName());
            } else {
              Log.e("Test", "error sending message");
            }
          }
        }
      }).start();
    }
  }

  /**
   * Initialize the Google API client which will be used to request the location.
   */
  private void initGoogleApiClient() {
    mGoogleApiClient = new GoogleApiClient.Builder(CollectionActivity.this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mGoogleApiClient.connect();
  }

  @Override
  protected void onStop() {
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      Wearable.DataApi.removeListener(mGoogleApiClient, this);
      mGoogleApiClient.disconnect();
    }
    super.onStop();
  }

  @Override
  public void onDataChanged(DataEventBuffer dataEventBuffer) {
    Log.i("Test", "Data changed event received");
    // Get the data changed event and handle it appropriately
    for(DataEvent event : dataEventBuffer) {
      if (event.getType() == DataEvent.TYPE_CHANGED) {
        DataItem item = event.getDataItem();
        String path = item.getUri().getPath();
        if (path.equals(LAYER_RESPONSE)) {
          handleLayerResponse(item);
        } else if (path.equals(FEATURE_TYPE_RESPONSE)) {
          handleFeatureTypeResponse(item);
        } else if (path.equals(STATUS_RESPONSE)) {
          handleStatusResponse(item);
        }
      }
    }
  }

  /**
   * Handles a layer response. The layer response includes a list of layers
   * that have recently been used on the mobile device that can be shown to
   * the user for selection.
   *
   * @param item the DataItem received from the mobile device
   */
  private void handleLayerResponse(DataItem item) {
    DataMap dm = DataMapItem.fromDataItem(item).getDataMap();
    // Get the list of layer names from the data map
    layerNames = dm.getStringArrayList("layers");
    // Set the content view to the selection scroll list
    setContentView(R.layout.selection_list);
    // Get the WearableListView and set its adapter and click listener
    WearableListView listView = (WearableListView) findViewById(R.id.wearable_list);
    listView.setAdapter(new Adapter(this, layerNames));
    listView.setClickListener(this);
    // Note that our next response type should be a layer response (when the user selects
    // an item)
    mNextResponseType = RESPONSE_TYPE.LAYER;
  }

  /**
   * Handle a FeatureType response. The FeatureType response includes a list
   * FeatureTypes for the selected layer that can be shown to the user.
   *
   * @param item the DataItem received from the mobile device
   */
  private void handleFeatureTypeResponse(DataItem item) {
    DataMap dm = DataMapItem.fromDataItem(item).getDataMap();
    // Get the list of feature types from the data map
    featureTypeNames = dm.getStringArrayList("featureTypes");
    // Set the content view to the selection scroll list
    setContentView(R.layout.selection_list);
    // Get the WearableListView and set its adapter and click listener
    WearableListView listView = (WearableListView) findViewById(R.id.wearable_list);
    listView.setAdapter(new Adapter(this, featureTypeNames));
    listView.setClickListener(this);
    // Note that our next response type should be a layer response (when the user selects
    // an item)
    mNextResponseType = RESPONSE_TYPE.FEATURE_TYPE;
  }

  /**
   * Handles a status response. The status response indicates whether the
   * feature was successfully collected or not, and if not, a reason why.
   *
   * @param item the DataItem received from the mobile device
   */
  private void handleStatusResponse(DataItem item) {
    // Gets the TextView that will be used to display the status
    TextView text = (TextView) findViewById(R.id.text);
    DataMap dm = DataMapItem.fromDataItem(item).getDataMap();
    boolean success = dm.getBoolean("success");
    // If the event was successfully, show a success message in green
    if (success) {
      text.setText(R.string.collect_success);
      text.setTextColor(Color.GREEN);
    } else {
      // If it failed, show the failure reason in red
      String reason = dm.getString("reason");
      text.setText(String.format("%s\n%s", getString(R.string.collect_failure), reason));
      text.setTextColor(Color.RED);
    }
    // After 2 seconds, finish the activity
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        CollectionActivity.this.finish();
      }
    }, 2000);
  }

  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d("Test", "Connected to Google Api Service");
    Wearable.DataApi.addListener(mGoogleApiClient, this);
    // Upon first connecting, send a request for layer names to the mobile device
    sendMessage(LAYER_REQUEST, null);
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.e("Test", "Failed to connect to Google Api Service");
  }

  @Override
  public void onClick(WearableListView.ViewHolder v) {
    Integer tag = (Integer) v.itemView.getTag();
    setContentView(R.layout.activity_collection);
    TextView text = (TextView) findViewById(R.id.text);
    // When the user clicks an item, send a request to the phone depending on our
    // current response type
    switch (mNextResponseType) {
      case LAYER:
        // If we are sending a layer response, send a message including the
        // name of the layer selected
        Log.d("Test", "Selected: " + layerNames.get(tag));
        sendMessage(FEATURE_TYPE_REQUEST, layerNames.get(tag).getBytes());
        // While waiting for the response, show text that indicates we are fetching
        // the feature types
        text.setText(R.string.fetching_types);
        break;
      case FEATURE_TYPE:
        // If we are sending a feature type response, send a message including
        // the type of the feature to be collected by the mobile device
        Log.d("Test", "Selected: " + featureTypeNames.get(tag));
        sendMessage(FEATURE_TYPE_RESPONSE, featureTypeNames.get(tag).getBytes());
        // While waiting for the response, show text that indicates we are collecting
        // the feature
        text.setText(R.string.collecting_feature);
        break;
    }
  }

  @Override
  public void onTopEmptyRegionClick() {
    // Don't do anything if the user clicks the empty region above
    // the top list item
  }

  /**
   * Specifies the different types of responses that can be made to the mobile device.
   */
  public enum RESPONSE_TYPE {
    LAYER,
    FEATURE_TYPE
  }

  /**
   * An adapter for the WearableListView that uses an ArrayList of Strings.
   */
  private static final class Adapter extends WearableListView.Adapter {
    private final LayoutInflater mInflater;
    private ArrayList<String> mDataset;

    // Provide a suitable constructor (depends on the kind of dataset)
    public Adapter(Context context, ArrayList<String> dataset) {
      mInflater = LayoutInflater.from(context);
      mDataset = dataset;
    }

    // Create new views for list items
    // (invoked by the WearableListView's layout manager)
    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
      // Inflate our custom layout for list items
      return new ItemViewHolder(mInflater.inflate(R.layout.list_item, null));
    }

    // Replace the contents of a list item
    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder,
                                 int position) {
      // retrieve the text view
      ItemViewHolder itemHolder = (ItemViewHolder) holder;
      TextView view = itemHolder.textView;
      // replace text contents
      view.setText(mDataset.get(position));
      // replace list item's metadata
      holder.itemView.setTag(position);
    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
      return mDataset.size();
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder {
      private TextView textView;

      public ItemViewHolder(View itemView) {
        super(itemView);
        // find the text view within the custom item's layout
        textView = (TextView) itemView.findViewById(R.id.name);
      }
    }
  }
}
