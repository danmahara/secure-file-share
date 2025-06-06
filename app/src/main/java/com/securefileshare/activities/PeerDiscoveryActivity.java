package com.securefileshare.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;
import com.securefileshare.adapters.PeerListAdapter;
import com.securefileshare.services.WiFiDirectBroadcastReceiver;

import java.util.ArrayList;
import java.util.List;

public class PeerDiscoveryActivity extends AppCompatActivity implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private final IntentFilter intentFilter = new IntentFilter();
    WifiManager wifiManager;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private final List<WifiP2pDevice> peerList = new ArrayList<>();
    private PeerListAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peer_discovery);

        recyclerView = findViewById(R.id.peerRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PeerListAdapter(peerList, this::connectToPeer);
        recyclerView.setAdapter(adapter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        // Setup intent filter for WiFi P2P
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this, this);

        discoverPeers();

        Button withdrawInvitation = findViewById(R.id.withdraw_invitation);
        Button refresh = findViewById(R.id.refresh);

        withdrawInvitation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                withdrawInvitation();
            }
        });
        refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                discoverPeers();
                peerList.clear();
            }
        });

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
            Toast.makeText(this, "Please turn on wifi.", Toast.LENGTH_SHORT).show();
        }
        try {
            manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(PeerDiscoveryActivity.this, "Discovery Started", Toast.LENGTH_SHORT).show();
                    Log.d("WifiDirect", "Discovery started");
                }

                @Override
                public void onFailure(int i) {
                    Log.d("WifiDirect", "Discovery failed- code:" + i);
                    Toast.makeText(PeerDiscoveryActivity.this, "Discovery failed:" + i, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToPeer(WifiP2pDevice device) {
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC; // Push Button Configuration


        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(PeerDiscoveryActivity.this, "Connection initiated", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int reason) {
                String reasonMessage;
                switch (reason) {
                    case WifiP2pManager.ERROR:
                        reasonMessage = "Internal error - Restart Wi-Fi and try again" + WifiP2pManager.ERROR;
                        break;
                    case WifiP2pManager.P2P_UNSUPPORTED:
                        reasonMessage = "Wi-Fi Direct is not supported on this device";
                        break;
                    case WifiP2pManager.BUSY:
                        reasonMessage = "Framework is busy, try again later";
                        break;
                    default:
                        reasonMessage = "Unknown error";
                        break;
                }

                Log.e("connection_status", "Connection failed: " + reasonMessage);
                Toast.makeText(PeerDiscoveryActivity.this, "Connection failed: " + reasonMessage, Toast.LENGTH_SHORT).show();
            }

        });

        manager.requestConnectionInfo(channel, this);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        peerList.clear();
        peerList.addAll(peers.getDeviceList());
        adapter.notifyDataSetChanged();

        if (peerList.isEmpty()) {
            Toast.makeText(this, "No peers found", Toast.LENGTH_SHORT).show();
        }
    }

//    @Override
//    public void onConnectionInfoAvailable(WifiP2pInfo info) {
//        if (info.groupFormed && info.isGroupOwner) {
//            // Receiver
//            Toast.makeText(this, "Connected as Group Owner", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(this, ReceivingProgressActivity.class);
//            intent.putExtra("port", 8988); // if you use this in ReceivingService or thread
//            startActivity(intent);
//            finish();
//
//        } else if (info.groupFormed) {
//            // Sender
//            Toast.makeText(this, "Connected to Peer. Sending files...", Toast.LENGTH_SHORT).show();
//            Intent intent = new Intent(this, TransferProgressActivity.class);
//
//            // 👇 Pass host address
//            intent.putExtra("host", info.groupOwnerAddress.getHostAddress());
//
//            // 👇 Also pass file_uri (you should already have it from file picker)
//            Uri fileUri = getIntent().getParcelableExtra("file_uri");
//            if (fileUri != null) {
//                intent.putExtra("file_uri", fileUri);
//            }
//
//            intent.putExtra("port", 8988); // default port used for communication
//            startActivity(intent);
//            finish();
//        }
//    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        Log.d("WifiDirect", "Connection info available - groupFormed: " + info.groupFormed
                + ", isGroupOwner: " + info.isGroupOwner);
        try {
            if (info.groupFormed && info.isGroupOwner) {
                // Receiver (Group Owner) case
                Log.d("WifiDirect", "Device is Group Owner, starting ReceivingProgressActivity");
                Toast.makeText(this, "Connected as Group Owner", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, ReceivingProgressActivity.class);
                intent.putExtra("port", 8988);

                // Add flags to clear the activity stack if needed
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish();

            } else if (info.groupFormed) {
                // Sender (Client) case
                Log.d("WifiDirect", "Device is Client, starting TransferProgressActivity");
                Toast.makeText(this, "Connected to Peer. Sending files...", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(this, TransferProgressActivity.class);
                intent.putExtra("host", info.groupOwnerAddress.getHostAddress());

                Uri fileUri = getIntent().getParcelableExtra("file_uri");
                if (fileUri != null) {
                    intent.putExtra("file_uri", fileUri);
                }

                intent.putExtra("port", 8988);

                // Add flags to clear the activity stack if needed
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

                startActivity(intent);
                finish();
            } else {
                Log.d("WifiDirect", "Group not formed yet");
                Toast.makeText(this, "Waiting for group formation...", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("WifiDirect", "Error in onConnectionInfoAvailable: " + e.getMessage());
            Toast.makeText(this, "Connection error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    private void withdrawInvitation() {
        manager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(PeerDiscoveryActivity.this, "Invitation withdrawn", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int i) {
                Toast.makeText(PeerDiscoveryActivity.this, "Invitation withdrawal failed", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
