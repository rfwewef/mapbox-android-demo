package com.mapbox.mapboxandroiddemo.labs;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.api.directions.v5.MapboxDirections;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode;
import com.mapbox.mapboxsdk.style.layers.LineLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mapbox.core.constants.Constants.PRECISION_6;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineDasharray;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineTranslate;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.lineWidth;

/**
 * Drag the map to automatically draw a directions route to wherever the map is centered.
 */
public class DottedLineDirectionsPickerActivity extends AppCompatActivity
  implements PermissionsListener, OnMapReadyCallback,
  MapboxMap.OnCameraIdleListener {

  private static final String TAG = "DottedLinePickActivity";
  private MapView mapView;
  private MapboxMap mapboxMap;
  private PermissionsManager permissionsManager;
  private LocationLayerPlugin locationPlugin;
  private FeatureCollection dottedLineDirectionsFeatureCollection;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_place_picker_dotted_directions_route);

    // Initialize the mapboxMap view
    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @SuppressWarnings( {"MissingPermission"})
  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    DottedLineDirectionsPickerActivity.this.mapboxMap = mapboxMap;

    // Enable the Mapbox Location Layer plugin so that the device location "puck" is displayed on the map.
    enableLocationPlugin();

    if (locationPlugin.getLastKnownLocation() == null) {
      Log.d(TAG, "onMapReady: locationPlugin.getLastKnownLocation() == null");
      CameraUpdateFactory.newLatLngZoom(new LatLng(38.89983, -77.03406
      ), 14);
    }

    // Add a marker on the map's center/"target" for the place picker UI
    ImageView hoveringMarker = new ImageView(this);
    hoveringMarker.setImageResource(R.drawable.red_marker);
    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
      ViewGroup.LayoutParams.WRAP_CONTENT,
      ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
    hoveringMarker.setLayoutParams(params);
    mapView.addView(hoveringMarker);

    initDottedLineLayer();
    mapboxMap.addOnCameraIdleListener(this);
  }

  @Override
  public void onCameraIdle() {
    if (mapboxMap != null) {
      Point destinationPoint = Point.fromLngLat(
        mapboxMap.getCameraPosition().target.getLongitude(),
        mapboxMap.getCameraPosition().target.getLatitude());
      getRoute(destinationPoint);
    }
  }

  /**
   * Set up a GeoJsonSource and LineLayer in order to show the directions route from the device location
   * to the place picker location
   */
  private void initDottedLineLayer() {
    dottedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(new Feature[] {});
    GeoJsonSource geoJsonSource = new GeoJsonSource("SOURCE_ID", dottedLineDirectionsFeatureCollection);
    mapboxMap.addSource(geoJsonSource);
    LineLayer dottedDirectionsRouteLayer = new LineLayer(
      "DIRECTIONS_LAYER_ID", "SOURCE_ID");
    dottedDirectionsRouteLayer.withProperties(
      lineWidth(4.5f),
      lineColor(Color.BLACK),
      lineTranslate(new Float[] {0f, 4f}),
      lineDasharray(new Float[] {1.2f, 1.2f})
    );
    mapboxMap.addLayerBelow(dottedDirectionsRouteLayer, "road-label-small");
  }

  /**
   * Make a call to the Mapbox Directions API to get the route from the device location to the
   * place picker location
   *
   * @param destination the location chosen by moving the map to the desired destination location
   */
  @SuppressWarnings( {"MissingPermission"})
  private void getRoute(Point destination) {
    MapboxDirections client = MapboxDirections.builder()
      .origin(locationPlugin.getLastKnownLocation() != null
        ? Point.fromLngLat(locationPlugin.getLastKnownLocation().getLongitude(),
        locationPlugin.getLastKnownLocation().getLatitude())
        : Point.fromLngLat(-122.3997, 37.78835029))
      .destination(destination)
      .overview(DirectionsCriteria.OVERVIEW_FULL)
      .profile(DirectionsCriteria.PROFILE_WALKING)
      .accessToken(getString(R.string.access_token))
      .build();

    client.enqueueCall(new Callback<DirectionsResponse>() {
      @Override
      public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        System.out.println(call.request().url().toString());

        // You can get the generic HTTP info about the response
        Log.d(TAG, "Response code: " + response.code());
        if (response.body() == null) {
          Log.d(TAG, "No routes found, make sure you set the right user and access token.");
          return;
        } else if (response.body().routes().size() < 1) {
          Log.d(TAG, "No routes found");
          return;
        }

        drawNavigationPolylineRoute(response.body().routes().get(0));
      }

      @Override
      public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
        Log.d(TAG, "Error: " + throwable.getMessage());
        if (!throwable.getMessage().equals("Coordinate is invalid: 0,0")) {
          Toast.makeText(DottedLineDirectionsPickerActivity.this,
            "Error: " + throwable.getMessage(), Toast.LENGTH_SHORT).show();
        }
      }
    });
  }

  /**
   * Update the GeoJson data that's part of the LineLayer.
   *
   * @param route The route to be drawn in the map's LineLayer that was set up above.
   */
  private void drawNavigationPolylineRoute(DirectionsRoute route) {
    List<Feature> directionsRouteFeatureList = new ArrayList<>();
    LineString lineString = LineString.fromPolyline(route.geometry(), PRECISION_6);
    List<Point> coordinates = lineString.coordinates();
    for (int i = 0; i < coordinates.size(); i++) {
      directionsRouteFeatureList.add(Feature.fromGeometry(LineString.fromLngLats(coordinates)));
    }
    dottedLineDirectionsFeatureCollection = FeatureCollection.fromFeatures(directionsRouteFeatureList);
    GeoJsonSource source = mapboxMap.getSourceAs("SOURCE_ID");
    if (source != null) {
      source.setGeoJson(dottedLineDirectionsFeatureCollection);
    }
  }

  @Override
  @SuppressWarnings( {"MissingPermission"})
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {
    Toast.makeText(this, R.string.user_location_permission_explanation,
      Toast.LENGTH_LONG).show();
  }

  @Override
  public void onPermissionResult(boolean granted) {
    if (granted) {
      enableLocationPlugin();
    } else {
      Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
      finish();
    }
  }

  /**
   * Enable the Mapbox Location Layer Plugin
   */
  @SuppressWarnings( {"MissingPermission"})
  private void enableLocationPlugin() {
    // Check if permissions are enabled and if not request
    if (PermissionsManager.areLocationPermissionsGranted(this)) {

      // Create an instance of the plugin. Adding in LocationLayerOptions is also an optional
      // parameter
      locationPlugin = new LocationLayerPlugin(mapView, mapboxMap);

      // Set the plugin's camera mode
      locationPlugin.setRenderMode(RenderMode.NORMAL);
      locationPlugin.setCameraMode(CameraMode.TRACKING);
      getLifecycle().addObserver(locationPlugin);
    } else {
      permissionsManager = new PermissionsManager(this);
      permissionsManager.requestLocationPermissions(this);
    }
  }

}