package com.securefileshare.fragments;

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

import com.securefileshare.R;
import com.securefileshare.adapters.MediaAdapter;
import com.securefileshare.models.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImagesFragment extends Fragment {

    private static ImagesFragment selectedInstance;

    private RecyclerView recyclerView;
    private MediaAdapter adapter;

    public static List<File> getSelectedImages() {
        return selectedInstance != null ? selectedInstance.adapter.getSelectedFiles() : new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewMedia);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3)); // Grid 3 columns
        selectedInstance=this;
        loadImages();

        return view;
    }

    private void loadImages() {
        List<FileItem> images = new ArrayList<>();

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.SIZE, MediaStore.Images.Media.DATA // deprecated but widely supported
        };

        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    long size = cursor.getLong(sizeColumn);
                    String path = cursor.getString(dataColumn);

                    images.add(new FileItem(id, name, size, path, FileItem.TYPE_IMAGE));
                }
            }
        }

        adapter = new MediaAdapter(getContext(), images);
        recyclerView.setAdapter(adapter);
    }
}

