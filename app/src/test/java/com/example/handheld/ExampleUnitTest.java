package com.example.handheld;

import com.example.handheld.lib.Common;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test() {
        int v = 4;
        int a = 0;
        byte b = (byte) ( (v & 0x0f) << 4 | (a & 0x0f));
        System.out.println(b);
    }

    @Test
    public void testCheckSum() {
        byte[] data = {(byte) 0xBB, 0x00, 0x03, 0x00, 0x01, 0x00, 0x04, 0x7E};
        System.out.println(Common.checkSum(data, 1, 6));
    }
}