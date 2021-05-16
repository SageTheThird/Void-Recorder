package com.client.voidrecorder.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.client.voidrecorder.models.RecordingDB;

import java.util.List;

/*Interface for database transactions*/

@Dao
public interface RecordingsDAO {

    @Insert
    long addRecordingToDB(RecordingDB recordingDB);

    @Update
    void updateRecordingDB(RecordingDB recordingDB);

    @Delete
    void deleteRecordingDB (RecordingDB recordingDB);

    @Query("DELETE from recordings where title==:recTitle")
    void deleteRecordingDbWithTitle(String recTitle);

    @Query("select * from recordings")
    List<RecordingDB> getAllSavedRecordings();

    @Query("select * from recordings where title ==:title")
    RecordingDB getRecording(String title);

}
