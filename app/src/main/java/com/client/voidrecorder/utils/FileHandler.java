package com.client.voidrecorder.utils;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.client.voidrecorder.App;
import com.client.voidrecorder.db.DatabaseTransactions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class FileHandler {

    private static final String TAG = "FileHandler";

    public long totalSizeOfFolderInBytes = 0;




    Context context;


    public FileHandler(){
        context = App.getAppContext();

    }

    public boolean rename(File from, File to) {

        return from.exists()  && from.renameTo(to);
    }

    public void deleteFile(File file) {



        if(file.exists()){
            boolean isDeleted = file.delete();
            if(isDeleted){
                Log.d(TAG, "deleteFile: File Deleted ");
            }else {
                Log.d(TAG, "deleteFile: Deletion Failed ");
            }

        }

    }

    private void sortFilesByDate(File[] files){
        Arrays.sort(files, new Comparator() {
            public int compare(Object o1, Object o2) {

                if (((File)o1).lastModified() > ((File)o2).lastModified()) {
                    return -1;
                } else if (((File)o1).lastModified() < ((File)o2).lastModified()) {
                    return +1;
                } else {
                    return 0;
                }
            }

        });
    }

    public File[] getFilesFromOutputFolder(){

        File outputFolder = new File(Paths.getOutputFolder());
        File[] files = outputFolder.listFiles();
        sortFilesByDate(files);

        return files;
    }

//    public List<ModelRecordings> getRecordingsListFromFiles(File[] files){
//
//        List<ModelRecordings> recordings = new ArrayList<>();
//
//        for (File file : files) {
//
//            Log.d(TAG, "getRecordingsListFromFiles: File Size : "+file.length());
//            recordings.add(getRecordingWithMetaDataFrom(file));
//
//        }
//
//        return recordings;
//    }
//
//    private ModelRecordings getRecordingWithMetaDataFrom(File file) {
//
//        Uri uri = Uri.fromFile(file);
//
//        Date date = new Date(file.lastModified());
//
//        SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");
//
//        int fileSizeInBytes = Integer.parseInt(String.valueOf(file.length()));
//        totalSizeOfFolderInBytes += fileSizeInBytes;
//
//        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//        mmr.setDataSource(context,uri);
//        String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
//        Log.d(TAG, "getRecordingWithMetaDataFrom: duration : "+durationStr);
//        long millSecond = Long.parseLong(durationStr);
//
//
//
//        return new ModelRecordings(file.getName(), Conversions.timeConversion(millSecond), format.format(date), fileSizeInBytes, isRecordingSaved(file.getName()), Uri.fromFile(file));
//
//    }








}
