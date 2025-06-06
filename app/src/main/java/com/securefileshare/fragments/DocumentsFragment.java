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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;
import com.securefileshare.adapters.FileAdapter;
import com.securefileshare.models.FileItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DocumentsFragment extends Fragment {

    private static DocumentsFragment selectedInstance;
    private final String[] DOCUMENT_EXTENSIONS = {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt"};
    private RecyclerView recyclerView;
    private FileAdapter adapter;

    public static List<File> getSelectedDocuments() {
        return selectedInstance != null ? selectedInstance.adapter.getSelectedFiles() : new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_documents, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewDocuments);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        selectedInstance=this;
        loadDocuments();

        return view;
    }

    private void loadDocuments() {
        List<FileItem> docs = new ArrayList<>();

        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.MIME_TYPE};

        // Selection query to get documents by extensions
        StringBuilder selection = new StringBuilder();
        for (int i = 0; i < DOCUMENT_EXTENSIONS.length; i++) {
            selection.append(MediaStore.Files.FileColumns.DISPLAY_NAME + " LIKE ?");
            if (i != DOCUMENT_EXTENSIONS.length - 1) {
                selection.append(" OR ");
            }
        }

        String[] selectionArgs = new String[DOCUMENT_EXTENSIONS.length];
        for (int i = 0; i < DOCUMENT_EXTENSIONS.length; i++) {
            selectionArgs[i] = "%" + DOCUMENT_EXTENSIONS[i];
        }

        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, selection.toString(), selectionArgs, MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    long size = cursor.getLong(sizeCol);
                    String path = cursor.getString(dataCol);

                    docs.add(new FileItem(id, name, size, path, FileItem.TYPE_DOCUMENT));
                }
            }
        }

        adapter = new FileAdapter(getContext(), docs);
        recyclerView.setAdapter(adapter);
    }
}
