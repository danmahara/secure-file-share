package com.securefileshare.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;
import com.securefileshare.models.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.FileViewHolder> {

    private Context context;
    private List<FileItem> files;
    private List<File> selectedFiles;

    public FileAdapter(Context context, List<FileItem> files) {
        this.context = context;
        this.files = files;
        this.selectedFiles = new ArrayList<>();
    }

    public List<File> getSelectedFiles() {
        return selectedFiles;
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        FileItem fileItem = files.get(position);
        holder.fileName.setText(fileItem.getName());

        // Set a generic icon based on extension
        holder.fileIcon.setImageResource(R.drawable.ic_file_document);

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
        return files.size();
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName;
        CheckBox checkBox;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.imageViewFileIcon);
            fileName = itemView.findViewById(R.id.textViewFileName);
            checkBox = itemView.findViewById(R.id.checkBoxFile);
        }
    }
}
