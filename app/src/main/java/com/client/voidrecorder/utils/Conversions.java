package com.client.voidrecorder.utils;

import android.annotation.SuppressLint;
import android.util.Log;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.concurrent.TimeUnit;




public class Conversions {
    private static final String TAG = "Conversions";


    /*Converts size in bytes into human readable format*/
    @SuppressLint("DefaultLocale")
    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes  && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }


    //converts time to human readable form
    @SuppressLint("DefaultLocale")
    public static String timeConversion(long value) {
        String audioTime;
        int dur = (int) value;
        int hrs = (dur / 3600000);
        int mns = (dur / 60000) % 60000;
        int scs = dur % 60000 / 1000;

        if (hrs > 0) {
            audioTime = String.format("%02d:%02d:%02d", hrs, mns, scs);
        } else {
            audioTime = String.format("%02d:%02d", mns, scs);
        }
        return audioTime;
    }


    @SuppressLint("DefaultLocale")
    public static String getFormattedCountDownFromMillis(long milliSeconds){

        return String.format(
                "%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(milliSeconds),
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(milliSeconds)
                ),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(milliSeconds)
                )
        );

    }


    /*Converts string into quality indicator*/
    public static String getQualityFromTitle(String fileName) {

        String withoutExt = fileName.substring(fileName.length()-6);
        Log.d(TAG, "getQualityFromTitle: withoutExt : "+withoutExt);
        String quality  = withoutExt.substring(0, withoutExt.length() -4);
        Log.d(TAG, "getQualityFromTitle: quality : "+quality);

        if(quality.equals("Hi")){
            return "Hi";
        }else if(quality.equals("Me")){
            return "Me";
        }else {
            return "Lo";
        }

    }

    /*Converts size into milliseconds - for determining duration of recording*/
    public static long getSecondsFromSize(long sizeInBytes, String quality, String extension){
        //High m4a - 32,207
        //Medium m4a - 16289
        //Low m4a - 8,244

        //Low 3gp - 1865

        if(extension.equals("m4a") || extension.equals("mp3")){
            switch (quality){
                case "Hi":
                    return sizeInBytes / 32207;
                case "Me":
                    return sizeInBytes / 16289;
                case "Lo":
                    return sizeInBytes / 8244;
                default:
                    return sizeInBytes;
            }
        }else {
            return sizeInBytes / 1865;
        }
    }

    /*Converts milliseconds into formatted duration for recording*/
    @SuppressLint("DefaultLocale")
    public static String getFormattedDurationFromSeconds(long milliSeconds){

//        milliSeconds = milliSeconds + 1000;

        return  String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(milliSeconds),
                TimeUnit.MILLISECONDS.toSeconds(milliSeconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds))
        );

    }

}
