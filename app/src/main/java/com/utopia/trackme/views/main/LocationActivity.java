package com.utopia.trackme.views.main;

import static com.utopia.trackme.utils.Constants.BROADCAST_DETECTED_LOCATION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_CODE;
import static com.utopia.trackme.utils.MyConstants.SEND_DURATION;
import static com.utopia.trackme.utils.MyConstants.SEND_LOCATION;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.utopia.trackme.R;
import com.utopia.trackme.databinding.ActivityLocationBinding;
import com.utopia.trackme.services.LocationService;
import com.utopia.trackme.views.sessions.SessionsActivity;
import java.util.Objects;

public class LocationActivity extends AppCompatActivity {

  private static final String TAG1 = LocationActivity.class.getSimpleName();
  private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private static final int REQUEST_LOCATIONS = 100;

  ActivityLocationBinding mBinding;

  private GoogleMap mGoogleMap;

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;
  private boolean hasMarket = false;

  BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
      if (BROADCAST_DETECTED_LOCATION.equals(intent.getAction())) {

        int code = intent.getIntExtra(EXTRA_CODE, 0);

        switch (code) {
          case SEND_DURATION:
            mBinding.contentMain.duration.setText(intent.getStringExtra("duration"));
            break;
          case SEND_LOCATION:
            double latitude = intent.getDoubleExtra("latitude", 0);
            double longitude = intent.getDoubleExtra("longitude", 0);
            mBinding.contentMain.location.setText(latitude + ", " + longitude);
            mBinding.contentMain.address.setText(intent.getStringExtra("address"));
            mBinding.contentMain.distance.setText(intent.getStringExtra("distance"));
            mBinding.contentMain.speed.setText(intent.getStringExtra("speed"));

            if (!hasMarket) {
              hasMarket = true;
              addMarker(new LatLng(latitude, longitude));
            }
            break;
        }
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_location);
    setSupportActionBar(mBinding.toolbar);

    LocalBroadcastManager.getInstance(this)
        .registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_DETECTED_LOCATION));

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        moveCamera(locationResult.getLastLocation());
      }
    };

    mLocationRequest = new LocationRequest();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    // add config location
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment);

    Objects.requireNonNull(mapFragment).getMapAsync(this::onMyMapReady);

    mBinding.contentMain.setStatus("record");
    mBinding.contentMain.record.setOnClickListener(v -> startTracking());
    mBinding.contentMain.pause.setOnClickListener(v -> stopTracking());
    mBinding.contentMain.refresh.setOnClickListener(v -> stopTracking());
    mBinding.contentMain.stop.setOnClickListener(v -> stopTracking());
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkPlayServices()) {
      mBinding.contentMain.location.setText(R.string.check_play_services_msg);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
  }

  private boolean checkPlayServices() {
    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
    int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
      if (apiAvailability.isUserResolvableError(resultCode)) {
        apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
      } else {
        finish();
      }
      return false;
    }
    return true;
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_history) {
      startActivity(new Intent(this, SessionsActivity.class));
      overridePendingTransition(0, 0);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void onMyMapReady(GoogleMap googleMap) {
    this.mGoogleMap = googleMap;
    this.mGoogleMap.setOnMapLoadedCallback(this::checkPermissionsLocation);
    this.mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    this.mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
  }

  private void startLocationUpdates() {
    mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
        .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
          @SuppressLint("MissingPermission")
          @Override
          public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            mFusedLocationClient
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
          }
        });
  }

  private void checkPermissionsLocation() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED
        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATIONS);
    } else {
      detectLocation();
    }
  }

  @SuppressLint("MissingPermission")
  private void detectLocation() {
    mBinding.contentMain.layoutBottom.setVisibility(View.VISIBLE);
    mGoogleMap.setMyLocationEnabled(true);
    startLocationUpdates();
  }

  public void displaySettingsDialog(String tile, String message) {
    new AlertDialog.Builder(this)
        .setTitle(tile)
        .setMessage(message)
        .setPositiveButton(R.string.settings, (dialog, whichButton) -> {
          Intent intent = new Intent(
              android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
          intent.setData(Uri.parse("package:" + getPackageName()));
          startActivity(intent);
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == REQUEST_LOCATIONS) {

      if (grantResults.length == 2
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        detectLocation();
      } else {
        if (!ActivityCompat
            .shouldShowRequestPermissionRationale(this, permission.ACCESS_FINE_LOCATION) &&
            !ActivityCompat
                .shouldShowRequestPermissionRationale(this, permission.ACCESS_COARSE_LOCATION)) {
          displaySettingsDialog(getString(R.string.enable_location),
              getString(R.string.request_setting_message));
        } else {
          Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
        }
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
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

  private void startTracking() {
    mBinding.contentMain.setStatus("pause");
    startService(new Intent(this, LocationService.class));
  }

  private void stopTracking() {
    mBinding.contentMain.setStatus("record");
    stopService(new Intent(this, LocationService.class));
  }
}