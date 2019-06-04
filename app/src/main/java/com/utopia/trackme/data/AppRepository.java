package com.utopia.trackme.data;

import androidx.lifecycle.MutableLiveData;
import com.utopia.trackme.data.local.AppDatabase;
import com.utopia.trackme.data.local.dao.SessionDao;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;

public class AppRepository {

  private final SessionDao mSessionDao;

  private static AppRepository mInstance;

  public static AppRepository getInstance() {
    if (mInstance == null) {
      mInstance = new AppRepository();
    }
    return mInstance;
  }

  private AppRepository() {
    AppDatabase appDatabase = AppDatabase.getDatabase(MyApplication.getInstance());
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

  public void startSession(SessionResponse session) {
    Observable.fromCallable(() -> {
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

          }

          @Override
          public void onError(Throwable e) {
            e.printStackTrace();
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

  public void deleteAllSession(MutableLiveData<List<SessionResponse>> sessions) {
    Observable.fromCallable(() -> {
      mSessionDao.delete(sessions.getValue());
      return mSessionDao.getAll();
    })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<List<SessionResponse>>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(List<SessionResponse> sessionResponses) {
            sessions.postValue(sessionResponses);
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
