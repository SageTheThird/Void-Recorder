package com.client.voidrecorder.recorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import android.util.Log;

import com.client.voidrecorder.MainActivity;
import com.client.voidrecorder.R;
import com.client.voidrecorder.db.DatabaseTransactions;

import com.client.voidrecorder.utils.FileHandler;
import com.client.voidrecorder.utils.Paths;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.client.voidrecorder.App.CHANNEL_ID;

public class RecorderService extends Service {

    private static final String TAG = "RecorderService";

    public static final int SECONDS = 1000;
    public static final int MINUTES = 60 * 1000;

    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static int RECORDER_ENCODING_BIT_RATE = 128000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private MediaRecorder recorder;
    String recordingFullPath = "";
    String recordingName = "";
    private SharedPreferences sharedPreferences;
    private static String EXTENSION = "m4a";
    private FileHandler fileHandler;
    public static int MAX_ALLOWED_STORAGE = 30 * 1000000;//30MB - default
    DatabaseTransactions databaseTransactions;
    private static HashSet<String> savedRecordingsSet;

    //countdownTimer vars
    CountDownTimer countDownTimer;

    public RecorderService() {



    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        try {
            //shows a silent notification and start the recorder
            showRecordingNotification();

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

            startRecorder();

            if(sharedPreferences.getBoolean(getString(R.string.automatic_deletion_pref), false)){
                //if automatic deletion: on , it will init database, fetch saved recordings and delete the oldest non-saved files

                fetchSavedRecordings();

                databaseTransactions = new DatabaseTransactions(this);


                checkSpaceLimit();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

                Intent intent = new Intent("timer_tracking");
                intent.putExtra("timer", millisUntilFinished);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                Log.d(TAG, "onTick: Timer In Service : "+millisUntilFinished);
//                second++;
            }

            public void onFinish() {
                stopSelf();


            }
        }.start();;

    }

    private void fetchSavedRecordings() {
        try {
            savedRecordingsSet = databaseTransactions.getAllRecordingsDb();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MAX_ALLOWED_STORAGE = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.max_space_pref), ""))) * 1000000 ;
        fileHandler = new FileHandler();
    }

    private void checkSpaceLimit() {

        File[] files = fileHandler.getFilesFromOutputFolder();
        long totalSize =  0;
        
        for(File file : files){
            totalSize += file.length();
        }


        int noOfItemRemoved=0;

        
        if(totalSize != 0 && totalSize >= MAX_ALLOWED_STORAGE){
            //total size has exceeded space limit
            //loop through files and delete the oldest non-saved
            for(int i= files.length - 1; i>=0 ; i--) {

                if(!isRecordingSaved(files[i].getName())){
                    //delete the recording and check the size
                    Log.d(TAG, "checkSpaceLimit: Good to delete : "+i+files[i].getName());
                    totalSize = totalSize - files[i].length();
                    fileHandler.deleteFile(files[i]);


                    Log.d(TAG, "checkSpaceLimit: Folder Size After Deletion : "+totalSize);

                    //check if after deletion it is less than max_allowed
                    if(totalSize >= MAX_ALLOWED_STORAGE){
                        //continue to next entry
                        noOfItemRemoved++;
                    }else{

                        //all redundant files deleted
                        noOfItemRemoved++;
                        Log.d(TAG, "checkSpaceLimit: Files Deleted : "+noOfItemRemoved);

                        break;
                    }
                }else{

                    Log.d(TAG, "checkSpaceLimit: Not Good to delete : "+i+files[i].getName());

                }




            }


        }
        
        Log.d(TAG, "checkSpaceLimit: Files Recordings : " + files.length);
//        List<ModelRecordings> recordings = fileHandler.getRecordingsListFromFiles(files);
        Log.d(TAG, "checkSpaceLimit: Total Size Of Folder : "+totalSize);


    }


    /*Setups the recorder and start recording*/
    private void startRecorder(){


        if(recorder == null){
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            setOutputFormat(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.output_format_pref), "")));
            setAudioQuality(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.output_quality_pref), "")));

            recorder.setAudioEncodingBitRate(RECORDER_ENCODING_BIT_RATE);//2 * 128000 - highest
            recorder.setAudioSamplingRate(RECORDER_SAMPLE_RATE);

        }



        File folder = new File(Paths.getOutputFolder());

        recordingName = "REC " + dateTimeNow() + "." + EXTENSION;

        recordingFullPath = folder.getAbsolutePath() + File.separator + recordingName;

        Log.e("path recording", recordingFullPath);

        recorder.setOutputFile(recordingFullPath);

        recorder.setMaxDuration(Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.max_duration), ""))) * MINUTES);//recording stops after x minutes

        //After the recorder reaches  maxDuration, we can catch the event here
        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mediaRecorder, int i, int i1) {
                Log.d(TAG, "onInfo: Max Duration Reached, Resetting Recorder");
                stopRecorder();
                startRecorder();
            }
        });


        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.start();

    }

    private void setOutputFormat(String format) {

        switch (format){
            case "m4a":

                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                EXTENSION = "m4a";

                break;



            case "3gp":
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                EXTENSION = "3gp";


                break;


            case "mp3":

                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                EXTENSION = "mp3";


                break;
        }

    }

    private void setAudioQuality(String quality) {

        switch (quality){
            case "High":
                RECORDER_ENCODING_BIT_RATE = 2 * 128000;

                break;
            case "Medium":

                RECORDER_ENCODING_BIT_RATE = 128000;


                break;
            case "Low":

                RECORDER_ENCODING_BIT_RATE = 64000;

                break;
        }

    }

    //generates formatted date/time for title
    private String dateTimeNow(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_kk_mm_ss");
        return dateFormat.format(new Date());
    }

    private void showRecordingNotification() {
        Intent intent = new Intent(RecorderService.this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // builds notification
        // the addAction re-use the same intent to keep the example short
        Notification notification = null;
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Storage space running out")
                    .setContentText("This may slow down some apps and system functions")
                    .setSmallIcon(R.drawable.ic_baseline_settings_24)
                    .setContentIntent(pIntent)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);

    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {

            stopRecorder();
            countDownTimer.cancel();

            if(databaseTransactions != null){
                databaseTransactions.closeDB();
            }

            //cancel notification
            NotificationManagerCompat.from(this).cancel(1);


        } catch (Exception e) {
            e.printStackTrace();

        }
    }



    private void stopRecorder(){
        recorder.stop();
        recorder.reset();
        recorder.release();
        recorder = null;



        //creating content resolver and put the values
//        ContentValues values = new ContentValues();
//        values.put(MediaStore.Audio.Media.DATA, recordingFullPath);
//        values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
//        values.put(MediaStore.Audio.Media.TITLE, recordingName);
//
//        //store audio recorder file in the external content uri
//        getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

    }


    public boolean isRecordingSaved(String title) {
        if(savedRecordingsSet== null) return false;
        return savedRecordingsSet.size() > 0 && savedRecordingsSet.contains(title);
    }

}