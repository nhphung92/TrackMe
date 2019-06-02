package com.utopia.trackme.data;

import android.app.Application;

public class MyApplication extends Application {

  private static MyApplication mInstance;

  @Override
  public void onCreate() {

//    StrictMode.setThreadPolicy(new ThreadPolicy.Builder()
//        .detectDiskReads()
//        .detectDiskWrites()
//        .detectNetwork()   // or .detectAll() for all detectable problems
//        .penaltyLog()
//        .build());
//    StrictMode.setVmPolicy(new VmPolicy.Builder()
//        .detectLeakedSqlLiteObjects()
//        .detectLeakedClosableObjects()
//        .penaltyLog()
//        .penaltyDeath()
//        .build());

    super.onCreate();
    mInstance = this;
  }

  public static synchronized MyApplication getInstance() {
    return mInstance;
  }
}
