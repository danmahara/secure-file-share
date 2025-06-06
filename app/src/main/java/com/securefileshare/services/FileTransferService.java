package com.securefileshare.services;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.securefileshare.utils.AESUtils;
import com.securefileshare.utils.FileUtils;
import com.securefileshare.utils.ProgressUpdater;
import com.securefileshare.utils.ProgressUpdaterHolder;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class FileTransferService extends IntentService {
    public static final String ACTION_SEND_FILE = "com.securefileshare.SEND_FILE";
    public static final String EXTRA_FILE_URI = "file_uri";
    public static final String EXTRA_HOST = "host";
    public static final String EXTRA_PORT = "port";

    public FileTransferService() {
        super("FileTransferService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_SEND_FILE)) {
            String host = intent.getStringExtra(EXTRA_HOST);
            int port = intent.getIntExtra(EXTRA_PORT, 8988);
            Uri fileUri = intent.getParcelableExtra(EXTRA_FILE_URI);

            try {
                Socket socket = new Socket(host, port);
                OutputStream outputStream = socket.getOutputStream();
                InputStream inputStream = getContentResolver().openInputStream(fileUri);

                // Send file name
                String fileName = FileUtils.getFileNameFromUri(getApplicationContext(), fileUri);
                byte[] fileNameBytes = fileName.getBytes("UTF-8");
                outputStream.write((fileNameBytes.length + "\n").getBytes());
                outputStream.write(fileNameBytes);

                // Send MIME type
                String mimeType = getContentResolver().getType(fileUri);
                byte[] mimeTypeBytes = mimeType.getBytes("UTF-8");
                outputStream.write((mimeTypeBytes.length + "\n").getBytes());
                outputStream.write(mimeTypeBytes);

                // Send file data
                long totalSize = getContentResolver().openAssetFileDescriptor(fileUri, "r").getLength();
                long totalSent = 0;

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byte[] actualData = new byte[bytesRead];
                    System.arraycopy(buffer, 0, actualData, 0, bytesRead);

                    byte[] encrypted = AESUtils.encrypt(actualData);
                    outputStream.write(encrypted);

                    totalSent += bytesRead;
                    int progress = (int) ((totalSent * 100) / totalSize);
                    updateProgress(progress, totalSent, totalSize);
                }

                inputStream.close();
                outputStream.close();
                socket.close();

            } catch (Exception e) {
                Log.e("FileTransferService", "Error sending file: " + e.getMessage());
            }
        }
    }



    private void updateProgress(int progress, long sent, long total) {
        ProgressUpdater updater = ProgressUpdaterHolder.getUpdater();
        if (updater != null) {
            updater.onProgressUpdate(progress, sent, total);
        }
    }
}
