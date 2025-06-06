package com.securefileshare.fragments;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;
import com.securefileshare.adapters.AppAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AppsFragment extends Fragment {

    private static AppsFragment selectedInstance;
    private RecyclerView recyclerView;
    private AppAdapter adapter;

    public static List<File> getSelectedFiles() {
        return selectedInstance != null ? selectedInstance.adapter.getSelectedApps() : new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewApps);
        selectedInstance = this;

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        loadInstalledApps();

        return view;
    }

    private void loadInstalledApps() {
        PackageManager pm = requireContext().getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<ApplicationInfo> userApps = new ArrayList<>();
        for (ApplicationInfo app : allApps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                userApps.add(app);
            }
        }

        adapter = new AppAdapter(getContext(), userApps);
        recyclerView.setAdapter(adapter);
    }
}

