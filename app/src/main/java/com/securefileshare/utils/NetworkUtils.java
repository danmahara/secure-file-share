package com.securefileshare.utils;

import android.content.Context;
import android.net.wifi.WifiManager;

public class NetworkUtils {
    public static void enableWifi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }
}
