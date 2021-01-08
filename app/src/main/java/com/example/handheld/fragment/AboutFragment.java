package com.example.handheld.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.example.handheld.MainActivity;
import com.example.handheld.R;
import com.example.handheld.SearchActivity;
import com.example.handheld.view.CycleBar;
import com.example.handheld.view.MyListView;

public class AboutFragment extends Fragment {

    private MainActivity activity;
    private MyListView listViewVersion;
    private Button buttonSearch;
    private Switch switchAutoConnect;
    private CycleBar cycleBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        activity = (MainActivity) getActivity();
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        listViewVersion = view.findViewById(R.id.list_view_version);
        listViewVersion.setAdapter(activity.versionListViewAdapter);

        listViewVersion.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    activity.upgrade();
                }
                return false;
            }
        });
        listViewVersion.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    String version = activity.device.getRfidVersion();
                    activity.aboutSettings.setRfidVersion(version);
                    activity.versionListViewAdapter.setData(activity.aboutSettings.getSettings());
                } else if (position == 4) {
                    activity.device.getBattery();
                }
            }
        });

        buttonSearch = view.findViewById(R.id.button_search);
        buttonSearch.setOnClickListener(onClickListener);
        cycleBar = view.findViewById(R.id.cycle_bar);

        switchAutoConnect = view.findViewById(R.id.switch_auto_connect);
        switchAutoConnect.setChecked(activity.mAutoConnect);
        switchAutoConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                activity.setAutoConnect(isChecked);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void setBattery(int percent) {
        cycleBar.setProgress(percent);
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_search:
                    activity.device.disconnect();
                    activity.startActivityForResult(new Intent(activity, SearchActivity.class), activity.REQUEST_BT);
                    break;
            }
        }
    };
}
