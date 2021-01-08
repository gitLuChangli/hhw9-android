package com.example.handheld;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.handheld.adapter.BarcodeListViewAdapter;
import com.example.handheld.adapter.FragmentAdapter;
import com.example.handheld.adapter.ListViewAdapter;
import com.example.handheld.adapter.VersionListViewAdapter;
import com.example.handheld.bean.BarcodeBean;
import com.example.handheld.bean.EpcBean;
import com.example.handheld.fragment.AboutFragment;
import com.example.handheld.fragment.BarcodeFragment;
import com.example.handheld.fragment.UHFFragment;
import com.example.handheld.lib.CommandException;
import com.example.handheld.lib.Common;
import com.example.handheld.lib.Device;
import com.example.handheld.lib.OnBarcodeListener;
import com.example.handheld.lib.OnBatteryListener;
import com.example.handheld.lib.OnConnectionListener;
import com.example.handheld.lib.OnHandleListener;
import com.example.handheld.lib.OnUHFListener;
import com.example.handheld.lib.OnUpgradeListener;
import com.example.handheld.utils.AboutSettings;
import com.example.handheld.utils.AppVersion;
import com.example.handheld.utils.Beep;
import com.example.handheld.view.ToastKey;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "HANDSET_P";
    public static SharedPreferences sharedPreferences;

    public BluetoothAdapter bluetoothAdapter;
    public static final int REQUEST_ENABLE_BT = 1;
    public static final int REQUEST_FINE_LOCATION = 0;
    public static final int REQUEST_BT = 2;
    public static final int REQUEST_FILE = 3;
    public static final String BT_ADDRESS = "bt_address";
    private boolean intent_ret;
    private String bluetoothAddress;


    private static UHFFragment uhf;
    private static BarcodeFragment barcode;
    private static AboutFragment about;


    private ViewPager viewPager;
    private TabLayout tabLayout;

    public Device device;
    public Beep beep;

    /**
     * UHF Data
     */
    public static CopyOnWriteArrayList<EpcBean> epcBeans = new CopyOnWriteArrayList<>();
    public static List<String> indexes = new LinkedList<>();
    public static ListViewAdapter listViewAdapter;


    /**
     * BarCode Data
     */
    public static ArrayList<BarcodeBean> barcodeBeans = new ArrayList<>();
    public static BarcodeListViewAdapter barcodeListViewAdapter;


    /**
     * About Data
     */
    public static AboutSettings aboutSettings;
    public static VersionListViewAdapter versionListViewAdapter;


    private Snackbar snackbar;

    public static boolean mAutoConnect;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.remove("android:support:fragments");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().setNavigationBarColor(getResources().getColor(R.color.colorPrimaryDark));
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        device = Device.getInstance(this);
        device.setOnConnectionListener(onConnectionListener);
        device.setOnUHFListener(onUHFListener);
        device.setOnBarcodeListener(onBarcodeListener);
        device.setOnUpgradeListener(onUpgradeListener);
        device.setOnHandleListener(onHandleListener);
        //device.setOnClientListener(onClientListener);
        device.setOnBatteryListener(onBatteryListener);
        device.setHandleInventory(true);
        device.setDebug(true);

        beep = new Beep(this);
        sharedPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

        mAutoConnect = sharedPreferences.getBoolean("auto_connect", true);

        findViews();


    }

    @Override
    protected void onResume() {
        super.onResume();
        if (device != null) {
            device.setOnConnectionListener(onConnectionListener);
        }
        if (!intent_ret) {
            checkLocationEnable();
        }
        Log.d(TAG, "resume");

        if (snackbar != null) {
            snackbar.dismiss();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (device != null) {
            handler.removeMessages(MSG_SPEED);
            device.setOnConnectionListener(null);
            device.disconnect();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult : " + requestCode);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_FINE_LOCATION && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            checkLocationEnable();
        } else if (requestCode == REQUEST_FILE && grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode != Activity.RESULT_OK) {
            checkBluetooth();
        } else if (requestCode == REQUEST_BT) {
            if (resultCode == RESULT_OK) {
                bluetoothAddress = data.getStringExtra(BT_ADDRESS);
                if (!TextUtils.isEmpty(bluetoothAddress)) {
                    Log.d(TAG, bluetoothAddress);
                    intent_ret = true;
                    connect();
                }
            } else {
                intent_ret = false;
            }
        }
        Log.d(TAG, "onActivityResult");
    }

    private void findViews() {
        List<Fragment> fragmentList = new ArrayList<>();
        uhf = new UHFFragment();
        barcode = new BarcodeFragment();
        about = new AboutFragment();

        fragmentList.add(uhf);
        fragmentList.add(barcode);
        fragmentList.add(about);

        viewPager = findViewById(R.id.view_pager_main);
        viewPager.setOffscreenPageLimit(10);
        FragmentAdapter fragmentAdapter = new FragmentAdapter(getSupportFragmentManager(), fragmentList);
        viewPager.setAdapter(fragmentAdapter);
        viewPager.addOnPageChangeListener(onPageChangeListener);
        tabLayout = findViewById(R.id.tabs_main);
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        listViewAdapter = new ListViewAdapter(this);
        barcodeListViewAdapter = new BarcodeListViewAdapter(this);

        aboutSettings = AboutSettings.getInstance(this);
        versionListViewAdapter = new VersionListViewAdapter(this);

        aboutSettings.setAppVersion(AppVersion.get(this));
        versionListViewAdapter.setData(aboutSettings.getSettings());
    }

    public void checkLocationEnable() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (isLocationOpen(this)) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
                    }, REQUEST_FINE_LOCATION);
                    return;
                }
            } else {
                new AlertDialog.Builder(this).setMessage(R.string.message_open_gps).setTitle(R.string.dialog_note).setPositiveButton(R.string.dialog_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(settingsIntent);
                    }
                }).setCancelable(false).show();
                return;
            }
        }
        checkBluetooth();
    }

    public boolean isLocationOpen(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsProvider = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkProvider = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return isGpsProvider || isNetworkProvider;
    }

    public void checkBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            checkBluetoothEnable();
        }
    }

    public void checkBluetoothEnable() {
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {

            bluetoothAddress = sharedPreferences.getString(BT_ADDRESS, "");
            if (TextUtils.isEmpty(bluetoothAddress)) {
                startActivityForResult(new Intent(this, SearchActivity.class), REQUEST_BT);
            } else {
                connectBefore();
            }
        }
    }

    public void connectBefore() {
        if (device != null) {
            if (mAutoConnect) {
                connect();
            } else {
                String msg = getString(R.string.msg_connect_before, bluetoothAddress);
                new AlertDialog.Builder(this).setTitle(R.string.dialog_title).setMessage(msg).setPositiveButton(R.string.dialog_button_connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        connect();
                    }
                }).setNegativeButton(R.string.button_search, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivityForResult(new Intent(MainActivity.this, SearchActivity.class), REQUEST_BT);
                    }
                }).setCancelable(false).show();
            }
        }
    }

    public void connect() {
        if (device != null) {
            device.connect(bluetoothAddress);
            Log.d(TAG, bluetoothAddress);
            showLoading(R.string.dialog_connect);
        }
    }

    private ViewPager.OnPageChangeListener onPageChangeListener = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int i, float v, int i1) {

        }

        @Override
        public void onPageSelected(int i) {
            tabLayout.getTabAt(i).select();
        }

        @Override
        public void onPageScrollStateChanged(int i) {

        }
    };

    private TabLayout.OnTabSelectedListener onTabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    final int MSG_SPEED = 1000;
    long speed_tick;

    Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            super.dispatchMessage(msg);
            switch (msg.what) {
                case MSG_SPEED:
                    long tick = System.currentTimeMillis();
                    sendEmptyMessageDelayed(MSG_SPEED, 1000);
                    uhf.setSpeed((cnt - last_cnt) * 1000 / (tick - speed_tick));
                    last_cnt = cnt;
                    speed_tick = tick;
                    break;
                default:
                    break;
            }
        }
    };

    private OnConnectionListener onConnectionListener = new OnConnectionListener() {
        @Override
        public void onConnecting() {
            Log.d(TAG, "onConnecting");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //showLoading(R.string.dialog_connect);
                }
            });
        }

        @Override
        public void onConnected() {
            Log.d(TAG, "onConnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    intent_ret = false;

                    sharedPreferences.edit().putString(BT_ADDRESS, bluetoothAddress).commit();

                    aboutSettings.setMac(bluetoothAddress);

                    hideLoading();

                    try {
                        String version = device.getVersion();
                        aboutSettings.setFirmVersion(version);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    /**
                     * RFID 模块还在初始化
                     */
                    try {
                        Thread.sleep(800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    try {
                        String version = device.getRfidVersion();
                        aboutSettings.setRfidVersion(version);
                    } catch (CommandException e) {
                        e.printStackTrace();
                    }

                    setEnable(true);

                    versionListViewAdapter.setData(aboutSettings.getSettings());
                }
            });
        }

        @Override
        public void onFailed() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoading();
                    snackbar = Snackbar.make(viewPager, R.string.msg_connect_failed, Snackbar.LENGTH_INDEFINITE);
                    View view = snackbar.getView();
                    view.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    snackbar.setActionTextColor(getResources().getColor(android.R.color.white));
                    snackbar.setAction(R.string.button_reconnect, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            connect();
                        }
                    });
                    snackbar.show();
                }
            });
        }

        @Override
        public void onDisconnected() {
            Log.d(TAG, "onDisconnected");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hideLoading();
                    setEnable(false);
                    snackbar = Snackbar.make(viewPager, R.string.msg_reconnect, Snackbar.LENGTH_INDEFINITE).setAction(R.string.dialog_button_reconnect, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            connect();
                        }
                    });
                    snackbar.setActionTextColor(getResources().getColor(android.R.color.white));
                    View view = snackbar.getView();
                    view.setBackgroundColor(getResources().getColor(R.color.colorRed));
                    snackbar.show();
                }
            });
        }
    };

    /**
     * Inventory speed
     */
    volatile long play_tick;
    volatile long last_cnt;
    volatile long cnt;

    private OnUHFListener onUHFListener = new OnUHFListener() {

        @Override
        public void onInventory(byte[] pc, byte[] epc, int rssi) {
            cnt++;
            String _pc = Common.bytes2String(pc, 0, 2);
            String _epc = Common.bytes2String(epc, 0, epc.length);
            final int index = indexes.indexOf(_epc);
            if (index == -1) {
                EpcBean epcBean = new EpcBean();
                epcBean.setPc(_pc);
                epcBean.setEpc(_epc);
                epcBean.setRssi(rssi);
                epcBean.setCount(1);
                indexes.add(_epc);
                epcBeans.add(epcBean);
            } else {
                epcBeans.get(index).setCount(epcBeans.get(index).getCount() + 1);
                epcBeans.get(index).setRssi(rssi);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listViewAdapter.setData(epcBeans);
                    if (System.currentTimeMillis() - play_tick > 40) {
                        beep.playInv();
                        play_tick = System.currentTimeMillis();
                    }

                    if (index == -1) {
                        uhf.setUnique(indexes.size());
                    }
                }
            });

        }

        @Override
        public void onInventoryStop() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handler.removeMessages(MSG_SPEED);
                    uhf.setInventoryReady(true);
                    setEnable(true);
                }
            });
        }
    };

    private AlertDialog dialog;

    private void showLoading(int msgId) {
        hideLoading();
        View view = getLayoutInflater().inflate(R.layout.dialog_loading, null);
        final TextView textViewMessage = view.findViewById(R.id.text_view_message);
        textViewMessage.setText(msgId);
        dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).show();
    }

    private void hideLoading() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }


    public void setEnable(boolean enable) {
        uhf.setEnable(enable);
        barcode.setEnable(enable);
    }

    private OnBarcodeListener onBarcodeListener = new OnBarcodeListener() {
        @Override
        public void onBarcode(final String code) {
            boolean found = false;
            for (BarcodeBean barcodeBean : barcodeBeans) {
                if (code.equalsIgnoreCase(barcodeBean.getBarcode())) {
                    barcodeBean.setCount(barcodeBean.getCount() + 1);
                    found = true;
                    break;
                }
            }
            if (!found) {
                BarcodeBean barcodeBean = new BarcodeBean();
                barcodeBean.setBarcode(code);
                barcodeBean.setCount(1);
                barcodeBeans.add(barcodeBean);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    barcodeListViewAdapter.setData(barcodeBeans);
                    beep.playOk();
                    barcode.setEnable(true);
                }
            });
        }

        @Override
        public void onFail() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beep.playError();
                    barcode.setEnable(true);
                }
            });
        }
    };


    public void upgrade() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_FILE);
            return;
        }

        final String directory = Environment.getExternalStorageDirectory() + "/";
        final String[] files = new File(directory).list();
        if (files != null) {
            new AlertDialog.Builder(this).setItems(files, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, final int i) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                device.upgrade(directory + files[i]);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Snackbar.make(viewPager, R.string.upgrade_failed, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }).show();
        }
    }

    private OnUpgradeListener onUpgradeListener = new OnUpgradeListener() {
        @Override
        public void onUpgrade(final int progress) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aboutSettings.setProgress(progress);
                    versionListViewAdapter.setData(aboutSettings.getSettings());
                }
            });
        }

        @Override
        public void onUpgradeResult(final boolean success) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Snackbar.make(viewPager, success ? R.string.upgrade_success : R.string.upgrade_failed, Snackbar.LENGTH_SHORT).show();

                    device.disconnect();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    final Intent intent = getPackageManager().getLaunchIntentForPackage(getPackageName());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                    finish();
                }
            });
        }
    };

    private OnHandleListener onHandleListener = new OnHandleListener() {
        @Override
        public void onDown(final int key) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastKey.show(MainActivity.this, getString(R.string.toast_key_down, key));
                }
            });
        }

        @Override
        public void onUp(final int key) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastKey.show(MainActivity.this, getString(R.string.toast_key_up, key));
                }
            });
        }
    };

    private OnBatteryListener onBatteryListener = new OnBatteryListener() {
        @Override
        public void onBattery(final int battery) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    aboutSettings.setBattery(battery);
                    about.setBattery(battery);
                    versionListViewAdapter.setData(aboutSettings.getSettings());
                }
            });
        }
    };

    public void setSelected(String epc) {
        uhf.setSelected(epc);
    }

    public void initBeforeInventory() {
        cnt = 0;
        last_cnt = 0;
        speed_tick = System.currentTimeMillis();
        handler.removeMessages(MSG_SPEED);
        handler.sendEmptyMessageDelayed(MSG_SPEED, 1000);
    }

    public void clearInventoryCache() {
        cnt = 0;
        last_cnt = 0;
        uhf.setSpeed(0);
        uhf.setUnique(0);
    }

    public void setAutoConnect(boolean autoConnect) {
        mAutoConnect = autoConnect;
        sharedPreferences.edit().putBoolean("auto_connect", autoConnect).commit();
    }
}