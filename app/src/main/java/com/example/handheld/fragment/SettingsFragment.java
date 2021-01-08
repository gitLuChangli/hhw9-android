package com.example.handheld.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import com.example.handheld.MainActivity;
import com.example.handheld.R;
import com.example.handheld.lib.CommandException;
import com.example.handheld.lib.Query;
import com.example.handheld.view.MyToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SettingsFragment extends Fragment {

    private MainActivity activity;
    private Spinner spinnerPower;
    private Button buttonPowerGet, buttonPowerSet, buttonSetDemodulator, buttonGetQuery, buttonSetQuery, buttonRegionGet, buttonRegionSet, buttonChannelGet, buttonChannelSet, buttonSaveParameters;
    private Spinner spinnerSession, spinnerQ, spinnerTarget, spinnerModulation, spinnerRegion, spinnerChannel, spinnerDemodulator;
    private Switch switchAutoFrequencyHopping;
    private static final int max_power = 26;
    private static final int min_power = 15;

    private static List<Integer> regionValues = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        spinnerPower = view.findViewById(R.id.spinner_power);
        ArrayList<String> power = new ArrayList<>();
        for (int i = max_power; i >= min_power; i--) {
            power.add(String.valueOf(i));
        }
        ArrayAdapter<String> adapterP = new ArrayAdapter<>(activity, R.layout.spinner_item, power);
        spinnerPower.setAdapter(adapterP);
        buttonPowerGet = view.findViewById(R.id.button_get_power);
        buttonPowerSet = view.findViewById(R.id.button_set_power);
        buttonPowerGet.setOnClickListener(onClickListener);
        buttonPowerSet.setOnClickListener(onClickListener);

        spinnerDemodulator = view.findViewById(R.id.spinner_demodulator);
        ArrayAdapter<String> adapterD = new ArrayAdapter<>(activity, R.layout.spinner_item, getResources().getStringArray(R.array.array_demodulator));
        spinnerDemodulator.setAdapter(adapterD);

        buttonSetDemodulator = view.findViewById(R.id.button_set_demodulator);
        buttonSetDemodulator.setOnClickListener(onClickListener);


        ArrayList<String> q = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            q.add("Q" + i);
        }
        ArrayAdapter<String> adapterQ = new ArrayAdapter<>(activity, R.layout.spinner_item, q);

        String[] session = {"S0", "S1", "S2", "S3"};
        ArrayAdapter<String> adapterSession = new ArrayAdapter<>(activity, R.layout.spinner_item, session);
        spinnerSession = view.findViewById(R.id.spinner_session);
        spinnerSession.setAdapter(adapterSession);
        spinnerQ = view.findViewById(R.id.spinner_q);
        spinnerQ.setAdapter(adapterQ);

        String[] target = {"A", "B"};
        ArrayAdapter<String> adapterTarget = new ArrayAdapter<>(activity, R.layout.spinner_item, target);
        spinnerTarget = view.findViewById(R.id.spinner_target);
        spinnerTarget.setAdapter(adapterTarget);

        String[] modulation = {"M1", "M2", "M4", "M8"};
        ArrayAdapter<String> adapterModulation = new ArrayAdapter<>(activity, R.layout.spinner_item, modulation);
        spinnerModulation = view.findViewById(R.id.spinner_m);
        spinnerModulation.setAdapter(adapterModulation);
        buttonGetQuery = view.findViewById(R.id.button_get_query);
        buttonSetQuery = view.findViewById(R.id.button_set_query);
        buttonGetQuery.setOnClickListener(onClickListener);
        buttonSetQuery.setOnClickListener(onClickListener);

        spinnerRegion = view.findViewById(R.id.spinner_region);
        ArrayAdapter<String> adapterRegion = new ArrayAdapter<>(activity, R.layout.spinner_item, getResources().getStringArray(R.array.array_region));
        spinnerRegion.setAdapter(adapterRegion);

        Integer[] region_value = {1, 4, 2, 3, 6};
        regionValues = Arrays.asList(region_value);

        spinnerChannel = view.findViewById(R.id.spinner_channel);
        buttonRegionGet = view.findViewById(R.id.button_region_get);
        buttonRegionSet = view.findViewById(R.id.button_region_set);
        buttonChannelGet = view.findViewById(R.id.button_channel_get);
        buttonChannelSet = view.findViewById(R.id.button_channel_set);
        buttonRegionGet.setOnClickListener(onClickListener);
        buttonRegionSet.setOnClickListener(onClickListener);
        buttonChannelGet.setOnClickListener(onClickListener);
        buttonChannelSet.setOnClickListener(onClickListener);

        switchAutoFrequencyHopping = view.findViewById(R.id.switch_auto_fh);
        switchAutoFrequencyHopping.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    activity.device.setFrequencyHopping(isChecked);
                    activity.beep.playOk();
                    MyToast.show(activity, R.string.toast_set_success);
                } catch (Exception e) {
                    e.printStackTrace();
                    activity.beep.playError();
                    MyToast.show(activity, R.string.toast_set_failed);
                }
            }
        });

        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                initChannelTable(regionValues.get(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        buttonSaveParameters = view.findViewById(R.id.button_save_parameters);
        buttonSaveParameters.setOnClickListener(onClickListener);

        setEnable(false);
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.activity = (MainActivity) context;
    }

    public void getSettings() {
        try {
            getPower();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            getQuery();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            getRegion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.button_get_power:
                    try {
                        getPower();
                        MyToast.show(activity, R.string.toast_get_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_get_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_set_power:
                    int power = max_power - spinnerPower.getSelectedItemPosition();
                    try {
                        activity.device.setPower(power);
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_set_demodulator:
                    int mixerG = spinnerDemodulator.getSelectedItemPosition();
                    try {
                        activity.device.setDemodulator(mixerG);
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_get_query:
                    try {
                        getQuery();
                        MyToast.show(activity, R.string.toast_get_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_get_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_set_query:
                    Query query = new Query();
                    query.setSession(spinnerSession.getSelectedItemPosition());
                    query.setQ(spinnerQ.getSelectedItemPosition());
                    query.setTarget(spinnerTarget.getSelectedItemPosition());
                    query.setM(spinnerModulation.getSelectedItemPosition());
                    query.setDR(1);
                    query.setTR(0);
                    query.setSel(0);
                    try {
                        activity.device.setQuery(query);
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_region_get:
                    try {
                        getRegion();
                        MyToast.show(activity, R.string.toast_get_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_get_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_region_set:
                    try {
                        activity.device.setRegion(regionValues.get(spinnerRegion.getSelectedItemPosition()));
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_channel_get:
                    try {
                        getChannel();
                        MyToast.show(activity, R.string.toast_get_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_get_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_channel_set:
                    try {
                        activity.device.setChannel(spinnerChannel.getSelectedItemPosition());
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                case R.id.button_save_parameters:
                    try {
                        activity.device.saveParameters();
                        MyToast.show(activity, R.string.toast_set_success);
                        activity.beep.playOk();
                    } catch (CommandException e) {
                        e.printStackTrace();
                        MyToast.show(activity, R.string.toast_set_failed);
                        activity.beep.playError();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private void getPower() throws CommandException {
        int power = activity.device.getPower();
        if (power <= max_power) {
            spinnerPower.setSelection(max_power - power);
        }
    }

    private void getQuery() throws CommandException {
        Query query = activity.device.getQuery();
        if (query.getQ() >= 0 && query.getQ() <= 15) {
            spinnerQ.setSelection(query.getQ());
        }
        if (query.getTarget() >= 0 && query.getTarget() <= 1) {
            spinnerTarget.setSelection(query.getTarget());
        }
        if (query.getM() >= 0 && query.getM() <= 3) {
            spinnerModulation.setSelection(query.getM());
        }

        if (query.getSession() >= 0 && query.getSession() <= 3) {
            spinnerSession.setSelection(query.getSession());
        }
    }

    private void getRegion() throws CommandException {
        int region = activity.device.getRegion();
        int index = regionValues.indexOf(region);
        if (index > -1) {
            spinnerRegion.setSelection(index);

            initChannelTable(region);
        }
    }

    private void getChannel() throws CommandException {
        int channel = activity.device.getChannel();
        if (channel < spinnerChannel.getCount()) {
            spinnerChannel.setSelection(channel);
        }
    }

    private void initChannelTable(int region) {
        double min = 0;
        double d = 0;
        int cnt = 0;
        if (region == 1) {
            min = 920.125;
            d = 0.25;
            cnt = 20;
        } else if (region == 2) {
            min = 902.25;
            d = 0.5;
            cnt = 52;
        } else if (region == 3) {
            min = 865.1;
            d = 0.2;
            cnt = 15;
        } else if (region == 4) {
            min = 840.125;
            d = 0.25;
            cnt = 20;
        } else if (region == 6) {
            min = 917.1;
            d = 0.2;
            cnt = 30;
        }

        List<String> channels = new ArrayList<>();

        for (int i = 0; i < cnt; i++) {
            channels.add((min + d * i) + "");
        }

        ArrayAdapter<String> adapterChannels = new ArrayAdapter<>(activity, R.layout.spinner_item, channels);
        spinnerChannel.setAdapter(adapterChannels);
    }

    public void setEnable(boolean enable) {
        buttonPowerGet.setEnabled(enable);
        buttonPowerSet.setEnabled(enable);
        buttonGetQuery.setEnabled(enable);
        buttonSetQuery.setEnabled(enable);
        buttonRegionGet.setEnabled(enable);
        buttonRegionSet.setEnabled(enable);
        buttonChannelSet.setEnabled(enable);
        buttonChannelGet.setEnabled(enable);
        switchAutoFrequencyHopping.setEnabled(enable);
        buttonSaveParameters.setEnabled(enable);
        buttonSetDemodulator.setEnabled(enable);
    }
}
