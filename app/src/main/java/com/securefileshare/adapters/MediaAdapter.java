package com.securefileshare.adapters;


import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.securefileshare.R;
import com.securefileshare.models.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {

    private Context context;
    private List<FileItem> mediaFiles;
    private List<File> selectedFiles;

    public MediaAdapter(Context context, List<FileItem> mediaFiles) {
        this.context = context;
        this.mediaFiles = mediaFiles;
        this.selectedFiles = new ArrayList<>();
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        FileItem fileItem = mediaFiles.get(position);
        Uri fileUri = Uri.fromFile(new File(fileItem.getPath()));

        Glide.with(context)
                .load(fileUri)
                .centerCrop()
                .placeholder(R.drawable.ic_file_placeholder)
                .into(holder.imageView);


        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(isSelected(fileItem.getPath()));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            File file = new File(fileItem.getPath());
            if (isChecked) {
                selectedFiles.add(file);
            } else {
                selectedFiles.remove(file);
            }
        });

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Do something, e.g., toggle checkbox
                boolean isChecked = holder.checkBox.isChecked();
                holder.checkBox.setChecked(!isChecked);

                // Optional: highlight the image or perform another action
//                Toast.makeText(context, "Clicked: " + fileItem.getName(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    private boolean isSelected(String path) {
        for (File file : selectedFiles) {
            if (file.getAbsolutePath().equals(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return mediaFiles.size();
    }

    static class MediaViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        CheckBox checkBox;

        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewMedia);
            checkBox = itemView.findViewById(R.id.checkBoxMedia);
        }
    }
}
