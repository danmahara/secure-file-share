package com.securefileshare.utils;

public class ProgressUpdaterHolder {
    private static ProgressUpdater updater;

    public static void setUpdater(ProgressUpdater u) {
        updater = u;
    }

    public static ProgressUpdater getUpdater() {
        return updater;
    }

    public static void clear() {
        updater = null;
    }
}
