package com.utopia.trackme;

import static com.utopia.trackme.Constants.BROADCAST_DETECTED_ACTIVITY;
import static com.utopia.trackme.Constants.CONFIDENCE;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProviders;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.utopia.trackme.databinding.ActivityMainBinding;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

  private static final String TAG = MainActivity.class.getSimpleName();
  public static final int REQUEST_ID_ACCESS_COURSE_FINE_LOCATION = 100;

  private GoogleMap mGoogleMap;
  private MainViewModel viewModel;
  private ActivityMainBinding binding;

  // location last updated time
  private String mLastUpdateTime;

  // location updates interval - 10sec
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

  // fastest updates interval - 5 sec
  // location updates will be received if another app is requesting the locations
  // than your app can handle
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

  private static final int REQUEST_CHECK_SETTINGS = 100;


  // bunch of location related apis
  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;
  private Location mCurrentLocation;

  // boolean flag to toggle the ui
  private Boolean mRequestingLocationUpdates;
  private BroadcastReceiver mBroadcastReceiver;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.fragment);

    setSupportActionBar(binding.toolbar);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        // location is received
        mCurrentLocation = locationResult.getLastLocation();
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());

        updateLocationUI();

        moveCamera(mCurrentLocation);
      }
    };

    mRequestingLocationUpdates = false;

    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();

    mapFragment.getMapAsync(this::onMyMapReady);

