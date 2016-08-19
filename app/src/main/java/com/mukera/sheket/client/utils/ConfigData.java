package com.mukera.sheket.client.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 4/2/16.
 */
public class ConfigData {
    public static String getAddress(Context context) {
        return "http://mukerax.com/api/";
        //return "http://192.168.0.102:8080/api/";
        //return "http://172.20.10.8:8080/api/";
        /*
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String ip_address = prefs.getString(context.getString(R.string.pref_ip_key),
                context.getString(R.string.pref_ip_default));
        return "http://" + ip_address + ":8000/";
        */
    }

}

