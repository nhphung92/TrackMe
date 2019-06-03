package com.utopia.trackme.services;

import static com.utopia.trackme.utils.MyConstants.BROADCAST_DETECTED_LOCATION;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnSuccessListener;
import com.utopia.trackme.utils.SystemUtils;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TrackMeService extends Service {

  private static final String TAG = TrackMeService.class.getSimpleName();
  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

  private FusedLocationProviderClient mFusedLocationClient;

  private LocationCallback mLocationCallback;

  private Timer mTimer = new Timer();
  private long mStartTime;
  private Location mLocationStart;
  private Location mLocationEnd;
  private double mDistance = 0, mSpeed;

  @Override
  public void onCreate() {
    super.onCreate();
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        Log.d(TAG, "-------------------------------------");
        Log.d(TAG, "onLocationResult");
        super.onLocationResult(locationResult);
        sendBroadcastLocation(locationResult.getLastLocation());

        if (mLocationStart == null) {
          mLocationStart = locationResult.getLastLocation();
          mLocationEnd = locationResult.getLastLocation();
        } else {
          mLocationEnd = locationResult.getLastLocation();
        }

        // The live feed of Distance and Speed are being set in the method below .
        String speedText;
        String timeText;
        String distText;

        mDistance = mDistance + (mLocationStart.distanceTo(mLocationEnd) / 1000.00);
        long diff = System.currentTimeMillis() - mStartTime;
        diff = TimeUnit.MILLISECONDS.toMinutes(diff);
        timeText = "Total Time: " + diff + " minutes";
        if (mSpeed > 0.0) {
          speedText = "Current mSpeed: " + new DecimalFormat("#.##").format(mSpeed) + " km/hr";
        } else {
          speedText = ".......";
        }
        distText = new DecimalFormat("#.###").format(mDistance) + " Km's.";
        mLocationStart = mLocationEnd;

        Log.d(TAG, "speedText: " + speedText);
        Log.d(TAG, "timeText: " + timeText);
        Log.d(TAG, "distText: " + distText);

        // calculating the mSpeed with getSpeed method it returns mSpeed in m/s so we are converting it into kmph
        mSpeed = locationResult.getLastLocation().getSpeed() * 18 / 5;
        Log.d(TAG, "speed: " + mSpeed);
      }
    };

    LocationRequest locationRequest = new LocationRequest();
    locationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(locationRequest);
    LocationSettingsRequest locationSettingsRequest = builder.build();

    SettingsClient settingsClient = LocationServices.getSettingsClient(this);
    settingsClient.checkLocationSettings(locationSettingsRequest)
        .addOnSuccessListener(new OnSuccessListener<LocationSettingsResponse>() {
          @SuppressLint("MissingPermission")
          @Override
          public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
            mFusedLocationClient
                .requestLocationUpdates(locationRequest, mLocationCallback, Looper.myLooper());
          }
        });

    mStartTime = System.currentTimeMillis();
    mTimer.scheduleAtFixedRate(new TimerTask() {
      @Override
      public void run() {
        long endTime = System.currentTimeMillis();
        long seconds = (endTime - mStartTime) / 1000;
        String duration = SystemUtils.convertTime(seconds);
        Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
        intent.putExtra("duration", duration);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
      }
    }, 0, 1000);
  }

  private void sendBroadcastLocation(Location location) {
    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
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
    Log.i(TAG, "Location updates stopped!");
    // stop timer
    mTimer.cancel();

    // stop location updates
    mFusedLocationClient.removeLocationUpdates(mLocationCallback).addOnCompleteListener(
        task -> Toast
            .makeText(getApplicationContext(), "Location updates stopped!", Toast.LENGTH_SHORT)
            .show());
  }
}
