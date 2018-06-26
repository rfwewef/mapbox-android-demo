package com.mapbox.mapboxandroiddemo.labs;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SnapHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.api.geocoding.v5.GeocodingCriteria;
import com.mapbox.api.geocoding.v5.MapboxGeocoding;
import com.mapbox.api.geocoding.v5.models.CarmenFeature;
import com.mapbox.api.geocoding.v5.models.GeocodingResponse;
import com.mapbox.core.exceptions.ServicesException;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Use the Android system's Content Provider retrieve information about a user's upcoming calendar events.
 * Then use the event location eventTitle with Mapbox geocoding to show the event's location on the map.
 */
public class CalendarIntegrationActivity extends AppCompatActivity implements
  OnMapReadyCallback, PermissionsListener {

  private static final int MY_CAL_REQ = 0;
  private String TAG = "CalendarIntegrationActivity";
  private MapView mapView;
  public MapboxMap mapboxMap;
  private RecyclerView recyclerView;
  private CalendarEventRecyclerViewAdapter locationAdapter;
  private ArrayList<SingleCalendarEvent> listOfCalendarEvents;
  private static final int TITLE_INDEX = 1;
  private static final int EVENT_LOCATION_INDEX = 2;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_calendar_content_provider);

    recyclerView = findViewById(R.id.calendar_rv_on_top_of_map);

    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    CalendarIntegrationActivity.this.mapboxMap = mapboxMap;


    printDataFromEventTable();
  }

  // Add the mapView lifecycle to the activity's lifecycle methods
  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mapView.onDestroy();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onExplanationNeeded(List<String> permissionsToExplain) {

  }

  @Override
  public void onPermissionResult(boolean granted) {

  }

  /**
   * POJO model class for a single location in the recyclerview
   */
  class SingleCalendarEvent {

    private String eventTitle;
    private String eventLocation;
    private LatLng locationCoordinates;

    public SingleCalendarEvent() {

    }

    public SingleCalendarEvent(String eventTitle, String eventDescription, String eventLocation) {
      this.eventTitle = eventTitle;
      this.eventLocation = eventLocation;
    }

    public String getEventTitle() {
      return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
      this.eventTitle = eventTitle;
    }

    public String getEventLocation() {
      return eventLocation;
    }

    public void setEventLocation(String eventLocation) {
      this.eventLocation = eventLocation;
    }

    public LatLng getLocationCoordinates() {
      return locationCoordinates;
    }

    public void setLocationCoordinates(LatLng locationCoordinates) {
      this.locationCoordinates = locationCoordinates;
    }
  }

  static class CalendarEventRecyclerViewAdapter extends
    RecyclerView.Adapter<CalendarEventRecyclerViewAdapter.MyViewHolder> {

    private List<SingleCalendarEvent> locationList;
    private MapboxMap map;

    public CalendarEventRecyclerViewAdapter(List<SingleCalendarEvent> locationList, MapboxMap mapBoxMap) {
      this.locationList = locationList;
      this.map = mapBoxMap;
    }

    @Override
    public CalendarEventRecyclerViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View itemView = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.calendar_rv_card, parent, false);
      return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(CalendarEventRecyclerViewAdapter.MyViewHolder holder, int position) {
      SingleCalendarEvent singleCalendarEvent = locationList.get(position);
      holder.title.setText(singleCalendarEvent.getEventTitle());
      holder.setClickListener(new ItemClickListener() {
        @Override
        public void onClick(View view, int position) {
          LatLng selectedLocationLatLng = locationList.get(position).getLocationCoordinates();
          CameraPosition newCameraPosition = new CameraPosition.Builder()
            .target(selectedLocationLatLng)
            .build();

          map.easeCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition));
        }
      });
    }


    @Override
    public int getItemCount() {
      return locationList.size();
    }

    static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
      TextView title;
      CardView singleCard;
      ItemClickListener clickListener;

      MyViewHolder(View view) {
        super(view);
        title = view.findViewById(R.id.calendar_event_title);
        singleCard = view.findViewById(R.id.single_calendar_event_cardview);
        singleCard.setOnClickListener(this);
      }

      public void setClickListener(ItemClickListener itemClickListener) {
        this.clickListener = itemClickListener;
      }

      @Override
      public void onClick(View view) {
        clickListener.onClick(view, getLayoutPosition());
      }
    }
  }

  public interface ItemClickListener {
    void onClick(View view, int position);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         String permissions[], int[] grantResults) {
    switch (requestCode) {
      case MY_CAL_REQ: {
        if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          Log.d(TAG, "onRequestPermissionsResult: calendar granted");
          printDataFromEventTable();
        } else {
          Toast.makeText(this, R.string.user_calendar_permission_explanation, Toast.LENGTH_LONG).show();
        }
        return;
      }
    }
  }

  public void printDataFromEventTable() {
    if (ContextCompat.checkSelfPermission(this,
      Manifest.permission.READ_CALENDAR)
      != PackageManager.PERMISSION_GRANTED) {

      // Permission is not granted, so check whether calendar needs a rationale
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.READ_CALENDAR)) {
      } else {
        // No explanation needed, so request the calendar permission
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CALENDAR}, MY_CAL_REQ);
      }
    } else {
      String[] projection = new String[] {"calendar_id", "title", "eventLocation"};
      Cursor cur = this.getContentResolver()
        .query(
          Uri.parse("content://com.android.calendar/events"), projection, null, null, null);

      listOfCalendarEvents = new ArrayList<>();

      int index = 0;
      while (cur.moveToNext()) {
        if (index <= 20) {
          String location = null;
          String title = null;

          title = cur.getString(TITLE_INDEX);
          location = cur.getString(EVENT_LOCATION_INDEX);

          if (!location.isEmpty()) {
            Log.d(TAG, "printDataFromEventTable: title = " + title);
            Log.d(TAG, "printDataFromEventTable: location = " + location);

            SingleCalendarEvent singleCalendarEvent = new SingleCalendarEvent();
            singleCalendarEvent.setEventTitle(title);
            makeMapboxGeocodingRequest(location, singleCalendarEvent);
          }
          index++;
        }
      }

      initRecyclerView();
    }
  }

  private void makeMapboxGeocodingRequest(String eventLocation, SingleCalendarEvent singleCalendarEvent) {
    try {
      // Build a Mapbox geocoding request
      MapboxGeocoding client = MapboxGeocoding.builder()
        .accessToken(getString(R.string.access_token))
        .query(eventLocation)
        .geocodingTypes(GeocodingCriteria.MODE_PLACES)
        .mode(GeocodingCriteria.MODE_PLACES)
        .build();
      client.enqueueCall(new Callback<GeocodingResponse>() {
        @Override
        public void onResponse(Call<GeocodingResponse> call,
                               Response<GeocodingResponse> response) {
          List<CarmenFeature> results = response.body().features();
          if (results.size() > 0) {

            Log.d(TAG, "onResponse: results.size() > 0");

            // Get the first Feature from the successful geocoding response
            CarmenFeature feature = results.get(0);
            if (feature != null) {

              Log.d(TAG, "onResponse: feature = " + feature);
              Log.d(TAG, "onResponse: feature center to string = " + feature.center().toString());

              Point featurePoint = (Point) feature.geometry();

              Log.d(TAG, "onResponse: feature point lat = " + featurePoint.latitude());
              Log.d(TAG, "onResponse: feature point long = " + featurePoint.longitude());

              LatLng featureLatLng = new LatLng(feature.center().latitude(), feature.center().longitude());

              if (featureLatLng != null) {
                singleCalendarEvent.setLocationCoordinates(featureLatLng);
                listOfCalendarEvents.add(singleCalendarEvent);
                addEventLocationMarker(featureLatLng, singleCalendarEvent.getEventTitle());
              }
            }

          } else {
            Toast.makeText(CalendarIntegrationActivity.this, R.string.no_results,
              Toast.LENGTH_SHORT).show();
          }
        }

        @Override
        public void onFailure(Call<GeocodingResponse> call, Throwable throwable) {
          Timber.e("Geocoding Failure: " + throwable.getMessage());
        }
      });
    } catch (ServicesException servicesException) {
      Timber.e("Error geocoding: " + servicesException.toString());
      servicesException.printStackTrace();
    }
  }

  private void addEventLocationMarker(LatLng coordinates, String eventTitle) {
    mapboxMap.addMarker(new MarkerOptions()
      .position(coordinates)
      .title(eventTitle));
  }

  private void initRecyclerView() {
    // Set up the recyclerView
    if (listOfCalendarEvents.size() > 0) {
      locationAdapter = new CalendarEventRecyclerViewAdapter(listOfCalendarEvents, mapboxMap);
      recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext(),
        LinearLayoutManager.HORIZONTAL, true));
      recyclerView.setItemAnimator(new DefaultItemAnimator());
      recyclerView.setAdapter(locationAdapter);
      SnapHelper snapHelper = new LinearSnapHelper();
      snapHelper.attachToRecyclerView(recyclerView);
    }
  }
}