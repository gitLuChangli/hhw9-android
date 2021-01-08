package com.example.handheld.lib;

public class UpgradeBean {

    private int index;
    private int times;
    private int length;
    private int last;
    private byte[] data;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getTimes() {
        return times;
    }

    public void setTimes(int times) {
        this.times = times;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLast() {
        return last;
    }

    public void setLast(int last) {
        this.last = last;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setNext() {
        ++index;
    }

    public boolean hasNext() {
        return index < times - 1;
    }

    public byte[] getNext() {
        byte[] send;
        if (index < times - 1) {
            send = new byte[length];
            Common.arrayCopy(send, data, 0, length * index, length);
        } else {
            send = new byte[last];
            Common.arrayCopy(send, data, 0, length * index, last);
        }
        return send;
    }

    public int getProgress() {
        return index * 100 / times;
    }
}
