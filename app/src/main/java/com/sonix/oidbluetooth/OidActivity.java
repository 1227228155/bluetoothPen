package com.sonix.oidbluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.collect.ArrayListMultimap;
import com.sonix.oid.DrawView;
import com.sonix.oid.RoundProgressBar;
import com.tqltech.tqlpencomm.Dot;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.util.BLEFileUtil;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;


public class OidActivity extends Activity {

    private final static String TAG = "OidActivity";
    private final static boolean isSaveLog = false;          //是否保存绘制数据到日志
    private final static String LOGPATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/TQL/"; //绘制数据保存目录

    private BluetoothLEService mService = null;              //蓝牙服务
    private static final int REQUEST_SELECT_DEVICE = 1;      //蓝牙扫描
    private static final int REQUEST_ENABLE_BT = 2;          //开启蓝牙
    private static final int REQUEST_LOCATION_CODE = 100;    //请求位置权限

    private int penType = 1;                                 //笔类型（0：TQL-101  1：TQL-111  2：TQL-112 3: TQL-101A）

    private double XDIST_PERUNIT = Constants.XDIST_PERUNIT;  //码点宽
    private double YDIST_PERUNIT = Constants.YDIST_PERUNIT;  //码点高
    private double A5_WIDTH = Constants.A5_WIDTH;            //本子宽
    private double A5_HEIGHT = Constants.A5_HEIGHT;          //本子高
    private int BG_REAL_WIDTH = Constants.BG_REAL_WIDTH;     //资源背景图宽
    private int BG_REAL_HEIGHT = Constants.BG_REAL_HEIGHT;   //资源背景图高

    private int BG_WIDTH;                                    //显示背景图宽
    private int BG_HEIGHT;                                   //显示背景图高
    private int A5_X_OFFSET;                                 //笔迹X轴偏移量
    private int A5_Y_OFFSET;                                 //笔迹Y轴偏移量
    private int gcontentLeft;                                //内容显示区域left坐标
    private int gcontentTop;                                 //内容显示区域top坐标

    public static float mWidth;                              //屏幕宽
    public static float mHeight;                             //屏幕高

    private float mov_x;                                     //声明起点坐标
    private float mov_y;                                     //声明起点坐标
    private int gCurPageID = -1;                             //当前PageID
    private int gCurBookID = -1;                             //当前BookID
    private float gScale = 1;                                //笔迹缩放比例
    private int gColor = 6;                                  //笔迹颜色
    private int gWidth = 1;                                  //笔迹粗细
    private int gSpeed = 30;                                 //笔迹回放速度
    private float gOffsetX = 0;                              //笔迹x偏移
    private float gOffsetY = 0;                              //笔迹y偏移

    private ArrayListMultimap<Integer, Dots> dot_number = ArrayListMultimap.create();  //Book=100笔迹数据
    private ArrayListMultimap<Integer, Dots> dot_number1 = ArrayListMultimap.create(); //Book=0笔迹数据
    private ArrayListMultimap<Integer, Dots> dot_number2 = ArrayListMultimap.create(); //Book=1笔迹数据
    private ArrayListMultimap<Integer, Dots> dot_number4 = ArrayListMultimap.create(); //笔迹回放数据
    private Intent serverIntent = null;
    private Intent LogIntent = null;
    private PenCommAgent bleManager;
    private String penAddress;

    public static float g_x0, g_x1, g_x2, g_x3;
    public static float g_y0, g_y1, g_y2, g_y3;
    public static float g_p0, g_p1, g_p2, g_p3;
    public static float g_vx01, g_vy01, g_n_x0, g_n_y0;
    public static float g_vx21, g_vy21;
    public static float g_norm;
    public static float g_n_x2, g_n_y2;

    private int gPIndex = -1;
    private boolean gbSetNormal = false;
    private boolean gbCover = false;

    private float pointX;
    private float pointY;
    private int pointZ;

    private boolean bIsOfficeLine = false;

    private ImageView gImageView;
    private RelativeLayout gLayout;
    private RelativeLayout layout;
    private TextView text;
    private DrawView[] bDrawl = new DrawView[2];  //add 2016-06-15 for draw
    private RelativeLayout dialog;
    private Button confirmBtn;
    private TextView textView;
    private RoundProgressBar bar;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(final ComponentName className, IBinder rawBinder) {
            mService = ((BluetoothLEService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                finish();
            }

            mService.setOnDataReceiveListener(new BluetoothLEService.OnDataReceiveListener() {
                @Override
                public void onDataReceive(final Dot dot) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                ProcessDots(dot);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }

                @Override
                public void onOfflineDataReceive(final Dot dot) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                ProcessDots(dot);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    });
                }

                @Override
                public void onFinishedOfflineDown(boolean success) {
                    Log.i(TAG, "---------onFinishedOfflineDown--------" + success);
                }

