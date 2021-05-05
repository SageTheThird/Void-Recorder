package com.client.voidrecorder.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.client.voidrecorder.models.RecordingDB;

import java.util.List;

@Dao
public interface RecordingsDAO {

    @Insert
    public  long addRecordingToDB(RecordingDB recordingDB);

    @Update
    public void updateRecordingDB(RecordingDB recordingDB);

    @Delete
    public void deleteRecordingDB (RecordingDB recordingDB);

    @Query("DELETE from recordings where title==:recTitle")
    public void deleteRecordingDbWithTitle(String recTitle);

    @Query("select * from recordings")
    public List<RecordingDB> getAllSavedRecordings();

    @Query("select * from recordings where title ==:title")
    public RecordingDB getRecording(String title);

}
