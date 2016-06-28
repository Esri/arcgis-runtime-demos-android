# EDS 2015

Some Android demo apps from sessions at the European Developer Summit 2015, held in Berlin.

http://www.esri.com/events/devsummit-europe

## Device Location

A very simple app showing the different modes available for LocationDisplayManager - navigation, compass, and default.

This app is written to target Marshmallow devices, and also demonstrates workflow for asking for location permissions, using the support library permissions API.

Uses ArcGIS Runtime SDK for Android version 10.2.7.

## JumpZoom

Quartz Beta 2 demo showing how to chain together asynchronous navigation methods, waiting until one is complete before beginning a second navigation call. If first navigation is cancelled by the user interacting with the MapView, then the second navigation call is not made. Uses MapView navigation methods that return ListenableFuture.

Uses ArcGIS Runtime SDK for Android Quartz release, Beta 2.

***Note: Quartz Beta 2 was released in March 2016. Beta 3 is now available.***
