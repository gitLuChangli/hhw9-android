package com.example.handheld.lib;

public class SelectParam {

    private int target;

    private int action;

    private int memoryBank;

    private int ptr;

    public int maskLength;

    public boolean truncate;

    public byte[] mask;

    public int getTarget() {
        return target;
    }

    public void setTarget(int target) {
        this.target = target;
    }

    public int getAction() {
        return action;
    }

    public void setAction(int action) {
        this.action = action;
    }

    public int getMemoryBank() {
        return memoryBank;
    }

    public void setMemoryBank(int memoryBank) {
        this.memoryBank = memoryBank;
    }

    public int getPtr() {
        return ptr;
    }

    public void setPtr(int ptr) {
        this.ptr = ptr;
    }

    public int getMaskLength() {
        return maskLength;
    }

    public void setMaskLength(int maskLength) {
        this.maskLength = maskLength;
    }

    public boolean isTruncate() {
        return truncate;
    }

    public void setTruncate(boolean truncate) {
        this.truncate = truncate;
    }

    public byte[] getMask() {
        return mask;
    }

    public void setMask(byte[] mask) {
        this.mask = mask;
    }

    public byte[] getData() {
        byte[] data = new byte[7 + maskLength];
        data[0] = (byte)( ((target & 0x07) << 5) | ((action & 0x07) << 2) | (memoryBank & 0x03) );
        int p = ptr * 8;
        data[1] = (byte) (p >> 24);
        data[2] = (byte) (p >> 16);
        data[3] = (byte) (p >> 8);
        data[4] = (byte) p;
        data[5] = (byte) (maskLength * 8);
        data[6] = truncate ? (byte) 0x80 : (byte) 0x00;
        if (maskLength > 0) {
            Common.arrayCopy(data, mask, 7, 0, maskLength);
        }
        return data;
    }

    public void setData(byte[] data) {
        target = (data[0] >> 5) & 0x07;
        action = (data[0] >> 2) & 0x07;
        memoryBank = data[0] & 0x02;
        ptr = (data[1] & 0xff) << 24 | (data[2] & 0xff << 16) | (data[3] & 0xff << 8) | (data[4] & 0xff);
        maskLength = (data[5] & 0xff) / 8;
        truncate = (data[6] & 0xff) == 0x80;
        Common.arrayCopy(mask, data, 0, 7,maskLength);
    }
}
