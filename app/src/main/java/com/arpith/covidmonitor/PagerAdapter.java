package com.arpith.covidmonitor;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class PagerAdapter extends FragmentPagerAdapter {
    private final List<String> mFragmentTitleList = new ArrayList<>();
    private final List<Fragment> mFragmentList = new ArrayList<>();
    private Context context;

    public PagerAdapter(@NonNull FragmentManager fm, int numberOfTabs) {
        super(fm, numberOfTabs);
        this.context = context;
    }

    public void addFragment(Fragment fragment, String title){
        mFragmentList.add(fragment);
        mFragmentTitleList.add(title);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position){
        return mFragmentTitleList.get(position);
    }
}
