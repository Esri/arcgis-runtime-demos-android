# Dev Summit 2015 ArcGIS Runtime for Android SDK Offline Capability Demo - Hawaii Five-O
The demo app showcase the offline functionality including geodatabase, KML, shapefile, raster and spatial analysis.

- Tested on 5.0 and 5.0.2, Recommended running on 5.0, as the 5.0.2 update has a bug and does not pick up the right color for the StatusBar from the Material theme
- Should be connected to internal network for the kml to work


## Features
* Geodatabase
* Direct read of KML files
* Direct read of shapefile
* Direct read of raster files
* Change raster renderer
* Line of Sight analysis
* Viewshed analysis

## Add Analysis Beta libs
This demo uses **Beta** native libs that are only available in the [SDK download](https://developers.arcgis.com/android) to support Line of Sight and Viewshed analysis. Download the SDK and follow the instructions below to work with this demo.

1. Create a **/src/main/jniLibs** directory in the demo project
2. From the root of your SDK download directory, copy the **/libs/[platform]/librs.main.so** and **/libs/[platform]/librender_script_analysis.so** into the **jniLibs** folder created in step 1.

Where **[platform]** represents the platform architecture your device is running, e.g.  **/libs/armeabi-v7a/librs.main.so** and **/libs/armeabi-v7a/librender_script_analysis.so** for ARM.

## Demo Data
This demo reads geodatabase, KML, shapefile and raster files stored on your device. The data used in the demo have been published on ArcGIS Online. You can download and copy these files to your device at the following location:

/your device external storage/ArcGIS/data

### KML files, shapefiles and geodatabases
 - **For KML**
download demo data from http://www.arcgis.com/home/item.html?id=1550ce10e06047fc9efd3a9ef052685a      
**HawaiiRadar.kml** - shows ground overlays over a network link node refreshing every 6 secs. The data is a 2 hour snapshot of radar images of Hawaii weather, showing precipitation, its motion and other data to estimate its type (rain, snow, hail etc.)

- **For shapefiles**
download demo data from http://www.arcgis.com/home/item.html?id=a438054921e4493ea5cc89b44e6a2037     
**hi_spd30m.shp** - a part of Wind Energy Resource Data collected using the MesoMap system.  Wind speed in the state of Hawaii for heights of 30 meters above ground.
http://files.hawaii.gov/dbedt/op/gis/data/wind_data.txt

- **For geodatabase**
download demo data from http://www.arcgis.com/home/item.html?id=3deb49b8f8754e0899f66053cd059bfc       
**blueprint_site_boundary_whawaii_4326.geodatabase** - NOAA Habitat Blueprint - The geodatabase for the Blueprint site on the Big Island of Hawaii, consisting of 1 polygon on the Northwest side of the island.
http://files.hawaii.gov/dbedt/op/gis/data/Blueprint_Site_Boundary_WHawaii.htm

### Raster rendering and Analysis
Two raster data are used in this demo. The Landsat 8 image is multi-bands composite image and used as the basemap of this demo. The DTED mosaic dataset provides elevation data used to perform the Line of Sight and Viewshed analysis. Download these data and copy to your devices at:

/your device external storage/ArcGIS/data/rasters/Hawaii

- ** Landsat 8 image **
download demo data from http://www.arcgis.com/home/item.html?id=7320a2d570d64055abc79ba21dec4287. It is true-color composite image of Hawaii created from Landsat 8 images of 2013 - 2014.

- ** DTED mosaic dataset **


#### Add Raster Files
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

### KML, geodatabases, shapefiles
- The user can add data from these sources to the MapView by using The add button. The add button is on the left bottom corner of the app's MapView. It can be used to select and add data from KML files, geodatabases and from shapefiles located on your device's Sdcard.
- Open files sent as attachment in an email. The device will prompt the user to open KML files and geodatabases sent as attachments. When selected the attachment is saved on to the device's internal storage and the data is added to the MapView.

## Licensing
Copyright 2015 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [license.txt](https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt) file.

For information about licensing your deployed app, see [License your app](https://developers.arcgis.com/android/guide/license-your-app.htm).
