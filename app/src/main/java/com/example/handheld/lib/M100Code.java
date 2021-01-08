package com.example.handheld.lib;

public class M100Code {

    /**
     * 帧头
     */
    public static final byte HEADER = (byte) 0xBB;

    /**
     * 帧尾
     */
    public static final byte FOOTER = (byte) 0x7E;

    /**
     * 帧头长度
     */
    public static final int HEADER_LEN = 5;

    /**
     * 命令帧
     */
    public static final byte TYPE_CMD = 0x00;

    /**
     * 响应帧
     */
    public static final byte TYPE_RESP = 0x01;

    /**
     * 通知帧
     */
    public static final byte TYPE_NOTIFY = 0x02;

    /**
     * 获取模块版本
     */
    public static final byte RFID_VERSION = 0x03;

    /**
     * 单次寻卡
     */
    public static final byte INV_ONCE = 0x22;

    /**
     * 多次轮询
     */
    public static final byte INV_MULTIPLE = 0x27;

    /**
     * 停止盘存
     */
    public static final byte INV_STOP = 0x28;

    /**
     * 选定参数
     */
    public static final byte SELECT_PARAM = 0x0C;

    /**
     * 选定标签
     */
    public static final byte SELECT_MODE = 0x12;

    /**
     * 读标签
     */
    public static final byte READ_TAG = 0x39;

    /**
     * 写标签
     */
    public static final byte WRITE_TAG = 0x49;

    /**
     * 锁卡
     */
    public static final byte LOCK_TAG = (byte) 0x82;

    /**
     * 注销卡片
     */
    public static final byte KILL_TAG = (byte) 0x65;

    /**
     * 获取Query
     */
    public static final byte QUERY_GET = (byte) 0x0D;

    /**
     * 设置 Query
     */
    public static final byte QUERY_SET = (byte) 0x0E;

    /**
     * 设置地区标准
     */
    public static final byte REGION_SET = (byte) 0x07;

    /**
     * 获取地区标准
     */
    public static final byte REGION_GET = (byte) 0x08;

    /**
     * 设置信道
     */
    public static final byte CHANNEL_SET = (byte) 0xAB;

    /**
     * 获取信道
     */
    public static final byte CHANNEL_GET = (byte) 0xAA;

    /**
     * 设置跳频
     */
    public static final byte FHSS_SET = (byte) 0xAD;

    /**
     * 获取功率
     */
    public static final byte POWER_GET = (byte) 0xB7;

    /**
     * 设置功率
     */
    public static final byte POWER_SET = (byte) 0xB6;

    /**
     * 设置载波
     */
    public static final byte CW_SET = (byte) 0xB0;

    /**
     * 设置调制解调器参数
     */
    public static final byte DEMODULATOR_SET = (byte) 0xF0;

    /**
     * 保存参数
     */
    public static final byte SAVE_PARAMETERS = (byte) 0x09;
}