//    restoreValuesFromBundle(savedInstanceState);

    mBroadcastReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        if (BROADCAST_DETECTED_ACTIVITY.equals(intent.getAction())) {
          int type = intent.getIntExtra("type", -1);
          int confidence = intent.getIntExtra("confidence", 0);
          handleUserActivity(type, confidence);
        }
      }
    };

    LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
        new IntentFilter(BROADCAST_DETECTED_ACTIVITY));

    binding.contentMain.setStatus("record");

    binding.contentMain.record.setOnClickListener(v -> {
      binding.contentMain.setStatus("pause");
      startTracking();
    });

    binding.contentMain.pause.setOnClickListener(v -> {
      binding.contentMain.setStatus("stop");
      stopTracking();
    });

    binding.contentMain.stop.setOnClickListener(v -> {
      binding.contentMain.setStatus("record");
    });

    binding.contentMain.refresh.setOnClickListener(v -> {
      binding.contentMain.setStatus("record");
    });
  }

  @Override
  public void onResume() {
    super.onResume();
    if (mRequestingLocationUpdates && checkPermissions()) {
      startLocationUpdates();
    }
    updateLocationUI();
  }

  @Override
  protected void onPause() {
    super.onPause();
    if (mRequestingLocationUpdates) {
      stopLocationUpdates();
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    stopTracking();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean("is_requesting_updates", mRequestingLocationUpdates);
    outState.putParcelable("last_known_location", mCurrentLocation);
    outState.putString("last_updated_on", mLastUpdateTime);
  }

  @RequiresApi(api = VERSION_CODES.M)
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == REQUEST_ID_ACCESS_COURSE_FINE_LOCATION) {
      if (grantResults.length >= 1
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this, "Permission granted!", Toast.LENGTH_LONG).show();
        if (checkSelfPermission(permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && checkSelfPermission(permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
          // TODO: Consider calling
          //    Activity#requestPermissions
          // here to request the missing permissions, and then overriding
          //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
          //                                          int[] grantResults)
          // to handle the case where the user grants the permission. See the documentation
          // for Activity#requestPermissions for more details.
          return;
        }
        this.mGoogleMap.setMyLocationEnabled(true);
      } else {
        Toast.makeText(this, "Permission denied!", Toast.LENGTH_LONG).show();
      }
    }
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.i(TAG, "onLocationChanged: " + location.getLatitude() + "," + location.getLongitude());
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {

  }

  @Override
  public void onProviderEnabled(String provider) {

  }

  @Override
  public void onProviderDisabled(String provider) {

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void restoreValuesFromBundle(Bundle savedInstanceState) {
    if (savedInstanceState != null) {
      if (savedInstanceState.containsKey("is_requesting_updates")) {
        mRequestingLocationUpdates = savedInstanceState.getBoolean("is_requesting_updates");
      }

      if (savedInstanceState.containsKey("last_known_location")) {
        mCurrentLocation = savedInstanceState.getParcelable("last_known_location");
      }

      if (savedInstanceState.containsKey("last_updated_on")) {
        mLastUpdateTime = savedInstanceState.getString("last_updated_on");
      }
    }

    updateLocationUI();
  }

  private void updateLocationUI() {
    if (mCurrentLocation != null) {
      binding.contentMain.locationResult
          .setText(mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude());

      // giving a blink animation on TextView
      binding.contentMain.locationResult.setAlpha(0);
      binding.contentMain.locationResult.animate().alpha(1).setDuration(300);

      // location last updated time
      binding.contentMain.updatedTime.setText(mLastUpdateTime);

      Geocoder geocoder = new Geocoder(this, Locale.getDefault());
      List<Address> addresses;
      try {
        addresses = geocoder
            .getFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), 1);
        binding.contentMain.address.setText(addresses.get(0).getAddressLine(0));
        binding.contentMain.address.setAlpha(0);
        binding.contentMain.address.animate().alpha(1).setDuration(300);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void onMyMapReady(GoogleMap googleMap) {
    this.mGoogleMap = googleMap;
    this.mGoogleMap.setOnMapLoadedCallback(this::startDetectLocation);
    this.mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    this.mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
  }

  private boolean checkPermissions() {
    int permissionState = ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION);
    return permissionState == PackageManager.PERMISSION_GRANTED;
  }

  public void stopLocationUpdates() {
    mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        .addOnCompleteListener(this, new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  private void handleUserActivity(int type, int confidence) {
    String label = getString(R.string.activity_unknown);
    int icon = R.drawable.ic_still;

    switch (type) {
      case DetectedActivity.IN_VEHICLE: {
        label = getString(R.string.activity_in_vehicle);
        icon = R.drawable.ic_driving;
        break;
      }
      case DetectedActivity.ON_BICYCLE: {
        label = getString(R.string.activity_on_bicycle);
        icon = R.drawable.ic_on_bicycle;
        break;
      }
      case DetectedActivity.ON_FOOT: {
        label = getString(R.string.activity_on_foot);
        icon = R.drawable.ic_walking;
        break;
      }
      case DetectedActivity.RUNNING: {
        label = getString(R.string.activity_running);
        icon = R.drawable.ic_running;
        break;
      }
      case DetectedActivity.STILL: {
        label = getString(R.string.activity_still);
        break;
      }
      case DetectedActivity.TILTING: {
        label = getString(R.string.activity_tilting);
        icon = R.drawable.ic_tilting;
        break;
      }
      case DetectedActivity.WALKING: {
        label = getString(R.string.activity_walking);
        icon = R.drawable.ic_walking;
        break;
      }
      case DetectedActivity.UNKNOWN: {
        label = getString(R.string.activity_unknown);
        break;
      }
    }

    Log.e(TAG, "User activity: " + label + ", Confidence: " + confidence);

    if (confidence > CONFIDENCE) {
      binding.contentMain.txtActivity.setText(label);
      binding.contentMain.txtConfidence.setText("Confidence: " + confidence);
      binding.contentMain.imgActivity.setImageResource(icon);
    }
  }

  private void startLocationUpdates() {
    mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
        .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
          @SuppressLint("MissingPermission")
          @Override
          public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            Log.i(TAG, "All location settings are satisfied.");

            Toast.makeText(getApplicationContext(), "Started location updates!", Toast.LENGTH_SHORT)
                .show();

            //noinspection MissingPermission
            mFusedLocationClient
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

            updateLocationUI();
          }
        })
        .addOnFailureListener(this, e -> {
          int statusCode = ((ApiException) e).getStatusCode();
          switch (statusCode) {
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
              Log.i(TAG,
                  "Location settings are not satisfied. Attempting to upgrade location settings");
              try {
                ResolvableApiException rae = (ResolvableApiException) e;
                rae.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
              } catch (IntentSender.SendIntentException sie) {
                Log.i(TAG, "PendingIntent unable to execute request.");
              }
              break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
              String errorMessage = "Location settings are inadequate, and cannot be fixed here. Fix in Settings.";
              Log.e(TAG, errorMessage);
              Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_LONG).show();
          }
          updateLocationUI();
        });
  }

  public void startDetectLocation() {
    Dexter.withActivity(this)
        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        .withListener(new PermissionListener() {
          @SuppressLint("MissingPermission")
          @Override
          public void onPermissionGranted(PermissionGrantedResponse response) {
            mRequestingLocationUpdates = true;
            mGoogleMap.setMyLocationEnabled(true);
            startLocationUpdates();
          }

          @Override
          public void onPermissionDenied(PermissionDeniedResponse response) {
            if (response.isPermanentlyDenied()) {
              openSettings();
            }
          }

          @Override
          public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
              PermissionToken token) {
            token.continuePermissionRequest();
          }
        }).check();
  }

  private void openSettings() {
    Intent intent = new Intent();
    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
    intent.setData(uri);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  private void moveCamera(Location location) {
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
    if (mGoogleMap != null) {
      mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
      CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15).build();
      mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
  }

  private void startTracking() {
    startService(new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class));
  }

  private void stopTracking() {
    stopService(new Intent(MainActivity.this, BackgroundDetectedActivitiesService.class));
  }
}