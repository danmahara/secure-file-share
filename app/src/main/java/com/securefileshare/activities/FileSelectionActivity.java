package com.securefileshare.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.securefileshare.R;
import com.securefileshare.adapters.FilePagerAdapter;
import com.securefileshare.fragments.AppsFragment;
import com.securefileshare.fragments.DocumentsFragment;
import com.securefileshare.fragments.ImagesFragment;
import com.securefileshare.fragments.VideosFragment;
import com.securefileshare.utils.SelectedFileManager;

import java.io.File;
import java.util.ArrayList;

public class FileSelectionActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button sendButton;
    private FilePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_file_selection);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // For Android 13 (API 33) and above, request NEARBY_WIFI_DEVICES permission
//            if (checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{android.Manifest.permission.NEARBY_WIFI_DEVICES}, 100);
//            }
//        }
//        requestLocationPermission();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ArrayList<String> permissionsToRequest = new ArrayList<>();

            if (checkSelfPermission(Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }

            if (!permissionsToRequest.isEmpty()) {
                requestPermissions(permissionsToRequest.toArray(new String[0]), 100);
            }
        } else {
            // For Android 12 and below, only request location
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        sendButton = findViewById(R.id.sendButton);

        pagerAdapter = new FilePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Apps");
                    break;
                case 1:
                    tab.setText("Images");
                    break;
                case 2:
                    tab.setText("Videos");
                    break;
                case 3:
                    tab.setText("Docs");
                    break;
            }
        }).attach();

        sendButton.setOnClickListener(v -> {
            ArrayList<File> selectedFiles = new ArrayList<>();

            selectedFiles.addAll(AppsFragment.getSelectedFiles());
            selectedFiles.addAll(ImagesFragment.getSelectedImages());
            selectedFiles.addAll(VideosFragment.getSelectedVideos());
            selectedFiles.addAll(DocumentsFragment.getSelectedDocuments());

            if(!selectedFiles.isEmpty()){
                SelectedFileManager.setFiles(selectedFiles);
                startActivity(new Intent(this, PeerDiscoveryActivity.class));

            }else{
                Toast.makeText(this, "Select at least one file", Toast.LENGTH_SHORT).show();
            }

        });
    }
    private void requestLocationPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            }
        }
    }
}
