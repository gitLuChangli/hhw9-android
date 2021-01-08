package com.example.handheld.lib;

import java.util.Iterator;
import java.util.List;

public class Common {

    public synchronized static byte bcc(byte[] data, int offset, int count) {
        byte b = 0;
        for (int i = 0; i < count; i++) {
            b ^= data[offset + i];
        }
        return b;
    }

    private synchronized static String hex2Word(byte b) {
        return ("" + "0123456789ABCDEF".charAt(0x0f & b >> 4) + "0123456789ABCDEF"
                .charAt(b & 0x0f));
    }

    public synchronized static String bytes2String(byte[] buf, int offset,
                                                   int size) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < size; i++) {
            sb.append(hex2Word(buf[offset + i]));
            if (i < (buf.length - 1))
                sb.append(' ');
        }
        return sb.toString();
    }

    public synchronized static String bytes2String2(byte[] data, int offset, int length) {
        if (data == null) return null;
        if (data.length < offset + length) return null;
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i++) {
            sb.append(hex2Word(data[offset + i]));
        }
        return sb.toString();
    }

    static int hexToInt(char ch) {
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        throw new IllegalArgumentException(String.valueOf(ch));
    }

    public synchronized static void hexStr2Bytes(String src, byte[] desc, int offset, int max) {
        int m = 0, n = 0;
        int l;
        src = src.replaceAll(" ", "");
        l = src.length() / 2;
        if (l > max)
            l = max;
        String str1;
        for (int i = 0; i < l; i++) {
            m = i * 2 + 1;
            n = m + 1;
            str1 = "0x" + src.substring(i * 2, m) + src.substring(m, n);
            try {
                desc[offset + i] = Integer.decode(str1).byteValue();
            } catch (Exception ex) {
            }
        }
    }

    public synchronized static void arrayCopy(byte[] desBuf, byte[] srcBuf, int desOffset, int srcOffset, int count) {
        for (int i = 0; i < count; i++) {
            desBuf[desOffset + i] = srcBuf[srcOffset + i];
        }
    }

    public synchronized static int byte2Int(byte b) {
        return (int) b & 0xff;
    }

    public synchronized static int crc(byte[] data, int offset, int size) {
        int i;
        int j = 0;
        int crc = 0xffff;
        while (size-- != 0) {
            for (i = 0x80; i != 0; i /= 2) {
                if ((crc & 0x8000) != 0) {
                    crc *= 2;
                    crc ^= 0x1021;
                } else {
                    crc *= 2;
                }
                if ((byte2Int(data[offset + j]) & i) != 0) {
                    crc ^= 0x1021;
                }
            }
            j++;
        }
        return crc & 0xffff;
    }

    public synchronized static short htons(short in) {
        short a = (short) ((in & 0xff) << 8);
        short b = (short) ((in & 0xff00) >> 8);

        return (short) (a | b);
    }

    public synchronized static short bytes2Short(byte[] data, int offset) {
        short num = 0;
        for (int i = offset; i < offset + 2; i++) {
            num <<= 8;
            num |= (data[i] & 0xff);
        }
        return num;
    }

    public synchronized static int crc_polynomial(byte b, int acc) {
        int acc_ = acc;
        acc_ = (acc_ >> 8 & 0xff) | (acc_ << 8);
        acc_ ^= ((int) b < 0 ? (int) b + 256 : (int) b);
        acc_ ^= ((acc_ & 0xff) >> 4 & 0xff);
        acc_ ^= ((acc_ << 8) << 4 & 0xffff);
        acc_ ^= (((acc_ & 0xff) << 4) << 1 & 0xffff);
        return acc_;
    }

    public synchronized static int crc_sum(byte[] data, int offset, int length, int crc) {
        short i;
        int crc_ = crc;
        for (i = 0; i < length; i++) {
            crc_ = crc_polynomial(data[i + offset], crc_);
        }
        return crc_ & 0xffff;
    }

    public synchronized static byte[] list2Bytes(List<Byte> list) {
        if (list == null || list.size() == 0) return null;
        byte[] bytes = new byte[list.size()];
        int i = 0;
        Iterator<Byte> iterator = list.iterator();
        while (iterator.hasNext()) {
            bytes[i] = iterator.next();
            i++;
        }
        return bytes;
    }

    public synchronized static byte[] hex2Bytes(String hex) {
        String h = hex.replace(" ", "");
        int length = h.length() / 2;
        byte[] data = new byte[length];
        String str1;
        int offset;
        int end;
        for (int i = 0; i < length; i++) {
            offset = i * 2 + 1;
            end = offset + 1;
            str1 = "0x" + h.substring(i * 2, offset) + h.substring(offset, end);
            try {
                data[i] = Integer.decode(str1).byteValue();
            } catch (Exception ex) {

            }
        }
        return data;
    }

    /**
     * 计算温度标签
     * @param ts
     * @param offset
     * @return
     */
    public synchronized static double calculateT(byte[] ts, int offset) {
        // 12位有效
        int C = ((ts[offset] & 0xff) << 8) | (ts[offset + 1] & 0xff);
        // System.out.println("C=" + C);
        int CODE1 = (((ts[offset + 4] & 0xff) & 0xff) << 4) | ((ts[offset + 5] & 0xff) >> 4) & 0xf;
        // System.out.println("C1=" + CODE1);
        int TEMP1 = (ts[offset + 5] & 0xff) &0x0f;
        TEMP1 <<= 7;
        TEMP1 = TEMP1 + ((ts[offset + 6] & 0xff) >> 1 & 0x7f);
        // System.out.println("T1=" + TEMP1);
        int CODE2 = (ts[offset + 6] & 0xff) & 0x01;
        CODE2 <<= 8;
        CODE2 += ts[offset + 7] & 0xff;
        CODE2 <<= 3;
        CODE2 += ((ts[offset + 8] & 0xff) >> 5) & 7;
        // System.out.println("C2=" + CODE2);
        int TEMP2 = (ts[offset + 8] & 0x1f);
        TEMP2 <<= 6;
        TEMP2 += ((ts[offset + 9] & 0xff) >> 2 & 0x3f);
        // System.out.println("T2=" + TEMP2);
        double t = ((TEMP2 - TEMP1) * (C - CODE1) / (CODE2 - CODE1) + TEMP1 - 800) / 10f;
        // System.out.println("T=" + t);
        return t;
    }

    public synchronized static long bytes2Long(byte[] data, int offset) {
        if (data == null || data.length < offset + 4) return -1;
        long v = (data[offset + 0] & 0xff) << 24 | (data[offset + 1] & 0xff) << 16 | (data[offset + 2] & 0xff) << 8 | data[offset + 3] & 0xff;
        return v;
    }

    public synchronized static byte checkSum(byte[] data, int offset, int length) {
        if (data == null || data.length < offset + length) {
            return -1;
        }

        int sum = 0;
        for (int i = offset; i < offset + length; i++) {
            sum += (data[i] & 0xff);
        }
        return (byte) sum;
    }
}
