package com.client.voidrecorder.recordings;
import android.net.Uri;

public class ModelRecordings {


    String title;
    String duration;
    String date;
    long size;
    boolean saved;
    Uri uri;


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