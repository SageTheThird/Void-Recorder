package com.client.voidrecorder.recordings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.client.voidrecorder.R;
import com.client.voidrecorder.db.DatabaseTransactions;
import com.client.voidrecorder.db.RecordingDB;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.FileHandler;
import com.client.voidrecorder.utils.Paths;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class RecordingsFragment extends Fragment {

    private static final String TAG = "RecordingsActivity";
    public static int MAX_ALLOWED_STORAGE = 30 * 1000000;//30MB - default


    ArrayList<ModelRecordings> recordingsList;
    RecyclerView recyclerView;
    MediaPlayer mediaPlayer;
    double current_pos, total_duration;
    TextView current, total, toolbarTextView;
    ImageView prevBtn, nextBtn, pauseBtn;
    SeekBar seekBar;
    int audio_index = 0;
    public static final int PERMISSION_READ = 0;
    private RecordingsAdapter adapter;
    Context mContext;

    //db vars
    private DatabaseTransactions databaseTransactions;
    private HashSet<String> savedRecordingsSet;//recordings which are permanently saved

    //space vars
    private long totalSizeOfFolderInBytes = 0;

    //settings vars
    private SharedPreferences sharedPreferences;

    //file operations
    private FileHandler fileHandler;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recordings, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mContext = getActivity();
        fileHandler = new FileHandler();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        MAX_ALLOWED_STORAGE = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(mContext.getString(R.string.max_space_pref), ""))) * 1000000;


        if (checkPermission()) {

            databaseTransactions = new DatabaseTransactions(mContext);
            getSavedRecordingsFromDB();

            bindViews(view);

            setupRecordings();
        }




    }


    /*Fetches all saved recordings*/
    private void getSavedRecordingsFromDB() {
        try {
            savedRecordingsSet = databaseTransactions.getAllRecordingsDb();
            Log.d(TAG, "onCreate: Saved Recordings : "+ savedRecordingsSet.size());
            for (String title : savedRecordingsSet) {
                System.out.println("Saved Recordings Title : "+title);
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void bindViews(View parentView){
        current = parentView.findViewById(R.id.current);
        total = parentView.findViewById(R.id.total);
        prevBtn = parentView.findViewById(R.id.prev);
        nextBtn = parentView.findViewById(R.id.next);
        pauseBtn = parentView.findViewById(R.id.pause);
        seekBar = parentView.findViewById(R.id.seekbar);
        toolbarTextView =  parentView.findViewById(R.id.toolbarTextView);
        recyclerView = parentView.findViewById(R.id.recycler_view);
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
            mediaPlayer.setDataSource(mContext, recordingsList.get(pos).getUri());
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

        total.setText(Conversions.timeConversion((long) total_duration));
        current.setText(Conversions.timeConversion((long) current_pos));
        seekBar.setMax((int) total_duration);

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    current_pos = mediaPlayer.getCurrentPosition();
                    current.setText(Conversions.timeConversion((long) current_pos));
                    seekBar.setProgress((int) current_pos);
                    handler.postDelayed(this, 1000);
                } catch (IllegalStateException ed) {
                    ed.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }




    /*Fetch Recorded clips from the output directory*/
    private void getRecordedClips(){

        File[] files = fileHandler.getFilesFromOutputFolder();


        assert files != null;
        Log.d("Files", "Size: "+ files.length);

        for (File file : files) {

            Uri uri = Uri.fromFile(file);

            Date date = new Date(file.lastModified());
            SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");

            int fileSizeInBytes = Integer.parseInt(String.valueOf(file.length()));
            totalSizeOfFolderInBytes += fileSizeInBytes;

//            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//            mmr.setDataSource(mContext,uri);
//            long millSecond = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            long millSecond = 12351;

            recordingsList.add(new ModelRecordings(file.getName(), Conversions.timeConversion(millSecond), format.format(date), fileSizeInBytes, isRecordingSaved(file.getName()), uri));

        }

        spaceLimitCheck();

    }

    /*Updates text on toolbar textView which shows size and no of recordings*/
    private void updateSizeTextView(long size){
        String sizeTemp = "Size : " +Conversions.humanReadableByteCountSI(size) + " (" + recordingsList.size() + ")";
        toolbarTextView.setText(sizeTemp);
    }

    /*Checks if the total space occupied by recordings has exceeded the max space allowed and deletes oldest files on confirm*/
    private void spaceLimitCheck() {

        updateSizeTextView(totalSizeOfFolderInBytes);

        //check if the total folder size exceeds the max allowed
        if(totalSizeOfFolderInBytes !=0 && totalSizeOfFolderInBytes >= MAX_ALLOWED_STORAGE){

            //show dialog asking to confirm delete older files | that is if the dialog is enabled from settings
            if(sharedPreferences.getBoolean(mContext.getString(R.string.space_limit_dialog_pref), false)){
                showFreeUpSpaceDialog();
            }


        }
    }

    private void deleteOldestFiles(){

        int noOfItemRemoved = 0;


        //delete the last modified file in recording list (if not saved as well) until we are below max_allowed
        if(recordingsList != null && recordingsList.size() > 0){

            for(int i=recordingsList.size()-1; i>=0 ; i--){

                //check if the clip is saved
                if(!isRecordingSaved(recordingsList.get(i).getTitle())){

                    totalSizeOfFolderInBytes = totalSizeOfFolderInBytes - recordingsList.get(i).getSize();

                    //delete statement goes here
                    Log.d(TAG, "getRecordedClips: " +recordingsList.get(i).getTitle() +  " Deleted Due to MAx_LIMIT SUCCEED " + i + " : Size Now : "+Conversions.humanReadableByteCountSI(totalSizeOfFolderInBytes));
                    delete(recordingsList.get(i).getTitle(), new File(Objects.requireNonNull(recordingsList.get(i).getUri().getPath())));
                    recordingsList.remove(i);


                    //check if after deletion it is less than max_allowed
                    if(totalSizeOfFolderInBytes >= MAX_ALLOWED_STORAGE){
                        //continue to next entry
                        noOfItemRemoved++;
                    }else{
                        //we have reduced the folder size to allowed size.

                        //for the last entry deletion
                        noOfItemRemoved++;

                        showSpaceFreedDialog(noOfItemRemoved);

                        updateSizeTextView(totalSizeOfFolderInBytes);
                        break;
                    }
                }



            }

            if(adapter != null){
                adapter.notifyDataSetChanged();
            }


        }
    }

    private boolean isRecordingSaved(String title) {
        if(savedRecordingsSet== null) return false;
        return savedRecordingsSet.size() > 0 && savedRecordingsSet.contains(title);
    }

    private void setupRecyclerView() {


        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new RecordingsAdapter(mContext, recordingsList);
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

                //Prompt the user for a file rename, while the current filename is already entered into EditField
                final ModelRecordings currentClip = recordingsList.get(pos);

                final View view = LayoutInflater.from(mContext).inflate(R.layout.save_dialog_layout, null);

                AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
                alertDialog.setTitle("Confirmation Dialog");
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Enter a new name for the audio file.");


                final EditText renameEditText = view.findViewById(R.id.etComments);
                renameEditText.setText(currentClip.getTitle());

                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        String newFileName = renameEditText.getText().toString();

                        //here we rename the audio file and save the entry in db so that it doesn't get deleted automatically
                        ModelRecordings curr_recording = recordingsList.get(pos);

                        Log.d(TAG, "onSwiped: File Uri : " + curr_recording.uri);

                        File from = new File(Paths.getOutputFolderPath() + curr_recording.getTitle());
                        File to = new File(Paths.getOutputFolderPath() +  newFileName );

                        Log.d(TAG, "onClick: Rename From : "+Paths.getOutputFolder() + "/" + curr_recording.getTitle() );
                        Log.d(TAG, "onClick: Rename To : "+Paths.getOutputFolder() + "/" + newFileName  );



                        if (fileHandler.rename(from, to)) {
                            //Rename Success
                            Log.i(TAG, "Rename File : Success");
                            recordingsList.get(pos).setTitle(newFileName);
                            adapter.notifyDataSetChanged();

                            //here we save to db
                            if(!savedRecordingsSet.contains(curr_recording.getTitle())){

                                savedRecordingsSet.add(curr_recording.getTitle());
                                databaseTransactions.saveRecordingToDb(curr_recording.getTitle(), curr_recording.uri.toString());
                                recordingsList.get(pos).setSaved(true);
                                adapter.notifyDataSetChanged();

                            }

                        } else {
                            //Fail
                            Log.i(TAG, "Rename File : Fail");
                        }





                        Log.d(TAG, "onSaveClick: Text Entered : "+newFileName);


                    }
                });


                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

                alertDialog.setView(view);
                alertDialog.show();



            }

            @Override
            public void onShareClick(int pos, View v) {
                //Take the audio file and share it across multiple apps using intent
                Toast.makeText(mContext, "Shared", Toast.LENGTH_LONG).show();
                shareFile(recordingsList.get(pos).getUri().getPath());

            }
        });
    }

    private void shareFile(String filePath){

        Uri uri = Uri.parse(filePath);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/3gpp");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share, "Share Sound File"));
    }

    public boolean checkPermission() {
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
        if ((READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ);
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
                        Toast.makeText(mContext, "Please allow storage permission", Toast.LENGTH_LONG).show();
                    } else {
                        getRecordedClips();
                    }
                }
            }
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }



    ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            Toast.makeText(mContext, "on Move", Toast.LENGTH_SHORT).show();
            return false;
        }

        @Override
        public void onSwiped(final RecyclerView.ViewHolder viewHolder, int swipeDir) {
            Toast.makeText(mContext, "on Swiped ", Toast.LENGTH_SHORT).show();

            Log.d(TAG, "onSwiped: Swiped Title |: "+recordingsList.get(viewHolder.getAdapterPosition()).getTitle());


            showDeleteFileConfirmationDialog(viewHolder.getAdapterPosition());



        }
    };




    /*Deletes the file from internal storage and db*/
    private void delete(String title,  File fileToDelete){
        Log.d(TAG, "onSwiped: Key Lookup : "+title);

        if(savedRecordingsSet.size() > 0 && savedRecordingsSet.contains(title)){
            databaseTransactions.deleteRecordingFromDB(title);
        }

        fileHandler.deleteFile(fileToDelete);
    }




    /*-------------------------------POP-UP DIALOGS--------------------------*/

    private void showDeleteFileConfirmationDialog(int position) {

        //Remove swiped item from list and notify the RecyclerView
        new AlertDialog.Builder(mContext)
                .setTitle("Delete entry")
                .setMessage("Are you sure you want to delete this entry?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        Uri fileUri = recordingsList.get(position).uri;
                        Log.d(TAG, "onSwiped: File Uri : " + fileUri);
                        File fileToDelete = new File(Objects.requireNonNull(fileUri.getPath()));
                        //deletes file from internal storage and db as well
                        delete(recordingsList.get(position).getTitle(), fileToDelete);

                        recordingsList.remove(position);
                        adapter.notifyItemRemoved(position);

                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        adapter.notifyDataSetChanged();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();


    }

    private void showFreeUpSpaceDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle("Max Limit Reached")
                .setMessage("Do you want to free up space by deleting the oldest non-saved recordings?\n" +
                        "\nNote: You can disable this alert by turning off automatic deletion or increasing max space allowed from settings." +
                        "" +
                        "")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // User agreed to delete
                        deleteOldestFiles();

                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showSpaceFreedDialog(int noOfItemRemoved){
        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle("Space Freed");
        alertDialog.setMessage(noOfItemRemoved+ " Oldest Non-Saved Recordings Were Deleted To Free Up Space!");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    /*-------------------------------POP-UP DIALOGS--------------------------*/

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

    /*-------------------------------LISTENERS------------------------------*/



}