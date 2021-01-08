package com.example.handheld.lib;

public class CMD {

    /**
     * 固件版本
     */
    public static final byte VERSION = 0x20;


    /**
     * 剩余电量
     */
    public static final byte BATTERY_DATA = 0x21;

    /**
     * 扫码
     */
    public static final byte BARCODE_SCAN = (byte) 0x22;

    /**
     * 进入休眠模式
     */
    public static final byte ENTRY_SLEEP = 0x23;

    /**
     * 按键按下
     */
    public static final byte KEY_DATA_UP = 0x31;

    /**
     * 按键松开
     */
    public static final byte KEY_DATA_DOWN = 0x30;


    /**
     * 升级封包大小
     */
    public static final int UPGRADE_SIZE = 64;

    /**
     * 升级指令
     */
    public static final byte UPDATE = 0x43;

    /**
     * RFID
     */
    public static final byte UHF_DATA = 0x1A;

    /**
     * 读取命令超时
     */
    public static final int TIMEOUT = 1000;

    /**
     * 封包头
     */
    public static final byte HEADER = (byte) 0x1B;

    /**
     * 封包头长度
     */
    public static final int HEADER_LENGTH = 5;

}
