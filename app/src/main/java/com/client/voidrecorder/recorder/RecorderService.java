package com.client.voidrecorder.recorder;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.provider.MediaStore;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.util.Log;

import com.client.voidrecorder.R;
import com.client.voidrecorder.utils.Paths;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.client.voidrecorder.App.CHANNEL_ID;

public class RecorderService extends Service {

    private static final String TAG = "RecorderService";

    public static final int SECONDS = 1000;
    public static final int MINUTES = 60 * 1000;

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private MediaRecorder recorder;
    String recordingFullPath = "";
    String recordingName = "";


    public RecorderService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        try {
            //shows a silent notification and start the recorder
            showRecordingNotification();
            startRecorder();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }


    /*Setups the recorder and start recording*/
    private void startRecorder(){


        if(recorder == null){
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setAudioSamplingRate(RECORDER_SAMPLERATE);
//            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
//            recorder.setAudioChannels(1);
//            recorder.setAudioSamplingRate(44100);
//            recorder.setAudioEncodingBitRate(96000);
        }



        File folder = new File(Paths.getOutputFolder());

        recordingName = "REC " + dateTimeNow() +".3gp";

        recordingFullPath = folder.getAbsolutePath() + File.separator + recordingName;

        Log.e("path recording", recordingFullPath);

        recorder.setOutputFile(recordingFullPath);

        recorder.setMaxDuration(30 * SECONDS);//recording stops after x minutes

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

    //generates formatted date/time for title
    private String dateTimeNow(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_kk_mm_ss");
        return dateFormat.format(new Date());
    }

    private void showRecordingNotification() {
        Intent intent = new Intent(RecorderService.this, RecorderActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);

        // builds notification
        // the addAction re-use the same intent to keep the example short
        Notification notification = null;
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Recording... ")
                    .setContentText("VoiceRecorder")
                    .setSmallIcon(R.mipmap.ic_launcher)
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

}