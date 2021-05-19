package com.client.voidrecorder.recordings;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.TransitionManager;
import com.client.voidrecorder.R;
import com.client.voidrecorder.db.DatabaseTransactions;
import com.client.voidrecorder.models.Recording;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.FileHandler;
import com.client.voidrecorder.utils.Paths;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/*
* Displays recorded clips
* Hosts a player for recordings
* Provide extra features : save, delete, share, for recordings
* */
public class RecordingsFragment extends Fragment {

    private static final String TAG = "RecordingsFragment";
    public static int MAX_ALLOWED_STORAGE = 30 * 1000000;//30MB - default
    public static String OUTPUT_QUALITY = "m4a";//30MB - default


    ArrayList<Recording> recordingsList;
    RecyclerView recyclerView;
    MediaPlayer mediaPlayer;
    double current_pos, total_duration;
    TextView current, total, folderInfoTv, titlePlayerTv;
    ImageView prevBtn, nextBtn, pauseBtn, backBtn, closePlayerBtn;
    RelativeLayout root;
    SeekBar seekBar;
    int audio_index = 0;
    public static final int PERMISSION_READ = 0;
    private RecordingsAdapter adapter;
    Context mContext;
    private ConstraintLayout playerRoot;

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
        fileHandler = new FileHandler(mContext);

