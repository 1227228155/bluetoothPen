package com.sonix.oidbluetooth;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.tqltech.tqlpencomm.Dot;
import com.tqltech.tqlpencomm.PenCommAgent;
import com.tqltech.tqlpencomm.PenStatus;
import com.tqltech.tqlpencomm.listener.TQLPenSignal;


public class BluetoothLEService extends Service {
    private final static String TAG = "BluetoothLEService";
    private String mBluetoothDeviceAddress;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
    public final static String ACTION_PEN_STATUS_CHANGE = "ACTION_PEN_STATUS_CHANGE";
    public final static String RECEVICE_DOT = "RECEVICE_DOT";

    public final static String DEVICE_DOES_NOT_SUPPORT_UART = "DEVICE_DOES_NOT_SUPPORT_UART";
    private PenCommAgent bleManager;
    private boolean isPenConnected = false;

    public boolean getPenStatus() {
        return isPenConnected;
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        public BluetoothLEService getService() {
            return BluetoothLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        bleManager = PenCommAgent.GetInstance(getApplication());
        bleManager.setTQLPenSignalListener(mPenSignalCallback);

        if (!bleManager.isSupportBluetooth()) {
            Log.e(TAG, "Unable to Support Bluetooth");
            return false;
        }

        if (!bleManager.isSupportBLE()) {
            Log.e(TAG, "Unable to Support BLE.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && bleManager.isConnect(address)) {
            Log.d(TAG, "Trying to use an existing pen for connection.===");
            return true;
        }

        Log.d(TAG, "Trying to create a new connection.");
        boolean flag = bleManager.connect(address);
        if (!flag) {
            Log.i(TAG, "bleManager.connect(address)-----false");
            return false;
        }

        Log.i(TAG, "bleManager.connect(address)-----true");
        return true;
    }

    public void disconnect() {
        bleManager.disconnect(mBluetoothDeviceAddress);
    }

    public void close() {
        if (bleManager == null) {
            return;
        }

        Log.w(TAG, "mBluetoothGatt closed");
        bleManager.disconnect(mBluetoothDeviceAddress);
        mBluetoothDeviceAddress = null;
        bleManager = null;
    }

    /// ===========================================================
    private OnDataReceiveListener onDataReceiveListener = null;

    public interface OnDataReceiveListener {

        void onDataReceive(Dot dot);

        void onOfflineDataReceive(Dot dot);

        void onFinishedOfflineDown(boolean success);

        void onOfflineDataNum(int num);

        void onReceiveOIDSize(int OIDSize);

        void onReceiveOfflineProgress(int i);

        void onDownloadOfflineProgress(int i);

        void onReceivePenLED(byte color);

        void onOfflineDataNumCmdResult(boolean success);

        void onDownOfflineDataCmdResult(boolean success);

        void onWriteCmdResult(int code);

        void onReceivePenType(int type);
    }

    public void setOnDataReceiveListener(OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    private TQLPenSignal mPenSignalCallback = new TQLPenSignal() {
        @Override
        public void onConnected() {
            Log.d(TAG, "TQLPenSignal had Connected");
            String intentAction;

            intentAction = ACTION_GATT_CONNECTED;
            broadcastUpdate(intentAction);
            Log.i(TAG, "Connected to GATT server.");
            isPenConnected = true;
        }

        @Override
        public void onDisconnected() {
            String intentAction;
            Log.d(TAG, "TQLPenSignal had onDisconnected");
            intentAction = ACTION_GATT_DISCONNECTED;
            Log.i(TAG, "C.");
            broadcastUpdate(intentAction);
            isPenConnected = false;
        }

        @Override
        public void onWriteCmdResult(int code) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onWriteCmdResult(code);
            }
        }

        @Override
        public void onDownOfflineDataCmdResult(boolean isSuccess) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onDownOfflineDataCmdResult(isSuccess);
            }
        }

