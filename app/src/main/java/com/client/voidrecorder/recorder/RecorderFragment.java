package com.client.voidrecorder.recorder;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import com.client.voidrecorder.R;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.ServiceUtil;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;



/*
* Host Fragment for Recording Service
* */
public class RecorderFragment extends Fragment {

    private static final String TAG = "RecorderFragment";

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

            setAudioRecorder();

            if (ServiceUtil.isServiceRunningInForeground(mContext, RecorderService.class)) {
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
        boolean record = false,storage =  false;
        if (requestCode == PERMISSION_ALL) {
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

    /*Attaches Timer Broadcast Receiver in RecorderService.class to this fragment*/
    private void registerCountDownReceiver() {
        countdownTimerReceiver = new CountdownTimerReceiver();
        requireActivity().registerReceiver(countdownTimerReceiver, new IntentFilter("CountDownIntent"));
    }

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


    /* When the recording is in progress*/
    private void showRecordingUI() {
        startBtn.setEnabled(false);
        recordingsBtn.setEnabled(true);
        startBtn.setVisibility(View.INVISIBLE);
        recordTV.setVisibility(View.INVISIBLE);
        stopBtn.setEnabled(true);
        stopBtn.setBackgroundResource(R.drawable.round_shape);
        stopBtn.setImageResource(R.drawable.ic_stop_black_35dp);
    }

    private void showRecordingStopUI(){
        startBtn.setEnabled(true);
        recordingsBtn.setEnabled(true);
        startBtn.setVisibility(View.VISIBLE);
        recordTV.setVisibility(View.VISIBLE);
        stopBtn.setEnabled(false);
        stopBtn.setBackgroundResource(R.drawable.normal_background);
        stopBtn.setImageResource(R.drawable.noraml_stop);
        recordingsBtn.setBackgroundResource(R.drawable.round_shape);
        recordingsBtn.setImageResource(R.drawable.ic_menu_black_35dp);
        timerTextView.setText(mContext.getString(R.string.dummy_timer));

        if(serviceStatusTv.getVisibility() == View.VISIBLE){
            serviceStatusTv.setVisibility(View.INVISIBLE);
        }
    }

    //runtime permissions check
    public void requestPermissions() {
        int RECORD_AUDIO_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.RECORD_AUDIO);
        int WRITE_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        ArrayList<String> PERMISSION_LIST = new ArrayList<>();
        if ((RECORD_AUDIO_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.RECORD_AUDIO);
        }
        if ((WRITE_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            PERMISSION_LIST.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!PERMISSION_LIST.isEmpty()) {
            requestPermissions(PERMISSION_LIST.toArray(new String[0]), PERMISSION_ALL);
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

            mContext.stopService(serviceIntent);
            showRecordingStopUI();

        }
    };

    View.OnClickListener recordingsBtnClickListener = v -> NavHostFragment.findNavController(RecorderFragment.this)
                .navigate(R.id.action_RecorderFragment_to_RecordingsFragment);

    View.OnClickListener settingsBtnClickListener = v -> NavHostFragment.findNavController(RecorderFragment.this)
            .navigate(R.id.action_RecorderFragment_to_SettingsFragment);

    View.OnClickListener infoBtnClickListener = v -> showSelectedOptionsDialog();

    /*Displays selected settings on info btn click*/
    private void showSelectedOptionsDialog()  {

        final View view = LayoutInflater.from(mContext).inflate(R.layout.selected_settings_dialog_layout, null);
        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle("Selected Settings");
        alertDialog.setCancelable(false);
        final TextView infoTv = view.findViewById(R.id.etComments);
        String settingsSelected = "Format : "+sharedPreferences.getString(getString(R.string.output_format_pref), "") + "\nQuality : "+sharedPreferences.getString(getString(R.string.output_quality_pref), "") + "\nDuration : "+sharedPreferences.getString(getString(R.string.max_duration), "") + " Minutes\nSpace Limit : "+sharedPreferences.getString(getString(R.string.max_space_pref), "")+ " MB" ;
        infoTv.setText(settingsSelected);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Close", (dialogInterface, i) -> dialogInterface.dismiss());
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
            }
        }

    }
}