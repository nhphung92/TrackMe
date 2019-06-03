package com.utopia.trackme.data;

import android.app.Application;
import android.location.Location;
import androidx.lifecycle.MutableLiveData;
import com.utopia.trackme.data.local.AppDatabase;
import com.utopia.trackme.data.local.dao.SessionDao;
import com.utopia.trackme.data.remote.pojo.MyLatLng;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import com.utopia.trackme.services.LocationService.SessionCallback;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class AppRepository {

  private final SessionDao mSessionDao;

  public SessionDao getSessionDao() {
    return mSessionDao;
  }

  private Application mApplication = MyApplication.getInstance();

  private static AppRepository mInstance;

  public static AppRepository getInstance() {
    if (mInstance == null) {
      mInstance = new AppRepository();
    }
    return mInstance;
  }

  private AppRepository() {
    AppDatabase appDatabase = AppDatabase.getDatabase(mApplication);
    mSessionDao = appDatabase.sessionDao();
  }

  public void getSessions(MutableLiveData<List<SessionResponse>> liveData) {
    Observable.fromCallable(mSessionDao::getAll)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<List<SessionResponse>>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(List<SessionResponse> userListResponse) {
            liveData.setValue(userListResponse);
          }

          @Override
          public void onError(Throwable e) {
            liveData.setValue(new ArrayList<>());
          }

          @Override
          public void onComplete() {

          }
        });
  }

  public void startSession(SessionCallback sessionCallback, long startTime, Location location) {
    Observable.fromCallable(() -> {
      SessionResponse session = new SessionResponse();
      session.setSessionId(startTime);
      session.setStartTime(startTime);
      session.setEndTime(startTime);
      session.setDistance(0);
      session.setDuration(0);
      session.setAverageSpeed(0);
      List<MyLatLng> latLngs = new ArrayList<>();
      latLngs.add(new MyLatLng(location.getLatitude(), location.getLongitude()));
      session.setLocations(latLngs);
      mSessionDao.insert(session);
      return session;
    })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<SessionResponse>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(SessionResponse session) {
            sessionCallback.onNewSession(session);
          }

          @Override
          public void onError(Throwable e) {
            sessionCallback.onNewSession(null);
          }

          @Override
          public void onComplete() {

          }
        });
  }

  public void updateSession(SessionResponse session) {
    Observable.fromCallable(() -> {
      mSessionDao.update(session);
      return session;
    })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<SessionResponse>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(SessionResponse session) {
          }

          @Override
          public void onError(Throwable e) {
          }

          @Override
          public void onComplete() {

          }
        });
  }
}
