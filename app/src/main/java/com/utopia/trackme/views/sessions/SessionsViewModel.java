package com.utopia.trackme.views.sessions;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.utopia.trackme.data.AppRepository;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import java.util.List;

public class SessionsViewModel extends AndroidViewModel {

  public SessionsViewModel(@NonNull Application application) {
    super(application);
  }

  private MutableLiveData<List<SessionResponse>> bookmarks = new MutableLiveData<>();

  MutableLiveData<List<SessionResponse>> getObservableSessions() {
    return bookmarks;
  }

  void getSessions() {
    AppRepository.getInstance().getSessions(bookmarks);
  }

  void deleteAllSession() {
    AppRepository.getInstance().deleteAllSession(bookmarks);
  }
}
