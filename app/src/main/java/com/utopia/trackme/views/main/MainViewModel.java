package com.utopia.trackme.views.main;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class MainViewModel extends AndroidViewModel {

  private boolean mRecordEnabled = false;

  public MainViewModel(@NonNull Application application) {
    super(application);
  }

  public boolean isRecordEnabled() {
    return mRecordEnabled;
  }

  public void setRecordEnabled(boolean b) {
    this.mRecordEnabled = b;
  }
}
