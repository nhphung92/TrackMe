package com.utopia.trackme.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.utopia.trackme.data.local.dao.SessionDao;
import com.utopia.trackme.data.remote.pojo.SessionResponse;

@Database(entities = {SessionResponse.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

  public abstract SessionDao sessionDao();

  private static AppDatabase INSTANCE;

  public static AppDatabase getDatabase(final Context context) {
    if (INSTANCE == null) {
      synchronized (AppDatabase.class) {
        if (INSTANCE == null) {
          INSTANCE = Room.databaseBuilder(context, AppDatabase.class, "trackme_db")
              .build();
        }
      }
    }
    return INSTANCE;
  }
}