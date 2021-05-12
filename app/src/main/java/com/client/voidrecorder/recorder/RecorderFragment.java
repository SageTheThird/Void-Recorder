package com.client.voidrecorder.recorder;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.client.voidrecorder.R;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.SharedPreferencesHelper;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;

public class RecorderFragment extends Fragment {

    private static final String TAG = "RecorderActivity";

    private TextView timerTextView,serviceStatusTv,recordTV;
    private ImageView startBtn, stopBtn, recordingsBtn, settingsBtn,infoBtn;
    public static final int PERMISSION_ALL = 0;
    private Intent serviceIntent;
    private Context mContext;
    private SharedPreferences sharedPreferences;

    private CountdownTimerReceiver countdownTimerReceiver;


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

        //receives count down from the service while recording
        registerCountDownReceiver();

        //binds views to fragment
        bindViews(view);

        //request necessary permissions for storage, recording, reading
        requestPermissions();

        if (checkPermissions()) {

            Log.d(TAG, "onViewCreated: Permissions Granted");
            setAudioRecorder();

            if (isServiceRunningInForeground(mContext, RecorderService.class)) {
                //when app is closed completely and then opened again, showRecording, ResumeTimer
                showRecordingUI();
                serviceStatusTv.setVisibility(View.VISIBLE);
                serviceStatusTv.setText(getString(R.string.service_recording));



            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult: called");
        boolean record = false,storage =  false;
        switch (requestCode) {
            case  PERMISSION_ALL: {
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

    private void registerCountDownReceiver() {
        countdownTimerReceiver = new CountdownTimerReceiver();
        requireActivity().registerReceiver(countdownTimerReceiver, new IntentFilter("CountDownIntent"));
    }


    /* */
    private void bindViews(View parentView) {
        timerTextView =  parentView.findViewById(R.id.text);
        startBtn = parentView.findViewById(R.id.start);
        stopBtn =  parentView.findViewById(R.id.stop);
        recordingsBtn = parentView.findViewById(R.id.recordings);
        settingsBtn =  parentView.findViewById(R.id.settingsBtn);
        serviceStatusTv =  parentView.findViewById(R.id.serviceStatusTV);
        recordTV =  parentView.findViewById(R.id.recordTV);
        infoBtn =  parentView.findViewById(R.id.infoBtn);
        serviceStatusTv.setVisibility(View.INVISIBLE);
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
        infoBtn.setOnClickListener(infoBtnClickListener);

    }


    /* */
    private void showRecordingUI() {
        startBtn.setEnabled(false);
        recordingsBtn.setEnabled(false);
        startBtn.setVisibility(View.INVISIBLE);
        recordTV.setVisibility(View.INVISIBLE);
        stopBtn.setEnabled(true);
        stopBtn.setBackgroundResource(R.drawable.round_shape);
        stopBtn.setImageResource(R.drawable.ic_stop_black_35dp);
        recordingsBtn.setBackgroundResource(R.drawable.normal_background);
        recordingsBtn.setImageResource(R.drawable.normal_menu);
    }

    //runtime permissions check
    public void requestPermissions() {
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
        if (!PERMISSION_LIST.isEmpty()) {
            requestPermissions(PERMISSION_LIST.toArray(new String[PERMISSION_LIST.size()]), PERMISSION_ALL);
        }
    }


    private boolean checkPermissions(){
        //permissions granted
        return ActivityCompat.checkSelfPermission(mContext,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mContext,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(countdownTimerReceiver != null){
            requireActivity().unregisterReceiver(countdownTimerReceiver);
        }
        Log.d(TAG, "onDestroyView: Receiver UnRegistered ...");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Called");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Called");
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
        }
    };


    View.OnClickListener stopBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            //clear timeResume pref
            if(SharedPreferencesHelper.get(mContext, getString(R.string.timer_resume), "") != null){
                SharedPreferencesHelper.remove(mContext, getString(R.string.timer_resume));
            }


            mContext.stopService(serviceIntent);


            //cancel count down timer
            startBtn.setEnabled(true);
            recordingsBtn.setEnabled(true);
            startBtn.setVisibility(View.VISIBLE);
            recordTV.setVisibility(View.VISIBLE);
            stopBtn.setEnabled(false);
            stopBtn.setBackgroundResource(R.drawable.normal_background);
            stopBtn.setImageResource(R.drawable.noraml_stop);
            recordingsBtn.setBackgroundResource(R.drawable.round_shape);
            recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
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

            NavHostFragment.findNavController(RecorderFragment.this)
                    .navigate(R.id.action_RecorderFragment_to_SettingsFragment);
        }



    };

    View.OnClickListener infoBtnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            showSelectedOptionsDialog();
        }



    };

    private void showSelectedOptionsDialog()  {

        final View view = LayoutInflater.from(mContext).inflate(R.layout.selected_settings_dialog_layout, null);

        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle("Selected Settings");
        alertDialog.setCancelable(false);


        final TextView renameEditText = view.findViewById(R.id.etComments);
        String settingsSelected = "Format : "+sharedPreferences.getString(getString(R.string.output_format_pref), "") + "\nQuality : "+sharedPreferences.getString(getString(R.string.output_quality_pref), "") + "\nDuration : "+sharedPreferences.getString(getString(R.string.max_duration), "") + " Minutes\nSpace Limit : "+sharedPreferences.getString(getString(R.string.max_space_pref), "")+ " MB" ;

        renameEditText.setText(settingsSelected);

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close", new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                dialogInterface.dismiss();

            }
        });



        alertDialog.setView(view);
        alertDialog.show();
    }



    /*Broadcast Receiver for getting count down from service*/
    class CountdownTimerReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getExtras() != null){
                long millisUntilFinished = intent.getLongExtra("timer",30000);
                timerTextView.setText(Conversions.getFormattedCountDownFromMillis(millisUntilFinished));
                Log.d(TAG, "getCountDownFromService: "+millisUntilFinished);
            }
        }

    }
}