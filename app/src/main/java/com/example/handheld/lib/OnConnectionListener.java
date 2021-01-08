package com.example.handheld.lib;

public interface OnConnectionListener {

    void onConnecting();

    void onConnected();

    void onFailed();

    void onDisconnected();
}
