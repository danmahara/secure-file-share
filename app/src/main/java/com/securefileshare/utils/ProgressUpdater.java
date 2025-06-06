package com.securefileshare.utils;

public interface ProgressUpdater {
    void onProgressUpdate(int percentage, long bytesTransferred, long totalBytes);
}
