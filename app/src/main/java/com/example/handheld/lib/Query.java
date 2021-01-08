package com.example.handheld.lib;

public class Query {

    private int DR;
    private int M;
    private int TR;
    private int Sel;
    private int Session;
    private int Target;
    private int Q;

    public int getDR() {
        return DR;
    }

    public void setDR(int DR) {
        this.DR = DR;
    }

    public int getM() {
        return M;
    }

    public void setM(int m) {
        M = m;
    }

    public int getTR() {
        return TR;
    }

    public void setTR(int TR) {
        this.TR = TR;
    }

    public int getSel() {
        return Sel;
    }

    public void setSel(int sel) {
        Sel = sel;
    }

    public int getSession() {
        return Session;
    }

    public void setSession(int session) {
        Session = session;
    }

    public int getTarget() {
        return Target;
    }

    public void setTarget(int target) {
        Target = target;
    }

    public int getQ() {
        return Q;
    }

    public void setQ(int q) {
        Q = q;
    }

    public void setBytes(byte[] data) {
        if (data == null) {
            throw new NullPointerException("query null");
        }

        this.DR = data[0] >> 7 & 1;
        this.M = data[0] >> 5 & 0x03;
        this.TR = data[0] >> 4 & 0x01;
        this.Sel = data[0] >> 2 & 0x03;
        this.Session = data[0] & 0x03;
        this.Target = data[1] >> 7;
        this.Q = data[1] >> 3 & 0x0f;
    }

    public byte[] getBytes() {
        byte[] data = new byte[2];
        data[0] = (byte) Session;
        data[0] |= (Sel << 2);
        data[0] |= (TR << 4);
        data[0] |= (M << 5);
        data[0] |= (DR << 7);
        data[1] = (byte) (Q << 3);
        data[1] |= Target << 7;
        return data;
    }
}
