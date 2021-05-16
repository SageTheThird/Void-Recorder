package com.client.voidrecorder.db;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.client.voidrecorder.models.RecordingDB;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;

/*
 * Responsible for interactions of UI with Database. Contains Insert, Delete, & GetAll methods.
 * */

public class DatabaseTransactions {
    private static final String TAG = "DatabaseTransactions";

    public static final String DATABASE_NAME = "RecordingsDB";
    private RecordingsAppDatabase recordingsAppDatabase;
    private RecordingsDAO recordingsDAO;

    public DatabaseTransactions(Context context) {

        recordingsAppDatabase = Room.databaseBuilder(context, RecordingsAppDatabase.class, DATABASE_NAME).addCallback(startupCallBack).build();
        recordingsDAO = recordingsAppDatabase.getRecordingsDAO();


    }

    public void closeDB() {
        if (recordingsAppDatabase != null) {
            recordingsAppDatabase.close();
        }

    }


    public void deleteRecordingFromDB(String title) {
        new DeleteRecordingFromDBAsyncTask(recordingsDAO).execute(title);
    }

    public void saveRecordingToDb(String title, String uri) {
        new SaveRecordingToDBAsyncTask(recordingsDAO).execute(new RecordingDB(0, title, uri));

    }

    public HashSet<String> getAllRecordingsDb() throws ExecutionException, InterruptedException {

        return new GetAllRecordingsDBAsyncTask(recordingsDAO).execute().get();

    }

    /*Adds saved recording to db, to prevent it from deleting automatically*/
    private static class SaveRecordingToDBAsyncTask extends AsyncTask<RecordingDB, Void, Long> {

        WeakReference<RecordingsDAO> recordingsDAO;

        public SaveRecordingToDBAsyncTask(RecordingsDAO recordingsDAO) {

            this.recordingsDAO = new WeakReference<>(recordingsDAO);

        }

        @Override
        protected Long doInBackground(RecordingDB... recording) {
            return recordingsDAO.get().addRecordingToDB(recording[0]);
        }


        @Override
        protected void onPostExecute(Long aVoid) {
            super.onPostExecute(aVoid);

//            contactsAdapter.notifyDataSetChanged();
        }
    }

    /*Deletes saved recording From db*/
    private static class DeleteRecordingFromDBAsyncTask extends AsyncTask<String, Void, Void> {


        WeakReference<RecordingsDAO> recordingsDAO;

        public DeleteRecordingFromDBAsyncTask(RecordingsDAO recordingsDAO) {

            this.recordingsDAO = new WeakReference<>(recordingsDAO);

        }


        @Override
        protected Void doInBackground(String... recordingDBS) {

            recordingsDAO.get().deleteRecordingDbWithTitle(recordingDBS[0]);

            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

//            contactsAdapter.notifyDataSetChanged();
        }
    }


    /*Gets All Saved Recordings From DB and saves them in a hashmap and return hashmap*/
    private static class GetAllRecordingsDBAsyncTask extends AsyncTask<Void, Void, HashSet<String>> {

        WeakReference<RecordingsDAO> recordingsDAO;

        public GetAllRecordingsDBAsyncTask(RecordingsDAO recordingsDAO) {

            this.recordingsDAO = new WeakReference<>(recordingsDAO);

        }

        @Override
        protected HashSet<String> doInBackground(Void... voids) {

            //map containing title as key and whole object as value, so later we can get constant lookup
            HashSet<String> savedSet = new HashSet<>();
            List<RecordingDB> recList = recordingsDAO.get().getAllSavedRecordings();

            for (int i = 0; i < recList.size(); i++) {

                savedSet.add(recList.get(i).getTitle());

            }
            return savedSet;
        }


        @Override
        protected void onPostExecute(HashSet<String> aVoid) {
            super.onPostExecute(aVoid);

        }
    }

    RoomDatabase.Callback startupCallBack = new RoomDatabase.Callback() {
        @Override
        public void onCreate(@NonNull SupportSQLiteDatabase db) {
            super.onCreate(db);
        }


        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
        }

    };
}
