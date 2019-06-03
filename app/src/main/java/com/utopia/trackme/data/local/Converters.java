package com.utopia.trackme.data.local;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.utopia.trackme.data.remote.pojo.MyLatLng;
import java.util.List;

public class Converters {

  @TypeConverter
  public static List<MyLatLng> stringToList(String s) {
    return new Gson().fromJson(s, new TypeToken<List<MyLatLng>>() {
    }.getType());
  }

  @TypeConverter
  public static String listToString(List<MyLatLng> list) {
    return new Gson().toJson(list);
  }
}