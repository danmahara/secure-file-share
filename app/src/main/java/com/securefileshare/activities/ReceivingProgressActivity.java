package com.securefileshare.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.securefileshare.R;
import com.securefileshare.utils.AESUtils;
import com.securefileshare.utils.FileUtils;
import com.securefileshare.utils.ProgressUpdater;
import com.securefileshare.utils.ProgressUpdaterHolder;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ReceivingProgressActivity extends AppCompatActivity implements ProgressUpdater {
    private TextView fileCountText;
    private ProgressBar receivingProgressBar;
    private TextView receivingProgressText;
    private TextView receivingSizeText;
    private Button cancelButton;
    private int port = 8988;
    private boolean isTransferActive = true;
    private ServerSocket serverSocket;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receiving_progress);

        // Initialize views
        fileCountText = findViewById(R.id.fileCountText);
        receivingProgressBar = findViewById(R.id.receivingProgressBar);
        receivingProgressText = findViewById(R.id.receivingProgressText);
        receivingSizeText = findViewById(R.id.receivingSizeText);
        // cancelButton = findViewById(R.id.cancelButton);

        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> {
                isTransferActive = false;
                finish();
            });
        }

        ProgressUpdaterHolder.setUpdater(this);
        startReceiverThread();
    }

    private void startReceiverThread() {
        new Thread(() -> {
            Socket client = null;
            DataInputStream dataInputStream = null;
            BufferedInputStream bufferedInputStream = null;

            try {
                // Enhanced port binding with better error handling
                int[] portsToTry = {8988, 8888, 8990, 8991, 8992};
                boolean socketBound = false;

                for (int tryPort : portsToTry) {
                    try {
                        serverSocket = new ServerSocket();
                        serverSocket.setReuseAddress(true);
                        serverSocket.bind(new java.net.InetSocketAddress(tryPort));
                        serverSocket.setSoTimeout(30000); // 30 seconds timeout for accept

                        port = tryPort;
                        socketBound = true;
                        Log.d("ReceiverThread", "ServerSocket bound to port " + port);
                        runOnUiThread(() -> receivingProgressText.setText("Waiting for sender on port " + port + "..."));
                        break;

                    } catch (BindException e) {
                        Log.w("ReceiverThread", "Port " + tryPort + " binding failed: " + e.getMessage());
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                            } catch (IOException ex) {
                                Log.e("ReceiverThread", "Failed to close socket: " + ex.getMessage());
                            }
                        }
                    }
                }

                if (!socketBound) {
                    throw new IOException("Failed to bind to any available port");
                }

                // Wait for connection with enhanced retry logic
                int connectionAttempts = 0;
                final int MAX_CONNECTION_ATTEMPTS = 10;

                while (connectionAttempts < MAX_CONNECTION_ATTEMPTS && isTransferActive) {
                    try {
                        connectionAttempts++;
                        Log.d("ReceiverThread", "Waiting for connection (attempt " + connectionAttempts + ")...");
                        int finalConnectionAttempts = connectionAttempts;
                        runOnUiThread(() -> receivingProgressText.setText("Waiting for connection (attempt " + finalConnectionAttempts + ")..."));

                        client = serverSocket.accept();
                        Log.d("ReceiverThread", "Connection accepted from " + client.getInetAddress().getHostAddress());
                        Socket finalClient = client;
                        runOnUiThread(() -> receivingProgressText.setText("Connection established with " + finalClient.getInetAddress().getHostAddress()));
                        break;

                    } catch (SocketTimeoutException e) {
                        Log.d("ReceiverThread", "Connection timeout, retrying... (" + connectionAttempts + "/" + MAX_CONNECTION_ATTEMPTS + ")");
                        if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS) {
                            throw new IOException("No connection received after " + MAX_CONNECTION_ATTEMPTS + " attempts");
                        }
                    }
                }

                if (!isTransferActive) {
                    Log.d("ReceiverThread", "Transfer cancelled by user");
                    return;
                }

                if (!client.isConnected()) {
                    throw new IOException("Failed to establish connection");
                }

                // Set up streams
                bufferedInputStream = new BufferedInputStream(client.getInputStream());
                dataInputStream = new DataInputStream(bufferedInputStream);

                // Handle connection test ping
                try {
                    String pingMessage = dataInputStream.readUTF();
                    if ("PING".equals(pingMessage)) {
                        Log.d("ReceiverThread", "Connection test received");
                    }
                } catch (IOException e) {
                    Log.w("ReceiverThread", "No ping received, continuing with file transfer");
                    // Reset streams if needed
                    bufferedInputStream = new BufferedInputStream(client.getInputStream());
                    dataInputStream = new DataInputStream(bufferedInputStream);
                }

                // Read total files count
                final int totalFiles = dataInputStream.readInt();
                Log.d("ReceiverThread", "Expecting " + totalFiles + " files");
                runOnUiThread(() -> receivingProgressText.setText("Receiving " + totalFiles + " files..."));

                int receivedFiles = 0;

                // Create base directory
                File baseDir = new File(android.os.Environment.getExternalStorageDirectory(), "SecureFileShare");
                if (!baseDir.exists() && !baseDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + baseDir.getAbsolutePath());
                }

                while (isTransferActive) {
                    String fileName = dataInputStream.readUTF();
                    if (fileName.isEmpty()) break; // End of transfer

                    receivedFiles++;
                    final int currentFileNumber = receivedFiles;
                    runOnUiThread(() -> fileCountText.setText(String.format("Receiving file %d of %d: %s", currentFileNumber, totalFiles, fileName)));

                    String mimeType = dataInputStream.readUTF();
                    long fileSize = dataInputStream.readLong();

                    Log.d("ReceiverThread", "Receiving file: " + fileName + " (" + FileUtils.humanReadableByteCount(fileSize) + ")");

                    // Create subdirectory based on file type
                    String subFolder = getSubFolderForMimeType(mimeType);
                    File subDir = new File(baseDir, subFolder);
                    if (!subDir.exists() && !subDir.mkdirs()) {
                        throw new IOException("Failed to create subdirectory: " + subDir.getAbsolutePath());
                    }

                    // Handle duplicate file names
                    File targetFile = new File(subDir, fileName);
                    int fileCounter = 1;
                    String baseName = fileName;
                    String extension = "";
                    int dotIndex = fileName.lastIndexOf('.');
                    if (dotIndex > 0) {
                        baseName = fileName.substring(0, dotIndex);
                        extension = fileName.substring(dotIndex);
                    }

                    while (targetFile.exists()) {
                        targetFile = new File(subDir, baseName + "_" + fileCounter + extension);
                        fileCounter++;
                    }

                    Log.d("ReceiverThread", "Saving to: " + targetFile.getAbsolutePath());

                    // Receive file content
                    try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                        long totalBytesRead = 0;
                        long lastProgressUpdate = 0;

                        while (totalBytesRead < fileSize && isTransferActive) {
                            int encryptedLength = dataInputStream.readInt();
                            byte[] encryptedChunk = new byte[encryptedLength];
                            dataInputStream.readFully(encryptedChunk);

                            byte[] decryptedChunk = AESUtils.decrypt(encryptedChunk);
                            bufferedOutputStream.write(decryptedChunk);

                            totalBytesRead += decryptedChunk.length;

                            // Update progress less frequently for better performance
                            if (totalBytesRead - lastProgressUpdate > 32768 || totalBytesRead >= fileSize) {
                                int progress = (int) ((totalBytesRead * 100) / fileSize);
                                onProgressUpdate(progress, totalBytesRead, fileSize);
                                lastProgressUpdate = totalBytesRead;
                            }
                        }

                        bufferedOutputStream.flush();
                    }

                    if (!isTransferActive) {
                        // Delete partially received file if transfer was cancelled
                        if (targetFile.exists()) {
                            targetFile.delete();
                        }
                        break;
                    }

                    Log.d("ReceiverThread", "File received successfully: " + fileName);
                }

                if (isTransferActive) {
                    int finalReceivedFiles = receivedFiles;
                    runOnUiThread(() -> {
                        receivingProgressText.setText("Transfer complete!");
                        fileCountText.setText("All " + finalReceivedFiles + " files received successfully");
                    });
                }

            } catch (Exception e) {
                Log.e("ReceiverThreadError", "Error: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    receivingProgressText.setText("Error: " + e.getMessage());
                    fileCountText.setText("Transfer failed");
                });
            } finally {
                try {
                    if (dataInputStream != null) dataInputStream.close();
                    if (bufferedInputStream != null) bufferedInputStream.close();
                    if (client != null) client.close();
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException e) {
                    Log.e("SocketCloseError", "Failed to close socket: " + e.getMessage());
                }
            }
        }).start();
    }

    private String getSubFolderForMimeType(String mimeType) {
        if (mimeType == null) {
            return "Other";
        }
        if (mimeType.startsWith("image/")) {
            return "Images";
        } else if (mimeType.startsWith("video/")) {
            return "Videos";
        } else if (mimeType.startsWith("audio/")) {
            return "Audio";
        } else if (mimeType.startsWith("application/pdf") || mimeType.startsWith("text/") || mimeType.contains("document") || mimeType.contains("msword") || mimeType.contains("officedocument")) {
            return "Documents";
        } else if (mimeType.contains("apk")) {
            return "Apps";
        } else {
            return "Other";
        }
    }

    @Override
    public void onProgressUpdate(int percentage, long bytesTransferred, long totalBytes) {
        runOnUiThread(() -> {
            receivingProgressBar.setProgress(percentage);
            receivingProgressText.setText("Progress: " + percentage + "%");
            receivingSizeText.setText("Transferred: " + FileUtils.humanReadableByteCount(bytesTransferred) + " / " + FileUtils.humanReadableByteCount(totalBytes));
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isTransferActive = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e("ReceiverThread", "Failed to close server socket: " + e.getMessage());
            }
        }
        ProgressUpdaterHolder.clear();
    }
}




