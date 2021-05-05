package com.client.voidrecorder.utils;

import android.util.Log;

import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
import java.util.Date;

public class Conversions {
    private static final String TAG = "Conversions";


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


    public static String getTimeDifference(String startTime, String endTime){

        String diff= "";

        try {

            SimpleDateFormat format = new SimpleDateFormat("kk:mm:ss aa");

            Date startTimeOb = format.parse(startTime);
            Date endTimeOb = format.parse(endTime);


            long difference = endTimeOb.getTime() - startTimeOb.getTime();

            Log.v("Data1", ""+startTimeOb.getTime());
            Log.v("Data2", ""+endTimeOb.getTime());

            long seconds = (int) (difference / 1000) % 60;
            int hours = (int) (difference/(1000 * 60 * 60));
            int mins = (int) (difference/(1000*60)) % 60;



//            long diffInSec = TimeUnit.MILLISECONDS.toSeconds(mills);


            diff = hours + ":" + mins + ":" +seconds; // updated value every1 second
            Log.d(TAG, "onViewCreated: Time Difference : "+diff);


        }catch (Exception e){
            e.printStackTrace();
        }

        return diff;
    }


    public static String getTimeNow(){


        String timeNow = null;

        try {

            SimpleDateFormat format = new SimpleDateFormat("kk:mm:ss aa");
            timeNow = format.format(new Date());


        }catch (Exception e){
            e.printStackTrace();
        }

        return timeNow;


    }

}
