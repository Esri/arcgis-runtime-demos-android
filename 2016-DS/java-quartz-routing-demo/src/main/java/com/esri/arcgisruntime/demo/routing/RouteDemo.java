/*
 * Copyright 2016 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.esri.arcgisruntime.demo.routing;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Map;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;
import com.esri.arcgisruntime.tasks.route.DirectionMessage;
import com.esri.arcgisruntime.tasks.route.Route;
import com.esri.arcgisruntime.tasks.route.RouteParameters;
import com.esri.arcgisruntime.tasks.route.RouteResult;
import com.esri.arcgisruntime.tasks.route.RouteTask;
import com.esri.arcgisruntime.tasks.route.Stop;

/**
 * Demonstrates how to find a route between two {@link Stop}s on a Map using an
 * offline {@link Route} route package.
 * <p>
 * Use {@link RouteParameters} to define a {@link RouteTask} when you want to
 * find a route between {@link Stop}s. Offline route task solves a route from an
 * offline routing package. Typically this type of network analysis is known as
 * routing, example being business {@link DirectionMessage}s that changes
 * frequently, such as displaying a route stops of vehicles as they make
 * deliveries.
 * <p>
 * To add a Route Task from an offline service:
 * <ol>
 * <li>Create an RouteTask using an URL from an online service.</li>
 * <li>Set its parameters via {@link RouteParameters}.</li>
 * <li>Display the route by adding it to a {@link GraphicsOverlay}.</li>
 * </ol>
 */
public class RouteDemo extends Application {

  private MapView mapView;
  private RouteTask routeTask;
  private RouteParameters routeParameters;
  private LocatorTask locatorTask;
  private ReverseGeocodeParameters locatorParameters;

  private FadeTransition anim;
  private Label streetLabel = new Label();
  private Label addressLabel = new Label();
  private Label coordinateLabel = new Label();

  private final NumberFormat formatter = new DecimalFormat("#0.00000");

  private Point start;
  private boolean calculating = false;

  private GraphicsOverlay routeGraphicsOverlay = new GraphicsOverlay();
  
