package com.example.handheld.lib;

public class Demodulator {
    private int mixerG;
    private int ifG;
    private int threshold;

    public int getMixerG() {
        return mixerG;
    }

    public void setMixerG(int mixerG) {
        this.mixerG = mixerG;
    }

    public int getIfG() {
        return ifG;
    }

    public void setIfG(int ifG) {
        this.ifG = ifG;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public void setData(byte[] data) {
        this.mixerG = data[0] & 0xff;
        this.ifG = data[1] & 0xff;
        this.threshold = (data[2] & 0xff) << 8 | (data[3] & 0xff);
    }

    public byte[] getData() {
        byte[] data = new byte[4];
        data[0] = (byte) mixerG;
        data[1] = (byte) ifG;
        data[2] = (byte) (threshold >> 8);
        data[3] = (byte) threshold;
        return data;
    }
}
