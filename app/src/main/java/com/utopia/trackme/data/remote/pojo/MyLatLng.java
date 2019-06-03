package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class MyLatLng implements Parcelable {

  @SerializedName("lat")
  @Expose
  public double lat;

  @SerializedName("lng")
  @Expose
  public double lng;

  public MyLatLng(double lat, double lng) {
    this.lat = lat;
    this.lng = lng;
  }

  protected MyLatLng(Parcel in) {
    lat = in.readDouble();
    lng = in.readDouble();
  }

  public static final Creator<MyLatLng> CREATOR = new Creator<MyLatLng>() {
    @Override
    public MyLatLng createFromParcel(Parcel in) {
      return new MyLatLng(in);
    }

    @Override
    public MyLatLng[] newArray(int size) {
      return new MyLatLng[size];
    }
  };

  public double getLat() {
    return lat;
  }

  public void setLat(double lat) {
    this.lat = lat;
  }

  public double getLng() {
    return lng;
  }

  public void setLng(double lng) {
    this.lng = lng;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeDouble(lat);
    dest.writeDouble(lng);
  }
}
