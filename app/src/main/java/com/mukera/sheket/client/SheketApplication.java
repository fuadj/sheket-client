package com.mukera.sheket.client;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

/**
 * Created by fuad on 8/24/16.
 */
public class SheketApplication extends MultiDexApplication {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public Tracker getTracker() {
        try {
            final GoogleAnalytics ga = GoogleAnalytics.getInstance(this);

            Tracker tracker = ga.newTracker(R.xml.track_app);

            ga.enableAutoActivityReports(this);

            ga.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
            return tracker;
        } catch (final Exception e) {
            return null;
        }
    }
}
