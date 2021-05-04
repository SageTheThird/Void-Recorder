package com.client.voidrecorder.recorder;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import com.client.voidrecorder.R;
import com.client.voidrecorder.utils.SharedPreferencesHelper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.os.CountDownTimer;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class RecorderFragment extends Fragment {

    private static final String TAG = "RecorderActivity";

    private TextView timerTextView;
    private ImageView startBtn, stopBtn, recordingsBtn, settingsBtn;
    private CountDownTimer countDownTimer;
    private int second = -1, minute, hour;// timer vars
    public static final int PERMISSION_ALL = 0;
    private Intent serviceIntent;
    private Context mContext;
    private SharedPreferences sharedPreferences;


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recorder, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        mContext = getActivity();
        //recording service controller
        serviceIntent = new Intent(mContext, RecorderService.class);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());

        String maxSpace = sharedPreferences.getString("maxSpace", "");
        String outputFormat = sharedPreferences.getString("outputFormatList", "");
        boolean automaticDeletion = sharedPreferences.getBoolean("spaceLimitDialog", false);

        Log.d(TAG, "onViewCreated: OutputFormat Shared Preferences : "+ maxSpace);
        Log.d(TAG, "onViewCreated: OutputFormat Shared Preferences : "+ outputFormat);
        Log.d(TAG, "onViewCreated: OutputFormat Shared Preferences : "+ automaticDeletion);

        bindViews(view);

        if (permissionsGranted()) {


            setAudioRecorder();

            if (isServiceRunningInForeground(mContext, RecorderService.class)) {
                //when app is closed completely and then opened again, showRecording, ResumeTimer
                Log.d(TAG, "onCreate: Service is running, toggle to recording");
                showRecordingUI();
            }

        }



    }




    /* */
    private void bindViews(View parentView) {
        timerTextView =  parentView.findViewById(R.id.text);
        startBtn = parentView.findViewById(R.id.start);
        stopBtn =  parentView.findViewById(R.id.stop);
        recordingsBtn = parentView.findViewById(R.id.recordings);
        settingsBtn =  parentView.findViewById(R.id.settingsBtn);
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
        int RECORD_AUDIO_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
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
            ActivityCompat.requestPermissions(requireActivity(), PERMISSION_LIST.toArray(new String[PERMISSION_LIST.size()]), PERMISSION_ALL);
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
                                Toast.makeText(mContext, "Please allow Microphone permission", Toast.LENGTH_LONG).show();
                            }
                        } else if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                storage = true;
                            } else {
                                Toast.makeText(mContext, "Please allow Storage permission", Toast.LENGTH_LONG).show();
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
    public void onDestroy() {
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
                mContext.startForegroundService(serviceIntent);
            } else {
                mContext.startService(serviceIntent);
            }

            showTimer();
        }
    };

    View.OnClickListener stopBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {


            mContext.stopService(serviceIntent);
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
            NavHostFragment.findNavController(RecorderFragment.this)
                        .navigate(R.id.action_RecorderFragment_to_RecordingsFragment);
//            startActivity(new Intent(mContext, RecordingsActivity.class));
        }
    };

    View.OnClickListener settingsBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Toast.makeText(mContext, "Settings Clicked", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(RecorderFragment.this)
                    .navigate(R.id.action_RecorderFragment_to_SettingsFragment);
        }



    };
}