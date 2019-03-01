package com.sonix.oidbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.BLEScanner;
import com.tqltech.tqlpencomm.PenCommAgent;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import no.nordicsemi.android.dfu.DfuProgressListener;
import no.nordicsemi.android.dfu.DfuServiceInitiator;
import no.nordicsemi.android.dfu.DfuServiceListenerHelper;

/**
 * Created by Administrator on 2018/8/15.
 */

public class OTAActivity extends Activity implements View.OnClickListener {

    private final static String TAG = "OTAActivity";
    private BluetoothLEService mService = null;
    private BluetoothDevice mDevice = null;
    private String mAddress = "";
    private PenCommAgent bleManager;

    private EditText filePath;
    private Button otaUpdata;

    /**
     * 服务连接对象
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        // 蓝牙连接
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((BluetoothLEService.LocalBinder) rawBinder).getService();
            if (mService == null) {
                finish();
            }

            Log.i(TAG, "mService is null? " + mService);
            if (!mService.initialize()) {
                Log.i(TAG, "mService init ok");
                finish();
            } else {
                Log.i(TAG, "mService init bad");
            }
            /*try {
                mService.setFlag(2);
            } catch (Exception e) {
                finish();
            }*/
        }

        // 蓝牙断开
        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ota);

        initView();

        bleManager = PenCommAgent.GetInstance(getApplication());

        // 获取当前设备的mac地址
        Intent intent = getIntent();
        String pAddress = intent.getStringExtra("addr");
        int pType = intent.getIntExtra("type", 1);
        String b = pAddress.substring(pAddress.length() - 2, pAddress.length());
        int a = 0;
        try {
            a = Integer.parseInt(b, 16);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        String str = Integer.toHexString(a + 1).toUpperCase();
        str = addZero(str);
        mAddress = pAddress.substring(0, pAddress.length() - 2) + str;

        // DFU监听
        Log.e("ota", "-----register listener1-----");
        DfuServiceListenerHelper.registerProgressListener(this, mDfuProgressListener);
        Log.e("ota", "-----register listener2-----");


        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        boolean bBind = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 组装hexString
     *
     * @param str
     * @return
     */
    private String addZero(String str) {
        String outputStr = str;
        if (str.length() == 1) {
            outputStr = "0" + str;
        }

        return outputStr;
    }

    /**
     * 监听DFU
     */
    private final DfuProgressListener mDfuProgressListener = new DfuProgressListener() {
        @Override
        public void onDeviceConnecting(String deviceAddress) {
            //当DFU服务开始与DFU目标连接时调用的方法
            Log.e("ota", "DFU服务开始与DFU目标连接," + deviceAddress);
        }

        @Override
        public void onDeviceConnected(String deviceAddress) {
            //方法在服务成功连接时调用，发现服务并在DFU目标上找到DFU服务。
            Log.d("ota", "服务成功连接,发现服务并在DFU目标上找到DFU服务." + deviceAddress);
        }

        @Override
        public void onDfuProcessStarting(String deviceAddress) {
            //当DFU进程启动时调用的方法。 这包括读取DFU版本特性，发送DFU START命令以及Init数据包（如果设置）。
            Log.d("ota", "DFU进程启动," + deviceAddress);
        }

        @Override
        public void onDfuProcessStarted(String deviceAddress) {
            //当DFU进程启动和要发送的字节时调用的方法。
            Log.d("ota", "DFU进程启动和要发送的字节," + deviceAddress);
        }

        @Override
        public void onEnablingDfuMode(String deviceAddress) {
            //当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方法。 将发送开关命令，并且DFU过程应该再次开始。 此调用后不会有onDeviceDisconnected（String）事件。
            Log.d("ota", "当服务发现DFU目标处于应用程序模式并且必须切换到DFU模式时调用的方");
        }

        @Override
        public void onProgressChanged(String deviceAddress, int percent, float speed, float avgSpeed, int currentPart, int partsTotal) {
            //在上传固件期间调用的方法。 它不会使用相同的百分比值调用两次，但是在小型固件文件的情况下，可能会省略一些值。\
            //mProgressBarOtaUpload.setProgress(percent);
            //bar.setVisibility(View.VISIBLE);
            //bar.setProgress(percent);
            Log.d("debug", "在上传固件期间调用的方法---" + percent);
        }

        @Override
        public void onFirmwareValidating(String deviceAddress) {
            //在目标设备上验证新固件时调用的方法。
            Log.d("debug", "目标设备上验证新固件时调用的方法");
        }

        @Override
        public void onDeviceDisconnecting(String deviceAddress) {
            //服务开始断开与目标设备的连接时调用的方法。
            Log.d("debug", "服务开始断开与目标设备的连接时调用的方法");
        }

        @Override
        public void onDeviceDisconnected(String deviceAddress) {
            //当服务从设备断开连接时调用的方法。 设备已重置。
            Log.d("debug", "当服务从设备断开连接时调用的方法。 设备已重置。");
        }

        @Override
        public void onDfuCompleted(String deviceAddress) {
            //Method called when the DFU process succeeded.
            //bar.setVisibility(View.GONE);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 设置Toast位置
                    Toast toast;
                    Display display = getWindowManager().getDefaultDisplay();
                    int height = display.getHeight();
                    toast = Toast.makeText(OTAActivity.this, "OTA已完成", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, height / 6);
                    toast.show();
                    finish();
                }
            });
            Log.d("debug", "DFU已完成");
            mService.disconnect();
            mService.close();
        }

        @Override
        public void onDfuAborted(String deviceAddress) {
            //当DFU进程已中止时调用的方法。
            Log.d("debug", "当DFU进程已中止时调用的方法。");
        }

        @Override
        public void onError(String deviceAddress, int error, int errorType, String message) {
            //发生错误时调用的方法。
            Log.d("debug", "发生错误时调用的方法onError");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 设置Toast位置
                    Toast toast;
                    Display display = getWindowManager().getDefaultDisplay();
                    int height = display.getHeight();
                    toast = Toast.makeText(OTAActivity.this, "OTA失败", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, height / 6);
                    toast.show();
                    finish();
                }
            });
        }
    };

    private void initView() {
        filePath = (EditText) findViewById(R.id.otaText);
        otaUpdata = (Button) findViewById(R.id.otaUpdata);
        otaUpdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 判断文件是否存在
                String otaText = filePath.getText().toString().trim();
                File file = new File(otaText);
                Log.i(TAG, "====otaText====" + otaText);
                if (!file.exists()) {
                    // 设置Toast位置
                    Toast toast;
                    Display display = getWindowManager().getDefaultDisplay();
                    int height = display.getHeight();

                    toast = Toast.makeText(OTAActivity.this, "文件不存在", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.TOP, 0, height / 6);
                    toast.show();
                    return;
                }

                bleManager.setOTAModel();
                //openOTA();
               /* byte[] cmd_data = new byte[3];
                cmd_data[0] = (byte) 0xF4;
                cmd_data[1] = (byte) 0x01;
                cmd_data[2] = (byte) 0xFF;
                writeCmd(cmd_data, 2);*/

                /*String str = "";
                for (int i = 0; i < cmd_data.length; ++i) {
                    str += addZero(Integer.toHexString(cmd_data[i] & 0xFF)) + " ";
                }
                listAdapter.add("OTA" + "->cmd：" + str);
                listAdapter.notifyDataSetChanged();*/

                /*if (mBluetoothAdapter != null) {
                    Log.i(TAG, "otaButton---" + mBluetoothAdapter.getBondedDevices());
                } else {
                    Log.i(TAG, "mBluetoothAdapter is null");
                }*/

                try {
                    scanLeDevice(true);
                } catch (Exception e) {
                    Log.e(TAG, "---scanLe---" + e.toString());
                }
            }
        });
    }

    /**
     * 写命令
     */
    private void openOTA() {
        try {
            Class<?> c = Class.forName("com.tqltech.tqlpencomm.PenCommAgent");
            Object obj1 = c.newInstance();
            //Object[] obj2 = new Object[1];
            Method payMethod = c.getMethod("writePenOTA");
            payMethod.invoke(obj1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            bleManager.FindAllDevices(new BLEScanner.OnBLEScanListener() {

                @Override
                public void onScanResult(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    /*mLeDeviceListAdapter.addDevice(device);
                    Log.e(TAG, "devices is " + device.getAddress());
                    mLeDeviceListAdapter.notifyDataSetChanged();


                    byte[] scanData = result.getScanRecord().getBytes();
                    Log.i(TAG, "======" + scanData.length + "===" + result.getDevice().getAddress());*/

                    Log.i(TAG, "onScanResult------");
                    Log.i(TAG, "scan result->" + device.getAddress() + "========" + mAddress + "===" + device.getName());
                    if (device.getAddress().equals(mAddress) && device.getName().contains("In DFU")) {
                        Log.e(TAG, "find DFU device");
                        Boolean flag = mService.connect(mAddress);
                        Log.i(TAG, "connect flag------" + flag);
                        if (flag) {
                            scanLeDevice(false);
                            mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(mAddress);
                            Log.i(TAG, "onScanResult mDevice------" + mDevice);
                            try {
                                handler.obtainMessage(0x123, "").sendToTarget();
                            } catch (Exception e) {
                                Log.e(TAG, "scan result----" + e.toString());
                            }
                        }
                    }
                }

                @Override
                public void onScanFailed(BLEException bleException) {

                }
            });

        } else {
            bleManager.stopFindAllDevices();
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0x123) {
                Log.i("debug", "---handler---");
                try {
                    //startDFU(mDevice, true, false, true, 0, "/sdcard/TQL112_ota_test.zip");
                    String otaText = filePath.getText().toString();
                    //String otaText = "/sdcard/ota_TQL-112-BT-B10.zip";
                    File file = new File(otaText);
                    Log.i(TAG, "111====otaText====" + otaText);
                    if (file.exists()) {
                        startDFU(mDevice, true, false, true, 0, otaText);
                    } else {
                        // 设置Toast位置
                        Toast toast;
                        Display display = getWindowManager().getDefaultDisplay();
                        int height = display.getHeight();

                        toast = Toast.makeText(OTAActivity.this, "文件不存在", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.TOP, 0, height / 6);
                        toast.show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "===handle===" + e.toString());
                }
            }
        }
    };

    /**
     * 启动DFU升级服务
     *
     * @param bluetoothDevice 蓝牙设备
     * @param keepBond        升级后是否保持连接
     * @param force           将DFU设置为true将防止跳转到DFU Bootloader引导加载程序模式
     * @param PacketsReceipt  启用或禁用数据包接收通知（PRN）过程。
     *                        默认情况下，在使用Android Marshmallow或更高版本的设备上禁用PEN，并在旧设备上启用。
     * @param numberOfPackets 如果启用分组接收通知过程，则此方法设置在接收PEN之前要发送的分组数。 PEN用于同步发射器和接收器。
     * @param filePath        约定匹配的ZIP文件的路径。
     */
    private void startDFU(BluetoothDevice bluetoothDevice, boolean keepBond, boolean force,
                          boolean PacketsReceipt, int numberOfPackets, String filePath) {
        Log.i("debug", "---startDFU---");
        new DfuServiceInitiator(mAddress)
                .setDisableNotification(true)
                .setKeepBond(false)
                .setZip(filePath)
                .start(OTAActivity.this, DfuService.class);
    }

    @Override
    public void onClick(View v) {

    }
}