package com.example.audio_player_and_recorder;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    class FileViewHolder extends RecyclerView.ViewHolder{
        View fileView;
        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileView = itemView;
        }
    }

    public interface OnFilePickedCallback {
        void OnFilePicked(File file);
    }

    OnFilePickedCallback mOnFilePickedCallback;
    private RecyclerView mRecyclerView;
    private List<File> mFiles;

    public FileAdapter(List<File> files, OnFilePickedCallback onFilePickedCallback){
        mFiles = files;
        mOnFilePickedCallback = onFilePickedCallback;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_view, parent, false);
        TextView view = new TextView(parent.getContext());
        view.setPadding(16, 16, 16, 16);
        view.setClickable(true);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mRecyclerView.getChildAdapterPosition(v);
                mOnFilePickedCallback.OnFilePicked(mFiles.get(index));
            }
        });

        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        ((TextView) holder.fileView).setText(mFiles.get(position).getName());
    }

    @Override
    public int getItemCount() {
        return mFiles.size();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }
}
