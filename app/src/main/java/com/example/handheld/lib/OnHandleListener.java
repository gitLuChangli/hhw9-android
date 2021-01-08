package com.example.handheld.lib;

public interface OnHandleListener {

    /**
     * 上传手柄状态：按下
     */
    void onDown(int key);

    /**
     * 上传手柄状态：松开
     */
    void onUp(int key);
}
