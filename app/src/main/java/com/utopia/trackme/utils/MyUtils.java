package com.utopia.trackme.utils;

public class MyUtils {

  public static String convertTime(long time) {
    long hours = time / 3600;
    long minutes = (time % 3600) / 60;
    long seconds = (time % 3600) % 60;

    String hoursStr = hours < 10 ? "0" + hours : String.valueOf(hours);
    String minutesStr = minutes < 10 ? "0" + minutes : String.valueOf(minutes);
    String secondsStr = seconds < 10 ? "0" + seconds : String.valueOf(seconds);

    if (hours == 0) {
      return minutesStr + ":" + secondsStr;
    }
    return hoursStr + ":" + minutesStr + ":" + secondsStr;
  }
}
