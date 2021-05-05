package com.client.voidrecorder;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.preference.PreferenceManager;


public class App extends Application {
    public static final String CHANNEL_ID = "recordingServiceChannel";

    private static Context context;

    NotificationManager manager;
    @Override
    public void onCreate() {
        super.onCreate();


        context = getApplicationContext();


        //sets default values for settings
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        createNotificationChannel();



    }
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "RecordingServiceNotificationChannel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }


    public static Context getAppContext(){
        return context;
    }

    public void cancelNotification(){


        manager.cancelAll();

    }
}
