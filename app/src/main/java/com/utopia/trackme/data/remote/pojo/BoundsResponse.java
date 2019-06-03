package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class BoundsResponse implements Parcelable {

  @SerializedName("northeast")
  private MyLatLng northeast;

  @SerializedName("southwest")
  private MyLatLng southwest;

  protected BoundsResponse(Parcel in) {
    northeast = in.readParcelable(MyLatLng.class.getClassLoader());
    southwest = in.readParcelable(MyLatLng.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(northeast, flags);
    dest.writeParcelable(southwest, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<BoundsResponse> CREATOR = new Creator<BoundsResponse>() {
    @Override
    public BoundsResponse createFromParcel(Parcel in) {
      return new BoundsResponse(in);
    }

    @Override
    public BoundsResponse[] newArray(int size) {
      return new BoundsResponse[size];
    }
  };

  public MyLatLng getNortheast() {
    return northeast;
  }

  public void setNortheast(MyLatLng northeast) {
    this.northeast = northeast;
  }

  public MyLatLng getSouthwest() {
    return southwest;
  }

  public void setSouthwest(MyLatLng southwest) {
    this.southwest = southwest;
  }
}
