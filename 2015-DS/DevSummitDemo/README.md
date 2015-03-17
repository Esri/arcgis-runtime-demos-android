# Dev Summit 2015 ArcGIS Runtime for Android SDK Offline Capability Demo - Hawaii Five-O
The demo app showcase the offline functionality including geodatabase, KML, shapefile, raster and spatial analysis.

- Tested on 5.0 and 5.0.2, Recommended running on 5.0, as the 5.0.2 update has a bug and does not pick up the right color for the statusbar from the Material theme 
- Should be connected to internal network for the kml to work 


## Features
* Geodatabase 
* Direct read of KML files
* Direct read of Shapefile
* Direct read of raster files 
* Change raster renderer
* Line of Sight analysis
* Viewshed analysis

## Demo Data
- EZ copy demo data-  copy the **data** folder from /apps-data/Data/sdk/android/Hawaii_Five-O/ to your device at the following location
/your device external storage/ArcGIS/        
This will give you all the data you need for the demo. 

# For Raster rendering and Analysis      
- Copy demo data from /apps-data/Data/sdk/android/Hawaii_Five-O/data/rasters/Hawaii to your device at the following location.
/your device external storage/ArcGIS/data/rasters/Hawaii       
      
# Kml files, Shape files and Geodatabases             
 - **For Kml**            
copy demo data from /apps-data/Data/sdk/android/Hawaii_Five-O/data/kml/      
**HawaiiRadar.kml** - shows ground overlays over a network link node refreshing every 6 secs. The data is a 2 hour snapshot of radar images of Hawaii weather, showing prescipitation, its motion and other data to estimate its type (rain, snow, hail etc.)         
            
- **For Shapefiles**           
copy demo data from /apps-data/Data/sdk/android/Hawaii_Five-O/data/shapefiles/      
**hi_spd30m.shp** - a part of Wind Energy Resource Data collected using the MesoMap system.  Wind speed in the state of Hawaii for heights of 30 meters above ground. 
http://files.hawaii.gov/dbedt/op/gis/data/wind_data.txt       

- **For Geodatabase**       
copy demo data from /apps-data/Data/sdk/android/Hawaii_Five-O/data/geodatabase/        
**blueprint_site_boundary_whawaii_4326.geodatabase** - NOAA Habitat Blueprint - The shapefile for the Blueprint site on the Big Island of Hawaii, consisting of 1 polygon on the Northwest side of the island.     
http://files.hawaii.gov/dbedt/op/gis/data/Blueprint_Site_Boundary_WHawaii.htm     
     
### Add Analysis Beta libs
This demo uses **Beta** native libs that are only available in the [SDK download](https://developers.arcgis.com/android).  Download the SDK and follow the instructions below to work with this demo.

1. Create a **/src/main/jniLibs** directory in the demo project
2. From the root of your SDK download directory, copy the **/libs/[platform]/librs.main.so** and **/libs/[platform]/librender_script_analysis.soo** into the **jniLibs** folder created in step 1.

Where **[platform]** represents the platform architecture your device is running, e.g.  **/libs/armeabi-v7a/librs.main.so** and **/libs/armeabi-v7a/librender_script_analysis.soo** for ARM.

### Add Raster Files
You will need to provision some raster files to your android device prior to working with this demo.  You can put your raster file anywhere on your device that the app has access to. By default the app will look for your raster file starting from the primary external storage directory returned by ```Environment.getExternalStorageDirectory()``` with **ArcGIS/data/rasters/Hawaii/** subdirectory. You can change the data path by editing the **string.xml** resource file.  Open **strings.xml** and edit the following string resource variables:

```xml
    <!-- raster data paths -->
    <string name="raster_dir">ArcGIS/data/rasters/Hawaii</string>
    <!-- raster file name of basemap layer -->
    <string name="raster_basemap_layer">Landsat8Hawaii_naturecolor.tif</string>
    <!-- raster file name of elevation data -->
    <string name="raster_task">DTED/hawaii_dted.sqlite</string>
```

#### Push file to device
The following ```adb``` command is used to **push** files to your device:  

```
$ adb push <local> <remote>
```

In the commands, ```<local>``` and ```<remote>``` refer to the paths to the target file/directory on your development machine and the device.  For Example:  

```
$ adb push raster.tiff /sdcard/ArcGIS/data/rasters/Hawaii
```

## Demo Usage
**demo**
This demo app supports both Line of Sight and Viewshed analysis on an elevation raster type. The app will open to with the raster file as the basemap of the map. It also allow users to change the renderer of the raster layer.

* Select the ```ActionBar``` gallery button. And choose between **Blend renderer** and **RGB renderer** for renderer.
* Select the ```ActionBar``` overlay button. And choose between **Line of Sight** and **Viewshed** for analysis

### Line of Sight
* The default observer is the center of the map
* Long press on the map to change the observer's position
* Single tap the map to change the target's position and execute the **Line of Sight** function
  * Alternatively, you can drag-move on the map to change the target's position

### Viewshed
* Single tap on the map to change the observer's position and execute the **Viewshed** function
  * Alternatively, you can drag-move on the map to change the observer's position

### Kml, Geodatabases, Shapefiles
- The user can add data from these sources to the MapView by using The add button. The add button is on the left bottom corner of the app's Mapview. It can be used to select and add data from kmlfiles, geodatabases and from shapefiles located on your device's Sdcard.        
- Open files sent as attachements in email. The device will prompt the user to open kml files and geodatabases sent as attachments. When selected the attachement is saved on to the device's internal storage and the data is added to the MapView.
