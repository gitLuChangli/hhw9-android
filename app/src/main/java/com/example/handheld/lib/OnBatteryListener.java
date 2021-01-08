package com.example.handheld.lib;

public interface OnBatteryListener {

    /**
     * 上传电量百分比
     *
     * @param battery
     */
    void onBattery(int battery);
}
