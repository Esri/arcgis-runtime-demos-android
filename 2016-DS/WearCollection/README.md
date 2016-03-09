Wear Collection App
=======================
This app showcases the ability to collect new features using an [Android Wear](http://www.android.com/wear/) device. The wear device presents the user with a list of recently used feature layers, and then with the feature types for the selected feature layer. The user can then choose a feature type and a new feature of that type will be collected at the device's current location. 

## Requirements
* Android Studio
* An Android Wear device
* An Android mobile device

## Instructions
You must install the "mobile" module onto a phone/tablet, and the "wear" module onto your Wear device. Below are separate instructions for how you can use both the mobile and wear applications

### Mobile app
The mobile app starts with a listview that is by default populated with a single entry, the Recreation sample feature layer. New layers can be added by clicking the floating action button. This will show a dialog in which you can enter the name and URL of a feature layer.

Once a feature layer is selected, a MapView will be shown with a Topographic basemap and the selected feature layer on top. To collect a new feature, click the floating action button and select the feature type to add, then click the "Add" dialog button.

### Wear app
The wear app starts by stating that it is fetching the feature layers. At this point it is requesting the list of layers established in the mobile app. Once a response is received, the layers are shown in a list on the wear device. The user can scroll through these and select one, which will then state that it is fetching feature types. At this point it is requesting the list of feature types available in the selected layer. Once a response is received, the feature types are shown in a list on the wear device. The user can scroll through tese and select one, which will then state that the feature is being collected. During this, the mobile device uses its current location to collect a new feature of the specified type.

If the collection is successful, the wear device will display a message in green stating that it was a success. If it failed, the wear device will display a message in red stating that it was a failure. In either case, the message will dismiss after a couple seconds.

## Licensing
Copyright 2016 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [license.txt](https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt) file.

For information about licensing your deployed app, see [License your app](https://developers.arcgis.com/android/guide/license-your-app.htm).
