package com.client.voidrecorder.recorder;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;

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
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.client.voidrecorder.App.CHANNEL_ID;

public class RecorderService extends Service {

    private static final String TAG = "RecorderService";

    public static final int MINUTES = 60 * 1000;
    private static final int RECORDER_SAMPLE_RATE = 44100;
    private static int RECORDER_ENCODING_BIT_RATE = 128000;
    private MediaRecorder recorder;
    String recordingFullPath = "";
    String recordingName = "";
    private SharedPreferences sharedPreferences;
    private static String EXTENSION = "m4a";
    private FileHandler fileHandler;
    public static int MAX_ALLOWED_STORAGE = 30 * 1000000;//30MB - default
    DatabaseTransactions databaseTransactions;
    private static HashSet<String> savedRecordingsSet;
    private static String OUTPUT_QUALITY = "";

    //countdownTimer vars
    CountDownTimer countDownTimer;

    public RecorderService() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            databaseTransactions = new DatabaseTransactions(this);
            fileHandler = new FileHandler(this);

            //shows a silent notification and start the recorder
            showRecordingNotification();

            //gets selected settings
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            OUTPUT_QUALITY = sharedPreferences.getString(getString(R.string.output_quality_pref), "");

            startRecorder();
            startTimer();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    /*Timer for recording*/
    private void startTimer() {
        Intent intent = new Intent("CountDownIntent");
        final long[] milliSeconds = {0};
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                milliSeconds[0] = milliSeconds[0] + 1000;
                intent.putExtra("timer", milliSeconds[0]);
                sendBroadcast(intent);
            }

            public void onFinish() {
                stopSelf();
            }
        }.start();
    }

    private void fetchSavedRecordings() {
        try {
            savedRecordingsSet = databaseTransactions.getAllRecordingsDb();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

        MAX_ALLOWED_STORAGE = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.max_space_pref), ""))) * 1000000 ;

    }

    /*Checks space limit and checks if the file is saved or not and delete accordingly*/
    private void checkSpaceLimitAndDelete() {

        File[] files = fileHandler.getFilesFromOutputFolder();
        long totalSize =  0;
        
        for(File file : files){
            totalSize += file.length();
        }
        
        if(totalSize != 0 && totalSize >= MAX_ALLOWED_STORAGE){
            //total size has exceeded space limit
            //loop through files and delete the oldest non-saved
            for(int i= files.length - 1; i>=0 ; i--) {

                if(!isRecordingSaved(files[i].getName())){
                    //recording is not saved, delete the recording and check the size
                    totalSize -= files[i].length();
                    fileHandler.deleteFile(files[i]);

                    //check if after deletion it is less than max_allowed
                    if(totalSize < MAX_ALLOWED_STORAGE){
                        break;
                    }
                }
            }
        }
    }

    /*Setup the recorder and start recording*/
    private void startRecorder(){
        //if automatic deletion: fetch saved recordings and delete the oldest non-saved files
        if(sharedPreferences.getBoolean(getString(R.string.automatic_deletion_pref), false)) {
            fetchSavedRecordings();
            checkSpaceLimitAndDelete();
        }

        if(recorder == null){

            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            setOutputFormat(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.output_format_pref), "")));
            setAudioQuality(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.output_quality_pref), "")));

            recorder.setAudioEncodingBitRate(RECORDER_ENCODING_BIT_RATE);//2 * 128000 - highest
            recorder.setAudioSamplingRate(RECORDER_SAMPLE_RATE);
        }

        File folder = new File(Paths.getOutputFolder());

        recordingName = "REC " + dateTimeNow() +  OUTPUT_QUALITY + "." + EXTENSION;

        recordingFullPath = folder.getAbsolutePath() + File.separator + recordingName;

        recorder.setOutputFile(recordingFullPath);
        recorder.setMaxDuration(Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(getApplicationContext().getString(R.string.max_duration), ""))) * MINUTES);//recording stops after x minutes

        //After the recorder reaches  maxDuration, we can catch the event here
        recorder.setOnInfoListener((mediaRecorder, i, i1) -> {
            stopRecorder();
            //if automatic deletion: on , fetch saved recordings and delete the oldest non-saved files
            deleteFilesIfSpaceLimitReached();
            startRecorder();
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
            case "Hi":
                RECORDER_ENCODING_BIT_RATE = 2 * 128000;
                break;
            case "Me":
                RECORDER_ENCODING_BIT_RATE = 128000;
                break;
            case "Lo":
                RECORDER_ENCODING_BIT_RATE = 64000;
                break;
        }

    }

    //generates formatted date/time for title
    private String dateTimeNow(){
        @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat = new SimpleDateFormat(getString(R.string.recordings_title_timestamp_format));
        return dateFormat.format(new Date());
    }

    private void showRecordingNotification() {
        Intent intent = new Intent(RecorderService.this, MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // builds notification
        // the addAction re-use the same intent to keep the example short
        Notification notification;
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.notification_title))
                    .setContentText(getString(R.string.notification_content))
                    .setSmallIcon(R.drawable.ic_baseline_settings_24)
                    .setContentIntent(pIntent)
                    .setPriority(Notification.PRIORITY_MIN)
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);
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
            countDownTimer.cancel();//cancel timer

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
    }

    public boolean isRecordingSaved(String title) {
        if(savedRecordingsSet== null) return false;
        return savedRecordingsSet.size() > 0 && savedRecordingsSet.contains(title);
    }
}