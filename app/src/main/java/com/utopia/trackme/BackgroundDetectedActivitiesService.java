package com.utopia.trackme;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class BackgroundDetectedActivitiesService extends Service {

  private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
  private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

  private FusedLocationProviderClient mFusedLocationClient;
  private SettingsClient mSettingsClient;
  private LocationRequest mLocationRequest;
  private LocationSettingsRequest mLocationSettingsRequest;
  private LocationCallback mLocationCallback;
  private Location mCurrentLocation;

  IBinder mBinder = new BackgroundDetectedActivitiesService.LocalBinder();

  public class LocalBinder extends Binder {

    public BackgroundDetectedActivitiesService getServerInstance() {
      return BackgroundDetectedActivitiesService.this;
    }
  }

  public BackgroundDetectedActivitiesService() {

  }

  private void sendBroadcastLocation(Location location) {
    Intent intent = new Intent(Constants.BROADCAST_DETECTED_ACTIVITY);
    intent.putExtra("latitude", location.getLatitude());
    intent.putExtra("longitude", location.getLongitude());
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
  }

  @Override
  public void onCreate() {
    super.onCreate();
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    mSettingsClient = LocationServices.getSettingsClient(this);

    mLocationCallback = new LocationCallback() {
      @Override
      public void onLocationResult(LocationResult locationResult) {
        super.onLocationResult(locationResult);
        mCurrentLocation = locationResult.getLastLocation();
        sendBroadcastLocation(mCurrentLocation);
      }
    };

    mLocationRequest = new LocationRequest();
    mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
    builder.addLocationRequest(mLocationRequest);
    mLocationSettingsRequest = builder.build();
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
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    stopLocationUpdates();
  }
}