        //fetch settings
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        MAX_ALLOWED_STORAGE = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString(mContext.getString(R.string.max_space_pref), ""))) * 1000000;
        OUTPUT_QUALITY = Objects.requireNonNull(sharedPreferences.getString(mContext.getString(R.string.output_quality_pref), ""));


        if (checkPermissions()) {

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
        } catch (ExecutionException | InterruptedException e) {
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
        folderInfoTv =  parentView.findViewById(R.id.folderToolTV);
        recyclerView = parentView.findViewById(R.id.recycler_view);
        backBtn = parentView.findViewById(R.id.backBtn);
        playerRoot = parentView.findViewById(R.id.playerRoot);
        titlePlayerTv = parentView.findViewById(R.id.titlePlayerTv);
        root = parentView.findViewById(R.id.root);
        closePlayerBtn = parentView.findViewById(R.id.closePlayerBtn);
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
            backBtn.setOnClickListener(backClickListener);
            closePlayerBtn.setOnClickListener(closePlayerClickListener);
            folderInfoTv.setOnClickListener(folderInfoClickListener);

        }
    }

    public void playRecording(int pos) {

        highlightSelectedRecording(pos);

        try {
            Recording currentRecording = recordingsList.get(pos);
            mediaPlayer.reset();
            mediaPlayer.setDataSource(mContext, currentRecording.getUri());
            mediaPlayer.prepare();
//            mediaPlayer.start();
            pauseBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            titlePlayerTv.setText(currentRecording.getTitle().substring(0, currentRecording.getTitle().length() - 6));
            audio_index = pos;
        } catch (Exception e) {
            e.printStackTrace();
        }

        setAudioProgress();
    }

    private void highlightSelectedRecording(int pos) {
        adapter.notifyItemChanged(adapter.selected_position);
        adapter.selected_position = pos;
        adapter.notifyItemChanged(adapter.selected_position);
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

        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("MMMM dd, yyyy");


        assert files != null;
        Log.d("Files", "Size: "+ files.length);

        for (File file : files) {

            Uri uri = Uri.fromFile(file);

            Date date = new Date(file.lastModified());

            int fileSizeInBytes = Integer.parseInt(String.valueOf(file.length()));
            totalSizeOfFolderInBytes += fileSizeInBytes;

            //takes the size of the clip and derives the duration from it
            String millSecond = Conversions.getFormattedDurationFromSeconds(TimeUnit.SECONDS.toMillis(Conversions.getSecondsFromSize(file.length(), Conversions.getQualityFromTitle(file.getName()), file.getName().substring(file.getName().length() -3))));

            recordingsList.add(new Recording(file.getName(), millSecond, format.format(date), fileSizeInBytes, isRecordingSaved(file.getName()), uri));


        }

        spaceLimitCheck();

    }

    /*Updates text on toolbar textView which shows size and no of recordings*/
    private void updateSizeTextView(long folderSize, int noOfRecordings){
        String sizeTemp = Conversions.humanReadableByteCountSI(folderSize) + "/" + Conversions.humanReadableByteCountSI(MAX_ALLOWED_STORAGE) + " (" + noOfRecordings + ")";
        folderInfoTv.setText(sizeTemp);
    }

    /*Checks if the total space occupied by recordings has exceeded the max space allowed and deletes oldest files on confirm*/
    private void spaceLimitCheck() {

        updateSizeTextView(totalSizeOfFolderInBytes, recordingsList.size());

        //check if the total folder size exceeds the max allowed
        if(totalSizeOfFolderInBytes !=0 && totalSizeOfFolderInBytes >= MAX_ALLOWED_STORAGE){

            //show dialog asking to confirm delete older files | that is if the dialog is enabled from settings
            if(sharedPreferences.getBoolean(mContext.getString(R.string.space_limit_dialog_pref), false)){
                showFreeUpSpaceDialog();
            }


        }
    }

    //deletes non-saved oldest files from storage, and local list
    private void deleteOldestFiles(){

        int noOfItemRemoved = 0;


        //delete the last modified file in recording list (if not saved as well) until we are below max_allowed
        if(recordingsList != null && recordingsList.size() > 0){

            for(int i=recordingsList.size()-1; i>=0 ; i--){

                //check if the clip is saved
                if(!isRecordingSaved(recordingsList.get(i).getTitle())){

                    totalSizeOfFolderInBytes = totalSizeOfFolderInBytes - recordingsList.get(i).getSize();

                    //delete statement goes here
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

                        updateSizeTextView(totalSizeOfFolderInBytes, recordingsList.size());
                        break;
                    }
                }



            }

            if(adapter != null){
                adapter.notifyDataSetChanged();
            }


        }
    }

    //checks if recording is saved
    private boolean isRecordingSaved(String title) {
        if(savedRecordingsSet== null) return false;
        return savedRecordingsSet.size() > 0 && savedRecordingsSet.contains(title);
    }


    private void setupRecyclerView() {


        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter = new RecordingsAdapter(mContext, recordingsList);
        recyclerView.setAdapter(adapter);

        adapter.setOnItemClickListener(new RecordingsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int pos, View v) {

                showPlayer();
                playRecording(pos);
                //make player visible
            }

            @Override
            public void onSaveClick(final int pos, View v) {

                //Prompt the user for a file rename, while the current filename is already entered into EditField
                showRenameAndSaveDialog(pos);


            }

            @Override
            public void onShareClick(int pos, View v) {
                //Take the audio file and share it across multiple apps using intent
                try {

                    shareFile(new File(Objects.requireNonNull(recordingsList.get(pos).getUri().getPath())));

                }catch (Exception e){
                    e.printStackTrace();
                }

            }

            @Override
            public void onDeleteClick(int pos, View v) {
                //delete item
                Log.d(TAG, "onDeleteClick: Delete Title |: "+recordingsList.get(pos).getTitle());
                showDeleteFileConfirmationDialog(pos);
            }
        });
    }



    //share file on other available social media apps
    private void shareFile(File filePath) {

        Uri uri = FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", filePath);
//        Uri uri = Uri.fromFile(filePath);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivity(Intent.createChooser(share, "Share Sound File"));

    }

    public boolean checkPermissions() {
        int READ_EXTERNAL_PERMISSION = ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE);
        if ((READ_EXTERNAL_PERMISSION != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_READ);
            return false;
        }
        return true;
    }

    private void showPlayer(){
        if(playerRoot.getVisibility() == View.GONE){
            TransitionManager.beginDelayedTransition(root);
            playerRoot.setVisibility(View.VISIBLE);
        }
    }

    private void hidePlayer(){
        if(playerRoot.getVisibility() == View.VISIBLE){
            TransitionManager.beginDelayedTransition(root);
            playerRoot.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
    }


    /*Deletes the file from internal storage and db(if present)*/
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
                .setTitle(getString(R.string.delete_dialog_title))
                .setMessage(getString(R.string.delete_dialog_content) + recordingsList.get(position).getTitle().substring(0, recordingsList.get(position).getTitle().length() - 6) + " (" + Conversions.humanReadableByteCountSI(recordingsList.get(position).getSize()) + ")?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    // Continue with delete operation
                    Uri fileUri = recordingsList.get(position).getUri();
                    File fileToDelete = new File(Objects.requireNonNull(fileUri.getPath()));
                    totalSizeOfFolderInBytes = totalSizeOfFolderInBytes - fileToDelete.length();
                    //deletes file from internal storage and db as well
                    delete(recordingsList.get(position).getTitle(), fileToDelete);
                    recordingsList.remove(position);
                    adapter.notifyItemRemoved(position);
                    updateSizeTextView(totalSizeOfFolderInBytes, recordingsList.size());

                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, (dialog, which) -> adapter.notifyDataSetChanged())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();


    }

    private void showFreeUpSpaceDialog() {
        new AlertDialog.Builder(mContext)
                .setTitle(getString(R.string.freeup_dialog_title))
                .setMessage(getString(R.string.freeup_dialog_content))

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(getString(R.string.freeup_dialog_positive_btn), (dialog, which) -> {
                    // User agreed to delete
                    deleteOldestFiles();

                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(getString(R.string.freeup_dialog_negative_btn), (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showSpaceFreedDialog(int noOfItemRemoved){
        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle(getString(R.string.space_freed_dialog_title));
        alertDialog.setMessage(noOfItemRemoved + " " + getString(R.string.space_freed_dialog_content));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    private void showRenameAndSaveDialog(int pos) {
        final Recording currentClip = recordingsList.get(pos);

        final View view = LayoutInflater.from(mContext).inflate(R.layout.save_dialog_layout, null);

        AlertDialog alertDialog = new AlertDialog.Builder(mContext).create();
        alertDialog.setTitle(getString(R.string.confirmation_dialog_title));
        alertDialog.setCancelable(false);
        alertDialog.setMessage(getString(R.string.space_freed_dialog_content));

        final EditText renameEditText = view.findViewById(R.id.etComments);
        String exclude = currentClip.getTitle().substring(currentClip.getTitle().length() - 6);
        renameEditText.setText(currentClip.getTitle().substring(0, currentClip.getTitle().length() - 6));

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Save", (dialogInterface, i) -> {

            String newFileName = renameEditText.getText().toString() + exclude;

            //here we rename the audio file and save the entry in db so that it doesn't get deleted automatically
            Recording curr_recording = recordingsList.get(pos);

            File from = new File(Paths.getOutputFolderPath() + curr_recording.getTitle());
            File to = new File(Paths.getOutputFolderPath() +  newFileName );

            if (fileHandler.rename(from, to)) {
                //Rename Success
                //set title, and Uri on current list

                curr_recording.setTitle(newFileName);
                curr_recording.setUri(Uri.fromFile(to));
                recordingsList.get(pos).setTitle(newFileName);
                recordingsList.get(pos).setUri(Uri.fromFile(to));

                adapter.notifyDataSetChanged();

                //here we save to db
                if(!savedRecordingsSet.contains(curr_recording.getTitle())) {
                    savedRecordingsSet.add(curr_recording.getTitle());
                    databaseTransactions.saveRecordingToDb(curr_recording.getTitle(), curr_recording.getUri().toString());
                    recordingsList.get(pos).setSaved(true);
                    adapter.notifyDataSetChanged();
                }

            } else {
                //Fail
                Log.i(TAG, "Rename File : Fail");
            }


        });


        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialogInterface, i) -> dialogInterface.dismiss());

        alertDialog.setView(view);
        alertDialog.show();

    }

    /*-------------------------------POP-UP DIALOGS--------------------------*/



    /*-------------------------------LISTENERS------------------------------*/

    MediaPlayer.OnCompletionListener trackCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            pauseBtn.setImageResource(R.drawable.ic_play_arrow_black_24dp);
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

    View.OnClickListener backClickListener = v -> requireActivity().onBackPressed();

    View.OnClickListener closePlayerClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mediaPlayer.isPlaying()){
                mediaPlayer.stop();
            }

            hidePlayer();

        }
    };

    View.OnClickListener folderInfoClickListener = v -> {
//            openFolder();

    };

    /*-------------------------------LISTENERS------------------------------*/



}