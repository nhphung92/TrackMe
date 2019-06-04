package com.utopia.trackme.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.utopia.trackme.data.remote.pojo.SessionResponse;
import java.util.List;

@Dao
public interface SessionDao {

  @Query("SELECT * FROM session ORDER BY start_time DESC")
  List<SessionResponse> getAll();

  @Insert
  void insert(SessionResponse session);

  @Insert
  void insertAll(List<SessionResponse> list);

  @Update
  void update(SessionResponse session);

  @Delete
  void delete(SessionResponse session);

  @Delete
  void delete(List<SessionResponse> session);
}