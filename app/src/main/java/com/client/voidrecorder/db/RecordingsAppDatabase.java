package com.client.voidrecorder.db;


import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.client.voidrecorder.models.RecordingDB;

@Database(entities = {RecordingDB.class},version = 1)

public abstract class RecordingsAppDatabase extends RoomDatabase {

    public abstract RecordingsDAO getRecordingsDAO();



}