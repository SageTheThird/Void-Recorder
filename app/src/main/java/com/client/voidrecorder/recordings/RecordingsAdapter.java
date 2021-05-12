package com.client.voidrecorder.recordings;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.client.voidrecorder.R;

import com.client.voidrecorder.models.Recording;
import com.client.voidrecorder.recorder.RecorderService;
import com.client.voidrecorder.utils.Conversions;
import com.client.voidrecorder.utils.ServiceUtil;

import java.util.ArrayList;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.viewHolder> {

    Context context;
    ArrayList<Recording> recordingsList;
    public OnItemClickListener onItemClickListener;
    int selected_position = RecyclerView.NO_POSITION; // You have to set this globally in the Adapter class


    public void setSelectedPosition(int position){
        selected_position = position;
    }

    public RecordingsAdapter(Context context, ArrayList<Recording> recordingsList) {
        this.context = context;
        this.recordingsList = recordingsList;
    }

    @NonNull
    @Override
    public RecordingsAdapter.viewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.recordings_list_item, viewGroup, false);
        return new viewHolder(view);
    }


    @Override
    public void onBindViewHolder(final RecordingsAdapter.viewHolder holder, final int i) {




        holder.itemView.setBackground(selected_position == i ? ContextCompat.getDrawable(context, R.drawable.recording_selected_round_background) : ContextCompat.getDrawable(context, R.drawable.recordings_round_background));


        holder.title.setText(titleWithoutExt(recordingsList.get(i).getTitle()));
        holder.date.setText(recordingsList.get(i).getDate());
        holder.duration.setText(recordingsList.get(i).getDuration());
        holder.size.setText(Conversions.humanReadableByteCountSI(recordingsList.get(i).getSize()));

        if(i == 0){
            if(ServiceUtil.isServiceRunningInForeground(context, RecorderService.class)){
                holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.recordings_recording_round_background));
                holder.duration.setText(context.getString(R.string.dummy_duration));
                holder.size.setText(context.getString(R.string.dummy_size));
                holder.shareBtn.setEnabled(false);
                holder.saveBtn.setEnabled(false);
                holder.deleteBtn.setEnabled(false);
                holder.itemView.setClickable(false);
            }
        }

        if(recordingsList.get(i).isSaved()){
            //show tick mark
            holder.saveBtn.setImageResource(R.drawable.ic_baseline_bookmark_24);
        }else{
            //show save icon
            holder.saveBtn.setImageResource(R.drawable.ic_baseline_save_24);

        }

        holder.saveBtn.setOnClickListener(view -> onItemClickListener.onSaveClick(holder.getAdapterPosition(), view));
        holder.shareBtn.setOnClickListener(view -> onItemClickListener.onShareClick(holder.getAdapterPosition(), view));
        holder.deleteBtn.setOnClickListener(view -> onItemClickListener.onDeleteClick(holder.getAdapterPosition(), view));

    }

    public String titleWithoutExt(String str) {

        String ext = str.substring(str.length() - 4);

        if (str.length() > 0) {
            str = str.substring(0, str.length() -6);
        }

        str = str + ext;
//        assert str != null;
//        if(str.contains("Lo")) { return str.replace("Lo", "");}
//        if(str.contains("Me")) { return str.replace("Me", "");}
//        if(str.contains("Hi")) { return str.replace("Hi", "");}


        return str;

    }


    @Override
    public int getItemCount() {
        return recordingsList.size();
    }

    public class viewHolder extends RecyclerView.ViewHolder {
        TextView title, date, duration, size;
        ImageView saveBtn, shareBtn, deleteBtn;
        View itemView;

        public viewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            title = itemView.findViewById(R.id.title);
            date = itemView.findViewById(R.id.date);
            duration = itemView.findViewById(R.id.duration);
            size = itemView.findViewById(R.id.size);

            saveBtn = itemView.findViewById(R.id.saveBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            deleteBtn = itemView.findViewById(R.id.deleteBtn);

            itemView.setOnClickListener(v -> {
                notifyItemChanged(selected_position);
                selected_position = getLayoutPosition();
                notifyItemChanged(selected_position);
                onItemClickListener.onItemClick(getAdapterPosition(), v);
            });
        }



    }
    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }
    public interface OnItemClickListener {
        void onItemClick(int pos, View v);
        void onSaveClick(int pos, View v);
        void onShareClick(int pos, View v);
        void onDeleteClick(int pos, View v);
    }
}