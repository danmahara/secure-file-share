package com.securefileshare.adapters;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.AppViewHolder> {

    private Context context;
    private List<ApplicationInfo> appList;
    private PackageManager packageManager;
    private List<File> selectedAppFiles;

    public AppAdapter(Context context, List<ApplicationInfo> appList) {
        this.context = context;
        this.appList = appList;
        this.packageManager = context.getPackageManager();
        this.selectedAppFiles = new ArrayList<>();
    }

    public List<File> getSelectedApps() {
        return selectedAppFiles;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_app, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplicationInfo appInfo = appList.get(position);
        Drawable icon = appInfo.loadIcon(packageManager);
        String name = appInfo.loadLabel(packageManager).toString();
        String apkPath = appInfo.sourceDir;

        holder.appIcon.setImageDrawable(icon);
        holder.appName.setText(name);
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(isSelected(apkPath));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            File file = new File(apkPath);
            if (isChecked) {
                selectedAppFiles.add(file);
            } else {
                selectedAppFiles.remove(file);
            }
        });
    }

    private boolean isSelected(String apkPath) {
        for (File file : selectedAppFiles) {
            if (file.getAbsolutePath().equals(apkPath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemCount() {
        return appList.size();
    }

    static class AppViewHolder extends RecyclerView.ViewHolder {
        ImageView appIcon;
        TextView appName;
        CheckBox checkBox;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            appIcon = itemView.findViewById(R.id.imageViewIcon);
            appName = itemView.findViewById(R.id.textViewAppName);
            checkBox = itemView.findViewById(R.id.checkBoxApp);
        }
    }
}
