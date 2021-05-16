package com.client.voidrecorder;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import java.util.Objects;


/*
* Activity that hosts RecorderFragment, RecordingsFragment, SettingsFragment
* */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RecorderActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        for (Fragment fragment : Objects.requireNonNull(getSupportFragmentManager().getPrimaryNavigationFragment()).getChildFragmentManager().getFragments())
        {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}