package com.utopia.trackme.services;

import static com.utopia.trackme.utils.MyConstants.EXTRA_CODE;
import static com.utopia.trackme.utils.MyConstants.SEND_DURATION;
import static com.utopia.trackme.utils.MyConstants.SEND_LOCATION;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.utopia.trackme.data.AppRepository;
import com.utopia.trackme.data.remote.pojo.MyLatLng;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.utils.Constants;
import com.utopia.trackme.utils.MyUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service
    implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, LocationListener {

  private static final String TAG = LocationService.class.getSimpleName();
  private GoogleApiClient mGoogleApiClient;
  private static final long UPDATE_INTERVAL = 5000, FASTEST_INTERVAL = 5000; // = 5 seconds
  private long mStartTime;
  private Timer mTimer = new Timer();
  private SessionResponse mSession;
  private Location mLocationStart;
  private Location mLocationEnd;
  private double mDistance = 0, mSpeed;
  private long mEndTime;

  // flag for GPS status
  boolean isGPSEnabled = false;

  // flag for network status
  boolean isNetworkEnabled = false;

  boolean canGetLocation = false;

  Location location; // location
  double latitude; // latitude
  double longitude; // longitude

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

  // Declaring a Location Manager
  protected LocationManager locationManager;

  public interface SessionCallback {

    void onNewSession(SessionResponse session);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    // we build google api client
    mGoogleApiClient = new GoogleApiClient.Builder(this).
        addApi(LocationServices.API).
        addConnectionCallbacks(this).
        addOnConnectionFailedListener(this).build();
    mGoogleApiClient.connect();
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");

    Log.d(TAG, "cancel timer");
    mTimer.cancel();

    // stop location updates
    Log.d(TAG, "stop location updates");
    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
      LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
      mGoogleApiClient.disconnect();
    }
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onConnected(@Nullable Bundle bundle) {
    Log.d(TAG, "onConnected");
    Log.d(TAG, "startLocationUpdates");

//    LocationRequest locationRequest = new LocationRequest();
//    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//    locationRequest.setInterval(UPDATE_INTERVAL);
//    locationRequest.setFastestInterval(FASTEST_INTERVAL);

    try {
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      // getting GPS status
      isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
      // getting network status
      isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
      if (!isGPSEnabled && !isNetworkEnabled) {
        onDestroy();
      } else {
        this.canGetLocation = true;
        // First get location from Network Provider
        if (isNetworkEnabled) {
          locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          Log.d("Network", "Network");
          if (locationManager != null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
              latitude = location.getLatitude();
              longitude = location.getLongitude();
            }
          }
        }
        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled) {
          if (location == null) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
            Log.d("GPS Enabled", "GPS Enabled");
            if (locationManager != null) {
              location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
              if (location != null) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
              }
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

//    LocationServices.FusedLocationApi
//        .requestLocationUpdates(mGoogleApiClient, locationRequest, this);

    mStartTime = System.currentTimeMillis();
    Log.d(TAG, "start new session " + mStartTime);

    AppRepository.getInstance().startSession(session -> mTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        mSession = session;
        mEndTime = System.currentTimeMillis();
        sendBroadcastDuration();
      }
    }, 0, 1000), mStartTime, location);
  }

  @Override
  public void onConnectionSuspended(int i) {
    Log.d(TAG, "onConnectionSuspended");
  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    Log.d(TAG, "onConnectionFailed");
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());

    if (mLocationEnd != null && location.getLatitude() == mLocationEnd.getLatitude()
        && location.getLongitude() == mLocationEnd.getLongitude()) {
      return;
    }

    if (mLocationStart == null) {
      mLocationStart = location;
      mLocationEnd = location;
    } else {
      mLocationEnd = location;
    }

    mEndTime = System.currentTimeMillis();

    // The live feed of Distance and Speed are being set in the method below .

//    mDistance = mDistance + (mLocationStart.distanceTo(mLocationEnd) / 1000.00); // kilometers
    mDistance = mLocationStart.distanceTo(mLocationEnd); // meters

//    long diff = mEndTime - mStartTime;
//    diff = TimeUnit.MILLISECONDS.toMinutes(diff);
//    String timeText = "Total Time: " + diff + " minutes";

    mLocationStart = mLocationEnd;
    long durations = (mEndTime - mStartTime) / 1000;
    if (mSession != null) {
      mSession.setEndTime(mEndTime);
      mSession.setAverageSpeed(mSpeed);
      mSession.setDuration(durations);
      mSession.setDistance(mDistance);
      mSession.getLocations().add(new MyLatLng(location.getLatitude(), location.getLongitude()));
      AppRepository.getInstance().updateSession(mSession);
    }

    Intent intent = new Intent(Constants.BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_LOCATION);
    intent.putExtra("latitude", location.getLatitude());
    intent.putExtra("longitude", location.getLongitude());
    intent.putExtra("address", getAddress(location));
    intent.putExtra("duration", MyUtils.convertTime(durations));
    intent.putExtra("distance", new DecimalFormat("#.###").format(mDistance));
    intent.putExtra("speed", mSpeed > 0.0 ? new DecimalFormat("#.##").format(mSpeed) : "0,00");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    // calculating the mSpeed with getSpeed method it returns mSpeed in m/s so we are converting it into kmph
    mSpeed = location.getSpeed() * 18 / 5;
  }

  private String getAddress(Location location) {
    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    List<Address> addresses;
    try {
      addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
      return addresses.get(0).getAddressLine(0);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "";
  }

  private void sendBroadcastDuration() {
    Intent intent = new Intent(Constants.BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_DURATION);
    intent.putExtra("duration", MyUtils.convertTime((mEndTime - mStartTime) / 1000));
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }
}
