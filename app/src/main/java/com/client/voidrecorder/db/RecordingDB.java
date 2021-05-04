package com.client.voidrecorder.db;

import android.net.Uri;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "recordings")
public class RecordingDB {

    @ColumnInfo(name="id")
    @PrimaryKey(autoGenerate =true)
    private long id;

    @ColumnInfo(name="title")
    private String title;

    @ColumnInfo(name="uri")
    private String uri;


    @Ignore
    public RecordingDB() {
    }



    public RecordingDB(long id, String title, String uri) {

        this.title = title;
        this.uri = uri;
        this.id = id;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "RecordingDB{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", uri='" + uri + '\'' +
                '}';
    }
}