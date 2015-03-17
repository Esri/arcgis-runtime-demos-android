package com.esri.android.devsummit;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.esri.android.map.FeatureLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ogc.kml.KmlLayer;
import com.esri.android.runtime.ArcGISRuntime;
import com.esri.core.geodatabase.Geodatabase;
import com.esri.core.geodatabase.GeodatabaseFeatureTable;
import com.esri.core.geodatabase.ShapefileFeatureTable;
import com.esri.core.geometry.Geometry;
import com.esri.core.renderer.SimpleRenderer;
import com.esri.core.runtime.LicenseLevel;
import com.esri.core.runtime.LicenseResult;
import com.esri.core.symbol.SimpleFillSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol.STYLE;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;


public class DevSummitDemoActivity extends Activity implements FloatingActionButton.OnCheckedChangeListener {
	
  private MapView mMap;
  
  private KmlLayer mKmllayer;

  private RasterAnalysisHelper mRasterHelper;

  private KmlLayer kmllayer;
  
  private static final int WRITE_REQUEST_CODE = 43;

  private static final String TAG = "**DevSummitDemo**";

  private ShapefileFeatureTable mTable;

  private FeatureLayer mFlayer;

  private Geodatabase mLocalGdb = null;
  
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setLicense();
      setContentView(R.layout.main);

      mMap = (MapView) findViewById(R.id.mapView);

    // loading a raster layer as basemap
    String path = Environment.getExternalStorageDirectory() + File.separator
        + this.getResources().getString(R.string.raster_dir) + File.separator;
    mRasterHelper = new RasterAnalysisHelper(mMap,
        (path + this.getResources().getString(R.string.raster_basemap_layer)),
        (path + this.getResources().getString(R.string.raster_task)));
     mRasterHelper.loadRasterAsBasemap();

