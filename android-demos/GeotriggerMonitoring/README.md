# android-geotrigger-monitoring-demo

This code sample demonstrates monitoring Geotriggers in a Foreground Service on Android. Learn more in the associated [blog post]().

The directory `GeotriggerMonitoringDemo-WithoutGeotriggers` demonstrates the app before adding Geotrigger monitoring, whereas the directory `GeotriggerMonitoringDemo-WithGeotriggers` demonstrates the app after adding Geotrigger monitoring.

![android-geotrigger-monitoring-demo](android-geotrigger-monitoring-demo.png)

## Features
* Geotriggers - Monitor device location as it enters and exits areas of interest
* Android best practices - Uses a clean architecture approach to using the ArcGIS Runtime SDK for Android

## Instructions

1. Fork and then clone the repo.
2. Set your API key in the `app/res/values/strings.xml` file. *
3. Run the app and accept permission requests.
4. Long-press the map to create a point of interest.
5. Tap "Start Monitoring" to create a geotrigger with fences at the points of interest, buffered by 50.0 meters.
6. Tap your device's home button to move the app to the background. Observe toast messages as your device enters and exits fences.
7. Pull down the notification bar to view the foreground notification and tap "Stop" to stop monitoring.
8. Tap "Clear points" to remove all points of interest.

* API key: A permanent key that gives your application access to Esri location services. Visit your ArcGIS Developers Dashboard to create a new API key or access an existing API key.

## Requirements

* Android device at API 23 or above
* Network access

## Resources

* [Maintaining Clean Architecture on Android with Geotriggers using Dependency Injection]()
* [Work with Geotriggers on Android](https://developers.arcgis.com/android/device-location/work-with-geotriggers/)
* [Work with Geotriggers on iOS](https://developers.arcgis.com/ios/device-location/work-with-geotriggers/)

## Issues

Find a bug or want to request a new feature?  Please let us know by submitting an issue.

## Contributing

Esri welcomes contributions from anyone and everyone. Please see our [guidelines for contributing](https://github.com/esri/contributing).

## Licensing
Copyright 2021 Esri

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

A copy of the license is available in the repository's [license.txt](license.txt) file.​
