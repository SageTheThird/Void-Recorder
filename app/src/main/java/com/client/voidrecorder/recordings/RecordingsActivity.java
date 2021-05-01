package com.client.voidrecorder.recordings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.client.voidrecorder.R;
import com.client.voidrecorder.utils.Conversions;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RecordingsActivity extends AppCompatActivity {

    private static final String TAG = "RecordingsActivity";


    ArrayList<ModelRecordings> recordingsList;
    RecyclerView recyclerView;
    MediaPlayer mediaPlayer;
    double current_pos, total_duration;
    TextView current, total;
    ImageView prevBtn, nextBtn, pauseBtn;
    SeekBar seekBar;
    int audio_index = 0;
    public static final int PERMISSION_READ = 0;
    private RecordingsAdapter adapter;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);
        mContext = RecordingsActivity.this;



        if (checkPermission()) {

            bindViews();

            setupRecordings();
        }






    }




    private void bindViews(){
        current = (TextView) findViewById(R.id.current);
        total = (TextView) findViewById(R.id.total);
        prevBtn = (ImageView) findViewById(R.id.prev);
        nextBtn = (ImageView) findViewById(R.id.next);
        pauseBtn = (ImageView) findViewById(R.id.pause);
        seekBar = (SeekBar) findViewById(R.id.seekbar);
    }

    public void setupRecordings() {

        recordingsList = new ArrayList<>();
        mediaPlayer = new MediaPlayer();

        getRecordedClips();
        setupRecyclerView();

        //
        if (!recordingsList.isEmpty()) {

            seekBar.setOnSeekBarChangeListener(clipSeekBarListener);
            mediaPlayer.setOnCompletionListener(trackCompletionListener);
            prevBtn.setOnClickListener(prevClickListener);
            nextBtn.setOnClickListener(nextClickListener);
            pauseBtn.setOnClickListener(pauseClickListener);

        }
    }

    public void playRecording(int pos) {
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, recordingsList.get(pos).getUri());
            mediaPlayer.prepare();
            mediaPlayer.start();
            pauseBtn.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);
            audio_index = pos;
        } catch (Exception e) {
            e.printStackTrace();
        }
        setAudioProgress();
    }


    /*Updates current, total textviews and seekbar*/
    public void setAudioProgress() {
        current_pos = mediaPlayer.getCurrentPosition();
        total_duration = mediaPlayer.getDuration();

        total.setText(timeConversion((long) total_duration));
        current.setText(timeConversion((long) current_pos));
        seekBar.setMax((int) total_duration);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    current_pos = mediaPlayer.getCurrentPosition();
                    current.setText(timeConversion((long) current_pos));
                    seekBar.setProgress((int) current_pos);
                    handler.postDelayed(this, 1000);
                } catch (IllegalStateException ed) {
                    ed.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }



    //converts time to human readable form
    public String timeConversion(long value) {
        String audioTime;
        int dur = (int) value;
        int hrs = (dur / 3600000);
        int mns = (dur / 60000) % 60000;
        int scs = dur % 60000 / 1000;

        if (hrs > 0) {
            audioTime = String.format("%02d:%02d:%02d", hrs, mns, scs);
        } else {
            audioTime = String.format("%02d:%02d", mns, scs);
        }
        return audioTime;
    }


    /*Access recorded clips and their meta data from MediaStore*/
    public void getRecordedClips() {

        ContentResolver contentResolver = getContentResolver();
        //creating content resolver and fetch audio files
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri, null, MediaStore.Audio.Media.DATA + " like ?", new String[]{"%voidclips%"}, MediaStore.Audio.Media.DATE_ADDED + " DESC");

        if (cursor != null && cursor.moveToFirst()) {
            do {


                String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));

                //parses the size from bytes to kilobytes
                String size = Conversions.humanReadableByteCountSI(Long.parseLong(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))));
                ModelRecordings modelRecordings = new ModelRecordings();
                modelRecordings.setTitle(title);
                File file = new File(data);
                Date date = new Date(file.lastModified());
                SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");

                modelRecordings.setDate(format.format(date));
                modelRecordings.setUri(Uri.parse(data));
                modelRecordings.setSize(size);

                //fetch the audio duration using MediaMetadataRetriever class
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(data);

                modelRecordings.setDuration(timeConversion(Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION))));
                recordingsList.add(modelRecordings);

            } while (cursor.moveToNext());
        }


    }



    private void setupRecyclerView() {

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new RecordingsAdapter(this, recordingsList);
        recyclerView.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);

        adapter.setOnItemClickListener(new RecordingsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int pos, View v) {
                playRecording(pos);
            }

            @Override
            public void onSaveClick(final int pos, View v) {


            }

            @Override
            public void onShareClick(int pos, View v) {
                //Take the audio file and share it across multiple apps using intent
                Toast.makeText(RecordingsActivity.this, "Shared", Toast.LENGTH_LONG).show();


            }
        });
    }

    public boolean checkPermission() {
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if ((READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ);
            return false;
        }
        return true;
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_READ: {
                if (grantResults.length > 0 && permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResults.length > 0 && permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        Toast.makeText(getApplicationContext(), "Please allow storage permission", Toast.LENGTH_LONG).show();
                    } else {
                        getRecordedClips();
                    }
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }


    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            Toast.makeText(RecordingsActivity.this, "on Move", Toast.LENGTH_SHORT).show();
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
            Toast.makeText(RecordingsActivity.this, "on Swiped ", Toast.LENGTH_SHORT).show();


            //Remove swiped item from list and notify the RecyclerView
            new AlertDialog.Builder(RecordingsActivity.this)
                    .setTitle("Delete entry")
                    .setMessage("Are you sure you want to delete this entry?")

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Continue with delete operation
                            int position = viewHolder.getAdapterPosition();
                            Uri fileUri = recordingsList.get(position).uri;
                            Log.d(TAG, "onSwiped: File Uri : " + fileUri);
                            File fileToDelete = new File(fileUri.getPath());
                            deleteFile(fileToDelete);
                            recordingsList.remove(position);
                            adapter.notifyItemRemoved(position);
                        }
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();




        }
    };

    private void deleteFile(File file) {
        // Set up the projection (we only need the ID)
//        String[] projection = { MediaStore.Audio.Media._ID };

        // Match on the file path
        String selection = MediaStore.Audio.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{file.getAbsolutePath()};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(queryUri, null, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
            Log.d(TAG, "deleteFile: File Deleted");

        } else {
            // File not found in media store DB
            Log.d(TAG, "deleteFile: File Not Found");
        }
        c.close();
    }



    /*-------------------------------LISTENERS------------------------------*/

    MediaPlayer.OnCompletionListener trackCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            audio_index++;
            if (audio_index < (recordingsList.size())) {
                playRecording(audio_index);
            } else {
                audio_index = 0;
                playRecording(audio_index);
            }

        }
    };

    SeekBar.OnSeekBarChangeListener clipSeekBarListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            current_pos = seekBar.getProgress();
            mediaPlayer.seekTo((int) current_pos);
        }
    };

    View.OnClickListener pauseClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                pauseBtn.setImageResource(R.drawable.ic_play_circle_filled_black_24dp);
            } else {
                mediaPlayer.start();
                pauseBtn.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);
            }
        }
    };

    View.OnClickListener prevClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (audio_index > 0) {
                audio_index--;
            } else {
                audio_index = recordingsList.size() - 1;
            }
            playRecording(audio_index);
        }
    };

    View.OnClickListener nextClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (audio_index < (recordingsList.size() - 1)) {
                audio_index++;
            } else {
                audio_index = 0;
            }
            playRecording(audio_index);
        }
    };

}