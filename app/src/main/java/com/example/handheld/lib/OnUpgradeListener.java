package com.example.handheld.lib;

public interface OnUpgradeListener {

    void onUpgrade(int progress);

    void onUpgradeResult(boolean success);
}