    FloatingActionButton fab1 = (FloatingActionButton) findViewById(R.id.fab_1);
    fab1.setOnCheckedChangeListener(this);

    }

    @Override
    public void onCheckedChanged(FloatingActionButton fabView, boolean isChecked) {
        openFileBrowser();
    }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    // Handle menu item selection
    switch (id) {
      case R.id.menu_renderer_blend:
        uncheckMenuitems();
        if (mRasterHelper != null) {
          mRasterHelper.applyBlendRenderer();
        }
        return true;
      case R.id.menu_renderer_RGB:
        uncheckMenuitems();
        if (mRasterHelper != null) {
          mRasterHelper.applyRGBRenderer(false);
        }
        return true;
      case R.id.menu_renderer_default:
        uncheckMenuitems();
        if (mRasterHelper != null) {
          mRasterHelper.applyRGBRenderer(true);
        }
        return true;
      case R.id.menu_analysis_los:
        if (item.isChecked()) {
          uncheckMenuitems();
          mRasterHelper.turnOffFunctionLayers();
        } else {
          item.setChecked(true);
          if (mRasterHelper != null) {
            mRasterHelper.performLOS(item);
          }
        }
        return true;
      case R.id.menu_analysis_viewshed:
        if (item.isChecked()) {
          uncheckMenuitems();
          mRasterHelper.turnOffFunctionLayers();
        } else {
          item.setChecked(true);
          if (mRasterHelper != null) {
            mRasterHelper.calculateViewshed(item);
          }
        }
        return true;
       case R.id.menu_removeall:
           uncheckMenuitems();
           mRasterHelper.turnOffFunctionLayers();
           Layer[] layers = mMap.getLayers();
           int mapLayers = mMap.getLayers().length;
           if (mapLayers > 1){
               for(int i=1; i<mapLayers-1; i++){
                   mMap.removeLayer(mapLayers-i);
               }
           }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void uncheckMenuItem(int id) {
    MenuItem item = (MenuItem)findViewById(id);
    if (item != null && item.isChecked()) {
      item.setChecked(false);
    }
  }

  private void uncheckMenuitems() {
    uncheckMenuItem(R.id.menu_analysis_los);
    uncheckMenuItem(R.id.menu_analysis_viewshed);
    invalidateOptionsMenu();
  }

  private void setLicense() {
    // Running this activity should result in a Standard license level
    LicenseLevel standardLicense = LicenseLevel.STANDARD;

    // Set clientId to upgrade DEVELOPER to BASIC
    String clientId = getString(R.string.license_client_id);
    LicenseResult licenseResult = ArcGISRuntime.setClientId(clientId);

    // Use a license string to set a license.
    String licenseString = getString(R.string.license_standard_string);
    licenseResult = ArcGISRuntime.License.setLicense(licenseString);
  }

    @Override
    protected void onNewIntent(Intent intent){
        Log.i(TAG, "onNewIntent()");
        super.onNewIntent(intent);
        String  action = intent.getAction();
        setIntent(intent);
        if (Intent.ACTION_VIEW.equals(action)){
            saveFileToSdcard();
        }

    }

    private void saveFileToSdcard(){

        Intent  intent = getIntent();
        InputStream is = null;
        FileOutputStream fos = null;
        String  fullPath = null;

        try {
            String  action = intent.getAction();

            if (!Intent.ACTION_VIEW.equals(action))
                return;

            Uri     uri = intent.getData();
            String  scheme = uri.getScheme();
            String  name = null;

            if (scheme.equals("file")) {
                List<String>
                        pathSegments = uri.getPathSegments();

                if (pathSegments.size() > 0)
                    name = pathSegments.get(pathSegments.size() - 1);

            }

            else if (scheme.equals("content")) {
                Cursor  cursor = getContentResolver().query(
                        uri,
                        new String[]{MediaStore.MediaColumns.DISPLAY_NAME},
                        null,
                        null,
                        null);

                cursor.moveToFirst();

                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (nameIndex >= 0)
                    name = cursor.getString(nameIndex);

            }

            else
                return;

            if (name == null)
                return;

            int     n = name.lastIndexOf(".");
            String  fileName,
                    fileExt;

            if (n == -1)
                return;

            else {
                fileName = name.substring(0, n);
                fileExt = name.substring(n);
                if (!fileExt.equals(".kml") && !fileExt.equals(".geodatabase"))
                    return;

            }

            // Get an inputStream
            is = getContentResolver().openInputStream(uri);

//          fos = new FileOutputStream(fullPath);
//          Get OutputStream for internal storage
            Log.d("devsummitdemo",  ""+getFilesDir());
            fos = openFileOutput(fileName+fileExt, Context.MODE_PRIVATE);

            byte[]  buffer = new byte[4096];
            int     count;

            while ((count = is.read(buffer)) > 0)
                fos.write(buffer, 0, count);

            fos.close();
            is.close();
            // We are done copying the file to internal storage
            // Now populate the MapView with the layers
            addLayer(getFilesDir() + "/" + fileName, fileExt);
        }

        catch (Exception e) {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e1) {
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception e1) {
                }
            }

            if (fullPath != null) {
                File f = new File(fullPath);

                f.delete();
            }
        }

    }

    /**
     * Method to write to External storage via dialog
     * @param mimeType
     * @param fileName
     */
    private void createFileOnStorage(String mimeType, String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

        // Filter to only show results that can be "opened", such as
        // a file (as opposed to a list of contacts or timezones).
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Create a file with the requested MIME type.
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, WRITE_REQUEST_CODE);
    }


    /**
     * Launch the File browser activity
     */
    private void openFileBrowser() {

      Intent fileExploreIntent = new Intent(
          ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.INTENT_ACTION_SELECT_FILE, null, this,
          ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.class);

      startActivityForResult(fileExploreIntent, 2);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      System.out.println(data);
      if (requestCode == 2) {
        if (resultCode == RESULT_OK) {
          String newFile = data
              .getStringExtra(ua.com.vassiliev.androidfilebrowser.FileBrowserActivity.returnFileParameter);

            if (newFile == null)
                return;

            int     n = newFile.lastIndexOf(".");
            String  fileName,
                    fileExt;

            if (n == -1)
                return;

            else {
                fileName = newFile.substring(0, n);
                fileExt = newFile.substring(n);
                if (!fileExt.equals(".geodatabase") && !fileExt.equals(".shp") && !fileExt.equals(".kml"))
                    return;

            }

          addLayer(fileName, fileExt);
        }
      }

    }

    private void addLayer(String filePath, String fileExt){

        if(fileExt.equals(".geodatabase")){

            try {
                mLocalGdb = new Geodatabase(filePath+fileExt);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            if (mLocalGdb != null) {
                for (GeodatabaseFeatureTable gdbFeatureTable : mLocalGdb.getGeodatabaseTables()) {
                    if (gdbFeatureTable.hasGeometry()) {
                        mMap.addLayer(new FeatureLayer(gdbFeatureTable));

                    }
                }
            }


        }

        else if(fileExt.equals(".shp")){
            try {
                
                mTable = new ShapefileFeatureTable(filePath+fileExt);
                mFlayer = new FeatureLayer(mTable);

            if(mTable.hasGeometry()){
                 if (mTable.getGeometryType() == Geometry.Type.POLYLINE){
                   mFlayer.setRenderer(new SimpleRenderer(new SimpleLineSymbol(Color.YELLOW, 0.50f)));
                } else if (mTable.getGeometryType() == Geometry.Type.POLYGON){
                     mFlayer.setRenderer(new SimpleRenderer(new SimpleFillSymbol(getResources().getColor(R.color.accent))));
                 } else if (mTable.getGeometryType() == Geometry.Type.POINT){
                     mFlayer.setRenderer(new SimpleRenderer(new SimpleMarkerSymbol(Color.YELLOW, 1, STYLE.CIRCLE)));
                 }
             }

             mMap.addLayer(mFlayer);
            } catch (FileNotFoundException e) {
             // TODO Auto-generated catch block
                 e.printStackTrace();
               }
            catch (Exception e1){
                Toast.makeText(getApplicationContext(), "Adding Shapefile has failed, please restart app to add shapefiles.", Toast.LENGTH_SHORT).show();
                e1.printStackTrace();
            }

        }

        else if(fileExt.equals(".kml")){
            mKmllayer = new KmlLayer(filePath+fileExt);
            mMap.addLayer(mKmllayer);
        }
    }



	@Override
	protected void onDestroy() {
		super.onDestroy();
 }
	@Override
	protected void onPause() {
		super.onPause();
		mMap.pause();
 }
	@Override
	protected void onResume() {
		super.onResume();
		mMap.unpause();
	}

}