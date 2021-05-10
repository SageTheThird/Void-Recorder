package com.client.voidrecorder.recordings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.client.voidrecorder.R;

import com.client.voidrecorder.models.Recording;
import com.client.voidrecorder.utils.Conversions;

import java.util.ArrayList;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.viewHolder> {

    Context context;
    ArrayList<Recording> recordingsList;
    public OnItemClickListener onItemClickListener;

    public RecordingsAdapter(Context context, ArrayList<Recording> recordingsList) {
        this.context = context;
        this.recordingsList = recordingsList;
    }

    @Override
    public RecordingsAdapter.viewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.recordings_list_item, viewGroup, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecordingsAdapter.viewHolder holder, final int i) {


        holder.title.setText(titleWithoutExt(recordingsList.get(i).getTitle()));
        holder.date.setText(recordingsList.get(i).getDate());
        holder.duration.setText(recordingsList.get(i).getDuration());
        holder.size.setText(Conversions.humanReadableByteCountSI(recordingsList.get(i).getSize()));

        if(recordingsList.get(i).isSaved()){
            //show tick mark
            holder.saveBtn.setImageResource(R.drawable.ic_baseline_bookmark_24);
        }else{
            //show save icon
            holder.saveBtn.setImageResource(R.drawable.ic_baseline_save_24);

        }

        holder.saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onSaveClick(holder.getAdapterPosition(), view);

            }
        });

        holder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onItemClickListener.onShareClick(holder.getAdapterPosition(), view);

            }
        });
    }

    public String titleWithoutExt(String str) {

        String ext = str.substring(str.length() - 4);

        if (str != null && str.length() > 0) {
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
        ImageView saveBtn, shareBtn;

        public viewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title);
            date = (TextView) itemView.findViewById(R.id.date);
            duration = (TextView) itemView.findViewById(R.id.duration);
            size = (TextView) itemView.findViewById(R.id.size);

            saveBtn = itemView.findViewById(R.id.saveBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onItemClickListener.onItemClick(getAdapterPosition(), v);
                }
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
    }
}