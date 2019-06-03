package com.utopia.trackme.services;

import static com.utopia.trackme.utils.MyConstants.EXTRA_CODE;
import static com.utopia.trackme.utils.MyConstants.SEND_DURATION;
import static com.utopia.trackme.utils.MyConstants.SEND_LOCATION;
import static com.utopia.trackme.utils.MyConstants.SEND_RESET;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
    implements LocationListener {

  private static final String TAG = LocationService.class.getSimpleName();
  private Timer mTimer = new Timer();
  private SessionResponse mSession;
  private Location mLocationStart;
  private long mStartTime, mEndTime;

  // flag for GPS status
  boolean isGPSEnabled = false;

  // flag for network status
  boolean isNetworkEnabled = false;

  private Location location; // location

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000; // 1 minute

  // Declaring a Location Manager
  protected LocationManager mLocationManager;

  public interface SessionCallback {

    void onNewSession(SessionResponse session);
  }

  @SuppressLint("MissingPermission")
  @Override
  public void onCreate() {
    super.onCreate();

    try {
      mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

      // getting GPS status
      isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

      // getting network status
      isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

      if (!isGPSEnabled && !isNetworkEnabled) {
        onDestroy();
      } else {
        // First get location from Network Provider
        if (isNetworkEnabled) {
          mLocationManager
              .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
                  MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          Log.d("Network", "Network");
          location = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled) {
          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
              MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          Log.d("GPS Enabled", "GPS Enabled");
          location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
      }

      mStartTime = System.currentTimeMillis();

      AppRepository.getInstance()
          .startSession(session -> mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
              mSession = session;
              mEndTime = System.currentTimeMillis();
              sendBroadcastDuration();
            }
          }, 0, 1000), mStartTime, location);
    } catch (Exception e) {
      e.printStackTrace();
      Intent intent = new Intent(Constants.BROADCAST_DETECTED_LOCATION);
      intent.putExtra(EXTRA_CODE, SEND_RESET);
      LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
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
    mTimer.cancel();
    Intent intent = new Intent(Constants.BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_RESET);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());
    handleLocationChanged(location);
  }

  private void handleLocationChanged(Location location) {
    Location mLocationEnd;
    if (mLocationStart == null) {
      mLocationStart = location;
      mLocationEnd = location;
    } else {
      mLocationEnd = location;
    }
    // set start location
    mLocationStart = mLocationEnd;

    // set end time
    mEndTime = System.currentTimeMillis();

    // distance
    double distance = mLocationStart.distanceTo(mLocationEnd); // meters

    // durations
    long durations = (mEndTime - mStartTime) / 1000; // seconds

    // speed
    double speed = distance / durations;

    Toast.makeText(this, "speed: " + speed, Toast.LENGTH_LONG).show();

    if (mSession != null) {
      mSession.setEndTime(mEndTime);
      mSession.setAverageSpeed(speed);
      mSession.setDuration(durations);
      mSession.setDistance(distance);
      mSession.getLocations().add(new MyLatLng(location.getLatitude(), location.getLongitude()));
      AppRepository.getInstance().updateSession(mSession);
    }

    Intent intent = new Intent(Constants.BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_LOCATION);
    intent.putExtra("latitude", location.getLatitude());
    intent.putExtra("longitude", location.getLongitude());
    intent.putExtra("address", getAddress(location));
    intent.putExtra("duration", MyUtils.convertTime(durations));
    intent.putExtra("distance", new DecimalFormat("#.###").format(distance));
    intent.putExtra("speed", speed > 0.0 ? new DecimalFormat("#.##").format(speed) : "0,00");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
