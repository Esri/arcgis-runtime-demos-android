Google Glass App
=======================
This app showcases how to display and interact with a map on the [Google Glass](https://developers.google.com/glass/). This sample includes head-tracking control using the Glass's built in sensors, as well as control using the [Thalmic Myo](https://www.thalmic.com/en/myo/). 

## Instructions
To launch the map, you can say "Ok Google... Show me a map." Then, when it says "click to show map", tap the touch pad to enter the map immersion.

To activate the head-tracking controls, do a two-finger long press on the trackpad. Once enabled, you can look left/right/up/down to pan in the corresponding direction. Doing a two finger scroll along the trackpad will zoom in/out of the map.

Considering it's still a bit of a beta device, many of you are unlikely to own or have access to a Myo. You may either comment out the related code, or simply download the SDK to allow the code to compile. In either case, you must modify the "app" module's build.gradle, to either remove the myo repository or point it to the local directory on your machine. In the event that you do have access to a Myo, the gestures are mapped as follows:

  * **Double-Tap**: Unlocks the Myo if it is locked, or locks the Myo if it is unlocked

**Map Control Mode**

  * **Fingers Spread**: Enabled/disables panning mode, which allows you to move your arm in the direction you want the map panned.
  
  * **Wave In**: Zooms out.
   
  * **Wave Out**: Zooms in.
  
  * **Fist**: While fist is held, you can roll your arm to rotate the map.

**Feature Control Mode**

  * **Fingers Spread**: Sets the currently visible features as the "selected" features, and highlights the first in the list.
  
  * **Wave In**: Highlight previous feature.
  
  * **Wave Out**: Highlight next feature.
  
  * **Fist**: Display popup for currently highlighted feature.


## Licensing
Copyright 2015 Esri

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

A copy of the license is available in the repository's [license.txt](https://github.com/Esri/arcgis-runtime-demos-android/blob/master/license.txt) file.

For information about licensing your deployed app, see [License your app](https://developers.arcgis.com/android/guide/license-your-app.htm).