                @Override
                public void onOfflineDataNum(final int num) {
                    Log.i(TAG, "---------onOfflineDataNum1--------" + num);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //if (num == 0) {
                                    //    return;
                                    //}

                                    dialog = (RelativeLayout) findViewById(R.id.dialog);
                                    dialog.setVisibility(View.VISIBLE);
                                    textView = (TextView) findViewById(R.id.textView2);
                                    textView.setText("离线数量有" + Integer.toString(num * 10) + "bytes");
                                    confirmBtn = (Button) findViewById(R.id.button);
                                    confirmBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dialog.setVisibility(View.GONE);
                                        }
                                    });
                                }
                            });
                        }
                    });
                }

                @Override
                public void onReceiveOIDSize(int OIDSize) {
                    Log.i("TEST1", "-----read OIDSize=====" + OIDSize);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            gCurPageID = -1;
                        }
                    });
                }

                @Override
                public void onReceiveOfflineProgress(final int i) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layout.setVisibility(View.VISIBLE);
                            text.setText("开始缓存离线数据");
                            bar.setProgress(i);
                            Log.e(TAG, "onReceiveOfflineProgress----" + i);
                            if (i == 100) {
                                layout.setVisibility(View.GONE);
                                bar.setProgress(0);
                            }
                        }
                    });
                }

                @Override
                public void onDownloadOfflineProgress(final int i) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            layout.setVisibility(View.VISIBLE);
                            text.setText("开始下载离线数据");
                            bar.setProgress(i);
                            if (i == 100) {
                                layout.setVisibility(View.GONE);
                                bar.setProgress(0);
                            }
                        }
                    });
                }

                @Override
                public void onReceivePenLED(final byte color) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "receive led is " + color);
                            switch (color) {
                                case 1: // blue
                                    gColor = 5;
                                    break;
                                case 2: // green
                                    gColor = 3;
                                    break;
                                case 3: // cyan
                                    gColor = 8;
                                    break;
                                case 4: // red
                                    gColor = 1;
                                    break;
                                case 5: // magenta
                                    gColor = 7;
                                    break;
                                case 6: // yellow
                                    gColor = 2;
                                    break;
                                case 7: // white
                                    gColor = 6;
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
                }

                @Override
                public void onOfflineDataNumCmdResult(boolean success) {
                    Log.i(TAG, "onOfflineDataNumCmdResult---------->" + success);
                }

                @Override
                public void onDownOfflineDataCmdResult(boolean success) {
                    Log.i(TAG, "onDownOfflineDataCmdResult---------->" + success);
                }

                @Override
                public void onWriteCmdResult(int code) {
                    Log.i(TAG, "onWriteCmdResult---------->" + code);
                }

                @Override
                public void onReceivePenType(int type) {
                    Log.i(TAG, "onReceivePenType type---------->" + type);
                    penType = type;
                }
            });
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSDK()) {
            PermissionUtils.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_LOCATION_CODE,
                    "5.0之后使用蓝牙需要位置权限",
                    new PermissionUtils.OnPermissionResult() {
                        @Override
                        public void granted(int requestCode) {

                        }

                        @Override
                        public void denied(int requestCode) {

                        }
                    });
        }

        // actionBar不显示标题
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(ApplicationResources.getLocalVersionName(this));
        setContentView(R.layout.draw);
        //StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.statusColor), true);

        Intent gattServiceIntent = new Intent(this, BluetoothLEService.class);
        boolean bBind = bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        bDrawl[0] = new DrawView(this);
        bDrawl[0].setVcolor(Color.YELLOW);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        mWidth = dm.widthPixels;
        mHeight = dm.heightPixels;

        float density = dm.density;         // 屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = dm.densityDpi;     // 屏幕密度dpi（120 / 160 / 240）
        Log.e(TAG, "density=======>" + density + ",densityDpi=======>" + densityDpi);
        // 屏幕宽度算法:屏幕宽度（像素）/屏幕密度
        int screenWidth = (int) (mWidth / density);  // 屏幕宽度(dp)
        int screenHeight = (int) (mHeight / density);// 屏幕高度(dp)
        Log.e(TAG, "width=======>" + screenWidth);
        Log.e(TAG, "height=======>" + screenHeight);

        Log.e(TAG, "-----screen pixel-----width:" + mWidth + ",height:" + mHeight);

        gLayout = (RelativeLayout) super.findViewById(R.id.mylayout);
        RelativeLayout.LayoutParams param = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        param.width = (int) mWidth;
        param.height = (int) mHeight;
        param.rightMargin = 1;
        param.bottomMargin = 1;

        gLayout.addView(bDrawl[0], param);
        drawInit();

        bar = (RoundProgressBar) findViewById(R.id.progressBar);
        layout = (RelativeLayout) findViewById(R.id.layout);
        text = (TextView) findViewById(R.id.text);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume color=" + gColor);
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        //if (sp == null) {
        //    sp = getSharedPreferences("user", Context.MODE_PRIVATE);
        //}
        //gColor = sp.getInt("color", 6);

        if (isFinishing()) {
            Log.i(TAG, "---onPause---");
            unbindService(mServiceConnection);
            mService.disconnect();
            mService.close();
            mService.stopSelf();
            mService = null;
        }
        if (mService != null) {
            isConnected = mService.getPenStatus();
        }
        if (!isConnected && gImageView != null) {
            gImageView.setVisibility(View.GONE);
            bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            gCurPageID = -1;
            /*dot_number.clear();
            dot_number1.clear();
            dot_number2.clear();
            dot_number4.clear();*/
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.i(TAG, "onPause color=" + gColor);
        //SharedPreferences.Editor editor = sp.edit();
        //editor.putInt("color", gColor);
        //editor.apply();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop" + isFinishing());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        bleManager.disconnect(penAddress);
        bDrawl[0].DrawDestroy();
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

        dot_number.clear();
        dot_number = null;

        dot_number1.clear();
        dot_number1 = null;

        dot_number2.clear();
        dot_number2 = null;

        dot_number4.clear();
        dot_number4 = null;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem mi = menu.getItem(i);
            String title = mi.getTitle().toString();
            Spannable newTitle = new SpannableString(title);
            newTitle.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.menuTextColor)), 0, newTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            mi.setTitle(newTitle);

            if (mi.getSubMenu() != null) {
                for (int j = 0; j < mi.getSubMenu().size(); ++j) {
                    MenuItem ni = mi.getSubMenu().getItem(j);
                    title = ni.getTitle().toString();
                    newTitle = new SpannableString(title);
                    newTitle.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.menuTextColor)), 0, newTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ni.setTitle(newTitle);
                }
            }
        }
        return true;
    }

    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equalsIgnoreCase("MenuBuilder")) {
                try {
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private static boolean checkSDK() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        if (!isConnected) {
            menu.findItem(R.id.flag1).setVisible(false);
            menu.findItem(R.id.flag2).setVisible(true);
            menu.findItem(R.id.clear).setVisible(false);
            menu.findItem(R.id.color).setVisible(false);
            menu.findItem(R.id.width).setVisible(false);
            menu.findItem(R.id.replay).setVisible(false);
            menu.findItem(R.id.setting).setVisible(false);
            menu.findItem(R.id.offline).setVisible(false);
            menu.findItem(R.id.LED).setVisible(false);
            menu.findItem(R.id.OTA).setVisible(false);
        } else {
            menu.findItem(R.id.flag1).setVisible(true);
            menu.findItem(R.id.flag2).setVisible(false);
            menu.findItem(R.id.clear).setVisible(true);
            menu.findItem(R.id.color).setVisible(true);
            menu.findItem(R.id.width).setVisible(true);
            menu.findItem(R.id.replay).setVisible(true);
            menu.findItem(R.id.setting).setVisible(true);
            menu.findItem(R.id.offline).setVisible(true);
            menu.findItem(R.id.LED).setVisible(false);
            menu.findItem(R.id.OTA).setVisible(false);
        }

        gImageView = (ImageView) findViewById(R.id.imageView2);//得到ImageView对象的引用
        gImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        //计算
        float ratio = 0.95f;
        ratio = (ratio * mWidth) / BG_REAL_WIDTH;
        BG_WIDTH = (int) (BG_REAL_WIDTH * ratio);
        BG_HEIGHT = (int) (BG_REAL_HEIGHT * ratio);

        gcontentLeft = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getLeft();
        gcontentTop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();

        A5_X_OFFSET = (int) (mWidth - gcontentLeft - BG_WIDTH) / 2;
        A5_Y_OFFSET = (int) (mHeight - gcontentTop - BG_HEIGHT) / 2;

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        bleManager = PenCommAgent.GetInstance(getApplication());

        // 0-free format;1-for A4;2-for A3
        Log.i(TAG, "-----------setDataFormat-------------");
        bleManager.setXYDataFormat(1);

        switch (item.getItemId()) {
            case R.id.connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, SelectDeviceActivity.class);
                startActivityForResult(serverIntent, REQUEST_SELECT_DEVICE);
                return true;
            case R.id.clear:
                drawInit();
                bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                if (!bIsReply) {
                    dot_number.clear();
                    dot_number1.clear();
                    dot_number2.clear();
                    dot_number4.clear();
                }
                return true;
            case R.id.color_blue:
                gColor = 5;
                bleManager.setPenLedConfig(0x01);
                return true;
            case R.id.color_green:
                gColor = 3;
                bleManager.setPenLedConfig(0x02);
                return true;
            case R.id.color_cyan:
                gColor = 8;
                bleManager.setPenLedConfig(0x03);
                return true;
            case R.id.color_red:
                gColor = 1;
                bleManager.setPenLedConfig(0x04);
                return true;
            case R.id.color_magenta:
                gColor = 7;
                bleManager.setPenLedConfig(0x05);
                return true;
            case R.id.color_yellow:
                gColor = 2;
                bleManager.setPenLedConfig(0x06);
                return true;
            case R.id.color_black:
                gColor = 6;
                bleManager.setPenLedConfig(0x07);
                return true;
            case R.id.w1:
                gWidth = 2;
                return true;
            case R.id.w2:
                gWidth = 4;
                return true;
            case R.id.w3:
                gWidth = 6;
                return true;
            case R.id.w4:
                gWidth = 8;
                return true;
            case R.id.w5:
                gWidth = 10;
                return true;
            case R.id.w6:
                gWidth = 12;
                return true;
            case R.id.s1:
                if (bIsReply) {
                    return false;
                }
                bIsReply = true;
                gSpeed = 10;
                RunReplay();
                return true;
            case R.id.s2:
                if (bIsReply) {
                    return false;
                }
                bIsReply = true;
                gSpeed = 20;
                RunReplay();
                return true;
            case R.id.s3:
                if (bIsReply) {
                    return false;
                }
                bIsReply = true;
                gSpeed = 30;
                RunReplay();
                return true;
            case R.id.s4:
                if (bIsReply) {
                    return false;
                }
                bIsReply = true;
                gSpeed = 40;
                RunReplay();
                return true;
            case R.id.s5:
                if (bIsReply) {
                    return false;
                }
                bIsReply = true;
                gSpeed = 50;
                RunReplay();
                return true;
            case R.id.setting:
                LogIntent = new Intent(this, FunActivity.class);
                startActivity(LogIntent);
                return true;
            case R.id.start:
                bleManager.ReqOfflineDataTransfer(true);
                return true;
            case R.id.stop:
                bleManager.ReqOfflineDataTransfer(false);
                return true;
            case R.id.clear_offline:
                bleManager.RemoveOfflineData();
                return true;
            case R.id.offline_num:
                bleManager.getPenOfflineDataList();
                return true;
            case R.id.white:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x07);
                return true;
            case R.id.yellow:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x06);
                return true;
            case R.id.magenta:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x05);
                return true;
            case R.id.red:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x04);
                return true;
            case R.id.cyan:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x03);
                return true;
            case R.id.green:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x02);
                return true;
            case R.id.blue:
                if (!bIsOfficeLine) {
                    break;
                }
                bleManager.setPenLedConfig(0x01);
                return true;
            case R.id.reset:
                if (bleManager != null) {
                    bleManager.setPenFactoryReset();
                }
                return true;
            case R.id.OTA:
                Intent intent = new Intent(this, OTAActivity.class);
                intent.putExtra("addr", penAddress);
                intent.putExtra("type", penType);
                startActivity(intent);
                return true;
        }
        return false;
    }

    private boolean bIsReply = false;

    public void RunReplay() {
        if (gCurPageID < 0) {
            bIsReply = false;
            return;
        }

        drawInit();
        bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ReplayCurrentPage(gCurBookID, gCurPageID, gSpeed);
            }
        }).start();
    }

    public void ReplayCurrentPage(int BookID, int PageID, int SpeedID) {
        if (BookID == 100) {
            dot_number4 = dot_number;
        } else if (BookID == 0) {
            dot_number4 = dot_number1;
        } else if (BookID == 1) {
            dot_number4 = dot_number2;
        }

        if (dot_number4.isEmpty()) {
            bIsReply = false;
            return;
        }

        Set<Integer> keys = dot_number4.keySet();
        for (int key : keys) {
            Log.i(TAG, "=========pageID=======" + PageID + "=====Key=====" + key);
            bIsReply = true;
            if (key == PageID) {
                List<Dots> dots = dot_number4.get(key);
                for (Dots dot : dots) {
                    Log.i(TAG, "=========pageID1111=======" + dot.pointX + "====" + dot.pointY + "===" + dot.ntype);
                    drawSubFountainPen1(bDrawl[0], gScale, gOffsetX, gOffsetY, dot.penWidth,
                            dot.pointX, dot.pointY, dot.force, dot.ntype, dot.ncolor);

                    bDrawl[0].postInvalidate();
                    SystemClock.sleep(SpeedID);
                }
            }
        }

        bIsReply = false;

        gPIndex = -1;
        return;
    }

    public void SetPenColor(int ColorIndex) {
        switch (ColorIndex) {
            case 0:
                bDrawl[0].paint.setColor(Color.GRAY);
                return;
            case 1:
                bDrawl[0].paint.setColor(Color.RED);
                return;
            case 2:
                bDrawl[0].paint.setColor(Color.rgb(192, 192, 0));
                return;
            case 3:
                bDrawl[0].paint.setColor(Color.rgb(0, 128, 0));
                return;
            case 4:
                bDrawl[0].paint.setColor(Color.rgb(0, 0, 192));
                return;
            case 5:
                bDrawl[0].paint.setColor(Color.BLUE);
                return;
            case 6:
                bDrawl[0].paint.setColor(Color.BLACK);
                return;
            case 7:
                bDrawl[0].paint.setColor(Color.MAGENTA);
                return;
            case 8:
                bDrawl[0].paint.setColor(Color.CYAN);
                return;
        }
        return;
    }

    public void drawInit() {
        bDrawl[0].initDraw();
        bDrawl[0].setVcolor(Color.WHITE);
        bDrawl[0].setVwidth(1);

        SetPenColor(gColor);
        bDrawl[0].paint.setStrokeCap(Paint.Cap.ROUND);
        bDrawl[0].paint.setStyle(Paint.Style.FILL);
        bDrawl[0].paint.setAntiAlias(true);
        bDrawl[0].invalidate();
    }

    private int connectNum = 0;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    try {
                        boolean flag = mService.connect(deviceAddress);
                        penAddress = deviceAddress;
                        // TODO spp
                        //bleManager.setSppConnect(deviceAddress);
                    } catch (Exception e) {
                        Log.i(TAG, "connect-----" + e.toString());
                    }
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "onBackPressed");
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLEService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLEService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLEService.RECEVICE_DOT);
        return intentFilter;
    }

    private String gStrHH = "";
    private boolean bLogStart = false;

    private void saveData(Integer bookID, Integer pageID, float pointX, float pointY, int force, int ntype, int penWidth, int color, int counter, int angle) {
        Log.i(TAG, "======savaData pageID======" + pageID + "========sdfsdf" + angle);
        Dots dot = new Dots(bookID, pageID, pointX, pointY, force, ntype, penWidth, color, counter, angle);

        if (bookID == 100) {
            dot_number.put(pageID, dot);
        } else if (bookID == 0) {
            dot_number1.put(pageID, dot);
        } else if (bookID == 1) {
            dot_number2.put(pageID, dot);
        }
    }

    private void saveOutDotLog(Integer bookID, Integer pageID, float pointX, float pointY, int force, int ntype, int penWidth, int color, int counter, int angle) {
        Log.i(TAG, "======savaData pageID======" + pageID + "========sdfsdf" + angle);
        Dots dot = new Dots(bookID, pageID, pointX, pointY, force, ntype, penWidth, color, counter, angle);



        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
        SimpleDateFormat formatter1 = new SimpleDateFormat("yyyyMMdd");
        Date curDate = new Date(System.currentTimeMillis());//获取当前时间
        String str = formatter.format(curDate);
        String str1 = formatter1.format(curDate);
        String hh = str.substring(0, 2);

        if (!gStrHH.equals(hh)) {
            Log.i(TAG, "sssssss " + gStrHH + " " + hh);
            gStrHH = hh;
            bLogStart = true;
        }

        String txt = str + "BookID: " + bookID + " PageID: " + pageID + " Counter: " + counter + "  pointX: " + gpointX + "  pointY: " + gpointY + "  force: " + force + "  angle: " + angle;
        String fileName = str1 + gStrHH + ".log";
        if (isSaveLog) {
            if (bLogStart) {
                BLEFileUtil.writeTxtToFile("-------------------------TQL SmartPen LOG--------------------------", LOGPATH, fileName);
                bLogStart = false;
            }

            BLEFileUtil.writeTxtToFile(txt, LOGPATH, fileName);
        }
    }

    private float gpointX;
    private float gpointY;
    private int[] bookID = new int[]{0, 1, 100, 168};
    private Boolean bBookIDIsValide = false;

    private void ProcessEachDot(Dot dot) {
        int counter = 0;
        pointZ = dot.force;
        counter = dot.Counter;

        if (pointZ < 0 && pointZ != 0) {
            Log.i(TAG, "Counter=" + counter + ", Pressure=" + pointZ + "  Cut!!!!!");
            return;
        }

        int tmpx = dot.x;
        pointX = dot.fx;
        pointX /= 100.0;
        pointX += tmpx;

        int tmpy = dot.y;
        pointY = dot.fy;
        pointY /= 100.0;
        pointY += tmpy;

        gpointX = pointX;
        gpointY = pointY;

        //getRedio(BG_REAL_WIDTH);
        pointX *= (BG_WIDTH);
        float ax = (float) (A5_WIDTH / XDIST_PERUNIT);
        pointX /= ax;

        pointY *= (BG_HEIGHT);
        float ay = (float) (A5_HEIGHT / YDIST_PERUNIT);
        pointY /= ay;

        pointX += A5_X_OFFSET;
        pointY += A5_Y_OFFSET;

        if (isSaveLog) {
            saveOutDotLog(gCurBookID, gCurPageID, pointX, pointY, pointZ, 1, gWidth, gColor, dot.Counter, dot.angle);
        }

        if (pointZ > 0) {
            if (dot.type == Dot.DotType.PEN_DOWN) {
                Log.i(TAG, "PEN_DOWN");
                gPIndex = 0;
                int PageID, BookID;
                PageID = dot.PageID;
                BookID = dot.BookID;
                if (PageID < 0 || BookID < 0 || PageID > 64) {
                    // 谨防笔连接不切页的情况
                    return;
                }

                for (int i = 0; i < bookID.length; ++i) {
                    if (BookID == bookID[i]) {
                        bBookIDIsValide = true;
                    }
                }

                if (!bBookIDIsValide) {
                    return;
                }
                Log.i(TAG, "PageID=" + PageID + ",gCurPageID=" + gCurPageID + ",BookID=" + BookID + ",gCurBookID=" + gCurBookID);
                if (PageID != gCurPageID || BookID != gCurBookID) {
                    gbSetNormal = false;
                    bBookIDIsValide = false;
                    SetBackgroundImage(BookID, PageID);
                    gImageView.setVisibility(View.VISIBLE);
                    bIsOfficeLine = true;
                    gCurPageID = PageID;
                    gCurBookID = BookID;
                    drawInit();
                    DrawExistingStroke(gCurBookID, gCurPageID);
                }

                SetPenColor(gColor);
                drawSubFountainPen2(bDrawl[0], gScale, gOffsetX, gOffsetY, gWidth, pointX, pointY, pointZ, 0);

                // 保存屏幕坐标，原始坐标会使比例缩小
                saveData(gCurBookID, gCurPageID, pointX, pointY, pointZ, 0, gWidth, gColor, dot.Counter, dot.angle);
                mov_x = pointX;
                mov_y = pointY;
                return;
            }

            if (dot.type == Dot.DotType.PEN_MOVE) {
                Log.i(TAG, "PEN_MOVE");
                gPIndex = 0;
                // Pen Move
                gPIndex += 1;
                mN += 1;
                mov_x = pointX;
                mov_y = pointY;

                SetPenColor(gColor);
                drawSubFountainPen2(bDrawl[0], gScale, gOffsetX, gOffsetY, gWidth, pointX, pointY, pointZ, 1);
                bDrawl[0].invalidate();
                // 保存屏幕坐标，原始坐标会使比例缩小
                saveData(gCurBookID, gCurPageID, pointX, pointY, pointZ, 1, gWidth, gColor, dot.Counter, dot.angle);
            }
        } else if (dot.type == Dot.DotType.PEN_UP) {
            Log.i(TAG, "PEN_UP");
            // Pen Up
            if (dot.x == 0 || dot.y == 0) {
                pointX = mov_x;
                pointY = mov_y;
            }

            gPIndex += 1;
            drawSubFountainPen2(bDrawl[0], gScale, gOffsetX, gOffsetY, gWidth, pointX, pointY, pointZ, 2);

            // 保存屏幕坐标，原始坐标会使比例缩小
            saveData(gCurBookID, gCurPageID, pointX, pointY, pointZ, 2, gWidth, gColor, dot.Counter, dot.angle);
            bDrawl[0].invalidate();

            pointX = 0;
            pointY = 0;
            mN = 0;
            gPIndex = -1;
        }
    }

    private void ProcessDots(Dot dot) {
        Log.i(TAG, "=======222draw dot=======" + dot.toString());

        // 回放模式，不接受点
        if (bIsReply) {
            return;
        }

        ProcessEachDot(dot);
    }

    boolean isConnected = false;
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLEService.ACTION_GATT_CONNECTED.equals(action)) {
                isConnected = true;
                bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                invalidateOptionsMenu();
            } else if (BluetoothLEService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Log.i(TAG, "BroadcastReceiver ACTION_GATT_DISCONNECTED");
                if (connectNum > 0) {
                    connectNum--;
                }

                if (layout.getVisibility() == View.VISIBLE) {
                    layout.setVisibility(View.GONE);
                    bar.setProgress(0);
                }

                gImageView.setVisibility(View.GONE);
                gColor = 6;
                gbCover = false;
                isConnected = false;
                bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                /*gCurPageID = -1;
                dot_number.clear();
                dot_number1.clear();
                dot_number2.clear();
                dot_number4.clear();*/
                invalidateOptionsMenu();
            }
        }
    };

    private void SetBackgroundImage(int BookID, int PageID) {
        if (!gbSetNormal) {
            LayoutParams para;
            para = gImageView.getLayoutParams();
            para.width = BG_WIDTH;
            para.height = BG_HEIGHT;
            gImageView.setLayoutParams(para);
            gbSetNormal = true;

            Log.i(TAG, "testOffset BG_WIDTH = " + BG_WIDTH + ", BG_HEIGHT =" + BG_HEIGHT + ", gcontentLeft = " + gcontentLeft + ", gcontentTop = " + gcontentTop);
            Log.i(TAG, "testOffset A5_X_OFFSET = " + A5_X_OFFSET + ", A5_Y_OFFSET = " + A5_Y_OFFSET);
            Log.i(TAG, "testOffset mWidth = " + mWidth + ", mHeight = " + mHeight);
            Log.i(TAG, "testOffset getTop = " + gImageView.getTop() + ", getLeft = " + gImageView.getLeft());
            Log.i(TAG, "testOffset getWidth = " + gImageView.getWidth() + ", getHeight = " + gImageView.getHeight());
            Log.i(TAG, "testOffset getMeasuredWidth = " + gImageView.getMeasuredWidth() + ", getMeasuredHeight = " + gImageView.getMeasuredHeight());
        }

        gbCover = true;
        bDrawl[0].canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (BookID == 168) {
            if (getResources().getIdentifier("p" + PageID, "drawable", getPackageName()) == 0) {
                return;
            }
            gImageView.setImageResource(getResources().getIdentifier("p" + PageID, "drawable", getPackageName()));
        }
        else  if (BookID == 100) {
            if (getResources().getIdentifier("p" + PageID, "drawable", getPackageName()) == 0) {
                return;
            }
            gImageView.setImageResource(getResources().getIdentifier("p" + PageID, "drawable", getPackageName()));
        } else if (BookID == 0) {
            if (getResources().getIdentifier("blank" + PageID, "drawable", getPackageName()) == 0) {
                return;
            }
            gImageView.setImageResource(getResources().getIdentifier("blank" + PageID, "drawable", getPackageName()));
        } else if (BookID == 1) {
            if (getResources().getIdentifier("zhen" + PageID, "drawable", getPackageName()) == 0) {
                return;
            }
            gImageView.setImageResource(getResources().getIdentifier("zhen" + PageID, "drawable", getPackageName()));
        }
    }

    public static float LINE_THICKNESS_SCALE = 1 / 1400f;

    // the actual data
    public int mN;
    protected Path mDrawPath = new Path();

    // Attributes
    static public float getScaledPenThickness(float tPenThickness, float scale) {
        return tPenThickness * scale * LINE_THICKNESS_SCALE;
    }

    private void drawSubFountainPen1(DrawView DV, float scale, float offsetX, float offsetY, int penWidth, float x, float y, int force, int ntype, int color) {
        if (ntype == 0) {
            g_x0 = x;
            g_y0 = y;
            g_x1 = x;
            g_y1 = y;
            Log.i(TAG, "--------draw pen down-------");
        }

        if (ntype == 2) {
            g_x1 = x;
            g_y1 = y;
            Log.i("TEST", "--------draw pen up--------");
            //return;
        } else {
            g_x1 = x;
            g_y1 = y;
            Log.i(TAG, "--------draw pen move-------");
        }

        DV.paint.setStrokeWidth(penWidth);
        SetPenColor(color);
        DV.canvas.drawLine(g_x0, g_y0, g_x1, g_y1, DV.paint);
        g_x0 = g_x1;
        g_y0 = g_y1;

        return;
    }

    private void drawSubFountainPen2(DrawView DV, float scale, float offsetX, float offsetY, int penWidth, float x, float y, int force, int ntype) {
        if (ntype == 0) {
            g_x0 = x;
            g_y0 = y;
            g_x1 = x;
            g_y1 = y;
            Log.i(TAG, "--------draw pen down-------");
        }
        if (ntype == 2) {
            g_x1 = x;
            g_y1 = y;
            Log.i("TEST", "--------draw pen up--------");
        } else {
            g_x1 = x;
            g_y1 = y;
            Log.i(TAG, "--------draw pen move-------");
        }

        DV.paint.setStrokeWidth(penWidth);
        DV.canvas.drawLine(g_x0, g_y0, g_x1, g_y1, DV.paint);
        DV.invalidate();
        g_x0 = g_x1;
        g_y0 = g_y1;

        return;
    }

    private void drawSubFountainPen(DrawView DV, float scale, float offsetX, float offsetY, int penWidth, float x, float y, int force) {
        // the first actual point is treated as a midpoint
        if (gPIndex == 0) {
            g_x0 = x * scale + offsetX + 0.1f;
            g_y0 = y * scale + offsetY;
            g_p0 = Math.max(1, penWidth * scale * force / 1023);
            DV.canvas.drawCircle((float) (g_x0), (float) (g_y0), (float) 0.5, DV.paint);
            return;
        }

        if (gPIndex == 1) {
            g_x1 = x * scale + offsetX + 0.1f;
            g_y1 = y * scale + offsetY;
            g_p1 = Math.max(1, penWidth * scale * force / 1023);
            g_vx01 = g_x1 - g_x0;
            g_vy01 = g_y1 - g_y0;

            // instead of dividing tangent/norm by two, we multiply norm by 2
            g_norm = (float) Math.sqrt(g_vx01 * g_vx01 + g_vy01 * g_vy01 + 0.0001f) * 2f;
            g_vx01 = g_vx01 / g_norm * g_p0;
            g_vy01 = g_vy01 / g_norm * g_p0;
            g_n_x0 = g_vy01;
            g_n_y0 = -g_vx01;
            return;
        }

        if (gPIndex > 1 && gPIndex < 10000) {
            // (x0,y0) and (x2,y2) are midpoints, (x1,y1) and (x3,y3) are actual
            // points
            g_x3 = x * scale + offsetX + 0.1f;
            g_y3 = y * scale + offsetY;
            g_p3 = Math.max(1, penWidth * scale * force / 1023);

            g_x2 = (g_x1 + g_x3) / 2f;
            g_y2 = (g_y1 + g_y3) / 2f;
            g_p2 = (g_p1 + g_p3) / 2f;
            g_vx21 = g_x1 - g_x2;
            g_vy21 = g_y1 - g_y2;
            g_norm = (float) Math.sqrt(g_vx21 * g_vx21 + g_vy21 * g_vy21 + 0.0001f) * 2f;
            g_vx21 = g_vx21 / g_norm * g_p2;
            g_vy21 = g_vy21 / g_norm * g_p2;
            g_n_x2 = -g_vy21;
            g_n_y2 = g_vx21;


            mDrawPath.rewind();
            mDrawPath.moveTo(g_x0 + g_n_x0, g_y0 + g_n_y0);
            // The + boundary of the stroke
            mDrawPath.cubicTo(g_x1 + g_n_x0, g_y1 + g_n_y0, g_x1 + g_n_x2, g_y1 + g_n_y2, g_x2 + g_n_x2, g_y2 + g_n_y2);
            // round out the cap
            mDrawPath.cubicTo(g_x2 + g_n_x2 - g_vx21, g_y2 + g_n_y2 - g_vy21, g_x2 - g_n_x2 - g_vx21, g_y2 - g_n_y2 - g_vy21, g_x2 - g_n_x2, g_y2 - g_n_y2);
            // THe - boundary of the stroke
            mDrawPath.cubicTo(g_x1 - g_n_x2, g_y1 - g_n_y2, g_x1 - g_n_x0, g_y1 - g_n_y0, g_x0 - g_n_x0, g_y0 - g_n_y0);
            // round out the other cap
            mDrawPath.cubicTo(g_x0 - g_n_x0 - g_vx01, g_y0 - g_n_y0 - g_vy01, g_x0 + g_n_x0 - g_vx01, g_y0 + g_n_y0 - g_vy01, g_x0 + g_n_x0, g_y0 + g_n_y0);
            DV.canvas.drawPath(mDrawPath, DV.paint);

            g_x0 = g_x2;
            g_y0 = g_y2;
            g_p0 = g_p2;
            g_x1 = g_x3;
            g_y1 = g_y3;
            g_p1 = g_p3;
            g_vx01 = -g_vx21;
            g_vy01 = -g_vy21;
            g_n_x0 = g_n_x2;
            g_n_y0 = g_n_y2;
            return;
        }

        if (gPIndex >= 10000) {
            //Last Point
            g_x2 = x * scale + offsetX + 0.1f;
            g_y2 = y * scale + offsetY;
            g_p2 = Math.max(1, penWidth * scale * force / 1023);

            g_vx21 = g_x1 - g_x2;
            g_vy21 = g_y1 - g_y2;
            g_norm = (float) Math.sqrt(g_vx21 * g_vx21 + g_vy21 * g_vy21 + 0.0001f) * 2f;
            g_vx21 = g_vx21 / g_norm * g_p2;
            g_vy21 = g_vy21 / g_norm * g_p2;
            g_n_x2 = -g_vy21;
            g_n_y2 = g_vx21;

            mDrawPath.rewind();
            mDrawPath.moveTo(g_x0 + g_n_x0, g_y0 + g_n_y0);
            mDrawPath.cubicTo(g_x1 + g_n_x0, g_y1 + g_n_y0, g_x1 + g_n_x2, g_y1 + g_n_y2, g_x2 + g_n_x2, g_y2 + g_n_y2);
            mDrawPath.cubicTo(g_x2 + g_n_x2 - g_vx21, g_y2 + g_n_y2 - g_vy21, g_x2 - g_n_x2 - g_vx21, g_y2 - g_n_y2 - g_vy21, g_x2 - g_n_x2, g_y2 - g_n_y2);
            mDrawPath.cubicTo(g_x1 - g_n_x2, g_y1 - g_n_y2, g_x1 - g_n_x0, g_y1 - g_n_y0, g_x0 - g_n_x0, g_y0 - g_n_y0);
            mDrawPath.cubicTo(g_x0 - g_n_x0 - g_vx01, g_y0 - g_n_y0 - g_vy01, g_x0 + g_n_x0 - g_vx01, g_y0 + g_n_y0 - g_vy01, g_x0 + g_n_x0, g_y0 + g_n_y0);
            DV.canvas.drawPath(mDrawPath, DV.paint);
            return;
        }
    }

    public void DrawExistingStroke(int BookID, int PageID) {
        if (BookID == 100) {
            dot_number4 = dot_number;
        } else if (BookID == 0) {
            dot_number4 = dot_number1;
        } else if (BookID == 1) {
            dot_number4 = dot_number2;
        }

        if (dot_number4.isEmpty()) {
            return;
        }

        Set<Integer> keys = dot_number4.keySet();
        for (int key : keys) {
            Log.i(TAG, "=========pageID=======" + PageID + "=====Key=====" + key);
            if (key == PageID) {
                List<Dots> dots = dot_number4.get(key);
                for (Dots dot : dots) {
                    Log.i(TAG, "=========pageID=======" + dot.pointX + "====" + dot.pointY + "===" + dot.ntype);

                    drawSubFountainPen1(bDrawl[0], gScale, gOffsetX, gOffsetY, dot.penWidth,
                            dot.pointX, dot.pointY, dot.force, dot.ntype, dot.ncolor);
                }
            }
        }

        bDrawl[0].postInvalidate();
        gPIndex = -1;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    private String getTypeStr(Dot.DotType type) {
        String str = "";
        if (type == Dot.DotType.PEN_DOWN) {
            str = "PEN_DOWN";
        } else if (type == Dot.DotType.PEN_MOVE) {
            str = "PEN_MOVE";
        } else if (type == Dot.DotType.PEN_UP) {
            str = "PEN_UP";
        }
        return str;
    }

}
