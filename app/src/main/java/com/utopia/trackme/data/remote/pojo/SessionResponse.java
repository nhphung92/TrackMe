package com.utopia.trackme.data.remote.pojo;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import com.utopia.trackme.data.local.Converters;
import java.util.List;

@Entity(tableName = "session")
public class SessionResponse implements Parcelable {

  @NonNull
  @PrimaryKey
  @ColumnInfo(name = "session_id")
  private String sessionId = "";

  @ColumnInfo(name = "start_time")
  private long startTime;

  @ColumnInfo(name = "end_time")
  private long endTime;

  @ColumnInfo(name = "distance")
  private String distance;

  @ColumnInfo(name = "duration")
  private String duration;

  @ColumnInfo(name = "average_speed")
  private String averageSpeed;

  @ColumnInfo(name = "locations")
  @TypeConverters(Converters.class)
  private List<MyLatLng> locations;

  public SessionResponse() {
  }

  protected SessionResponse(Parcel in) {
    sessionId = in.readString();
    startTime = in.readLong();
    endTime = in.readLong();
    distance = in.readString();
    duration = in.readString();
    averageSpeed = in.readString();
    locations = in.createTypedArrayList(MyLatLng.CREATOR);
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(sessionId);
    dest.writeLong(startTime);
    dest.writeLong(endTime);
    dest.writeString(distance);
    dest.writeString(duration);
    dest.writeString(averageSpeed);
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

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
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

  public String getDistance() {
    return distance;
  }

  public void setDistance(String distance) {
    this.distance = distance;
  }

  public String getDuration() {
    return duration;
  }

  public void setDuration(String duration) {
    this.duration = duration;
  }

  public String getAverageSpeed() {
    return averageSpeed;
  }

  public void setAverageSpeed(String averageSpeed) {
    this.averageSpeed = averageSpeed;
  }

  public List<MyLatLng> getLocations() {
    return locations;
  }

  public void setLocations(List<MyLatLng> locations) {
    this.locations = locations;
  }

  public void addLocation(MyLatLng myLatLng) {
    this.locations.add(myLatLng);
  }
}
