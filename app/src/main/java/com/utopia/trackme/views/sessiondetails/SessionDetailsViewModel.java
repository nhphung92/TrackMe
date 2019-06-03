package com.utopia.trackme.views.sessiondetails;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.maps.model.PolylineOptions;
import com.utopia.trackme.data.AppRepository;
import retrofit2.http.Query;

public class SessionDetailsViewModel extends AndroidViewModel {

  public SessionDetailsViewModel(@NonNull Application application) {
    super(application);
  }

  private MutableLiveData<PolylineOptions> polylineOptions = new MutableLiveData<>();

  public MutableLiveData<PolylineOptions> getPolylineOptions() {
    return polylineOptions;
  }

  public void getDirections(String origin, String destination, String mode, String key) {
    AppRepository.getInstance().getDirections(polylineOptions, origin, destination, mode, key);
  }
}
