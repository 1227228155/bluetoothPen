package com.sonix.oidbluetooth;

import android.app.Activity;
import android.util.Log;

import no.nordicsemi.android.dfu.DfuBaseService;

/**
 * Created by tiany on 2018/5/4.
 */

public class DfuService extends DfuBaseService {

    @Override
    protected Class<? extends Activity> getNotificationTarget()
    {
//        try {
//            Log.i("debug", "getNotificationTarget");
//            //return TestBT.class;
//            //return null;
//        }catch (Exception e)
//        {
//            Log.i("test", "DfuBaseService-----"+e.toString());
//        }
//
//        return null;

        return null;
    }
}
