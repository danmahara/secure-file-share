package com.securefileshare.utils;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.text.DecimalFormat;

public class FileUtils {

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

    public static String readableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static boolean isApkFile(File file) {
        return file.getName().endsWith(".apk");
    }
}
