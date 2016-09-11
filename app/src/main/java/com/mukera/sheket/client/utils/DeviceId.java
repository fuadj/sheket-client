package com.mukera.sheket.client.utils;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

/**
 * Created by fuad on 8/31/16.
 */
public class DeviceId {
    /**
     * Generates a unique id that identifies the app install(it actually identifies the device).
     */
    public static String getUniqueDeviceId(Context context) {
        /**
         * We are using both ANDROID_ID + DeviceId because sometimes either might be
         * unreliable(don't generate a unique id OR return an empty result). So we join both.
         *
         * See http://stackoverflow.com/a/16869491/5753416.
         */
        String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        TelephonyManager manager = (TelephonyManager) context.getSystemService(
                Context.TELEPHONY_SERVICE);
        String device_id = manager.getDeviceId();

        return android_id + device_id;
    }
}
