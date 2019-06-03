package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.SerializedName;

public class RoutesResponse implements Parcelable {

  @SerializedName("bounds")
  private BoundsResponse bounds;

  protected RoutesResponse(Parcel in) {
    bounds = in.readParcelable(BoundsResponse.class.getClassLoader());
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(bounds, flags);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<RoutesResponse> CREATOR = new Creator<RoutesResponse>() {
    @Override
    public RoutesResponse createFromParcel(Parcel in) {
      return new RoutesResponse(in);
    }

    @Override
    public RoutesResponse[] newArray(int size) {
      return new RoutesResponse[size];
    }
  };

  public BoundsResponse getBounds() {
    return bounds;
  }

  public void setBounds(BoundsResponse bounds) {
    this.bounds = bounds;
  }
}
