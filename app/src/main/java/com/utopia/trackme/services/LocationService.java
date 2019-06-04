package com.utopia.trackme.services;

import static com.utopia.trackme.utils.MyConstants.BROADCAST_DETECTED_LOCATION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_CODE;
import static com.utopia.trackme.utils.MyConstants.EXTRA_DISTANCE;
import static com.utopia.trackme.utils.MyConstants.EXTRA_DURATION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_MESSAGE;
import static com.utopia.trackme.utils.MyConstants.EXTRA_SESSION;
import static com.utopia.trackme.utils.MyConstants.EXTRA_SPEED;
import static com.utopia.trackme.utils.MyConstants.EXTRA_STATUS;
import static com.utopia.trackme.utils.MyConstants.SEND_RESET;
import static com.utopia.trackme.utils.MyConstants.SEND_SESSION;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
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
import com.utopia.trackme.utils.SystemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service implements LocationListener {

  private static final String TAG = LocationService.class.getSimpleName();
  private Timer mTimer;
  private SessionResponse mSession;
  private Location mFirstLocation, mLastLocation;
  private long mStartTime;
  private long mEndTime;
  private List<Double> mSpeeds = new ArrayList<>();

  // flag for GPS status
  boolean isGPSEnabled = false;

  // flag for network status
  boolean isNetworkEnabled = false;

  // The minimum distance to change Updates in meters
  private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 5 meters

  // The minimum time between updates in milliseconds
  private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

  // Declaring a Location Manager
  protected LocationManager mLocationManager;
  private double speed = 0;
  private double distance = 0;
  private long durations;

  public LocationService() {
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
        sendBroadcastError("GPS and Network not enabled. Please turn on.");
      } else {

        // First get location from Network Provider
        if (isNetworkEnabled) {
          mLocationManager
              .requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES,
                  MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          Log.d("Network", "Network");
          mFirstLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
          mLastLocation = mFirstLocation;
        }

        // if GPS Enabled get lat/long using GPS Services
        if (isGPSEnabled && !isNetworkEnabled) {
          mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES,
              MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
          Log.d("GPS Enabled", "GPS Enabled");
          mFirstLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
          mLastLocation = mFirstLocation;
        }

        mStartTime = System.currentTimeMillis();

        // start session
        mSession = initSession();

        if (mFirstLocation != null) {
          mSession.addLocation(
              new MyLatLng(mFirstLocation.getLatitude(), mFirstLocation.getLongitude()));
        }

        AppRepository.getInstance().startSession(mSession);

        // start timer
        startTimer();
      }
    } catch (Exception e) {
      e.printStackTrace();
      sendBroadcastError(e.getMessage());
    }
  }

  private void startTimer() {
    mTimer = new Timer();
    mTimer.scheduleAtFixedRate(new TimerTask() {

      @Override
      public void run() {

        // set end time
        mEndTime = System.currentTimeMillis();

        if (!mSession.getLocations().isEmpty()) {
          MyLatLng lastLatLng = mSession.getLocations().get(mSession.getLocations().size() - 1);
          Location lastLocationOld = new Location("");
          lastLocationOld.setLatitude(lastLatLng.lat);
          lastLocationOld.setLongitude(lastLatLng.lng);
          mFirstLocation = lastLocationOld;
        }

        // distance (meters)
        distance = distance + SystemUtils.calculationByDistance(mFirstLocation, mLastLocation);

        // durations (second), milliseconds -> seconds
        durations = getDuration(); // seconds

        // speed (m/s)
        speed = durations > 0 && distance > 0 ? distance / durations : 0.0;
        mSpeeds.add(speed);

        double totalSpeed = 0;
        for (double d : mSpeeds) {
          totalSpeed += d;
        }
        double averageSpeed = totalSpeed / mSpeeds.size();

        mSession.setEndTime(mEndTime);
        mSession.setAverageSpeed(String.valueOf(averageSpeed));
        mSession.setDuration(String.valueOf(durations));
        mSession.setDistance(String.valueOf(distance));
        mSession.getLocations()
            .add(new MyLatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
        AppRepository.getInstance().updateSession(mSession);

        sendBroadcastSession(durations, distance, speed);

        Log.i(TAG, "distance (meters): " + distance);
        Log.i(TAG, "durations (second): " + durations);
        Log.i(TAG, "speed (m/s): " + speed);
        Log.i(TAG, "averageSpeed (m/s): " + averageSpeed);
      }
    }, 0, 1000);
  }

  private SessionResponse initSession() {
    SessionResponse session = new SessionResponse();
    session.setSessionId(String.valueOf(mStartTime));
    session.setStartTime(mStartTime);
    session.setEndTime(mStartTime);
    session.setDistance("0.0");
    session.setDuration("0.0");
    session.setAverageSpeed("0.0");
    session.setLocations(new ArrayList<>());
    return session;
  }

  private void sendBroadcastError(String message) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_RESET);
    intent.putExtra(EXTRA_MESSAGE, message);
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
    String status = intent.getStringExtra(EXTRA_STATUS);
    Log.d(TAG, "onStartCommand: " + status);

    if ("pause".equals(status)) {
      mTimer.cancel();
    } else if ("resume".equals(status)) {
      mStartTime = mStartTime + (System.currentTimeMillis() - mEndTime);
      startTimer();
    }
    return START_NOT_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
    mTimer.cancel();
    mLocationManager.removeUpdates(this);

    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_RESET);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  private long getDuration() {
    return (mEndTime - mStartTime) / 1000;
  }

  @Override
  public void onLocationChanged(Location location) {
    Log.d(TAG, "--------------------------");
    Log.d(TAG, "onLocationChanged: " + location.getLatitude() + ", " + location.getLongitude());
    handleLocationChanged(location);
  }

  private void handleLocationChanged(Location location) {
    if (mFirstLocation == null) {
      mFirstLocation = location;
      mLastLocation = location;
    } else {
      mLastLocation = location;
    }
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

  private void sendBroadcastSession(long durations, double distance, double speed) {
    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_SESSION);
    intent.putExtra(EXTRA_SESSION, mSession);
    intent.putExtra(EXTRA_DURATION, SystemUtils.convertTime(durations));
    intent.putExtra(EXTRA_DISTANCE, distance > 0.0 ? SystemUtils.formatNumber(distance) : "0,00");
    intent.putExtra(EXTRA_SPEED, speed > 0.0 ? SystemUtils.formatNumber(speed) : "0,00");
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }
}
