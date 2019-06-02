package com.utopia.trackme.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.DateUtils;
import com.utopia.trackme.data.MyApplication;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SystemUtils {

  public static boolean isNetworkConnected() {
    ConnectivityManager cm = (ConnectivityManager) MyApplication.getInstance()
        .getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
  }

  public static String formatDate(long timeSTamp) {
    DateFormat formatter = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy", Locale.ENGLISH);
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timeSTamp * 1000);
    return formatter.format(calendar.getTime());
  }

  public static String formatNumber(int price) {
    NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
    return format.format(Double.valueOf(price));
  }

  public static String getTimeAgo(long mReferenceTime) {
    long now = System.currentTimeMillis();
    final long diff = now - mReferenceTime;
    if (diff < DateUtils.WEEK_IN_MILLIS) {
      return (diff <= 1000) ?
          "just now" :
          DateUtils
              .getRelativeTimeSpanString(mReferenceTime, now, DateUtils.MINUTE_IN_MILLIS,
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
}
