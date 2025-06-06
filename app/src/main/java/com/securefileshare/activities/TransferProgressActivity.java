package com.securefileshare.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.securefileshare.R;
import com.securefileshare.services.FileTransferService;
import com.securefileshare.utils.ProgressUpdater;
import com.securefileshare.utils.ProgressUpdaterHolder;

public class TransferProgressActivity extends AppCompatActivity implements ProgressUpdater {

    private ProgressBar progressBar;
    private TextView progressText, sizeText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        sizeText = findViewById(R.id.sizeText);

        ProgressUpdaterHolder.setUpdater(this); // 👈 Important

        // You can start service or thread to handle sending here


        Intent serviceIntent = new Intent(this, FileTransferService.class);
        serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);

        serviceIntent.putExtra(FileTransferService.EXTRA_FILE_URI, (Uri) getIntent().getParcelableExtra("file_uri"));
        serviceIntent.putExtra(FileTransferService.EXTRA_HOST, getIntent().getStringExtra("host"));
        serviceIntent.putExtra(FileTransferService.EXTRA_PORT, getIntent().getIntExtra("port", 8988));
        startService(serviceIntent);


    }

    @Override
    public void onProgressUpdate(int percentage, long bytesTransferred, long totalBytes) {
        progressBar.setProgress(percentage);
        progressText.setText("Sent: " + formatSize(bytesTransferred) + " / " + formatSize(totalBytes));
        sizeText.setText("Total : "+formatSize(totalBytes));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProgressUpdaterHolder.clear(); // 👈 Avoid memory leak
    }

    private String formatSize(long size) {
        return android.text.format.Formatter.formatFileSize(this, size);
    }

}
