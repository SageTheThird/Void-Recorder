package com.client.voidrecorder.models;
import android.net.Uri;

import androidx.annotation.NonNull;


/*Locally used model for recordings*/
public class Recording {


    String title;
    String duration;
    String date;
    long size;
    boolean saved;
    Uri uri;


    public Recording(String title, String duration, String date, long size, boolean saved, Uri uri) {
        this.title = title;
        this.duration = duration;
        this.date = date;
        this.size = size;
        this.saved = saved;
        this.uri = uri;
    }

    public boolean isSaved() {
        return saved;
    }

    public void setSaved(boolean saved) {
        this.saved = saved;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @NonNull
    @Override
    public String toString() {
        return "ModelRecordings{" +
                "title='" + title + '\'' +
                ", duration='" + duration + '\'' +
                ", date='" + date + '\'' +
                ", size='" + size + '\'' +
                ", saved=" + saved +
                ", uri=" + uri +
                '}';
    }
}