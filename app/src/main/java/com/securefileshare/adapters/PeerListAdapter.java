package com.securefileshare.adapters;

import android.net.wifi.p2p.WifiP2pDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.securefileshare.R;

import java.util.List;

public class PeerListAdapter extends RecyclerView.Adapter<PeerListAdapter.PeerViewHolder> {

    public interface OnPeerClickListener {
        void onPeerClick(WifiP2pDevice device);
    }

    private final List<WifiP2pDevice> peerList;
    private final OnPeerClickListener listener;

    public PeerListAdapter(List<WifiP2pDevice> peerList, OnPeerClickListener listener) {
        this.peerList = peerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_peer_device, parent, false);
        return new PeerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
        WifiP2pDevice device = peerList.get(position);
        holder.deviceName.setText(device.deviceName);
        holder.deviceStatus.setText(getDeviceStatus(device.status));

        holder.connectButton.setOnClickListener(v -> listener.onPeerClick(device));
    }

    @Override
    public int getItemCount() {
        return peerList.size();
    }

    public static class PeerViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceStatus;
        Button connectButton;

        public PeerViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            deviceStatus = itemView.findViewById(R.id.deviceStatus);
            connectButton = itemView.findViewById(R.id.connectButton);
        }
    }

    private String getDeviceStatus(int status) {
        switch (status) {
            case WifiP2pDevice.AVAILABLE:
                return "Available";
            case WifiP2pDevice.INVITED:
                return "Invited";
            case WifiP2pDevice.CONNECTED:
                return "Connected";
            case WifiP2pDevice.FAILED:
                return "Failed";
            case WifiP2pDevice.UNAVAILABLE:
                return "Unavailable";
            default:
                return "Unknown";
        }
    }
}
