package com.mukera.sheket.client.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.TimeZone;

/**
 * Created by fuad on 8/27/16.
 */
public class DeviceConfigurationChangeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            AlarmReceiver.startPeriodicPaymentAlarm(context);
        }

        /**
         * Time set changes don't only occur when the user manually changes the time,
         * but also when the network updates the time and the phone is configured to use network time.
         * This has the undesired effect of the network changing time only slightly(seconds) and
         * we firing the alarm. So, only consider the time has changed if there is a "BIG" difference.
         *
         * See this for more details {@link http://stackoverflow.com/a/26905894/5753416}
         */
        if (intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
                /**
                 * There is an android bug that causes the intent {@code ACTION_DATE_CHANGED} to be
                 * broadcast if only we are going to the future. If the user goes back, it won't
                 * broadcast this.
                 */
                intent.getAction().equals(Intent.ACTION_DATE_CHANGED)) {
            final String PREF_TIME_ZONE = "pref_time_zone_change";
            final String PREF_PREVIOUS_TIME = "pref_previous_time";

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

            String oldTimeZone = preferences.getString(PREF_TIME_ZONE, null);
            String newTimeZone = TimeZone.getDefault().getID();
            long previous_time = preferences.getLong(PREF_PREVIOUS_TIME, -1);

            long now = System.currentTimeMillis();

            final long MINUTE = 60 * 1000;      // in milliseconds
            final long HOUR = 60 * MINUTE;

            boolean time_changed = false;
            if (previous_time == -1 ||
                    Math.abs(now - previous_time) > HOUR) {
                time_changed = true;
            } else if (oldTimeZone == null ||
                    (TimeZone.getTimeZone(oldTimeZone).getOffset(now) !=
                            TimeZone.getTimeZone(newTimeZone).getOffset(now))) {
                time_changed = true;
            }

            preferences.edit().
                    putString(PREF_TIME_ZONE, newTimeZone).
                    putLong(PREF_PREVIOUS_TIME, now).
                    commit();

            if (time_changed) {
                AlarmReceiver.startPeriodicPaymentAlarm(context);
            }
        }
    }
}
