package com.utopia.trackme;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import com.utopia.trackme.databinding.ActivityLocationBinding;

public class LocationActivity extends AppCompatActivity {

  LocationService myService;
  static boolean status;
  LocationManager locationManager;
  static TextView dist, time, speed;
  static long startTime, endTime;
  static int p = 0;

  private ServiceConnection sc = new ServiceConnection() {

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      LocationService.LocalBinder binder = (LocationService.LocalBinder) service;
      myService = binder.getService();
      status = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      status = false;
    }
  };

  void bindService() {
    if (status) {
      return;
    }
    bindService(new Intent(getApplicationContext(), LocationService.class), sc, BIND_AUTO_CREATE);
    status = true;
    startTime = System.currentTimeMillis();
  }

  void unbindService() {
    if (!status) {
      return;
    }
    unbindService(sc);
    status = false;
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (status) {
      unbindService();
    }
  }

  @Override
  public void onBackPressed() {
    if (!status) {
      super.onBackPressed();
    } else {
      moveTaskToBack(true);
    }
  }

  ActivityLocationBinding binding;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = DataBindingUtil.setContentView(this, R.layout.activity_location);

    dist = binding.distancetext;
    time = binding.timetext;
    speed = binding.speedtext;

    binding.start.setOnClickListener(v -> {
      //The method below checks if Location is enabled on device or not. If not, then an alert dialog box appears with option
      //to enable gps.
      checkGps();
      locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
      if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        return;
      }

      // Here, the Location Service gets bound and the GPS Speedometer gets Active.
      if (!status) {
        bindService();
      }
      binding.start.setVisibility(View.GONE);
      binding.pause.setVisibility(View.VISIBLE);
      binding.pause.setText(R.string.pause);
      binding.stop.setVisibility(View.VISIBLE);
    });

    binding.pause.setOnClickListener(v -> {
      if (binding.pause.getText().toString().equalsIgnoreCase("pause")) {
        binding.pause.setText(R.string.resume);
        p = 1;

      } else if (binding.pause.getText().toString().equalsIgnoreCase("Resume")) {
        checkGps();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          return;
        }
        binding.pause.setText(R.string.pause);
        p = 0;
      }
    });

    binding.stop.setOnClickListener(v -> {
      if (status) {
        unbindService();
      }
      binding.start.setVisibility(View.VISIBLE);
      binding.pause.setText(R.string.pause);
      binding.pause.setVisibility(View.GONE);
      binding.stop.setVisibility(View.GONE);
      p = 0;
    });
  }

  //This method leads you to the alert dialog box.
  void checkGps() {
    locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
    if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
      showGPSDisabledAlertToUser();
    }
  }

  private void showGPSDisabledAlertToUser() {
    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
    alertDialogBuilder.setMessage("Enable GPS to use application").setCancelable(false)
        .setPositiveButton("Enable GPS", (dialog, id) -> {
          Intent callGPSSettingIntent = new Intent(
              android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
          startActivity(callGPSSettingIntent);
        });
    alertDialogBuilder.setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());
    AlertDialog alert = alertDialogBuilder.create();
    alert.show();
  }

}