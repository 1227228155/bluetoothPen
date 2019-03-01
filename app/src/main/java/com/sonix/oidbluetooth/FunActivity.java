package com.sonix.oidbluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.githang.statusbar.StatusBarCompat;
import com.tqltech.tqlpencomm.PenCommAgent;

import java.text.SimpleDateFormat;
import java.util.Date;


public class FunActivity extends Activity {
    private final static String TAG = "FunActivity";

    private TextView tvMac;
    private TextView tvVer;
    private TextView tvMCU;
    private TextView tvCustomerID;
    private TextView tvRtcTime;
    private TextView tvBat;
    private TextView tvUsedMem;
    private Button btRTC;
    private EditText edPenName;
    private Button btPenName;
    private EditText edSensi;
    private Button btSensi;
    private EditText edofftime;
    private Button btofftime;
    private Switch swPowerMode;
    private Switch swBeepMode;
    private Switch swEnableLED;
    private PenCommAgent bleManager;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(false);

        Log.i(TAG, "on Create start");
        setContentView(R.layout.pen_status);
        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.statusColor), true);

        tvMac = (TextView) findViewById(R.id.mac);
        tvVer = (TextView) findViewById(R.id.ver);
        tvMCU = (TextView) findViewById(R.id.ver1);
        tvCustomerID = (TextView) findViewById(R.id.ver2);
        tvRtcTime = (TextView) findViewById(R.id.timer);
        tvBat = (TextView) findViewById(R.id.battery);
        tvUsedMem = (TextView) findViewById(R.id.usedmem);

        btRTC = (Button) findViewById(R.id.setRTC);

        edPenName = (EditText) findViewById(R.id.penname);
        btPenName = (Button) findViewById(R.id.pennamebt);

        edPenName = (EditText) findViewById(R.id.penname);
        btPenName = (Button) findViewById(R.id.pennamebt);

        edofftime = (EditText) findViewById(R.id.offtime);
        btofftime = (Button) findViewById(R.id.offtimebt);

        edSensi = (EditText) findViewById(R.id.sensi);
        btSensi = (Button) findViewById(R.id.sensibt);

        swPowerMode = (Switch) findViewById(R.id.powermodesw);
        swBeepMode = (Switch) findViewById(R.id.beepsw);
        swEnableLED = (Switch) findViewById(R.id.enableLed);
        bleManager = PenCommAgent.GetInstance(getApplication());

        Log.i(TAG, "on Create end");
        setStatusVal();

        btRTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bleManager.ReqAdjustRTC();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            bleManager.getPenAllStatus();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 1000);
            }
        });

        btPenName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    String name = edPenName.getText().toString();
                    ApplicationResources.tmp_mPenName = name;
                    bleManager.setPenName(name);
                }
            }
        });

        btSensi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    int sens = Integer.parseInt(edSensi.getText().toString());
                    if (sens < 0 || sens > 4) {
                        // 通过AlertDialog.Builder这个类来实例化我们的一个AlertDialog的对象
                        AlertDialog.Builder builder = new AlertDialog.Builder(FunActivity.this);
                        //    设置Title的图标
                        builder.setIcon(R.drawable.ic_launcher);
                        //    设置Title的内容
                        builder.setTitle("提示");
                        //    设置Content来显示一个信息
                        builder.setMessage("灵敏度范围0-4");
                        builder.setPositiveButton("确定", null);
                        builder.show();

                        return;
                    }

                    ApplicationResources.tmp_mPenSens = sens;
                    bleManager.setPenSensitivity((short) sens);
                }
            }
        });

        btofftime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    int offtime = Integer.parseInt(edofftime.getText().toString());
                    if (offtime < 0 || offtime > 120) {
                        // 通过AlertDialog.Builder这个类来实例化我们的一个AlertDialog的对象
                        AlertDialog.Builder builder = new AlertDialog.Builder(FunActivity.this);
                        //    设置Title的图标
                        builder.setIcon(R.drawable.ic_launcher);
                        //    设置Title的内容
                        builder.setTitle("提示");
                        //    设置Content来显示一个信息
                        builder.setMessage("自动关机时间在0-120之内");
                        builder.setPositiveButton("确定", null);
                        builder.show();

                        return;
                    }

                    ApplicationResources.tmp_mPowerOffTime = offtime;
                    bleManager.setPenAutoShutDownTime((short) offtime);
                }
            }
        });

        swPowerMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    ApplicationResources.tmp_mPowerOnMode = isChecked;
                    Log.e(TAG, "set penEnableLed-------");
                    bleManager.setPenAutoPowerOnMode(isChecked);
                }
            }
        });

        swBeepMode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    ApplicationResources.tmp_mBeep = isChecked;
                    Log.e(TAG, "set setPenBeepMode-------");
                    bleManager.setPenBeepMode(isChecked);
                }
            }
        });

        swEnableLED.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if ((bleManager != null) && (bleManager.isConnect())) {
                    ApplicationResources.tmp_mEnableLED = isChecked;
                    Log.e(TAG, "set penEnableLed-------");
                    bleManager.setPenEnableLED(isChecked);
                }
            }
        });
    }

    /**
     * 时间戳转换成固定格式字符串
     *
     * @param time
     * @return
     */
    public static String timedate(Long time) {
        SimpleDateFormat sdr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        @SuppressWarnings("unused")
        int i = 0;
        long j = 0;
        try {
            j = Long.parseLong(String.valueOf(time));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        String times = sdr.format(new Date(j * 1000L));
        Log.i(TAG, "====timedate====" + j + "====" + (j * 1000L) + "==times is ==" + times);
        return times;
    }

    /**
     * 状态显示
     */
    private void setStatusVal() {
        long a = 1262275200 + ApplicationResources.mTimer;
        tvRtcTime.setText(timedate(a));
        tvMac.setText(ApplicationResources.mBTMac);
        tvVer.setText(ApplicationResources.mFirmWare);
        tvMCU.setText(ApplicationResources.mMCUFirmWare);
        tvCustomerID.setText(ApplicationResources.mCustomerID);
        tvBat.setText(Integer.toString(ApplicationResources.mBattery));
        tvUsedMem.setText(Integer.toString(ApplicationResources.mUsedMem));

        Log.i(TAG, "name=" + ApplicationResources.mPenName);
        edPenName.setText(ApplicationResources.mPenName);
        edofftime.setText(Integer.toString(ApplicationResources.mPowerOffTime));
        edSensi.setText(Integer.toString(ApplicationResources.mPenSens));

        if (ApplicationResources.mPowerOnMode == true) {
            Log.e(TAG, "power set check true");
            swPowerMode.setChecked(true);
        } else {
            Log.e(TAG, "power set check false");
            swPowerMode.setChecked(false);
        }

        if (ApplicationResources.mBeep == true) {
            Log.e(TAG, "beep set check true");
            swBeepMode.setChecked(true);
        } else {
            Log.e(TAG, "beep set check false");
            swBeepMode.setChecked(false);
        }

        if (ApplicationResources.tmp_mEnableLED == true) {
            Log.e(TAG, "led set check true");
            swEnableLED.setChecked(true);
        } else {
            Log.e(TAG, "led set check false");
            swEnableLED.setChecked(false);
        }
    }

    private final BroadcastReceiver mPenStatuUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_PEN_STATUS_CHANGE.equals(action)) {
                setStatusVal();
            }
        }
    };

    @Override
    protected void onResume() {
        Log.i(TAG, "on resume start");
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_PEN_STATUS_CHANGE);
        registerReceiver(mPenStatuUpdateReceiver, intentFilter);
        if ((bleManager != null) && (bleManager.isConnect())) {
            Log.i(TAG, "Get Pen ALL Status.....");
            try {
                bleManager.getPenAllStatus();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    protected void onPause() {
        Log.i(TAG, "on pause start");
        super.onPause();
        if (mPenStatuUpdateReceiver != null) {
            unregisterReceiver(mPenStatuUpdateReceiver);
        }
    }
}
