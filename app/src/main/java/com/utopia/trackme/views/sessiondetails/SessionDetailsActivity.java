package com.utopia.trackme.views.sessiondetails;

import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;

import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.utopia.trackme.R;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.databinding.ActivitySessionDetailsBinding;
import com.utopia.trackme.utils.FetchURL;
import com.utopia.trackme.utils.MyUtils;
import com.utopia.trackme.utils.TaskLoadedCallback;
import com.utopia.trackme.views.main.LocationActivity;
import java.util.Objects;

public class SessionDetailsActivity extends AppCompatActivity implements OnMapReadyCallback,
    TaskLoadedCallback {

  private static final String TAG = LocationActivity.class.getSimpleName();

  ActivitySessionDetailsBinding mBinding;
  private GoogleMap mGoogleMap;
  private MarkerOptions place1, place2;
  private Polyline currentPolyline;
  SessionResponse mSession;
  LatLng startLatLng;
  LatLng endLatLng;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_session_details);
    setSupportActionBar(mBinding.toolbar);
    mBinding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

    mSession = getIntent().getParcelableExtra(EXTRA_SESSION);
    mBinding.contentMain.distance.setText(String.valueOf(mSession.getDistance()));
    mBinding.contentMain.duration.setText(MyUtils.convertTime((long) mSession.getDuration()));
    mBinding.contentMain.speed.setText(String.valueOf(mSession.getAverageSpeed()));

    mBinding.contentMain.btnGetDirection.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        new FetchURL(SessionDetailsActivity.this)
            .execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");
      }
    });

    //27.658143,85.3199503
    //27.667491,85.3208583

    startLatLng = new LatLng(27.658143, 85.3199503);
    endLatLng = new LatLng(
        mSession.getLocations().get(mSession.getLocations().size() - 1).lat,
        mSession.getLocations().get(mSession.getLocations().size() - 1).lng);

    place1 = new MarkerOptions().position(startLatLng).title("Location 1");
    place2 = new MarkerOptions().position(endLatLng).title("Location 2");

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment);
    Objects.requireNonNull(mapFragment).getMapAsync(this);
  }

  private String getUrl(LatLng origin, LatLng dest, String directionMode) {
    // Origin of route
    String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
    // Destination of route
    String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
    // Mode
    String mode = "mode=" + directionMode;
    // Building the parameters to the web service
    String parameters = str_origin + "&" + str_dest + "&" + mode;
    // Output format
    String output = "json";
    // Building the url to the web service
    String url =
        "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key="
            + getString(R.string.google_maps_key);
    return url;
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
    overridePendingTransition(0, 0);
  }

  private void moveCamera(Location location) {
    if (mGoogleMap != null) {
      LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
      mGoogleMap.clear();
      mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
      mGoogleMap.animateCamera(CameraUpdateFactory
          .newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(15).build()));
    }
  }

  private void addMarker(LatLng latLng) {
    mGoogleMap.clear();
    mGoogleMap.addMarker(new MarkerOptions().position(latLng));
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mGoogleMap = googleMap;
    Log.d("mylog", "Added Markers");
    mGoogleMap.addMarker(place1);
    mGoogleMap.addMarker(place2);

    CameraPosition googlePlex = CameraPosition.builder()
        .target(endLatLng)
        .zoom(7)
        .bearing(0)
        .tilt(45)
        .build();

    mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(googlePlex), 5000, null);
  }

  @Override
  public void onTaskDone(Object... values) {
    if (currentPolyline != null) {
      currentPolyline.remove();
    }
    currentPolyline = mGoogleMap.addPolyline((PolylineOptions) values[0]);
  }
}