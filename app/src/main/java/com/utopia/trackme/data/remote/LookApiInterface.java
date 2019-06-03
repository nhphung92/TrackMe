package com.utopia.trackme.data.remote;

import com.utopia.trackme.data.remote.pojo.DirectionsResponse;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface LookApiInterface {

  @GET("directions/json?")
  Observable<DirectionsResponse> getDirections(
      @Query("origin") String origin,
      @Query("destination") String destination,
      @Query("mode") String mode,
      @Query("key") String key);
}
