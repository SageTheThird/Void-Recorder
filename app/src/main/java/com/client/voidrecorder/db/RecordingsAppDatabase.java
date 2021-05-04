package com.client.voidrecorder.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {RecordingDB.class},version = 1)

public abstract class RecordingsAppDatabase extends RoomDatabase {

    public abstract RecordingsDAO getRecordingsDAO();



}