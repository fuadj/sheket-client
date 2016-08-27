package com.mukera.sheket.client.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 8/27/16.
 */
public class DeviceBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            PrefUtil.setIsPaymentServiceRunning(context, true);
            AlarmReceiver.startPeriodicPaymentAlarm(context);
        }
    }
}
