package com.utopia.trackme.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import com.utopia.trackme.data.MyApplication;

public class SystemUtils {

  public static boolean isNetworkConnected() {
    ConnectivityManager cm = (ConnectivityManager) MyApplication.getInstance()
        .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  public static String getTimeAgo(long timeSTamp) {
    long now = System.currentTimeMillis();
    final long diff = now - timeSTamp;
    if (diff < DateUtils.WEEK_IN_MILLIS) {
      return (diff <= 1000) ?
          "just now" :
          DateUtils
              .getRelativeTimeSpanString(timeSTamp, now, DateUtils.MINUTE_IN_MILLIS,
                  DateUtils.FORMAT_ABBREV_RELATIVE).toString();
    } else if (diff <= 4 * DateUtils.WEEK_IN_MILLIS) {
      int week = (int) (diff / (DateUtils.WEEK_IN_MILLIS));
      return week > 1 ? week + " weeks ago" : week + " week ago";
    } else if (diff < DateUtils.YEAR_IN_MILLIS) {
      int month = (int) (diff / (4 * DateUtils.WEEK_IN_MILLIS));
      return month > 1 ? month + " months ago" : month + " month ago";
    } else {
      int year = (int) (diff / DateUtils.YEAR_IN_MILLIS);
      return year > 1 ? year + " years ago" : year + " year ago";
    }
  }

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
