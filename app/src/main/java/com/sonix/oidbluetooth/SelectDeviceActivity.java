package com.sonix.oidbluetooth;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.githang.statusbar.StatusBarCompat;
import com.tqltech.tqlpencomm.BLEException;
import com.tqltech.tqlpencomm.BLEScanner;
import com.tqltech.tqlpencomm.PenCommAgent;

import java.lang.reflect.Field;
import java.util.ArrayList;


public class SelectDeviceActivity extends Activity {
    private final static String TAG = "USB_HOST";
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private boolean mScanning;
    private static final int REQUEST_ENABLE_BT = 1;

    private ListView listView;
    private PenCommAgent bleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "on Create start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.device);
        ActionBar actionBar = getActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(ApplicationResources.getLocalVersionName(this));

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE is not supported", Toast.LENGTH_SHORT).show();
            finish();
        }

        StatusBarCompat.setStatusBarColor(this, getResources().getColor(R.color.statusColor), true);

        bleManager = PenCommAgent.GetInstance(getApplication());
        listView = (ListView) findViewById(R.id.lv_device);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bleManager != null) {
            Log.e(TAG, "select devices resume");
            bleManager.init();
        }

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setOnItemClickListener(itemClickListener);
        //listView.setListAdapter(mLeDeviceListAdapter);

        View rootView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        int actionBarHeight = getActionBar().getHeight();
        int statusHeight = getStatusBarHeight();
        rootView.setY(actionBarHeight + statusHeight + 10);

        try {
            scanLeDevice(true);
        } catch (Exception e) {
            Log.i(TAG, "onResume scan----" + e.toString());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "on pause start");
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            Log.d(TAG, "onListItemClick device " + device);
            if (device == null) {
                return;
            }
            try {
                bleManager.stopFindAllDevices();
                Bundle b = new Bundle();
                b.putString(BluetoothDevice.EXTRA_DEVICE, mLeDeviceListAdapter.getDevice(position).getAddress());

                Intent result = new Intent();
                result.putExtras(b);
                setResult(Activity.RESULT_OK, result);
                finish();
            } catch (Exception e) {
                Log.i(TAG, "---scan finish---" + e.toString());
            }
        }
    };
    //@Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        Log.d(TAG, "onListItemClick device " + device);
        if (device == null) {
            return;
        }
        try {
            bleManager.stopFindAllDevices();
            Bundle b = new Bundle();
            b.putString(BluetoothDevice.EXTRA_DEVICE, mLeDeviceListAdapter.getDevice(position).getAddress());

            Intent result = new Intent();
            result.putExtras(b);
            setResult(Activity.RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Log.i(TAG, "---scan finish---" + e.toString());
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            bleManager.FindAllDevices(new BLEScanner.OnBLEScanListener() {

                                          @Override
                                          public void onScanResult(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                                              //runOnUiThread(new Runnable() {
                                              //    @Override
                                              //    public void run() {
                                              mLeDeviceListAdapter.addDevice(device);
                                              Log.e(TAG, "devices is " + device.getAddress());
                                              mLeDeviceListAdapter.notifyDataSetChanged();
                                              //    }
                                              //});
                                          }

                                          @Override
                                          public void onScanFailed(BLEException bleException) {
                                              Log.e(TAG, bleException.getMessage());
                                          }
                                      }
            );
            mScanning = true;
        } else {
            mScanning = false;
            bleManager.stopFindAllDevices();
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter(Context context) {
            super();
            mLeDevices = new ArrayList<>();
            mInflator = LayoutInflater.from(context);
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }


    private int getStatusBarHeight() {
        Class<?> c = null;
        Object obj = null;
        Field field = null;
        int x = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            obj = c.newInstance();
            field = c.getField("status_bar_height");
            x = Integer.parseInt(field.get(obj).toString());
            return getResources().getDimensionPixelSize(x);
        } catch (Exception e1) {
            Log.d(TAG, "get status bar height fail");
            e1.printStackTrace();
            return 75;
        }

    }
}
