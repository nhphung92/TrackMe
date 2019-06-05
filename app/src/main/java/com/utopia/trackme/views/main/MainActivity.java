package com.utopia.trackme.views.main;

import static com.utopia.trackme.utils.MyConstants.BROADCAST_DETECTED_LOCATION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_CODE;
import static com.utopia.trackme.utils.MyConstants.EXTRA_DISTANCE;
import static com.utopia.trackme.utils.MyConstants.EXTRA_DURATION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_SPEED;
import static com.utopia.trackme.utils.MyConstants.EXTRA_STATUS;
import static com.utopia.trackme.utils.MyConstants.SEND_RESET;
import static com.utopia.trackme.utils.MyConstants.SEND_SESSION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
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
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.utopia.trackme.R;
import com.utopia.trackme.data.remote.pojo.MyLatLng;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.databinding.ActivityLocationBinding;
import com.utopia.trackme.services.LocationService;
import com.utopia.trackme.views.sessions.SessionsActivity;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

  private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
  private static final int REQUEST_LOCATIONS = 100;
  private static final int RQ_NEW_SESSION = 1;
  private static final int PERMISSIONS_REQUEST = 2;

  ActivityLocationBinding mBinding;

  private GoogleMap mGoogleMap;

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;
  private MainViewModel mViewModel;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mViewModel = ViewModelProviders.of(this).get(MainViewModel.class);

    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_location);
    setSupportActionBar(mBinding.toolbar);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        moveCamera(locationResult.getLastLocation());
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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
    mBinding.contentMain.record.setOnClickListener(v -> checkGPSEnabled());
    mBinding.contentMain.pause.setOnClickListener(v -> pauseTracking());
    mBinding.contentMain.refresh.setOnClickListener(v -> resumeTracking());
    mBinding.contentMain.stop.setOnClickListener(v -> stopTracking());
  }

  private void checkGPSEnabled() {
    // Check GPS is enabled
    LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
    if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      Toast.makeText(this, "Please enable location services", Toast.LENGTH_SHORT).show();
      finish();
    }

    // Check location permission is granted - if it is, start
    // the service, otherwise request the permission
    int permission = ContextCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION);
    if (permission == PackageManager.PERMISSION_GRANTED) {
      startTracking();
    } else {
      ActivityCompat.requestPermissions(this,
          new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
          PERMISSIONS_REQUEST);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST && grantResults.length == 1
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      // Start the service when the permission is granted
      startTracking();
    } else {
      Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (!checkPlayServices()) {
      mBinding.contentMain.location.setText(R.string.check_play_services_msg);
    }
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_DETECTED_LOCATION));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
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
      startActivityForResult(new Intent(this, SessionsActivity.class), RQ_NEW_SESSION);
      overridePendingTransition(0, 0);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

//  @Override
//  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//      @NonNull int[] grantResults) {
//    if (requestCode == REQUEST_LOCATIONS) {
//
//      if (grantResults.length == 2
//          && grantResults[0] == PackageManager.PERMISSION_GRANTED
//          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
//        detectLocation();
//      } else {
//        if (!ActivityCompat
//            .shouldShowRequestPermissionRationale(this, permission.ACCESS_FINE_LOCATION) &&
//            !ActivityCompat
//                .shouldShowRequestPermissionRationale(this, permission.ACCESS_COARSE_LOCATION)) {
//          displaySettingsDialog(getString(R.string.enable_location),
//              getString(R.string.request_setting_message));
//        } else {
//          Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
//        }
//      }
//    } else {
//      super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//    }
//  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == RQ_NEW_SESSION && resultCode == RESULT_OK) {
      startTracking();
    }
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

  private void moveCamera(Location location) {
    if (mGoogleMap != null) {
      LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
      mGoogleMap.clear();
      mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
      mGoogleMap.animateCamera(CameraUpdateFactory
          .newCameraPosition(new CameraPosition.Builder().target(latLng).zoom(15).build()));
    }
  }

  private void startTracking() {
    if (!mViewModel.isRecordEnabled()) {
      mViewModel.setRecordEnabled(true);
      mBinding.contentMain.setStatus("pause");
      startTrackerService("start");
    } else {
      Snackbar.make(mBinding.contentMain.layoutBottom, R.string.session_is_running,
          Snackbar.LENGTH_LONG).show();
    }
  }

  private void resumeTracking() {
    mBinding.contentMain.setStatus("pause");
    startTrackerService("resume");
  }

  private void pauseTracking() {
    mBinding.contentMain.setStatus("stop");
    startTrackerService("pause");
  }

  private void startTrackerService(String status) {
    Intent intent = new Intent(this, LocationService.class);
    intent.putExtra(EXTRA_STATUS, status);
    startService(intent);
  }

  private void stopTracking() {
    mViewModel.setRecordEnabled(false);
    mBinding.contentMain.setStatus("record");
    stopService(new Intent(this, LocationService.class));
  }

  BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

    @SuppressLint("SetTextI18n")
    @Override
    public void onReceive(Context context, Intent intent) {
      if (BROADCAST_DETECTED_LOCATION.equals(intent.getAction())) {

        int code = intent.getIntExtra(EXTRA_CODE, 0);

        switch (code) {
          case SEND_RESET:
            mGoogleMap.clear();
            mBinding.contentMain.duration.setText(R.string.time);
            mBinding.contentMain.distance.setText(R.string.distance_default);
            mBinding.contentMain.speed.setText(R.string.distance_default);
            break;
          case SEND_SESSION:

            if (intent.hasExtra(EXTRA_DURATION)) {
              mBinding.contentMain.duration.setText(intent.getStringExtra(EXTRA_DURATION));
            }
            if (intent.hasExtra(EXTRA_DISTANCE)) {
              mBinding.contentMain.distance.setText(intent.getStringExtra(EXTRA_DISTANCE));
            }

            if (intent.hasExtra(EXTRA_SPEED)) {
              mBinding.contentMain.speed.setText(intent.getStringExtra(EXTRA_SPEED));
            }

            mGoogleMap.clear();

            SessionResponse session = intent.getParcelableExtra(EXTRA_SESSION);
            for (int i = 0; i < session.getLocations().size() - 1; i++) {
              LatLng latLng1 = new LatLng(session.getLocations().get(i).lat,
                  session.getLocations().get(i).lng);
              LatLng latLng2 = new LatLng(session.getLocations().get(i + 1).lat,
                  session.getLocations().get(i + 1).lng);
              mGoogleMap.addPolyline(new PolylineOptions()
                  .add(latLng1, latLng2)
                  .width(20)
                  .color(Color.RED));
            }

            // add a marker for first location
            MyLatLng firstLat = session.getLocations().get(0);

            mGoogleMap
                .addMarker(new MarkerOptions().position(new LatLng(firstLat.lat, firstLat.lng)));
            break;
        }
      }
    }
  };
}