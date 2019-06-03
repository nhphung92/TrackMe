package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.utopia.trackme.data.local.Converters;
import java.util.List;

@Entity(tableName = "session")
public class SessionResponse implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "session_id")
  @SerializedName("session_id")
  @Expose
  private long sessionId;

  @ColumnInfo(name = "start_time")
  @SerializedName("start_time")
  @Expose
  private long startTime;

  @ColumnInfo(name = "end_time")
  @SerializedName("end_time")
  @Expose
  private long endTime;

  @ColumnInfo(name = "distance")
  @SerializedName("distance")
  @Expose
  private double distance;

  @ColumnInfo(name = "duration")
  @SerializedName("duration")
  @Expose
  private double duration;

  @ColumnInfo(name = "average_speed")
  @SerializedName("average_speed")
  @Expose
  private double averageSpeed;

  @ColumnInfo(name = "locations")
  @SerializedName("locations")
  @Expose
  @TypeConverters(Converters.class)
  private List<MyLatLng> locations;

  public SessionResponse() {
  }

  protected SessionResponse(Parcel in) {
    sessionId = in.readLong();
    startTime = in.readLong();
    endTime = in.readLong();
    distance = in.readDouble();
    duration = in.readDouble();
    averageSpeed = in.readDouble();
    locations = in.createTypedArrayList(MyLatLng.CREATOR);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(sessionId);
    dest.writeLong(startTime);
    dest.writeLong(endTime);
    dest.writeDouble(distance);
    dest.writeDouble(duration);
    dest.writeDouble(averageSpeed);
    dest.writeTypedList(locations);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<SessionResponse> CREATOR = new Creator<SessionResponse>() {
    @Override
    public SessionResponse createFromParcel(Parcel in) {
      return new SessionResponse(in);
    }

    @Override
    public SessionResponse[] newArray(int size) {
      return new SessionResponse[size];
    }
  };

  public long getSessionId() {
    return sessionId;
  }

  public void setSessionId(long sessionId) {
    this.sessionId = sessionId;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public double getDistance() {
    return distance;
  }

  public void setDistance(double distance) {
    this.distance = distance;
  }

  public double getDuration() {
    return duration;
  }

  public void setDuration(double duration) {
    this.duration = duration;
  }

  public double getAverageSpeed() {
    return distance / duration;
  }

  public void setAverageSpeed(double averageSpeed) {
    this.averageSpeed = averageSpeed;
  }

  public List<MyLatLng> getLocations() {
    return locations;
  }

  public void setLocations(List<MyLatLng> locations) {
    this.locations = locations;
  }
}
