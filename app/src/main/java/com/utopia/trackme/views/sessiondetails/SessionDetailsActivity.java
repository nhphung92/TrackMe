package com.utopia.trackme.views.sessiondetails;

import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
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
import com.utopia.trackme.utils.SystemUtils;
import com.utopia.trackme.utils.TaskLoadedCallback;
import java.text.DecimalFormat;
import java.util.Objects;

public class SessionDetailsActivity extends AppCompatActivity implements OnMapReadyCallback,
    TaskLoadedCallback {

  ActivitySessionDetailsBinding mBinding;
  private GoogleMap mGoogleMap;
  private MarkerOptions mPlace1, mPlace2;
  private Polyline mCurrentPolyline;
  SessionResponse mSession;
  LatLng mStartLatLng;
  LatLng mEndLatLng;
  private Polyline currentPolyline;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SessionDetailsViewModel viewModel = ViewModelProviders.of(this)
        .get(SessionDetailsViewModel.class);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_session_details);
    setSupportActionBar(mBinding.toolbar);
    mBinding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

    mSession = getIntent().getParcelableExtra(EXTRA_SESSION);
    mBinding.contentMain.distance
        .setText(new DecimalFormat("#.###").format(mSession.getDistance()));
    mBinding.contentMain.duration.setText(SystemUtils.convertTime((long) mSession.getDuration()));
    mBinding.contentMain.speed.setText(String.valueOf(mSession.getAverageSpeed()));

    mStartLatLng = new LatLng(
        mSession.getLocations().get(0).lat,
        mSession.getLocations().get(0).lng);

    mEndLatLng = new LatLng(
        mSession.getLocations().get(mSession.getLocations().size() - 1).lat,
        mSession.getLocations().get(mSession.getLocations().size() - 1).lng);

    mPlace1 = new MarkerOptions().position(mStartLatLng).title("Location 1");
    mPlace2 = new MarkerOptions().position(mEndLatLng).title("Location 2");

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment);
    Objects.requireNonNull(mapFragment).getMapAsync(this);

    viewModel.getPolylineOptions().observe(this, polylineOptions -> {
      if (mCurrentPolyline != null) {
        mCurrentPolyline.remove();
      }
      mCurrentPolyline = mGoogleMap.addPolyline(polylineOptions);
    });
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
    overridePendingTransition(0, 0);
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
    return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key="
        + getString(R.string.google_maps_key);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mGoogleMap = googleMap;
    mGoogleMap.addMarker(mPlace1);
    mGoogleMap.addMarker(mPlace2);

    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mEndLatLng, 15));
    mGoogleMap.animateCamera(CameraUpdateFactory
        .newCameraPosition(new CameraPosition.Builder().target(mEndLatLng).zoom(15).build()));

    new FetchURL(this)
        .execute(getUrl(mPlace1.getPosition(), mPlace2.getPosition(), "driving"), "driving");
  }

  @Override
  public void onTaskDone(Object... values) {
    if (currentPolyline != null) {
      currentPolyline.remove();
    }
    currentPolyline = mGoogleMap.addPolyline((PolylineOptions) values[0]);
  }
}