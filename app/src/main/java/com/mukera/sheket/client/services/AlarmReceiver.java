package com.mukera.sheket.client.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.Calendar;

/**
 * Created by fuad on 8/27/16.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // schedule the "next time" alarm to fire
        AlarmReceiver.startPeriodicPaymentAlarm(context);

        startWakefulService(context,
                new Intent(context, PaymentService.class));
    }

    public static void startPeriodicPaymentAlarm(Context context) {
        Intent paymentIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, paymentIntent, 0);

        final long MINUTE = 60 * 1000;
        final long HOUR = 60 * MINUTE;

        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR, 1);
        // fire the alarm at the exact hour
        nextHour.set(Calendar.MINUTE, 0);
        nextHour.set(Calendar.SECOND, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            manager.setRepeating(AlarmManager.RTC_WAKEUP,
                    nextHour.getTimeInMillis(),
                    HOUR,
                    pendingIntent);
        } else {
            manager.setExact(AlarmManager.RTC_WAKEUP,
                    nextHour.getTimeInMillis(),
                    pendingIntent);
        }
    }
}
