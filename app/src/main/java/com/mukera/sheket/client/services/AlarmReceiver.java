package com.mukera.sheket.client.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by fuad on 8/27/16.
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(
                new Intent(context, PaymentService.class));
    }

    public static void startPeriodicPaymentAlarm(Context context) {
        Intent paymentIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, paymentIntent, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        manager.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME, System.currentTimeMillis(),
                AlarmManager.INTERVAL_HALF_DAY,
                pendingIntent);

    }
}
