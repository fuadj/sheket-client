package com.mukera.sheket.client.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import java.util.Calendar;

/**
 * Created by fuad on 8/27/16.
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // schedule the "next time" alarm to fire
        AlarmReceiver.scheduleNextPaymentCheck(context);

        startWakefulService(context,
                new Intent(context, PaymentService.class));
    }

    public static void startPaymentCheckNow(Context context) {
        Intent paymentIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, paymentIntent, 0);

        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // fire the alarm now
        manager.set(AlarmManager.RTC,
                System.currentTimeMillis(),
                pendingIntent);
    }

    /**
     * Starts the alarm to fire the payment service at the START of the next hour.
     * @param context
     */
    public static void scheduleNextPaymentCheck(Context context) {
        Intent paymentIntent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 0, paymentIntent, 0);

        final long MINUTE = 60 * 1000;
        final long HOUR = 60 * MINUTE;

        Calendar nextHour = Calendar.getInstance();
        nextHour.add(Calendar.HOUR, 1);

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
