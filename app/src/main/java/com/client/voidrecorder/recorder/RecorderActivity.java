package com.client.voidrecorder.recorder;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import com.client.voidrecorder.R;
import com.client.voidrecorder.recordings.RecordingsActivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class RecorderActivity extends AppCompatActivity {

    private static final String TAG = "RecorderActivity";

    private TextView timerTextView;
    private ImageView startBtn, stopBtn, recordingsBtn, settingsBtn;
    private CountDownTimer countDownTimer;
    private int second = -1, minute, hour;// timer vars
    public static final int PERMISSION_ALL = 0;
    private Intent serviceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);


        //recording service controller
        serviceIntent = new Intent(this, RecorderService.class);

        bindViews();

        if (permissionsGranted()) {


            setAudioRecorder();

            if (isServiceRunningInForeground(this, RecorderService.class)) {
                //when app is closed completely and then opened again, showRecording, ResumeTimer
                Log.d(TAG, "onCreate: Service is running, toggle to recording");
                showRecordingUI();
            }

        }


    }


    /* */
    private void bindViews() {
        timerTextView = (TextView) findViewById(R.id.text);
        startBtn = (ImageView) findViewById(R.id.start);
        stopBtn = (ImageView) findViewById(R.id.stop);
        recordingsBtn = (ImageView) findViewById(R.id.recordings);
        settingsBtn = (ImageView) findViewById(R.id.settingsBtn);
    }

    /* */
    public void setAudioRecorder() {

        stopBtn.setEnabled(false);
        stopBtn.setBackgroundResource(R.drawable.normal_background);
        stopBtn.setImageResource(R.drawable.noraml_stop);

        startBtn.setOnClickListener(startBtnClickListener);
        stopBtn.setOnClickListener(stopBtnClickListener);
        recordingsBtn.setOnClickListener(recordingsBtnClickListener);
        settingsBtn.setOnClickListener(settingsBtnClickListener);

    }


    /* */
    private void showRecordingUI() {
        startBtn.setEnabled(false);
        recordingsBtn.setEnabled(false);
        stopBtn.setEnabled(true);
        stopBtn.setBackgroundResource(R.drawable.round_shape);
        stopBtn.setImageResource(R.drawable.ic_stop_black_35dp);
        recordingsBtn.setBackgroundResource(R.drawable.normal_background);
        recordingsBtn.setImageResource(R.drawable.normal_menu);
    }


    //display recording time
    public void showTimer() {
        countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                second++;
                timerTextView.setText(recorderTime());
            }

            public void onFinish() {

            }
        };
        countDownTimer.start();
    }


    //recorder time
    public String recorderTime() {
        if (second == 60) {
            minute++;
            second = 0;
        }
        if (minute == 60) {
            hour++;
            minute = 0;
        }
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }

    //runtime permissions check
    public boolean permissionsGranted() {
        int RECORD_AUDIO_PERMISSION = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        ArrayList<String> PERMISSION_LIST = new ArrayList<>();
        if ((RECORD_AUDIO_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.RECORD_AUDIO);
        }
        if ((WRITE_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if ((READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!PERMISSION_LIST.isEmpty()) {
            ActivityCompat.requestPermissions(this, PERMISSION_LIST.toArray(new String[PERMISSION_LIST.size()]), PERMISSION_ALL);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean record = false, storage = false;
        switch (requestCode) {
            case PERMISSION_ALL: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (permissions[i].equals(Manifest.permission.RECORD_AUDIO)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                record = true;
                            } else {
                                Toast.makeText(getApplicationContext(), "Please allow Microphone permission", Toast.LENGTH_LONG).show();
                            }
                        } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                storage = true;
                            } else {
                                Toast.makeText(getApplicationContext(), "Please allow Storage permission", Toast.LENGTH_LONG).show();
                            }

                        }
                    }
                }
                if (record && storage) {
                    setAudioRecorder();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //here we save the current exact time in sharedpref and then when the app opens subtract it from
        // that time and show on timer textview

    }

    public static boolean isServiceRunningInForeground(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }

            }
        }
        return false;
    }





    /* ----------------------------LISTENERS--------------------------------------------*/

    View.OnClickListener startBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            showRecordingUI();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            showTimer();
        }
    };

    View.OnClickListener stopBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            stopService(serviceIntent);
            //cancel count down timer
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            startBtn.setEnabled(true);
            recordingsBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            stopBtn.setBackgroundResource(R.drawable.normal_background);
            stopBtn.setImageResource(R.drawable.noraml_stop);
            recordingsBtn.setBackgroundResource(R.drawable.round_shape);
            recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
            second = -1;
            minute = 0;
            hour = 0;
            timerTextView.setText("00:00:00");
        }
    };

    View.OnClickListener recordingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            startBtn.setEnabled(true);
            recordingsBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            stopBtn.setBackgroundResource(R.drawable.normal_background);
            stopBtn.setImageResource(R.drawable.noraml_stop);
            recordingsBtn.setBackgroundResource(R.drawable.round_shape);
            recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
            startActivity(new Intent(getApplicationContext(), RecordingsActivity.class));
        }
    };

    View.OnClickListener settingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Toast.makeText(RecorderActivity.this, "Settings Clicked", Toast.LENGTH_LONG).show();
        }



    };

}