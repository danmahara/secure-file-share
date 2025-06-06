package com.securefileshare.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.securefileshare.R;
import com.securefileshare.services.WiFiDirectBroadcastReceiver;

public class ReceiveActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {
    private final IntentFilter intentFilter = new IntentFilter();
    WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);


        // Setup intent filter for WiFi P2P
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Button refreshButton = findViewById(R.id.btnRefresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
            }
        });

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(),null);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, this);

        checkWifiStatus();

        // Here you can handle accepting connections and receiving files
    }

    private void discoverPeers() {
        // Request permissions based on Android version

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 (API 33) and above, request NEARBY_WIFI_DEVICES permission
            if (checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.NEARBY_WIFI_DEVICES}, 100);
            }
        } else {
            // For devices below Android 13, check ACCESS_FINE_LOCATION permission
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }
        }
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);  // Turn on Wi-Fi
            Toast.makeText(this, "Please turn on wifi", Toast.LENGTH_SHORT).show();
        }
//        try {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(ReceiveActivity.this, "Discovery Started", Toast.LENGTH_SHORT).show();
                    Log.d("WifiDirect", "Discovery started");
                }

                @Override
                public void onFailure(int i) {
                    Log.d("WifiDirect", "Discovery failed- code:" + i);
                    Toast.makeText(ReceiveActivity.this, "Discovery failed:" + i, Toast.LENGTH_SHORT).show();
                }
            });
//        } catch (Exception e) {
//            Toast.makeText(this, "Manager not found", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        if (info.groupFormed && info.isGroupOwner) {
            // This device is group owner - wait for incoming files if receive mode
            Toast.makeText(this, "Connected as Group Owner", Toast.LENGTH_SHORT).show();
        } else if (info.groupFormed) {
            // This device is client - send files to group owner
            Toast.makeText(this, "Connected to Peer. Sending files...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, TransferProgressActivity.class);
            intent.putExtra("hostAddress", info.groupOwnerAddress.getHostAddress());
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {

    }

    private void checkWifiStatus() {
        if (manager != null && !wifiManager.isWifiEnabled()) {
            // WiFi is OFF, prompt user
            new AlertDialog.Builder(this).setTitle("WiFi is OFF").setMessage("Your WiFi is currently off. Do you want to turn it on?").setPositiveButton("Open Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                this.startActivity(intent);
            }).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
        } else {
            // WiFi is ON
//            Toast.makeText(this, "WiFi is ON!", Toast.LENGTH_SHORT).show();
            discoverPeers();

        }
    }

}
