package com.utopia.trackme.data.local;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.maps.model.LatLng;
import java.util.List;

public class Converters {

  @TypeConverter
  public static List<LatLng> stringToList(String s) {
    return new Gson().fromJson(s, new TypeToken<List<LatLng>>() {
    }.getType());
  }

  @TypeConverter
  public static String listToString(List<LatLng> list) {
    return new Gson().toJson(list);
  }
}