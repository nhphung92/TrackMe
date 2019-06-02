package com.utopia.trackme;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult.Callback;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.utopia.trackme.utils.Constants;
import com.utopia.trackme.utils.MyUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TrackMeService extends Service implements
    LocationListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

  private static final String TAG = TrackMeService.class.getSimpleName();
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

  private Timer mTimer = new Timer();
  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;

  IBinder mBinder = new TrackMeService.LocalBinder();
  private long mStartTime;
  private Location mLastLocation;
  private long totalDistance = 0;
  private GeoApiContext mGeoApiContext;

  private Location mCurrentLocation, lStart, lEnd;
  private static double distance = 0;
  private double speed;

  @Override
  public void onConnected(@Nullable Bundle bundle) {

  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

  }

  @Override
  public void onLocationChanged(Location location) {
    mCurrentLocation = location;
    if (lStart == null) {
      lStart = mCurrentLocation;
      lEnd = mCurrentLocation;
    } else {
      lEnd = mCurrentLocation;
    }

    //Calling the method below updates the  live values of distance and speed to the TextViews.
    updateUI();
    //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph
    speed = location.getSpeed() * 18 / 5;
  }

  //The live feed of Distance and Speed are being set in the method below .
  private void updateUI() {
    if (LocationActivity.p == 0) {
      distance = distance + (lStart.distanceTo(lEnd) / 1000.00);
      LocationActivity.endTime = System.currentTimeMillis();
      long diff = LocationActivity.endTime - LocationActivity.startTime;
      diff = TimeUnit.MILLISECONDS.toMinutes(diff);
      LocationActivity.time.setText("Total Time: " + diff + " minutes");
      if (speed > 0.0) {
        LocationActivity.speed.setText("Current speed: " + new DecimalFormat("#.##").format(speed) + " km/hr");
      } else {
        LocationActivity.speed.setText(".......");
      }
      LocationActivity.dist.setText(new DecimalFormat("#.###").format(distance) + " Km's.");
      lStart = lEnd;
    }
  }

  public class LocalBinder extends Binder {

    public TrackMeService getServerInstance() {
      return TrackMeService.this;
    }
  }

  public TrackMeService() {

  }

  @Override
  public void onCreate() {
    super.onCreate();

    mGeoApiContext = new GeoApiContext.Builder().apiKey(getString(R.string.google_maps_key))
        .build();
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        sendBroadcastLocation(locationResult.getLastLocation());
      }
    };

    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();

    startLocationUpdates();
    startTimer();
  }

  private void handleDistance(Location location1, Location location2) {
    String[] origins = {String.valueOf(location1.getLatitude()),
        String.valueOf(location1.getLongitude())};
    String[] destinations = {String.valueOf(location2.getLatitude()),
        String.valueOf(location2.getLongitude())};

    DistanceMatrixApi
        .getDistanceMatrix(mGeoApiContext, origins, destinations)
        .mode(TravelMode.WALKING)
        .setCallback(new Callback<DistanceMatrix>() {
          @Override
          public void onResult(DistanceMatrix result) {
            long temp = result.rows[0].elements[0].distance.inMeters;
            totalDistance += temp;
            Log.i(TAG, "totalDistance: " + totalDistance);
          }

          @Override
          public void onFailure(Throwable e) {
            Log.i(TAG, "onFailure: " + e.getMessage());
          }
        });
  }

  private void startTimer() {
    mStartTime = System.currentTimeMillis();
    mTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        long endTime = System.currentTimeMillis();
        long seconds = (endTime - mStartTime) / 1000;
        String duration = MyUtils.convertTime(seconds);
        Log.i(TAG, mStartTime + " - " + endTime + " - " + seconds + " - " + duration);

        Intent intent = new Intent(Constants.BROADCAST_DETECTED_ACTIVITY);
        intent.putExtra("duration", duration);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
      }
    }, 0, 1000);
  }

  private void sendBroadcastLocation(Location location) {
    Intent intent = new Intent(Constants.BROADCAST_DETECTED_ACTIVITY);
    intent.putExtra("latitude", location.getLatitude());
    intent.putExtra("longitude", location.getLongitude());

    Geocoder geocoder = new Geocoder(this, Locale.getDefault());
    List<Address> addresses;
    try {
      addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
      intent.putExtra("address", addresses.get(0).getAddressLine(0));
    } catch (IOException e) {
      e.printStackTrace();
    }

    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  private void startLocationUpdates() {
    mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
        .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
          @SuppressLint("MissingPermission")
          @Override
          public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            mFusedLocationClient
                .requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
          }
        });
  }

  public void stopLocationUpdates() {
    mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        .addOnCompleteListener(new OnCompleteListener<Void>() {
          @Override
          public void onComplete(@NonNull Task<Void> task) {
            Toast.makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT)
                .show();
          }
        });
  }

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    super.onStartCommand(intent, flags, startId);
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    mTimer.cancel();
    stopLocationUpdates();
  }
}
