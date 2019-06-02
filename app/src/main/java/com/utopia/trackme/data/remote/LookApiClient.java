package com.utopia.trackme.data.remote;

import com.google.gson.GsonBuilder;
import com.utopia.trackme.utils.MyConfig;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class LookApiClient {

  public static LookApiInterface getApiClient() {

    OkHttpClient httpClient = new Builder().addNetworkInterceptor(chain -> {
      Request request = chain.request();
      Headers.Builder builder = request.headers().newBuilder();
      request = request.newBuilder().headers(builder.build()).build();
      return chain.proceed(request);
    }).addNetworkInterceptor(
        new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)).build();

    httpClient.newBuilder().connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES);

    Retrofit retrofit = new Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(new GsonBuilder().create()))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .client(httpClient)
        .baseUrl(MyConfig.API_URL)
        .build();
    return retrofit.create(LookApiInterface.class);
  }
}
