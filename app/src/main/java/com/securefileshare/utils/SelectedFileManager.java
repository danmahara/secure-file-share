package com.securefileshare.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SelectedFileManager {
    private static final ArrayList<File> selectedFiles = new ArrayList<>();

    public static void setFiles(List<File> files) {
        selectedFiles.clear();
        selectedFiles.addAll(files);
    }

    public static List<File> getFiles() {
        return selectedFiles;
    }

    public static void clear() {
        selectedFiles.clear();
    }
}
