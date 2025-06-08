//package com.securefileshare.utils;
//
//import android.content.Context;
//import android.net.Uri;
//
//import java.io.File;
//import java.text.DecimalFormat;
//
//public class FileUtils {
//
//    public static File getTargetFile(Context context, String fileName, String mimeType) {
//        String folderName = "Others";
//
//        if (mimeType != null) {
//            if (mimeType.startsWith("audio")) {
//                folderName = "Music";
//            } else if (mimeType.startsWith("image")) {
//                folderName = "Images";
//            } else if (mimeType.startsWith("video")) {
//                folderName = "Videos";
//            } else if (mimeType.equals("application/vnd.android.package-archive")) {
//                folderName = "Apps";
//            } else if (mimeType.equals("application/pdf")) {
//                folderName = "Documents";
//            }
//        }
//
//        File dir = new File(context.getExternalFilesDir(null), folderName);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        return new File(dir, fileName);
//    }
//
//
//    public static String getFileNameFromUri(Context context, Uri uri) {
//        String result = null;
//        if (uri.getScheme().equals("content")) {
//            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
//                if (cursor != null && cursor.moveToFirst()) {
//                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
//                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
//                }
//            }
//        }
//        if (result == null) {
//            result = uri.getLastPathSegment();
//        }
//        return result;
//    }
//
//    public static String readableFileSize(long size) {
//        if (size <= 0) return "0 B";
//        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
//        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
//        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
//    }
//
//    public static boolean isApkFile(File file) {
//        return file.getName().endsWith(".apk");
//    }
//}


package com.securefileshare.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;

public class FileUtils {

    public static String getDeviceIpAddress(WifiManager wifiManager) {
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    // In FileUtils.java
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public static String humanReadableByteCount(long bytes) {
        int unit = 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(unit, exp), pre);
    }

    public static File getTargetFile(Context context, String fileName, String mimeType) {
        String folderName = "Others";

        if (mimeType != null) {
            if (mimeType.startsWith("audio")) {
                folderName = "Music";
            } else if (mimeType.startsWith("image")) {
                folderName = "Images";
            } else if (mimeType.startsWith("video")) {
                folderName = "Videos";
            } else if (mimeType.equals("application/vnd.android.package-archive")) {
                folderName = "Apps";
            } else if (mimeType.equals("application/pdf")) {
                folderName = "Documents";
            }
        }

        File dir = new File(context.getExternalFilesDir(null), folderName);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new File(dir, fileName);
    }


//    public static File getTargetFile(Context context, String fileName, String mimeType) {
//
//        String folderName = "Others";
//
//        if (mimeType != null) {
//            if (mimeType.startsWith("audio")) {
//                folderName = "Music";
//            } else if (mimeType.startsWith("image")) {
//                folderName = "Images";
//            } else if (mimeType.startsWith("video")) {
//                folderName = "Videos";
//            } else if (mimeType.equals("application/vnd.android.package-archive")) {
//                folderName = "Apps";
//            } else if (mimeType.equals("application/pdf")) {
//                folderName = "Documents";
//            }
//        }
//
//        File dir = new File(context.getExternalFilesDir(null), folderName);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        return new File(dir, fileName);
//    }

    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    public static File getUniqueFileName(File directory, String fileName) {
        File file = new File(directory, fileName);

        // If file doesn't exist, return it as is
        if (!file.exists()) {
            return file;
        }

        // Extract name and extension
        String name = fileName;
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            name = fileName.substring(0, dotIndex);
            extension = fileName.substring(dotIndex);
        }

        // Find unique name by adding numbers
        int counter = 1;
        while (file.exists()) {
            String newFileName = name + "(" + counter + ")" + extension;
            file = new File(directory, newFileName);
            counter++;
        }

        return file;
    }

    // Add this utility method to FileUtils.java
    public static boolean isOnSameSubnet(String ip1, String ip2) {
        try {
            String[] parts1 = ip1.split("\\.");
            String[] parts2 = ip2.split("\\.");
            return parts1[0].equals(parts2[0]) &&
                    parts1[1].equals(parts2[1]) &&
                    parts1[2].equals(parts2[2]);
        } catch (Exception e) {
            return false;
        }
    }

    private static String insertSuffix(String filename, String suffix) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex != -1) {
            return filename.substring(0, dotIndex) + suffix + filename.substring(dotIndex);
        } else {
            return filename + suffix;
        }
    }

    public static String getMimeType(String filePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension != null) {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        }
        return null;
    }
}