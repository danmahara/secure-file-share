package com.securefileshare.activities;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.securefileshare.R;
import com.securefileshare.utils.AESUtils;
import com.securefileshare.utils.FileUtils;
import com.securefileshare.utils.ProgressUpdater;
import com.securefileshare.utils.ProgressUpdaterHolder;
import com.securefileshare.utils.SelectedFileManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

public class TransferProgressActivity extends AppCompatActivity implements ProgressUpdater {
    private ProgressBar progressBar;
    private TextView progressText, sizeText, fileCountText;
    private String hostAddress;
    private int port;
    private List<File> filesToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_progress);

        progressBar = findViewById(R.id.sendingProgressBar);
        progressText = findViewById(R.id.sendingProgressText);
        sizeText = findViewById(R.id.sendingSizeText);
        fileCountText = findViewById(R.id.fileCountText);

        hostAddress = getIntent().getStringExtra("host");
        port = getIntent().getIntExtra("port", 8988);
        filesToSend = SelectedFileManager.getFiles();

        if (hostAddress == null || hostAddress.isEmpty()) {
            Log.e("SenderThreadError", "Invalid host address");
            runOnUiThread(() -> progressText.setText("Error: Invalid host address"));
            return;
        }

        ProgressUpdaterHolder.setUpdater(this);
        startSenderThread();
    }

    private void startSenderThread() {
        new Thread(() -> {
            Socket socket = null;
            DataOutputStream dataOutputStream = null;
            BufferedOutputStream bufferedOutputStream = null;

            try {
                // Validate host address and check for self-connection
                try {
                    InetAddress targetAddress = InetAddress.getByName(hostAddress);
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    String deviceIp = FileUtils.getDeviceIpAddress(wifiManager);

                    Log.d("SenderThread", "Target IP: " + targetAddress.getHostAddress());
                    Log.d("SenderThread", "Device IP: " + deviceIp);

                    // Prevent self-connection
                    if (targetAddress.getHostAddress().equals(deviceIp)) {
                        throw new IOException("Cannot connect to own IP address: " + hostAddress);
                    }
                } catch (UnknownHostException e) {
                    throw new IOException("Invalid host address: " + hostAddress);
                }

                // Enhanced retry connection logic with multiple ports
                int[] portsToTry = {port, 8988, 8888, 8990, 8991}; // Try original port first, then fallbacks
                int retryCount = 0;
                final int MAX_RETRIES = 3;
                final int RETRY_DELAY_MS = 3000; // 3 seconds between retries
                final int CONNECT_TIMEOUT_MS = 10000; // Increased to 10 seconds

                boolean connected = false;

                for (int currentPort : portsToTry) {
                    if (connected) break;

                    retryCount = 0; // Reset retry count for each port
                    while (retryCount < MAX_RETRIES && !connected) {
                        try {
                            Log.d("SenderThread", "Attempting to connect to " + hostAddress + ":" + currentPort + " (Attempt " + (retryCount + 1) + ")");

                            socket = new Socket();
                            socket.setReuseAddress(true);
                            socket.setSoTimeout(30000); // 30 seconds read timeout
                            socket.connect(new InetSocketAddress(hostAddress, currentPort), CONNECT_TIMEOUT_MS);

                            bufferedOutputStream = new BufferedOutputStream(socket.getOutputStream());
                            dataOutputStream = new DataOutputStream(bufferedOutputStream);

                            Log.d("SenderThread", "Connected successfully to " + hostAddress + ":" + currentPort);
                            port = currentPort; // Update port to the successful one
                            connected = true;

                            runOnUiThread(() -> progressText.setText("Connected to " + hostAddress + ":" + currentPort));

                        } catch (SocketTimeoutException e) {
                            retryCount++;
                            Log.w("SenderThread", "Connection timeout to port " + currentPort + ", retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                            closeSocket(socket);
                            if (retryCount < MAX_RETRIES) {
                                Thread.sleep(RETRY_DELAY_MS);
                            }
                        } catch (IOException e) {
                            retryCount++;
                            Log.w("SenderThread", "Connection failed to port " + currentPort + ": " + e.getMessage() + ", retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                            closeSocket(socket);
                            if (retryCount < MAX_RETRIES) {
                                Thread.sleep(RETRY_DELAY_MS);
                            }
                        }
                    }
                }

                if (!connected) {
                    throw new IOException("Failed to connect to " + hostAddress + " on any port after multiple attempts");
                }

                // Test connection by sending a ping
                try {
                    dataOutputStream.writeUTF("PING");
                    dataOutputStream.flush();
                    Log.d("SenderThread", "Connection test successful");
                } catch (IOException e) {
                    throw new IOException("Connection test failed: " + e.getMessage());
                }

                // Notify receiver about total files
                dataOutputStream.writeInt(filesToSend.size());
                dataOutputStream.flush();

                runOnUiThread(() -> {
                    progressText.setText("Starting file transfer...");
                    fileCountText.setText("Sending " + filesToSend.size() + " files");
                });

                // Send each file
                int fileIndex = 0;
                for (File file : filesToSend) {
                    fileIndex++;
                    final int currentFileIndex = fileIndex;
                    runOnUiThread(() -> fileCountText.setText("Sending file " + currentFileIndex + " of " + filesToSend.size() + ": " + file.getName()));

                    sendSingleFile(file, dataOutputStream);
                }

                // Send end-of-transfer marker
                dataOutputStream.writeUTF("");
                dataOutputStream.flush();

                runOnUiThread(() -> {
                    progressText.setText("Transfer complete!");
                    fileCountText.setText("All files sent successfully");
                });

            } catch (Exception e) {
                Log.e("SenderThreadError", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    progressText.setText("Error: " + e.getMessage());
                    fileCountText.setText("Transfer failed");
                });
            } finally {
                try {
                    if (dataOutputStream != null) dataOutputStream.close();
                    if (bufferedOutputStream != null) bufferedOutputStream.close();
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    Log.e("SocketCloseError", "Failed to close socket: " + e.getMessage());
                }
            }
        }).start();
    }

    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("SenderThread", "Failed to close socket: " + e.getMessage());
            }
        }
    }

    private void sendSingleFile(File file, DataOutputStream dataOutputStream) throws Exception {
        String fileName = file.getName();
        String mimeType = FileUtils.getMimeType(file.getAbsolutePath());

        // Fallback to "application/octet-stream" if mimeType is null
        if (mimeType == null) {
            mimeType = "application/octet-stream";
            Log.w("SendFileWarning", "MIME type null for file: " + fileName + ", using default: " + mimeType);
        }

        long fileSize = file.length();

        try {
            // Send metadata
            dataOutputStream.writeUTF(fileName);
            dataOutputStream.writeUTF(mimeType);
            dataOutputStream.writeLong(fileSize);
            dataOutputStream.flush(); // Ensure metadata is sent immediately

            Log.d("SenderThread", "Sending file: " + fileName + " (" + FileUtils.humanReadableByteCount(fileSize) + ")");

            // Send file content in chunks using BufferedInputStream
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
            byte[] buffer = new byte[8192]; // Increased buffer size
            int bytesRead;
            long totalBytesRead = 0;
            long lastProgressUpdate = 0;

            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);

                byte[] encrypted = AESUtils.encrypt(chunk);
                dataOutputStream.writeInt(encrypted.length);
                dataOutputStream.write(encrypted);
                dataOutputStream.flush();

                totalBytesRead += bytesRead;

                // Update progress less frequently to improve performance
                if (totalBytesRead - lastProgressUpdate > 32768 || totalBytesRead == fileSize) { // Update every 32KB or at end
                    int progress = (int) ((totalBytesRead * 100) / fileSize);
                    onProgressUpdate(progress, totalBytesRead, fileSize);
                    lastProgressUpdate = totalBytesRead;
                }
            }

            bufferedInputStream.close();
            Log.d("SenderThread", "File sent successfully: " + fileName);

        } catch (Exception e) {
            Log.e("SendFileError", "Failed to send file: " + fileName + ", Error: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public void onProgressUpdate(int percentage, long bytesTransferred, long totalBytes) {
        runOnUiThread(() -> {
            progressBar.setProgress(percentage);
            progressText.setText("Progress: " + percentage + "%");
            sizeText.setText("Transferred: " + FileUtils.humanReadableByteCount(bytesTransferred) + " / " + FileUtils.humanReadableByteCount(totalBytes));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ProgressUpdaterHolder.clear();
    }
}


