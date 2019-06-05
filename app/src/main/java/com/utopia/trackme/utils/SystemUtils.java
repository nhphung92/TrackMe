package com.utopia.trackme.utils;

import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import com.utopia.trackme.data.MyApplication;
import java.text.DecimalFormat;

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

  public static double calculationByDistance(Location location1, Location location2) {
    int Radius = 6371;//radius of earth in Km

    double dLat = Math.toRadians(location2.getLatitude() - location1.getLatitude());
    double dLon = Math.toRadians(location2.getLongitude() - location1.getLongitude());
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(location1.getLatitude())) * Math
            .cos(Math.toRadians(location2.getLatitude())) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.asin(Math.sqrt(a));
    double valueResult = Radius * c;

    double km = valueResult / 1;
    DecimalFormat newFormat = new DecimalFormat("####");
    int kmInDec = Integer.valueOf(newFormat.format(km));
    double meter = valueResult % 1000;
    int meterInDec = Integer.valueOf(newFormat.format(meter));
    return valueResult * 1000; // Meter
  }

  public static String formatNumber(Double num) {
    DecimalFormat decimalFormat = new DecimalFormat("###,###.###");
    return decimalFormat.format(num);
  }
}
