package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DirectionsResponse implements Parcelable {

  @SerializedName("routes")
  @Expose
  private List<RoutesResponse> routes;

  public DirectionsResponse() {
  }

  protected DirectionsResponse(Parcel in) {
    routes = in.createTypedArrayList(RoutesResponse.CREATOR);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(routes);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<DirectionsResponse> CREATOR = new Creator<DirectionsResponse>() {
    @Override
    public DirectionsResponse createFromParcel(Parcel in) {
      return new DirectionsResponse(in);
    }

    @Override
    public DirectionsResponse[] newArray(int size) {
      return new DirectionsResponse[size];
    }
  };

  public List<RoutesResponse> getRoutes() {
    return routes;
  }

  public void setRoutes(List<RoutesResponse> routes) {
    this.routes = routes;
  }
}
