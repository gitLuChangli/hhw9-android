package com.example.handheld.utils;

import android.content.Context;

import com.example.handheld.R;
import com.example.handheld.bean.VersionBean;

import java.util.ArrayList;

public class AboutSettings {

    private static AboutSettings about;

    private ArrayList<VersionBean> settings = new ArrayList<>();

    public static AboutSettings getInstance(Context context) {
        synchronized (AboutSettings.class) {
            if (about == null) {
                about = new AboutSettings(context);
            }
        }
        return about;
    }

    private AboutSettings(Context context) {
        settings.clear();
        String[] items = context.getResources().getStringArray(R.array.array_about);
        for (String item : items) {
            settings.add(new VersionBean(item));
        }
    }

    public void setMac(String mac) {
        settings.get(3).setValue(mac);
    }

    public void setFirmVersion(String version) {
        settings.get(0).setValue(version);
    }

    public void setRfidVersion(String version) {
        settings.get(1).setValue(version);
    }

    public void setAppVersion(String version) {
        settings.get(2).setValue(version);
    }

    public void setBattery(int battery) {
        settings.get(4).setValue(battery + "%");
    }

    public ArrayList<VersionBean> getSettings() {
        return settings;
    }

    public void setProgress(int progress) {
        settings.get(0).setValue(progress + "%");
    }
}
