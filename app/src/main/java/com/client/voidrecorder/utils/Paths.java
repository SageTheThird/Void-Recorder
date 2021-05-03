package com.client.voidrecorder.utils;

import android.os.Environment;

import java.io.File;

public class Paths {


    public static final String OUTPUT_FOLDER_NAME = "/voidclips/";

    public static String getOutputFolder(){
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), Paths.OUTPUT_FOLDER_NAME);

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder.getAbsolutePath();
    }


    public static String getOutputFolderPath(){
        return Environment.getExternalStorageDirectory().getAbsolutePath() + Paths.OUTPUT_FOLDER_NAME;
    }
}
