package com.securefileshare.fragments;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.adapters.MediaAdapter;
import com.securefileshare.models.FileItem;
import com.securefileshare.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class VideosFragment extends Fragment {

    private static VideosFragment selectedInstance;

    private RecyclerView recyclerView;
    private MediaAdapter adapter;

    public static List<File> getSelectedVideos() {
        return selectedInstance != null ? selectedInstance.adapter.getSelectedFiles() : new ArrayList<>();
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMedia);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        selectedInstance=this;
        loadVideos();

        return view;
    }

    private void loadVideos() {
        List<FileItem> videos = new ArrayList<>();

        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATA
        };

        try (Cursor cursor = requireContext().getContentResolver().query(
                uri,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    String path = cursor.getString(dataColumn);

                    videos.add(new FileItem(id, name, size, path, FileItem.TYPE_VIDEO));
                }
            }
        }

        adapter = new MediaAdapter(getContext(), videos);
        recyclerView.setAdapter(adapter);
    }
}