  @Override
  public void start(Stage stage) throws Exception {

    // create javafx scene
    StackPane stackPane = new StackPane();
    Scene scene = new Scene(stackPane);
    stage.setTitle("Routing Demo");
    stage.setWidth(800);
    stage.setHeight(700);
    stage.setScene(scene);
    stage.show();

    // create a box to show the stop address
    VBox addressBox = new VBox();
    addressBox.setMaxSize(200, 50);
    addressBox.setOpacity(0); //invisible
    addressBox.setStyle(
        "-fx-padding: 10; -fx-spacing: 8; -fx-background-color: #fff; -fx-effect: dropshadow(gaussian, #888, 10, 0, 0, 0)}");
    streetLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #000;");
    addressLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #000;");
    coordinateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #808080;");
    addressBox.getChildren().addAll(streetLabel, addressLabel, coordinateLabel);

    // create a transition animation
    anim = new FadeTransition(Duration.millis(500), addressBox);
    anim.setInterpolator(Interpolator.EASE_OUT);
    anim.setFromValue(0);
    anim.setToValue(1);

    try {
      // create a map with a Streets basemap
      Map map = new Map(Basemap.createStreets());

      // set the map to be displayed in this view 
      mapView = new MapView();
      mapView.setMap(map);

      // set the viewpoint over San Francisco
      mapView.setViewpointCenterWithScaleAsync(new Point(-13630860.859476, 4546121.380675, SpatialReferences
          .getWebMercator()), 20000);

      // add the graphic overlays to the map view
      mapView.getGraphicsOverlays().add(routeGraphicsOverlay);

      // create the route task
	  routeTask = new RouteTask("./data/RuntimeSanFrancisco.geodatabase", "Streets_ND");
      routeTask.loadAsync();
      routeTask.addDoneLoadingListener(() -> {
        try {
          routeParameters = routeTask.generateDefaultParametersAsync().get();
          routeParameters.setOutputSpatialReference(SpatialReferences.getWebMercator());
          System.out.println("Route task successfully loaded");
        } catch (Exception e) {
          e.printStackTrace();
        }
      });

      // create the locator task
			locatorTask = new LocatorTask("./data/SanFranciscoLocator.loc");
      locatorTask.loadAsync();
      locatorTask.addDoneLoadingListener(() -> {
        try {
          locatorParameters = new ReverseGeocodeParameters();
          locatorParameters.setOutputSpatialReference(SpatialReferences.getWebMercator());
          locatorParameters.getResultAttributeNames().add("*"); //return all attributes
          System.out.println("Locator task successfully loaded");
        } catch (Exception e) {
          e.printStackTrace();
        }
      });

      SimpleMarkerSymbol stopSymbol = new SimpleMarkerSymbol(0xFF00FFFF, 10, SimpleMarkerSymbol.Style.CIRCLE);
      SimpleLineSymbol pathSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00FFFF, 5);

      // create mouse moved event handler
      MouseMovedHandler handler = new MouseMovedHandler();

      // start or stop route task
      mapView.setOnMouseClicked(evt -> {

        if (evt.isStillSincePress() && evt.getButton() == MouseButton.PRIMARY) {
          Point2D clickLocation = new Point2D(evt.getX(), evt.getY());
          Point click = mapView.screenToLocation(clickLocation);

          if (calculating) {
            calculating = false;
            mapView.setOnMouseMoved(null); //remove the mouse moved handler
            Graphic finalGraphic = new Graphic(click, stopSymbol);
            routeGraphicsOverlay.getGraphics().add(finalGraphic);
          } else {
            start = click;
            anim.play();
            routeParameters.getStops().clear();
            routeParameters.getStops().add(new Stop(start));
            routeParameters.getStops().add(new Stop(start));
            routeGraphicsOverlay.getGraphics().clear();
            Graphic startGraphic = new Graphic(start, stopSymbol);
            PointCollection stops = new PointCollection(Arrays.asList(new Point[] {
                start, start
            }));
            Graphic routeGraphic = new Graphic(new Polyline(stops), pathSymbol);
            routeGraphicsOverlay.getGraphics().add(startGraphic);
            routeGraphicsOverlay.getGraphics().add(routeGraphic);
            calculating = true;
            mapView.setOnMouseMoved(handler);
          }
        }
      });

      // add the map view and control panel to stack pane
      stackPane.getChildren().addAll(mapView, addressBox);
      StackPane.setAlignment(addressBox, Pos.BOTTOM_CENTER);
      StackPane.setMargin(addressBox, new Insets(0, 0, 15, 0));

    } catch (Exception e) {
      // on any error, display the stack trace.
      e.printStackTrace();
    }
  }

  private class MouseMovedHandler implements EventHandler<MouseEvent> {

    @Override
    public void handle(MouseEvent event) {
      Point stop = mapView.screenToLocation(new Point2D(event.getX(), event.getY()));
      routeParameters.getStops().remove(1);
      routeParameters.getStops().add(new Stop(stop));

      // run the route task
      ListenableFuture<RouteResult> routeResults = routeTask.solveAsync(routeParameters);
      routeResults.addDoneListener(() -> {
        try {
          // update the route
          RouteResult routeResult = routeResults.get();
          List<Route> routes = routeResult.getRoutes();
          Route route = routes.get(0);
          Geometry path = route.getRouteGeometry();
          routeGraphicsOverlay.getGraphics().get(1).setGeometry(path);

          // run the locator geocode task
          ListenableFuture<List<GeocodeResult>> locatorResults = locatorTask.reverseGeocodeAsync(stop,
              locatorParameters);
          locatorResults.addDoneListener(() -> {
            try {
              List<GeocodeResult> locatorResult = locatorResults.get();
              GeocodeResult geocode = locatorResult.get(0);
              String street = geocode.getAttributes().get("Street").toString();
              String city = geocode.getAttributes().get("City").toString();
              String state = geocode.getAttributes().get("State").toString();
              String zip = geocode.getAttributes().get("ZIP").toString();
              Platform.runLater(() -> {
                streetLabel.setText(street);
                addressLabel.setText(city + ", " + state + " " + zip);
                coordinateLabel.setText(formatter.format(stop.getX()) + ", " + formatter.format(stop.getY()));
              });
            } catch (Exception e) {
              //e.printStackTrace();
            }
          });
        } catch (Exception e) {
          //e.printStackTrace();
        }
      });
    }
  }

  /**
   * Stops and releases all resources used in application.
   */
  @Override
  public void stop() throws Exception {

    if (mapView != null) {
      mapView.dispose();
    }
  }

  /**
   * Opens and runs application.
   *
   * @param args arguments passed to this application
   */
  public static void main(String[] args) {

    Application.launch(args);
  }
}
