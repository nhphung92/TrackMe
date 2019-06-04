package com.utopia.trackme.views.sessiondetails;

import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;

import android.graphics.Color;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.utopia.trackme.R;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.databinding.ActivitySessionDetailsBinding;
import com.utopia.trackme.utils.SystemUtils;
import java.util.Objects;

public class SessionDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

  private ActivitySessionDetailsBinding mBinding;
  private GoogleMap mGoogleMap;
  private MarkerOptions mFirstPlace, mLastPlace2;
  private SessionResponse mSession;
  private LatLng mStartLatLng;
  private LatLng mEndLatLng;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    SessionDetailsViewModel viewModel = ViewModelProviders.of(this)
        .get(SessionDetailsViewModel.class);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_session_details);
    setSupportActionBar(mBinding.toolbar);
    mBinding.toolbar.setNavigationOnClickListener(v -> onBackPressed());

    mSession = getIntent().getParcelableExtra(EXTRA_SESSION);
    mBinding.contentMain.distance.setText(SystemUtils.formatNumber(
        Double.valueOf(mSession.getDistance())));
    mBinding.contentMain.speed.setText(SystemUtils.formatNumber(
        Double.valueOf(mSession.getAverageSpeed())));
    mBinding.contentMain.duration
        .setText(SystemUtils.convertTime(Long.parseLong(mSession.getDuration())));

    mStartLatLng = new LatLng(
        mSession.getLocations().get(0).lat,
        mSession.getLocations().get(0).lng);

    mEndLatLng = new LatLng(
        mSession.getLocations().get(mSession.getLocations().size() - 1).lat,
        mSession.getLocations().get(mSession.getLocations().size() - 1).lng);

    mFirstPlace = new MarkerOptions().position(mStartLatLng).title("Location 1");
    mLastPlace2 = new MarkerOptions().position(mEndLatLng).title("Location 2");

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment);
    Objects.requireNonNull(mapFragment).getMapAsync(this);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    finish();
    overridePendingTransition(0, 0);
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    mGoogleMap = googleMap;

    for (int i = 0; i < mSession.getLocations().size() - 1; i++) {

      LatLng latLng1 = new LatLng(mSession.getLocations().get(i).lat,
          mSession.getLocations().get(i).lng);

      LatLng latLng2 = new LatLng(mSession.getLocations().get(i + 1).lat,
          mSession.getLocations().get(i + 1).lng);

      mGoogleMap.addPolyline(new PolylineOptions()
          .add(latLng1, latLng2)
          .width(20)
          .color(Color.RED));
    }

    mGoogleMap.addMarker(mFirstPlace);
    mGoogleMap.addMarker(mLastPlace2);

    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mEndLatLng, 17));
    mGoogleMap.animateCamera(CameraUpdateFactory
        .newCameraPosition(new CameraPosition.Builder().target(mEndLatLng).zoom(17).build()));
  }
}