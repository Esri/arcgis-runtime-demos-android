package com.esri.arcgis.sample;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FileFragment extends DialogFragment {

  private static final String EXTERNAL = Environment.getExternalStorageDirectory().getPath();
  
  private static final String DATA_DIR = "ArcGIS";
  
  private static final String DATA_TYPE_KEY = "datatype";
  
  private FileCallback mCallback;
  
  public interface FileCallback {
    
   void onFileSelected(String absolutePath);
   
  }
  
  public enum DataType {
    TRANSPORTAION_NETWORK,
    GEODATABASE,
    LOCATOR,
    LOCAL_TILED_LAYER
  }
  
  public FileFragment() {
    
  }
  
  public static FileFragment newInstance(DataType dataType) {
    FileFragment fragment = new FileFragment();
    
    Bundle args = new Bundle();
    args.putInt(DATA_TYPE_KEY, dataType.ordinal());
    fragment.setArguments(args);
    
    return fragment;
  }
  
  public FileFragment bindCallback(FileCallback callback) {
    mCallback = callback;
    return this;
  }
  
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    
    Bundle args = getArguments();
    int ordinal = args.getInt(DATA_TYPE_KEY);
    DataType dataType = DataType.values()[ordinal];
    
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    List<File> files = new ArrayList<File>();
    
    File searchDir = new File(EXTERNAL + File.separator + DATA_DIR);
    if (searchDir.exists() && searchDir.isDirectory()) {
      scrapeData(searchDir, dataType, files);             
    } 
    
    if (!files.isEmpty()) {
      ListView listView = new ListView(getActivity());
      FileAdapter adapter = new FileAdapter(getActivity(), R.layout.file_item, R.id.file_item_main_text, R.id.file_item_sub_text, R.id.file_item_thumbnail, R.drawable.ic_action_file, files);
      listView.setAdapter(adapter);
      listView.setOnItemClickListener(new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          
          if (mCallback != null) {
            File file = (File) parent.getItemAtPosition(position);
            mCallback.onFileSelected(file.getAbsolutePath());
          }
          
          FileFragment.this.dismiss();
        }
      });
      builder.setTitle("Content")
             .setView(listView);
    } else {
      builder.setTitle("No Content Found")
             .setMessage("No content was found in : " + EXTERNAL + File.separator + DATA_DIR);
    }
    
    return builder.create();
  }
  
  private static class FileAdapter extends ArrayAdapter<File> {

    private final int mImageViewId;
    
    private final int mSubTextId;
    
    private final int mMainTextId;
    
    private final int mImageResourceId;
    
    public FileAdapter(Context context, int resource, int mainTextResourceId,
        int subTextResourceId, int imageViewId, int imageResourceId, List<File> objects) {
      super(context, resource, mainTextResourceId, objects);
      
      mImageViewId = imageViewId;
      mSubTextId = subTextResourceId;
      mMainTextId = mainTextResourceId;
      mImageResourceId = imageResourceId;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      View baseView = super.getView(position, convertView, parent);
      
      File file = this.getItem(position);
      
      if (mMainTextId > 0) {
        TextView mainText = (TextView) baseView.findViewById(mMainTextId);
        mainText.setText(file.getName());
      }
      
      if (mSubTextId > 0) {        
        TextView subText = (TextView) baseView.findViewById(mSubTextId);
        subText.setText(file.getAbsolutePath());        
      }
      
      if (mImageViewId > 0 && mImageResourceId > 0) {
        ImageView thumbnail = (ImageView) baseView.findViewById(mImageViewId);
        thumbnail.setImageResource(mImageResourceId);
      }      
      
      return baseView;
    }
    
  }
  
  /**
   * Recursively scrape some from a specified folder. 
   * 
   * @param directory The directory to scrape from.
   * @param type The type of file to look for.
   * @param foundFiles The found files.
   */
  private void scrapeData(File directory, DataType type, List<File> foundFiles) {
    
    if (directory == null || !directory.isDirectory())
      return;
    
    for (File file : directory.listFiles()) {
      
      switch (type) {
      case GEODATABASE:
        if (Utils.isGeodatabase(file))
          foundFiles.add(file);
        break;
      case TRANSPORTAION_NETWORK:
        if (Utils.isGeodatabase(file) && Utils.findTransportationNetwork(file.getAbsolutePath()) != null)
          foundFiles.add(file);
        break;
      case LOCATOR:
        if (Utils.isLocator(file))
          foundFiles.add(file);
        break;
      case LOCAL_TILED_LAYER:
        if (Utils.isLocalTiledLayer(file))
          foundFiles.add(file);
        break;
      default:
        break;
      }
      
      scrapeData(file, type, foundFiles);
    }  
  }
  
}
