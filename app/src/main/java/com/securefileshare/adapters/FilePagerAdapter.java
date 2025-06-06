package com.securefileshare.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.securefileshare.fragments.AppsFragment;
import com.securefileshare.fragments.DocumentsFragment;
import com.securefileshare.fragments.ImagesFragment;
import com.securefileshare.fragments.VideosFragment;

public class FilePagerAdapter extends FragmentStateAdapter {

    public FilePagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0: return new AppsFragment();
            case 1: return new ImagesFragment();
            case 2: return new VideosFragment();
            case 3: return new DocumentsFragment();
            default: return new AppsFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // apps, images, videos, docs
    }
}

