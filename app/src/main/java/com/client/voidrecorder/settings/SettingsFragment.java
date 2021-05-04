package com.client.voidrecorder.settings;

import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

import com.client.voidrecorder.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.settings);

        //output setting
        //sample rate setting
        //automatic deletion after max_space_allowed exceeds
        //


    }
}
