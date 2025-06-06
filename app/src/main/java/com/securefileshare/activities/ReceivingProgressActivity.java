    package com.securefileshare.activities;

    import android.os.Build;
    import android.os.Bundle;
    import android.widget.ProgressBar;
    import android.widget.TextView;

    import androidx.appcompat.app.AppCompatActivity;

    import com.securefileshare.R;
    import com.securefileshare.utils.AESUtils;
    import com.securefileshare.utils.FileUtils;
    import com.securefileshare.utils.ProgressUpdater;
    import com.securefileshare.utils.ProgressUpdaterHolder;

    import java.io.File;
    import java.io.FileOutputStream;
    import java.io.InputStream;
    import java.net.ServerSocket;
    import java.net.Socket;

    public class ReceivingProgressActivity extends AppCompatActivity implements ProgressUpdater {

        private ProgressBar progressBar;
        private TextView progressText, sizeText;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_receiving_progress);

            progressBar = findViewById(R.id.receivingProgressBar);
            progressText = findViewById(R.id.receivingProgressText);
            sizeText = findViewById(R.id.receivingSizeText);

            ProgressUpdaterHolder.setUpdater(this); // ✅ required for progress update

            startReceiverThread(); // 👇 extracted for clarity


        }

        private void startReceiverThread() {
            new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(8988);
                    Socket client = serverSocket.accept();

                    InputStream inputStream = client.getInputStream();

                    // STEP 1: Read file name
                    StringBuilder lengthBuilder = new StringBuilder();
                    int ch;
                    while ((ch = inputStream.read()) != -1 && ch != '\n') {
                        lengthBuilder.append((char) ch);
                    }
                    int nameLength = Integer.parseInt(lengthBuilder.toString());
                    byte[] nameBytes = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        nameBytes = inputStream.readNBytes(nameLength);
                    }
                    String fileName = new String(nameBytes, "UTF-8");

                    // STEP 2: Read MIME type
                    lengthBuilder.setLength(0);
                    while ((ch = inputStream.read()) != -1 && ch != '\n') {
                        lengthBuilder.append((char) ch);
                    }
                    int mimeLength = Integer.parseInt(lengthBuilder.toString());
                    byte[] mimeBytes = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mimeBytes = inputStream.readNBytes(mimeLength);
                    }
                    String mimeType = new String(mimeBytes, "UTF-8");

                    // STEP 3: Create correct folder using MIME type and file name
                    File targetFile = FileUtils.getTargetFile(getApplicationContext(), fileName, mimeType);
                    FileOutputStream fileOutputStream = new FileOutputStream(targetFile);

                    long totalBytes = 1024 * 1024 * 10; // placeholder (10MB)
                    long bytesReadTotal = 0;

                    byte[] buffer = new byte[4096];
                    int bytesRead;

                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byte[] actualData = new byte[bytesRead];
                        System.arraycopy(buffer, 0, actualData, 0, bytesRead);

                        byte[] decrypted = AESUtils.decrypt(actualData);
                        fileOutputStream.write(decrypted);

                        bytesReadTotal += bytesRead;
                        int progress = (int) ((bytesReadTotal * 100) / totalBytes);
                        onProgressUpdate(progress, bytesReadTotal, totalBytes);
                    }

                    fileOutputStream.close();
                    inputStream.close();
                    client.close();
                    serverSocket.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        }


        @Override
        public void onProgressUpdate(int percentage, long bytesTransferred, long totalBytes) {
            runOnUiThread(() -> {
                progressBar.setProgress(percentage);
                progressText.setText("Progress: " + percentage + "%");
                sizeText.setText("Transferred: " + bytesTransferred + "/" + totalBytes + " bytes");
            });
        }
        @Override
        protected void onDestroy() {
            super.onDestroy();
            ProgressUpdaterHolder.clear();
        }

    }