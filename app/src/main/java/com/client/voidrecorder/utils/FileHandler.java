package com.client.voidrecorder.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileHandler {

    private static final String TAG = "FileHandler";
    Context context;


    public FileHandler(Context context){
        this.context = context;
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




}
