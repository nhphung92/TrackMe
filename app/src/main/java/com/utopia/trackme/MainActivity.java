package com.utopia.trackme;

import static com.utopia.trackme.utils.Constants.BROADCAST_DETECTED_ACTIVITY;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.utopia.trackme.databinding.ActivityMainBinding;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

  private static final String TAG = MainActivity.class.getSimpleName();

  private GoogleMap mGoogleMap;
  private ActivityMainBinding mBinding;

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;
  private BroadcastReceiver mBroadcastReceiver;

  private double mLatitude;
  private double mLongitude;
  private String mAddress;
  static int p = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    setSupportActionBar(mBinding.toolbar);

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        mLatitude = locationResult.getLastLocation().getLatitude();
        mLongitude = locationResult.getLastLocation().getLongitude();
        moveCamera();
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

    mBroadcastReceiver = new BroadcastReceiver() {

      @Override
      public void onReceive(Context context, Intent intent) {
        if (BROADCAST_DETECTED_ACTIVITY.equals(intent.getAction())) {
          mLatitude = intent.getDoubleExtra("latitude", 0);
          mLongitude = intent.getDoubleExtra("longitude", 0);
          mAddress = intent.getStringExtra("address");

          if (intent.hasExtra("duration")) {
            mBinding.contentMain.duration.setText(intent.getStringExtra("duration"));
          }
        }
      }
    };

    mBinding.contentMain.setStatus("record");

    mBinding.contentMain.record.setOnClickListener(v -> startTracking());
    mBinding.contentMain.pause.setOnClickListener(v -> stopTracking());

    LocalBroadcastManager.getInstance(this)
        .registerReceiver(mBroadcastReceiver, new IntentFilter(BROADCAST_DETECTED_ACTIVITY));
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
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
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void onMyMapReady(GoogleMap googleMap) {
    this.mGoogleMap = googleMap;
    this.mGoogleMap.setOnMapLoadedCallback(this::startDetectLocation);
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

  private void startDetectLocation() {
    Dexter.withActivity(this)
        .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        .withListener(new PermissionListener() {
          @SuppressLint("MissingPermission")
          @Override
          public void onPermissionGranted(PermissionGrantedResponse response) {
            mGoogleMap.setMyLocationEnabled(true);
            startLocationUpdates();
          }

          @Override
          public void onPermissionDenied(PermissionDeniedResponse response) {
            if (response.isPermanentlyDenied()) {
              Intent intent = new Intent();
              intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
              Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
              intent.setData(uri);
              intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity(intent);
            }
          }

          @Override
          public void onPermissionRationaleShouldBeShown(PermissionRequest permission,
              PermissionToken token) {
            token.continuePermissionRequest();
          }
        }).check();
  }

  private void moveCamera() {
    if (mGoogleMap != null && mLatitude != 0 && mLongitude != 0) {
      LatLng latLng = new LatLng(mLatitude, mLongitude);
      mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 13));
      CameraPosition cameraPosition = new CameraPosition.Builder().target(latLng).zoom(15).build();
      mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }
  }

  private void startTracking() {
    mBinding.contentMain.setStatus("pause");
    startService(new Intent(this, TrackMeService.class));
  }

  private void stopTracking() {
    mBinding.contentMain.setStatus("record");
    stopService(new Intent(this, TrackMeService.class));
  }
}