package com.example.handheld.lib;

public interface OnUHFListener {

    void onInventory(byte[] pc, byte[] epc, int rssi);

    void onInventoryStop();
}
