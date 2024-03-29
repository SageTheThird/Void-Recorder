package com.client.voidrecorder.recordings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.recyclerview.widget.RecyclerView;

import com.client.voidrecorder.R;

import java.util.ArrayList;

public class RecordingsAdapter extends RecyclerView.Adapter<RecordingsAdapter.viewHolder> {

    Context context;
    ArrayList<ModelRecordings> audioArrayList;
    public OnItemClickListener onItemClickListener;

    public RecordingsAdapter(Context context, ArrayList<ModelRecordings> audioArrayList) {
        this.context = context;
        this.audioArrayList = audioArrayList;
    }

    @Override
    public RecordingsAdapter.viewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(context).inflate(R.layout.recordings_list_item, viewGroup, false);
        return new viewHolder(view);
    }

    @Override
    public void onBindViewHolder(final RecordingsAdapter.viewHolder holder, final int i) {


        holder.title.setText(audioArrayList.get(i).getTitle());
        holder.date.setText(audioArrayList.get(i).getDate());
        holder.duration.setText(audioArrayList.get(i).getDuration());
        holder.size.setText(audioArrayList.get(i).getSize());

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



    @Override
    public int getItemCount() {
        return audioArrayList.size();
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