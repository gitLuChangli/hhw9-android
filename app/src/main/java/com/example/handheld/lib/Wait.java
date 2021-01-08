package com.example.handheld.lib;

public class Wait {

    private Object object;

    private boolean notified;

    private byte[] data;

    private int length;

    private int cmd;

    public Wait() {
        this.object = new Object();
    }

    public Object getObject() {
        return object;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getCmd() {
        return cmd;
    }

    public void setCmd(int cmd) {
        this.cmd = cmd;
    }
}
