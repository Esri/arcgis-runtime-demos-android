package com.esri.runtime.android.localgeofence;


import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the list of AlertItems in Recycler View
 */
public class GeofenceListViewerAdapter extends  RecyclerView.Adapter<GeofenceListViewerAdapter.GeofenceAlertItemHolder>{

  private List<GeofenceAlertItem> geofenceAlertItemList;
  private List<GeofenceAlertItem> selectedAlertItemList;
  GeofenceAlertItemHolder geofenceAlertItemHolder;
  OnAlertItemClickListener mItemClickListener;
  OnAlertItemLongClickListener mItemLongClickListener;

  public GeofenceListViewerAdapter(List<GeofenceAlertItem> geofenceAlertItemList) {
    this.geofenceAlertItemList = geofenceAlertItemList;
  }

  @Override
  public GeofenceAlertItemHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
    View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.geofencealertitem_cardview, viewGroup, false);
    geofenceAlertItemHolder = new GeofenceAlertItemHolder(v);
    return geofenceAlertItemHolder;
  }

  @Override
  public void onBindViewHolder(final GeofenceAlertItemHolder geofenceAlertItemHolder, int i) {
    final int position = i;
    GeofenceAlertItem alertItem = geofenceAlertItemList.get(i);
    geofenceAlertItemHolder.alertItemTitle.setText(geofenceAlertItemList.get(i).title);
    geofenceAlertItemHolder.alertItemFeatureName.setText("County: " + geofenceAlertItemList.get(i).featureName);
    geofenceAlertItemHolder.alertItemThumbnail.setImageBitmap(geofenceAlertItemList.get(i).thumbnail);
    //geofenceAlertItemHolder.alertItemFetchLocationUpdates.setChecked(geofenceAlertItemList.get(i).fetchingLocationUpdates());
    geofenceAlertItemHolder.alertItemFetchLocationUpdates.setChecked(geofenceAlertItemList.get(i).fetchingLocationUpdates);

    geofenceAlertItemHolder.alertItemFetchLocationUpdates.setTag(geofenceAlertItemList.get(i));

    geofenceAlertItemHolder.alertItemFetchLocationUpdates.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton v, boolean isChecked) {
        SwitchCompat cb = (SwitchCompat) v;
        GeofenceAlertItem checkedGeofenceAlertItem = (GeofenceAlertItem) cb.getTag();
        if (cb.isChecked()){
          for(int i=0;i<geofenceAlertItemList.size();i++){
            geofenceAlertItemList.get(i).setFetchingLocationUpdates(false);
          }
          geofenceAlertItemList.get(position).setFetchingLocationUpdates(cb.isChecked());
          notifyDataSetChanged();
          MainActivity.updateLocalFence(position);
          if(checkIfAnyItemIsCheckedOn() && !MainActivity.mLocationUpdatesStarted){
            MainActivity.startGeofenceService();
          }
        }else {
          geofenceAlertItemList.get(position).setFetchingLocationUpdates(cb.isChecked());
          if(!checkIfAnyItemIsCheckedOn()) {
            MainActivity.stopServices();
          }
        }
    }
    });

  }

  @Override
  public int getItemCount() {
    return geofenceAlertItemList.size();
  }

  @Override
  public void onAttachedToRecyclerView(RecyclerView recyclerView) {
    super.onAttachedToRecyclerView(recyclerView);
  }

  public class GeofenceAlertItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

    CardView cardView;
    TextView alertItemTitle;
    TextView alertItemFeatureName;

    ImageView alertItemThumbnail;
    SwitchCompat alertItemFetchLocationUpdates;

    public GeofenceAlertItemHolder(View itemView) {
      super(itemView);
      cardView = (CardView) itemView.findViewById(R.id.alertItemCardView);
      alertItemTitle = (TextView) itemView.findViewById(R.id.alertItemTitle);
      alertItemFeatureName = (TextView) itemView.findViewById(R.id.alertItemFeatureName);
      alertItemThumbnail = (ImageView) itemView.findViewById(R.id.alertItemThumbnail);
      alertItemFetchLocationUpdates = (SwitchCompat)itemView.findViewById(R.id.alertItemFetchLocationUpdates);
      itemView.setOnClickListener(this);
      itemView.setOnLongClickListener(this);
    }

    @Override
    public void onClick(View v) {
      if (mItemClickListener != null) {
        mItemClickListener.onItemClick(v, getPosition());
      }
    }

    @Override
    public boolean onLongClick(View v) {
      if (mItemLongClickListener != null) {
        mItemLongClickListener.onItemLongClick(v, getPosition());
      }
      return true;
    }
  }

  public interface OnAlertItemClickListener {
    public void onItemClick(View view, int position);
  }

  public interface OnAlertItemLongClickListener {
    public void onItemLongClick(View view, int position);
  }

  public void setOnAlertItemClickListener(final OnAlertItemClickListener mItemClickListener) {
    this.mItemClickListener = mItemClickListener;
  }

  public void setOnAlertItemLongClickListener(final OnAlertItemLongClickListener mItemLongClickListener) {
    this.mItemLongClickListener = mItemLongClickListener;
  }

  public void add(GeofenceAlertItem item, int position) {
    geofenceAlertItemList.add(position, item);
    notifyItemInserted(position);
  }

  public void add(GeofenceAlertItem item) {
    geofenceAlertItemList.add(item);
    notifyDataSetChanged();
  }


  public void remove(int position) {
    geofenceAlertItemList.remove(position);
    notifyItemRemoved(position);
  }
  public void remove(GeofenceAlertItem item) {
    geofenceAlertItemList.remove(item);
    notifyDataSetChanged();
  }

  public GeofenceAlertItem getItem(int position) {
    return geofenceAlertItemList.get(position);
  }

  public List<GeofenceAlertItem> getGeofenceAlertItemList() {
    return geofenceAlertItemList;
  }


  public List<GeofenceAlertItem> getSelectedAlertItemList() {
    selectedAlertItemList = new ArrayList<>();
    for (GeofenceAlertItem geofenceAlertItem: geofenceAlertItemList){
      if(geofenceAlertItem.isFetchingLocationUpdates()){
        selectedAlertItemList.add(geofenceAlertItem);
      }
    }
    return selectedAlertItemList;
  }

  public boolean checkIfAnyItemIsCheckedOn() {
    boolean checkedOn = false;
    for (GeofenceAlertItem geofenceAlertItem: geofenceAlertItemList){
      if(geofenceAlertItem.isFetchingLocationUpdates()){
        checkedOn = true;
      }
    }
    return checkedOn ;
  }

  public int getIndexOfSelectedAlertItem() {
    int selectedItemPosition = -1;
    for (GeofenceAlertItem geofenceAlertItem: geofenceAlertItemList){
      if(geofenceAlertItem.isFetchingLocationUpdates()){
        selectedItemPosition = geofenceAlertItemList.indexOf(geofenceAlertItem);
      }
    }
    return selectedItemPosition;
  }

  public int getIndexOfAlertItem(String featureId){
    for(GeofenceAlertItem geofenceAlertItem: geofenceAlertItemList){
      if(geofenceAlertItem.getFeatureId().equals(featureId)) {
        return geofenceAlertItemList.indexOf(geofenceAlertItem);
      }
    }
    return -1;
  }
}