        @Override
        public void onOfflineDataListCmdResult(boolean isSuccess) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onOfflineDataNumCmdResult(isSuccess);
            }
        }

        @Override
        public void onOfflineDataList(int offlineNotes) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onOfflineDataNum(offlineNotes);
            }
        }

        @Override
        public void onStartOfflineDownload(boolean isSuccess) {

        }

        @Override
        public void onFinishedOfflineDownload(boolean isSuccess) {
            Log.d(TAG, "-------offline download success-------");
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onFinishedOfflineDown(isSuccess);
            }
        }

        @Override
        public void onReceiveOfflineStrokes(Dot dot) {
            Log.d(TAG, dot.toString());
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onOfflineDataReceive(dot);
            }
        }

        @Override
        public void onDownloadOfflineProgress(int i) {
            Log.e(TAG, "DownloadOfflineProgress----" + i);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onDownloadOfflineProgress(i);
            }
        }

        @Override
        public void onReceiveOfflineProgress(int i) {
            Log.e(TAG, "onReceiveOfflineProgress----" + i);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceiveOfflineProgress(i);
            }
        }

        @Override
        public void onPenConfirmRecOfflineDataResponse(boolean isSuccess) {

        }

        @Override
        public void onPenDeleteOfflineDataResponse(boolean isSuccess) {

        }

        @Override
        public void onReceiveDot(Dot dot) {
            Log.d(TAG, "bluetooth service recivice=====" + dot.toString());
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onDataReceive(dot);
            }
        }


        @Override
        public void onUpDown(boolean isUp) {

        }

        @Override
        public void onPenNameSetupResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mPenName = ApplicationResources.tmp_mPenName;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenTimetickSetupResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mTimer = ApplicationResources.tmp_mTimer;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenAutoShutdownSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mPowerOffTime = ApplicationResources.tmp_mPowerOffTime;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenFactoryResetSetUpResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenAutoPowerOnSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mPowerOnMode = ApplicationResources.tmp_mPowerOnMode;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenBeepSetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mBeep = ApplicationResources.tmp_mBeep;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenSensitivitySetUpResponse(boolean bIsSuccess) {
            if (bIsSuccess) {
                ApplicationResources.mPenSens = ApplicationResources.tmp_mPenSens;
            }
            String intentAction = ACTION_PEN_STATUS_CHANGE;
            Log.i(TAG, "Disconnected from GATT server.");
            broadcastUpdate(intentAction);
        }

        @Override
        public void onPenLedConfigResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenDotTypeResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenChangeLedColorResponse(boolean bIsSuccess) {

        }

        @Override
        public void onPenOTAMode(boolean bIsSuccess) {

        }

        @Override
        public void onReceivePenAllStatus(PenStatus status) {
            ApplicationResources.mBattery = status.mPenBattery;
            ApplicationResources.mUsedMem = status.mPenMemory;
            ApplicationResources.mTimer = status.mPenTime;
            Log.e(TAG, "ApplicationResources.mTimer is " + ApplicationResources.mTimer + ", status is " + status.toString());
            ApplicationResources.mPowerOnMode = status.mPenPowerOnMode;
            ApplicationResources.mPowerOffTime = status.mPenAutoOffTime;
            ApplicationResources.mBeep = status.mPenBeep;
            ApplicationResources.mPenSens = status.mPenSensitivity;
            ApplicationResources.tmp_mEnableLED = status.mPenEnableLed;

            ApplicationResources.mPenName = status.mPenName;
            ApplicationResources.mBTMac = status.mPenMac;
            ApplicationResources.mFirmWare = status.mBtFirmware;
            ApplicationResources.mMCUFirmWare = status.mPenMcuVersion;
            ApplicationResources.mCustomerID = status.mPenCustomer;

            String intentAction = ACTION_PEN_STATUS_CHANGE;
            broadcastUpdate(intentAction);
        }

        @Override
        public void onReceivePenMac(String penMac) {
            Log.e(TAG, "receive pen Mac " + penMac);
            mBluetoothDeviceAddress = penMac;
        }

        @Override
        public void onReceivePenName(String penName) {

        }

        @Override
        public void onReceivePenBtFirmware(String penBtFirmware) {

        }

        @Override
        public void onReceivePenTime(long penTime) {

        }

        @Override
        public void onReceivePenBattery(byte penBattery, Boolean bIsCharging) {
            Log.e(TAG, "receive pen battery is " + penBattery);
        }

        @Override
        public void onReceivePenMemory(byte penMemory) {

        }

        @Override
        public void onReceivePenAutoPowerOnModel(Boolean bIsOn) {

        }

        @Override
        public void onReceivePenBeepModel(Boolean bIsOn) {

        }

        @Override
        public void onReceivePenAutoOffTime(byte autoOffTime) {

        }

        @Override
        public void onReceivePenMcuVersion(String penMcuVersion) {

        }

        @Override
        public void onReceivePenCustomer(String penCustomerID) {

        }

        @Override
        public void onReceivePenSensitivity(byte penSensitivity) {

        }

        @Override
        public void onReceivePenType(byte penType) {
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenType((int)penType);
            }
        }

        @Override
        public void onReceivePenDotType(byte penDotType) {

        }

        @Override
        public void onReceivePenDataType(byte penDataType) {

        }

        @Override
        public void onReceivePenLedConfig(byte penLedConfig) {
            Log.e(TAG, "receive hand write color is " + penLedConfig);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenLED(penLedConfig);
            }
        }

        @Override
        public void onReceivePenEnableLed(Boolean bEnableFlag) {

        }

        @Override
        public void onReceiveOIDFormat(long penOIDSize) {
            Log.e(TAG, "onReceiveOIDFormat---> " + penOIDSize);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceiveOIDSize((int) penOIDSize);
            }
        }

        @Override
        public void onReceivePenHandwritingColor(byte color) {
            Log.e(TAG, "receive hand write color is " + color);
            if (onDataReceiveListener != null) {
                onDataReceiveListener.onReceivePenLED(color);
            }
        }
    };
}

