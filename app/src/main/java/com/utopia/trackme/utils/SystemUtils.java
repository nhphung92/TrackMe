package com.utopia.trackme.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import android.util.Log;
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

  public static float getDistance(double lat1, double lng1, double lat2, double lng2) {
    double earthRadius = 3958.75;
    double dLat = Math.toRadians(lat2 - lat1);
    double dLng = Math.toRadians(lng2 - lng1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1))
        * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
        * Math.sin(dLng / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    double dist = earthRadius * c;

    int meterConversion = 1609;

    return (float) (dist * meterConversion);

  }

  public static double calculationByDistance(double lat1, double lon1, double lat2, double lon2) {
    int Radius = 6371;//radius of earth in Km

    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);
    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2);
    double c = 2 * Math.asin(Math.sqrt(a));
    double valueResult = Radius * c;

    double km = valueResult / 1;
    DecimalFormat newFormat = new DecimalFormat("####");
    int kmInDec = Integer.valueOf(newFormat.format(km));
    double meter = valueResult % 1000;
    int meterInDec = Integer.valueOf(newFormat.format(meter));
    Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec + " Meter   " + meterInDec);

    return valueResult * 1000; // Meter
  }

  public static String formatNumber(String num) {
    return new DecimalFormat("#.###").format(num);
  }
}
