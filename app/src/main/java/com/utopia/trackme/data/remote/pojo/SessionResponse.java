package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.google.maps.model.LatLng;
import com.utopia.trackme.data.local.Converters;
import java.util.List;

@Entity(tableName = "session")
public class SessionResponse implements Parcelable {

  @PrimaryKey
  @ColumnInfo(name = "session_id")
  private int sessionId;

  @ColumnInfo(name = "start_time")
  private long startTime;

  @ColumnInfo(name = "end_time")
  private long endTime;

  @ColumnInfo(name = "distance")
  private double distance;

  @ColumnInfo(name = "duration")
  private double duration;

  @ColumnInfo(name = "average_speed")
  private double averageSpeed;

  @ColumnInfo(name = "locations")
  @TypeConverters(Converters.class)
  private List<LatLng> locations;

  public SessionResponse() {
  }

  protected SessionResponse(Parcel in) {
    sessionId = in.readInt();
    startTime = in.readLong();
    endTime = in.readLong();
    distance = in.readDouble();
    duration = in.readDouble();
    averageSpeed = in.readDouble();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(sessionId);
    dest.writeLong(startTime);
    dest.writeLong(endTime);
    dest.writeDouble(distance);
    dest.writeDouble(duration);
    dest.writeDouble(averageSpeed);
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

  public int getSessionId() {
    return sessionId;
  }

  public void setSessionId(int sessionId) {
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
    return averageSpeed;
  }

  public void setAverageSpeed(double averageSpeed) {
    this.averageSpeed = averageSpeed;
  }

  public List<LatLng> getLocations() {
    return locations;
  }

  public void setLocations(List<LatLng> locations) {
    this.locations = locations;
  }
}
