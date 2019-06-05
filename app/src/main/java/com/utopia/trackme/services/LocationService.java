package com.utopia.trackme.services;

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
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.utopia.trackme.data.AppRepository;
import com.utopia.trackme.data.remote.pojo.MyLatLng;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.utils.SystemUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationService extends Service {

  private static final String TAG = LocationService.class.getSimpleName();

  private Timer mTimer;
  private SessionResponse mSession;
  private long mStartTime, mEndTime, mDurations;
  private List<Double> mSpeedList = new ArrayList<>();
  private double mCurrentSpeed = 0, mDistance = 0;

  private Location mFirstLocation, mLastLocation;
  private FusedLocationProviderClient mFusedLocationProviderClient;

  private LocationCallback mLocationCallback = new LocationCallback() {

    @Override
    public void onLocationResult(LocationResult locationResult) {
      Location location = locationResult.getLastLocation();
      if (location != null) {
        Log.d(TAG, "latitude " + location.getLatitude());
        Log.d(TAG, "longitude " + location.getLongitude());

        if (mFirstLocation == null) {
          mFirstLocation = location;
        }
        mLastLocation = location;
      }
    }
  };

  private LocationRequest request;

  public LocationService() {
  }

  private void requestLocationUpdates() {
    int permission = ContextCompat
        .checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

    if (permission == PackageManager.PERMISSION_GRANTED) {
      // Request location updates and when an update is received
      mFusedLocationProviderClient.requestLocationUpdates(request, mLocationCallback, null);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();

    mStartTime = System.currentTimeMillis();

    mSession = initSession();
    AppRepository.getInstance().startSession(mSession);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        Location location = locationResult.getLastLocation();
        if (location != null) {
          Log.d(TAG, "latitude " + location.getLatitude());
          Log.d(TAG, "longitude " + location.getLongitude());

          if (mFirstLocation == null) {
            mFirstLocation = location;
          }
          mLastLocation = location;
        }
      }
    };

    request = new LocationRequest();
    request.setInterval(10000);
    request.setFastestInterval(5000);
    request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    requestLocationUpdates();
  }

  private void startTimer() {
    mTimer = new Timer();

    mTimer.scheduleAtFixedRate(new TimerTask() {

      @Override
      public void run() {

        // set end time
        mEndTime = System.currentTimeMillis();

        // durations (second), milliseconds -> seconds
        mDurations = (mEndTime - mStartTime) / 1000; // seconds
        sendBroadcastDuration();

        if (mFirstLocation != null && mLastLocation != null) {

          if (!mSession.getLocations().isEmpty()) {
            MyLatLng lastLatLng = mSession.getLocations().get(mSession.getLocations().size() - 1);
            Location lastLocationOld = new Location("");
            lastLocationOld.setLatitude(lastLatLng.lat);
            lastLocationOld.setLongitude(lastLatLng.lng);
            mFirstLocation = lastLocationOld;
          }

          // Returns the distance, in meters, between two latitude/longitude coordinates.
          double computeDistanceBetween = SphericalUtil.computeDistanceBetween(
              new LatLng(mFirstLocation.getLatitude(), mFirstLocation.getLongitude()),
              new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));

          Log.i(TAG, "computeDistanceBetween: " + computeDistanceBetween);

          if (computeDistanceBetween > 0) {
            mDistance = mDistance + computeDistanceBetween;
          }

          // speed (m/s)
          mCurrentSpeed = mDurations > 0 && mDistance > 0 ? mDistance / mDurations : 0.0;
          mSpeedList.add(mCurrentSpeed);

          double totalSpeed = 0;
          for (double d : mSpeedList) {
            totalSpeed += d;
          }
          double averageSpeed = totalSpeed / mSpeedList.size();

          mSession.setEndTime(mEndTime);
          mSession.setAverageSpeed(String.valueOf(averageSpeed));
          mSession.setDuration(String.valueOf(mDurations));
          mSession.setDistance(String.valueOf(mDistance));
          mSession.getLocations()
              .add(new MyLatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude()));
          AppRepository.getInstance().updateSession(mSession);

          Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
          intent.putExtra(EXTRA_CODE, SEND_SESSION);
          intent.putExtra(EXTRA_SESSION, mSession);
          intent.putExtra(EXTRA_DURATION, SystemUtils.convertTime(mDurations));
          intent
              .putExtra(EXTRA_DISTANCE,
                  mDistance > 0.0 ? SystemUtils.formatNumber(mDistance) : "0,00");
          intent.putExtra(EXTRA_SPEED,
              mCurrentSpeed > 0.0 ? SystemUtils.formatNumber(mCurrentSpeed) : "0,00");
          LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);

//          Log.i(TAG, "distance (meters): " + distance);
//          Log.i(TAG, "durations (second): " + durations);
//          Log.i(TAG, "speed (m/s): " + speed);
//          Log.i(TAG, "averageSpeed (m/s): " + averageSpeed);
        }
      }
    }, 0, 1000);
  }

  private void sendBroadcastDuration() {
    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_DURATION, SystemUtils.convertTime(mDurations));
    LocalBroadcastManager.getInstance(LocationService.this).sendBroadcast(intent);
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

    switch (status) {
      case "start":
        startTimer();
        break;
      case "pause":
        mTimer.cancel();
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        break;
      case "resume":
        mStartTime = mStartTime + (System.currentTimeMillis() - mEndTime);
        requestLocationUpdates();
        startTimer();
        break;
    }
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");

    // stop timer
    if (mTimer != null) {
      mTimer.cancel();
    }

    // stop update location
    if (mFusedLocationProviderClient != null && mLocationCallback != null) {
      mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
    }

    // send reset event to UI
    Intent intent = new Intent(BROADCAST_DETECTED_LOCATION);
    intent.putExtra(EXTRA_CODE, SEND_RESET);
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    // stop service
    stopSelf();
  }
}
