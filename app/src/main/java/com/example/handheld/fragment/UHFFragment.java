package com.example.handheld.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.handheld.R;
import com.example.handheld.adapter.FragmentAdapter;

import java.util.ArrayList;
import java.util.List;

public class UHFFragment extends Fragment {

    private ViewPager viewPager;
    private TabLayout tabLayout;

    private InventoryFragment inventory;
    private SettingsFragment settings;
    private AccessFragment access;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = getLayoutInflater().inflate(R.layout.fragment_uhf, container, false);

        viewPager = view.findViewById(R.id.view_pager_uhf);

        List<Fragment> fragments = new ArrayList<>();
        inventory = new InventoryFragment();
        settings = new SettingsFragment();
        access = new AccessFragment();
        fragments.add(inventory);
        fragments.add(settings);
        fragments.add(access);
        viewPager.setOffscreenPageLimit(3);

        FragmentAdapter fragmentAdapter = new FragmentAdapter(getChildFragmentManager(), fragments);
        viewPager.setAdapter(fragmentAdapter);

        tabLayout = view.findViewById(R.id.tab_main);
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);
        viewPager.addOnPageChangeListener(onPageChangeListener);
        return view;
    }

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {
            tabLayout.getTabAt(position).select();
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    };

    public void setInventoryReady(boolean ready) {
    }

    public void setUnique(int unique) {
        if (inventory != null) {
            inventory.setUnique(unique);
        }
    }

    public void setSpeed(long speed) {
        if (inventory != null) {
            inventory.setSpeed(speed);
        }
    }

    public void setEnable(boolean enable) {
        if (inventory != null) {
            inventory.setEnable(enable);
        }
        if (settings != null) {
            settings.setEnable(enable);
        }
        if (access != null) {
            access.setEnable(enable);
        }
    }

    public void init() {

    }

    public void setSelected(String tag) {
        if (access != null) {
            access.setSelected(tag);
        }
    }

    public void doInventory() {
        if (inventory != null) {
            inventory.inventory();
        }
    }
}
