package com.example.handheld.lib;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Device {

    private static final String TAG = "handheld_p";

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static String SERVICE_CONFIG = "0000fff0-0000-1000-8000-00805f9b34fb";
    private static String CHARACTERISTIC_1 = "0000fff1-0000-1000-8000-00805f9b34fb";
    private static String CHARACTERISTIC_2 = "0000fff2-0000-1000-8000-00805f9b34fb";

    private OnConnectionListener onConnectionListener;

    private Object obj_next = new Object();

    public void setOnConnectionListener(OnConnectionListener onConnectionListener) {
        this.onConnectionListener = onConnectionListener;
    }

    private static Device device;
    private Context context;

    private BluetoothGattCharacteristic characteristicWrite;

    private boolean D;

    public void setDebug(boolean debug) {
        this.D = debug;
    }

    private Wait wait;

    private volatile boolean connected = false;

    private volatile boolean bInvFlag;

    private volatile long invTimes;

    private volatile boolean needStop;

    private boolean handleInventory;

    public static long lastHandleTick;

    public static Device getInstance(Context context) {
        synchronized (Device.class) {
            if (device == null) {
                device = new Device(context);
            }
        }
        return device;
    }

    private Device(Context context) {
        this.context = context;
        initialize();

        wait = new Wait();
    }

    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                connected = true;
                Log.d(TAG, "Connected to GATT server.");
                Log.i(TAG, "Attempting to discover services after successful connection." + bluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT service.");
                if (onConnectionListener != null) {
                    if (connected) {
                        onConnectionListener.onDisconnected();
                    } else {
                        onConnectionListener.onFailed();
                    }
                    connected = false;
                }
                close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> bluetoothLeServices = getSupportedGattServices();
                getGattCharacteristic(bluetoothLeServices);
                if (connectionState == STATE_CONNECTED) {
                    if (onConnectionListener != null) {
                        onConnectionListener.onConnected();
                    }
                    sendAlive();
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received : " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            final byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                if (D) {
                    Log.d(TAG, " <<< " + Common.bytes2String(data, 0, data.length));
                }
                translate(data, data.length);
            }
        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            synchronized (obj_next) {
                obj_next.notify();
            }
        }
    };

    private boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                if (D) {
                    Log.e(TAG, "Unable to initialize BluetoothManager.");
                }
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            if (D) {
                Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            }
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (TextUtils.isEmpty(address)) {
            return false;
        }

        if (bluetoothDeviceAddress != null && address.equalsIgnoreCase(bluetoothDeviceAddress) && bluetoothGatt != null) {
            if (bluetoothGatt.connect()) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                connectionState = STATE_CONNECTING;
                if (onConnectionListener != null) {
                    onConnectionListener.onConnecting();
                    return true;
                }
            } else {
                return false;
            }
        }

        final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
        if (bluetoothDevice == null) {
            if (D) {
                Log.d(TAG, "Device not found. Unable to connect.");
            }
            return false;
        }

        bluetoothGatt = bluetoothDevice.connectGatt(context, false, bluetoothGattCallback);
        if (bluetoothGatt == null) {
            return false;
        }
        /*
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothGatt.requestMtu(128);
        }*/
        if (D) {
            Log.d(TAG, "Trying to create new connection.");
        }
        bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        if (onConnectionListener != null) {
            onConnectionListener.onConnecting();
        }
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        cancelAlive();

        try {
            bluetoothGatt.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (thread != null) {
            thread.cancel();
        }
        if (clientThread != null) {
            clientThread.cancel();
        }
    }

    private void close() {
        if (bluetoothGatt == null) {
            return;
        }

        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enable) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        bluetoothGatt.setCharacteristicNotification(characteristic, enable);

        // This is specific to Heart Rate Measurement.
        if (CHARACTERISTIC_1.equals(characteristic.getUuid()) || CHARACTERISTIC_2.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    private List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return null;
        }

        return bluetoothGatt.getServices();
    }

    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }
        bluetoothGatt.writeCharacteristic(characteristic);
    }

    private void getGattCharacteristic(List<BluetoothGattService> gattServices) {
        for (BluetoothGattService gattService : gattServices) {
            Log.d(TAG, gattService.getUuid().toString());
            if (gattService.getUuid().toString().equalsIgnoreCase(SERVICE_CONFIG)) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(CHARACTERISTIC_2)) {
                        characteristicWrite = gattCharacteristic;
                        characteristicWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                        setCharacteristicNotification(gattCharacteristic, true);
                    } else if (gattCharacteristic.getUuid().toString().equalsIgnoreCase(CHARACTERISTIC_1)) {
                        setCharacteristicNotification(gattCharacteristic, true);
                    }
                }
            }
        }
    }

    private void write(byte cmd, int seq, byte[] data, int length) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized.");
            return;
        }

        if (characteristicWrite == null) {
            Log.w(TAG, "Bluetooth not connected.");
            return;
        }

        byte[] send = new byte[6 + length];
        send[0] = 0x1B;
        send[1] = cmd;
        send[2] = (byte) seq;
        send[3] = (byte) (length % 256);
        send[4] = (byte) (length / 256);
        Common.arrayCopy(send, data, 5, 0, length);
        send[5 + length] = Common.bcc(data, 0, length);

        int each_size = 19;

        int sendTimes = send.length / each_size;
        int lastSend = send.length % each_size;
        for (int i = 0; i < sendTimes; i++) {
            byte[] sd = new byte[each_size];
            Common.arrayCopy(sd, send, 0, i * each_size, each_size);

            if (D) {
                Log.d(TAG, " >>> " + Common.bytes2String(sd, 0, each_size));
            }

            characteristicWrite.setValue(sd);
            writeCharacteristic(characteristicWrite);

            synchronized (obj_next) {
                try {
                    obj_next.wait(20);
                } catch (InterruptedException e) {
                    if (D) {
                        Log.d(TAG, "write failed: " + e.getMessage());
                    }
                }
            }
        }

        if (lastSend > 0) {
            byte[] sd = new byte[lastSend];
            Common.arrayCopy(sd, send, 0, sendTimes * each_size, lastSend);

            if (D) {
                Log.d(TAG, " >>> " + Common.bytes2String(sd, 0, lastSend));
            }

            characteristicWrite.setValue(sd);
            writeCharacteristic(characteristicWrite);

            synchronized (obj_next) {
                try {
                    obj_next.wait(20);
                } catch (InterruptedException e) {
                    if (D) {
                        Log.d(TAG, "write failed: " + e.getMessage());
                    }
                }
            }
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
        new ScanBarcodeTask().execute();
    }

    class ScanBarcodeTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... strings) {
            synchronized (wait.getObject()) {
                wait.setNotified(false);
                write(CMD.BARCODE_SCAN, null, 0);
                try {
                    wait.getObject().wait(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (!wait.isNotified()) {
                    if (onBarcodeListener != null) {
                        onBarcodeListener.onFail();
                    }
                }
            }
            return null;
        }
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
        //byte[] data = {0};
        //writeCommand(M100Code.INV_ONCE, data, data.length);
        writeCommand(M100Code.INV_ONCE, null, 0);
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
        byte[] data = {0x22};
        writeCommand(M100Code.INV_MULTIPLE, data, data.length);
        invTimes = 0;
        needStop = false;
    }

    private void inventoryWithoutInitial() {
        byte[] data = {0x22};
        writeCommand(M100Code.INV_MULTIPLE, data, data.length);
        invTimes = 0;
    }

    /**
     * 停止盘存
     */
    public void stopInventory() {
        writeCommand(M100Code.INV_STOP, null, 0);
        if (onUHFListener != null) {
            onUHFListener.onInventoryStop();
        }
        needStop = true;
    }

    private void stopInventoryWithoutResponse() {
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
                synchronized (wait.getObject()) {
                    wait.setNotified(true);
                    wait.getObject().notify();
                }
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
                if (handleInventory) {
                    if (di == 1 || di == 2) {
                        if (System.currentTimeMillis() - lastHandleTick > 500) {
                            if (!bInvFlag) {
                                device.stopInventory();
                            } else {
                                device.inventory();
                            }
                            lastHandleTick = System.currentTimeMillis();
                        }
                    }
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

                invTimes++;

                if (needStop) {
                    stopInventoryWithoutResponse();
                } else {
                    if (invTimes > 5000 && bInvFlag) {
                        inventoryWithoutInitial();
                    }
                }
                break;
            case M100Code.INV_STOP:
                bInvFlag = false;
                //if (onUHFListener != null) onUHFListener.onInventoryStop();
                break;
        }
    }

    /**
     * 加入RFComm
     */
    private AcceptThread thread;
    private ClientThread clientThread;
    private volatile boolean bReceiveClient = true;

    private OnClientListener onClientListener;

    public void setOnClientListener(OnClientListener onClientListener) {
        this.onClientListener = onClientListener;
    }

    private void startServer() {
        thread = new AcceptThread();
        thread.start();
    }

    private class AcceptThread extends Thread {

        private volatile boolean bAcceptThreadFlag;

        private BluetoothServerSocket serverSocket;

        public AcceptThread() {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("RFID", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            bAcceptThreadFlag = true;
            while (bAcceptThreadFlag) {
                try {
                    BluetoothSocket socket = serverSocket.accept();
                    if (socket != null) {
                        Log.d(TAG, socket.getRemoteDevice().getAddress());
                        if (bReceiveClient) {
                            manageConnectedSocket(socket);
                            if (onClientListener != null) {
                                onClientListener.onConnect();
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            bAcceptThreadFlag = false;
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void manageConnectedSocket(BluetoothSocket socket) {
            clientThread = new ClientThread(socket);
            clientThread.start();
        }
    }

    public void setHandleInventory(boolean handleInventory) {
        this.handleInventory = handleInventory;
    }

    private class ClientThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ClientThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = in;
            outputStream = out;
        }

        @Override
        public void run() {
            super.run();
            byte[] buffer = new byte[1024];
            int bytes;
            bReceiveClient = false;
            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    Log.d(TAG, new String(buffer, 0, bytes));
                } catch (IOException e) {
                    e.printStackTrace();
                    if (D) {
                        Log.d(TAG, "Client disconnected");
                    }
                    if (onClientListener != null) {
                        onClientListener.onDisconnect();
                    }
                    bReceiveClient = true;
                    break;
                }
            }
        }

        public void write(String msg) {
            if (outputStream != null) {
                try {
                    outputStream.write((msg + "\r\n").getBytes());
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
