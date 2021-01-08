/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.handheld.lib;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    /**
     * 连接状态监听器
     */
    private OnConnectionListener onConnectionListener;

    public void setOnConnectionListener(OnConnectionListener onConnectionListener) {
        this.onConnectionListener = onConnectionListener;
    }

    private Wait wait;

    private volatile boolean bInvFlag;

    private static BluetoothChatService service;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    public static BluetoothChatService getInstance(Context context) {
        synchronized (BluetoothChatService.class) {
            if (service == null) {
                service = new BluetoothChatService(context);
            }
        }
        return service;
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     */
    private BluetoothChatService(Context context) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        wait = new Wait();
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        if (onConnectionListener != null) {
            switch (state) {
                case STATE_CONNECTED:
                    onConnectionListener.onConnected();
                    break;
                case STATE_CONNECTING:
                    onConnectionListener.onConnecting();
                    break;
            }
        }
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);

        sendAlive();
    }

    /**
     * Stop all threads
     */
    public synchronized void disconnect() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }
        setState(STATE_NONE);
        cancelAlive();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        if (onConnectionListener != null) {
            onConnectionListener.onFailed();
        }
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        setState(STATE_LISTEN);

        if (onConnectionListener != null) {
            onConnectionListener.onDisconnected();
        }
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                //tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                //tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                // Start the service over to restart listening mode
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    int length = mmInStream.available();
                    if (length > 0) {
                        byte[] data = new byte[length];
                        mmInStream.read(data);
                        if (D) {
                            Log.d(TAG, " <<< " + Common.bytes2String(data, 0, data.length));
                        }
                        // Send the obtained bytes to the UI Activity
                        translate(data, data.length);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * 解析数据
         */
        volatile int head_count = 0;
        volatile int data_count = 0;
        volatile int data_length = 0;
        byte[] cmd_temp;
        volatile byte cmd;
        volatile byte seq;

        volatile int uhf_head_count = 0;
        volatile int uhf_data_count = 0;
        volatile int uhf_data_length = 0;
        volatile byte uhf_cmd = 0;
        volatile byte type = 0;
        volatile byte[] uhf_temp = new byte[1024];

        /**
         * 解析数据
         *
         * @param data
         * @param length
         */
        private void translate(byte[] data, int length) {
            for (int i = 0; i < length; i++) {

                byte b = data[i];

                if (head_count < CMD.HEADER_LENGTH) {
                    switch (head_count) {
                        case 0:
                            if (CMD.HEADER == b) {
                                head_count++;
                            } else {
                                head_count = 0;
                            }
                            break;
                        case 1:
                            cmd = b;
                            head_count++;
                            break;
                        case 2:
                            seq = b;
                            head_count++;
                            break;
                        case 3:
                            data_length = b & 0xff;
                            head_count++;
                            break;
                        case 4:
                            data_length += Common.byte2Int(b) * 256;
                            if (data_length > 1024) {
                                head_count = 0;
                            } else {
                                head_count++;
                                data_count = 0;

                                cmd_temp = new byte[data_length];
                            }
                            break;
                        default:
                            break;
                    }
                } else {
                    if (data_count < data_length) {
                        cmd_temp[data_count++] = b;
                    } else {
                        byte bcc = Common.bcc(cmd_temp, 0, data_length);
                        if (bcc == b) {
                            command();
                        }

                        head_count = 0;
                        data_count = 0;
                        data_length = 0;
                        cmd = 0;
                    }
                }
            }
        }

        /**
         * 区分命令码
         */
        private void command() {
            switch (cmd) {
                case CMD.UHF_DATA:
                    translateUHF();
                    break;
                case CMD.VERSION:
                    synchronized (wait.getObject()) {
                        byte[] data = new byte[data_length];
                        Common.arrayCopy(data, cmd_temp, 0, 0, data_length);
                        wait.setData(data);
                        wait.setLength(data_length);
                        wait.setNotified(true);
                        wait.getObject().notify();
                    }
                    break;
                case CMD.BARCODE_SCAN:
                    if (data_length == 0) {
                        onBarcodeListener.onFail();
                    } else {
                        String barcode = new String(cmd_temp, 0, data_length);
                        if (onBarcodeListener != null) {
                            onBarcodeListener.onBarcode(barcode);
                        }
                    }
                    break;
                case CMD.UPDATE:
                    if (data_length > 0 && cmd_temp[0] == (byte) 0x80) {
                        synchronized (object_upgrade) {
                            object_upgrade.notify();
                        }
                        break;
                    }
                    if (onUpgradeListener != null) {
                        onUpgradeListener.onUpgradeResult(false);
                    }
                    upgradeBean = null;
                    break;
                case CMD.BATTERY_DATA:
                    int battery = cmd_temp[0] & 0xff;
                    if (battery > 100) battery = 100;
                    if (battery < 0) battery = 0;

                    if (onBatteryListener != null && !bInvFlag) {
                        onBatteryListener.onBattery(battery);
                    }
                    break;
                case CMD.KEY_DATA_UP:
                    int uv = (cmd_temp[0] & 0xff) | ((cmd_temp[1] & 0xff) << 8);
                    int ui = (int) (Math.log(uv) / Math.log(2));
                    if (onHandleListener != null) {
                        onHandleListener.onUp(ui);
                    }
                    break;
                case CMD.KEY_DATA_DOWN:
                    int dv = (cmd_temp[0] & 0xff) | ((cmd_temp[1] & 0xff) << 8);
                    int di = (int) (Math.log(dv) / Math.log(2));
                    if (onHandleListener != null) {
                        onHandleListener.onDown(di);
                    }
                    break;
                default:
                    break;
            }
        }

        private void translateUHF() {
            byte b;
            int i;
            for (i = 0; i < data_length; i++) {
                b = cmd_temp[i];
                if (uhf_head_count < M100Code.HEADER_LEN) {
                    switch (uhf_head_count) {
                        case 0:
                            if (M100Code.HEADER == b) {
                                uhf_head_count++;
                                uhf_temp[0] = b;
                            }
                            break;
                        case 1:
                            type = b;
                            uhf_head_count++;
                            uhf_temp[1] = b;
                            break;
                        case 2:
                            uhf_cmd = b;
                            uhf_head_count++;
                            uhf_temp[2] = b;
                            break;
                        case 3:
                            uhf_data_length = (b & 0xff) << 8;
                            uhf_head_count++;
                            uhf_data_count = 0;
                            uhf_temp[3] = b;
                            break;
                        case 4:
                            uhf_data_length += (b & 0xff);
                            uhf_head_count++;
                            uhf_temp[4] = b;
                            break;
                        default:
                            break;
                    }
                } else {
                    if (uhf_data_count < uhf_data_length) {
                        uhf_temp[5 + uhf_data_count++] = b;
                    } else {
                        byte sum = Common.checkSum(uhf_temp, 1, 4 + uhf_data_length);
                        if (sum == b) {
                            commandUHF();
                        }

                        uhf_head_count = 0;
                        uhf_data_count = 0;
                        uhf_data_length = 0;
                        uhf_cmd = 0;
                    }
                }
            }
        }

        private void commandUHF() {
            switch (uhf_cmd) {
                case M100Code.RFID_VERSION:
                case M100Code.SELECT_PARAM:
                case M100Code.SELECT_MODE:
                case M100Code.READ_TAG:
                case M100Code.WRITE_TAG:
                case M100Code.LOCK_TAG:
                case M100Code.KILL_TAG:
                case M100Code.QUERY_GET:
                case M100Code.QUERY_SET:
                case M100Code.REGION_SET:
                case M100Code.REGION_GET:
                case M100Code.CHANNEL_SET:
                case M100Code.CHANNEL_GET:
                case M100Code.FHSS_SET:
                case M100Code.POWER_GET:
                case M100Code.POWER_SET:
                case M100Code.CW_SET:
                case M100Code.DEMODULATOR_SET:
                case M100Code.SAVE_PARAMETERS:
                    synchronized (wait.getObject()) {
                        byte[] data = new byte[uhf_data_length];
                        Common.arrayCopy(data, uhf_temp, 0, 5, uhf_data_length);
                        wait.setData(data);
                        wait.setLength(uhf_data_length);
                        wait.setNotified(true);
                        wait.getObject().notify();
                    }
                    break;
                case M100Code.INV_ONCE:
                    bInvFlag = true;
                    if (onUHFListener == null) break;
                    int rssi = (uhf_temp[5] & 0xff) - 256;
                    // CRC
                    byte[] pc = new byte[2];
                    Common.arrayCopy(pc, uhf_temp, 0, 6, 2);
                    int size = (pc[0] & 0xff) / 8 * 2;
                    byte[] epc = new byte[size];
                    Common.arrayCopy(epc, uhf_temp, 0, 8, size);

                    int crc1 = ~Common.crc(uhf_temp, 6, size + 2) & 0xffff;
                    int crc2 = (uhf_temp[size + 8] & 0xff) << 8 | (uhf_temp[size + 9] & 0xff);
                    if (crc1 == crc2) {
                        onUHFListener.onInventory(pc, epc, rssi);
                    }
                    break;
                case M100Code.INV_STOP:
                    bInvFlag = false;
                    if (onUHFListener != null) onUHFListener.onInventoryStop();
                    break;
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }


        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }


    private void write(byte cmd, int seq, byte[] data, int length) {
        byte[] send = new byte[6 + length];
        send[0] = 0x1B;
        send[1] = cmd;
        send[2] = (byte) seq;
        send[3] = (byte) (length % 256);
        send[4] = (byte) (length / 256);
        Common.arrayCopy(send, data, 5, 0, length);
        send[5 + length] = Common.bcc(data, 0, length);
        write(send);
        if (D) {
            Log.d(TAG, " >>> " + Common.bytes2String(send, 0, send.length));
        }
    }

    private void write(byte cmd, byte[] data, int length) {
        write(cmd, 0, data, length);
    }

    private void writeCommand(byte cmd, byte[] data, int length) {
        wait.setCmd(cmd);
        byte[] send = new byte[M100Code.HEADER_LEN + 2 + length];
        send[0] = M100Code.HEADER;
        send[1] = M100Code.TYPE_CMD;
        send[2] = cmd;
        send[3] = (byte) (length >> 8);
        send[4] = (byte) length;
        if (length > 0) {
            Common.arrayCopy(send, data, 5, 0, length);
        }
        send[5 + length] = Common.checkSum(send, 1, M100Code.HEADER_LEN + length);
        send[6 + length] = M100Code.FOOTER;
        write(CMD.UHF_DATA, send, send.length);
    }

    /**
     * 获取固件版本号
     *
     * @return
     */
    public String getVersion() throws Exception {
        synchronized (wait.getObject()) {
            wait.setNotified(false);
            wait.setCmd(CMD.VERSION);
            write(CMD.VERSION, null, 0);
            wait.getObject().wait(CMD.TIMEOUT);
            if (wait.isNotified()) {
                return new String(wait.getData(), 0, wait.getLength()).trim();
            }
        }
        throw new Exception("Get device's version failed.");
    }

    public void scanBarcode() {
        write(CMD.BARCODE_SCAN, null, 0);
    }

    /**
     * 获取 RFID 模块版本号
     *
     * @return
     */
    public String getRfidVersion() throws CommandException {
        byte[] data = {0};
        byte[] answer = answerCommand(M100Code.RFID_VERSION, data);
        return new String(answer);
    }

    public void inventoryOnce() {
        byte[] data = {0};
        writeCommand(M100Code.INV_ONCE, data, data.length);
    }

    /**
     * 多次轮询
     */
    public void inventoryTimes(int times) {
        byte[] data = new byte[3];
        data[0] = 0x22;
        data[1] = (byte) (times >> 8);
        data[2] = (byte) times;
        writeCommand(M100Code.INV_MULTIPLE, data, data.length);
    }

    /**
     * 连续盘存
     */
    public void inventory() {
        //byte[] data = {0x22};
        //writeCommand(M100Code.INV_MULTIPLE, data, data.length);
    }

    /**
     * 停止盘存
     */
    public void stopInventory() {
        writeCommand(M100Code.INV_STOP, null, 0);
    }

    /**
     * 设置调制解调器参数
     */
    public void setDemodulator(int mixerG) {
        Demodulator demodulator = new Demodulator();
        demodulator.setMixerG(mixerG);
        demodulator.setIfG(6);
        demodulator.setThreshold(0xA0);
        setDemodulator(demodulator);
    }

    public void setDemodulator(Demodulator demodulator) {
        if (demodulator == null) throw new CommandException(-1);
        byte[] data = demodulator.getData();
        transferCommand(M100Code.DEMODULATOR_SET, data);
    }

    /**
     * 保存参数
     */
    public void saveParameters() {
        byte[] data = {1};
        transferCommand(M100Code.SAVE_PARAMETERS, data);
    }

    public void setSelectParam(SelectParam selectParam) {
        byte[] data = selectParam.getData();
        transferCommand(M100Code.SELECT_PARAM, data);
    }

    public void setSelectParam(int bank, int start, byte[] mask) {
        int length = 7;
        if (mask != null) {
            length += mask.length;
        }
        byte[] send = new byte[length];
        send[0] = (byte) (0x80 | bank);
        start *= 8;
        send[1] = (byte) (start / 256 / 256 / 256);
        send[2] = (byte) (start / 256 / 256);
        send[3] = (byte) (start / 256);
        send[4] = (byte) (start % 256);
        if (mask != null) {
            send[5] = (byte) (mask.length * 8);
        }
        send[6] = 0;
        if (mask != null) {
            Common.arrayCopy(send, mask, 7, 0, mask.length);
        }
        transferCommand(M100Code.SELECT_PARAM, send);
    }

    public void setSelectMode(int mode) {
        byte[] data = {(byte) mode};
        transferCommand(M100Code.SELECT_MODE, data);
    }


    private void transferCommand(byte cmd, byte[] data) throws CommandException {
        synchronized (wait.getObject()) {
            wait.setNotified(false);
            int length = 0;
            if (data != null) length = data.length;
            writeCommand(cmd, data, length);
            try {
                wait.getObject().wait(CMD.TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new CommandException(-1);
            }
            if (wait.isNotified()) {
                if (wait.getData() != null) {
                    if (wait.getData()[0] == 0) {
                        return;
                    } else {
                        throw new CommandException(wait.getData()[1]);
                    }
                }
            }
            throw new CommandException(-1);
        }
    }

    private byte[] answerCommand(byte cmd, byte[] data) throws CommandException {
        synchronized (wait.getObject()) {
            wait.setNotified(false);
            int length = 0;
            if (data != null) {
                length = data.length;
            }
            writeCommand(cmd, data, length);
            try {
                wait.getObject().wait(CMD.TIMEOUT);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new CommandException(-1);
            }
            if (wait.isNotified()) {
                if (wait.getData() != null) {
                    return wait.getData();
                }
            }
            throw new CommandException(-1);
        }
    }

    /**
     * 读标签
     */
    public byte[] readTag(int bank, int start, int length, byte[] access_password) throws CommandException {
        byte[] send = new byte[9];
        Common.arrayCopy(send, access_password, 0, 0, 4);
        send[4] = (byte) bank;
        send[5] = (byte) (start / 256);
        send[6] = (byte) (start & 0xff);
        send[7] = (byte) (length / 256);
        send[8] = (byte) (length & 0xff);
        byte[] result = answerCommand(M100Code.READ_TAG, send);
        int offset = (result[0] & 0xff) + 1;
        byte[] read = new byte[result.length - offset];
        Common.arrayCopy(read, result, 0, offset, read.length);
        return read;
    }

    /**
     * 写标签
     */
    public void writeTag(int bank, int start, int length, byte[] access_password, byte[] data) throws CommandException {
        int l_w = data.length;
        if (l_w > length * 2) {
            l_w = length * 2;
        }
        byte[] send = new byte[9 + length * 2];
        Common.arrayCopy(send, access_password, 0, 0, 4);
        send[4] = (byte) (bank & 0xff);
        send[5] = (byte) (start / 256);
        send[6] = (byte) (start & 0xff);
        send[7] = (byte) (length / 256);
        send[8] = (byte) (length & 0xff);
        if (l_w > 0) {
            Common.arrayCopy(send, data, 9, 0, l_w);
        }
        answerCommand(M100Code.WRITE_TAG, send);
    }

    public static final byte ACCESSIBLE = 0;
    public static final byte ALWAYS_ACCESSIBLE = 1;
    public static final byte SECURED_ACCESSIBLE = 2;
    public static final byte ALWAYS_NOT_ACCESSIBLE = 3;
    public static final byte NO_CHANGE = 4;

    public static final byte LOCK_KILL_PWD = 0;
    public static final byte LOCK_ACCESS_PWD = 1;
    public static final byte LOCK_EPC_BANK = 2;
    public static final byte LOCK_TID_BANK = 3;
    public static final byte LOCK_USER_BANK = 4;

    /**
     * 锁卡
     *
     * @param access_password
     * @param area
     * @param type
     */
    public void lockTag(byte[] access_password, int area, int type) throws CommandException {
        byte mask = 0;
        byte action = 0;
        if (type == ACCESSIBLE) {
            mask = 2;
            action = 0;
        } else if (type == ALWAYS_ACCESSIBLE) {
            mask = 1;
            action = 0;
        } else if (type == SECURED_ACCESSIBLE) {
            mask = 2;
            action = 2;
        } else if (type == ALWAYS_NOT_ACCESSIBLE) {
            mask = 3;
            action = 3;
        } else if (type == NO_CHANGE) {
            mask = 0;
            action = 0;
        }

        long mask_action = 0;
        if (area == LOCK_KILL_PWD) {
            mask_action = (mask << 18) | (action << 8);
        } else if (area == LOCK_ACCESS_PWD) {
            mask_action = (mask << 16) | (action << 6);
        } else if (area == LOCK_EPC_BANK) {
            mask_action = (mask << 14) | (action << 4);
        } else if (area == LOCK_TID_BANK) {
            mask_action = (mask << 12) | (action << 2);
        } else if (area == LOCK_USER_BANK) {
            mask_action = (mask << 10) | action;
        }

        byte[] send = new byte[7];
        Common.arrayCopy(send, access_password, 0, 0, 4);
        send[4] = (byte) ((mask_action >> 16) & 0xff);
        send[5] = (byte) ((mask_action >> 8) & 0xff);
        send[6] = (byte) (mask_action & 0xff);
        answerCommand(M100Code.LOCK_TAG, send);
    }

    /**
     * 注销卡
     *
     * @param kill_password
     */
    public void killTag(byte[] kill_password) throws CommandException {
        answerCommand(M100Code.KILL_TAG, kill_password);
    }

    /**
     * 获取超高频Query
     *
     * @return
     */
    public Query getQuery() throws CommandException {
        byte[] result = answerCommand(M100Code.QUERY_GET, null);
        Query query = new Query();
        query.setBytes(result);
        return query;
    }

    /**
     * 设置超高频模块Query
     *
     * @param query
     */
    public void setQuery(Query query) throws CommandException {
        byte[] send = query.getBytes();
        transferCommand(M100Code.QUERY_SET, send);
    }

    /**
     * 获取地区标准
     *
     * @return
     */
    public int getRegion() throws CommandException {
        byte[] result = answerCommand(M100Code.REGION_GET, null);
        return result[0] & 0xff;
    }

    /**
     * 设置地区标准
     *
     * @param region
     */
    public void setRegion(int region) throws CommandException {
        byte[] send = {(byte) region};
        transferCommand(M100Code.REGION_SET, send);
    }

    /**
     * 设置超高频模块通道
     *
     * @param channel
     */
    public void setChannel(int channel) throws CommandException {
        byte[] send = {(byte) channel};
        transferCommand(M100Code.CHANNEL_SET, send);
    }

    /**
     * 获取超高频模块通道
     *
     * @return
     */
    public int getChannel() throws CommandException {
        byte[] result = answerCommand(M100Code.CHANNEL_GET, null);
        return result[0] & 0xff;
    }

    /**
     * 设置自动跳频
     *
     * @param auto
     */
    public void setFrequencyHopping(boolean auto) {
        byte[] send = {auto ? (byte) 0xff : 0};
        transferCommand(M100Code.FHSS_SET, send);
    }

    /**
     * 获取读写器参数
     *
     * @return
     */
    public int getPower() throws CommandException {
        byte[] result = answerCommand(M100Code.POWER_GET, null);
        return (((result[0] & 0xff) << 8) | (result[1] & 0xff)) / 100;
    }

    /**
     * 设置功率
     *
     * @param power
     */
    public void setPower(int power) throws CommandException {
        int p = power * 100;
        byte[] data = new byte[2];
        data[0] = (byte) (p >> 8);
        data[1] = (byte) p;
        transferCommand(M100Code.POWER_SET, data);
    }

    /**
     * 设置载波信号
     *
     * @param on
     */
    public void setCarrier(boolean on) {
        byte[] data = {on ? (byte) 0xff : 0};
        transferCommand(M100Code.CW_SET, data);
    }

    /**
     * 获取电量
     */
    public void getBattery() {
        write(CMD.BATTERY_DATA, null, 0);
    }

    private volatile Object object_upgrade = new Object();
    private volatile UpgradeBean upgradeBean;

    /**
     * 升级207固件
     *
     * @param fileName 文件名称
     * @throws IOException
     */
    public void upgrade(String fileName) throws Exception {
        if (fileName.lastIndexOf(".bin") == -1) {
            throw new Exception("Upgrade file error");
        }
        cancelAlive();
        new UpgradeThread(fileName).start();
    }

    class UpgradeThread extends Thread {

        public UpgradeThread(String fileName) {
            try {
                FileInputStream fileInputStream = new FileInputStream(fileName);
                int length = fileInputStream.available();
                byte[] data = new byte[length];
                fileInputStream.read(data);

                upgradeBean = new UpgradeBean();
                upgradeBean.setData(data);
                upgradeBean.setLength(CMD.UPGRADE_SIZE);
                int times = length / CMD.UPGRADE_SIZE;
                int last = length % CMD.UPGRADE_SIZE;
                if (last > 0) {
                    upgradeBean.setTimes(times + 1);
                    upgradeBean.setLast(last);
                } else {
                    upgradeBean.setTimes(times);
                    upgradeBean.setLast(CMD.UPGRADE_SIZE);
                }
                upgradeBean.setIndex(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            sendBin();
            while (true) {
                synchronized (object_upgrade) {
                    try {
                        object_upgrade.wait();
                        if (upgradeBean.hasNext()) {
                            upgradeBean.setNext();
                            sendBin();
                            if (onUpgradeListener != null) {
                                onUpgradeListener.onUpgrade(upgradeBean.getProgress());
                            }
                        } else {
                            if (onUpgradeListener != null) {
                                onUpgradeListener.onUpgradeResult(true);
                                onUpgradeListener.onUpgrade(100);
                            }
                            upgradeBean = null;
                            break;
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        if (onUpgradeListener != null) {
                            onUpgradeListener.onUpgradeResult(false);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * 发送升级文件
     */
    private void sendBin() {
        byte[] send = upgradeBean.getNext();
        write((byte) 0x43, upgradeBean.getIndex(), send, send.length);
    }

    /**
     * 升级监听器
     */
    private OnUpgradeListener onUpgradeListener;

    public void setOnUpgradeListener(OnUpgradeListener onUpgradeListener) {
        this.onUpgradeListener = onUpgradeListener;
    }

    /**
     * 设置超高频监听器
     */
    private OnUHFListener onUHFListener;

    public void setOnUHFListener(OnUHFListener onUHFListener) {
        this.onUHFListener = onUHFListener;
    }

    /**
     * 条码监听器
     */
    private OnBarcodeListener onBarcodeListener;

    public void setOnBarcodeListener(OnBarcodeListener onBarcodeListener) {
        this.onBarcodeListener = onBarcodeListener;
    }

    /**
     * 电量监听器
     */
    private OnBatteryListener onBatteryListener;

    public void setOnBatteryListener(OnBatteryListener onBatteryListener) {
        this.onBatteryListener = onBatteryListener;
    }

    /**
     * 设置手柄监听器
     */
    private OnHandleListener onHandleListener;

    public void setOnHandleListener(OnHandleListener onHandleListener) {
        this.onHandleListener = onHandleListener;
    }

    /**
     * 获取电量，1分钟获取一次
     */
    Timer timer;
    TimerTask timerTask;

    public void sendAlive() {

        if (timer != null || timerTask != null) {
            return;
        }

        timerTask = new TimerTask() {
            @Override
            public void run() {
                write(CMD.BATTERY_DATA, null, 0);
            }
        };

        timer = new Timer();
        timer.schedule(timerTask, 1000, 60000);
    }

    private void cancelAlive() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        if (timer != null) {
            timer.cancel();
        }

        timerTask = null;
        timer = null;
    }
}
