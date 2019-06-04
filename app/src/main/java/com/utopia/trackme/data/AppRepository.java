package com.utopia.trackme.data;

import static com.utopia.trackme.utils.MyConstants.DIRECTION_MODE;

import android.app.Application;
import android.graphics.Color;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.utopia.trackme.data.local.AppDatabase;
import com.utopia.trackme.data.local.dao.SessionDao;
import com.utopia.trackme.data.remote.LookApiClient;
import com.utopia.trackme.data.remote.pojo.DirectionsResponse;
import com.utopia.trackme.data.remote.pojo.RoutesResponse;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
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

  public void getDirections(MutableLiveData<PolylineOptions> polylineOptions, String origin,
      String destination, String directionMode, String key) {
    LookApiClient.getApiClient().getDirections(origin, destination, directionMode, key)
        .map((Function<DirectionsResponse, Object>) directions -> {
          ArrayList<LatLng> points = new ArrayList<>();
          PolylineOptions lineOptions = new PolylineOptions();

          // Traversing through all the routes
          for (RoutesResponse routes : directions.getRoutes()) {
            lineOptions = new PolylineOptions();

            points.add(new LatLng(routes.getBounds().getSouthwest().lat,
                routes.getBounds().getSouthwest().lng));
            points.add(new LatLng(routes.getBounds().getNortheast().lat,
                routes.getBounds().getNortheast().lng));

            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);

            if (directionMode.equalsIgnoreCase(DIRECTION_MODE)) {
              lineOptions.width(10);
              lineOptions.color(Color.MAGENTA);
            } else {
              lineOptions.width(20);
              lineOptions.color(Color.RED);
            }
            Log.d("PointsParser", "onPostExecute lineoptions decoded");
          }

          return lineOptions;
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Object>() {
          @Override
          public void onSubscribe(Disposable d) {

          }

          @Override
          public void onNext(Object o) {
            polylineOptions.postValue((PolylineOptions) o);
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
