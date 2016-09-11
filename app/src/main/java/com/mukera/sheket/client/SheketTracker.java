package com.mukera.sheket.client;

import android.app.Activity;

import com.google.android.gms.analytics.Tracker;

import java.util.Map;

/**
 * Created by fuad on 8/26/16.
 */
public class SheketTracker {
    public static final String SCREEN_NAME_LOGIN = "login_page";
    public static final String SCREEN_NAME_MAIN = "main_page";

    public static final String CATEGORY_LOGIN = "login";

    public static final String CATEGORY_MAIN_CONFIGURATION = "configuration changes";
    public static final String CATEGORY_MAIN_NAVIGATION = "navigation";
    public static final String CATEGORY_MAIN_DIALOG = "dialogs";

    private static Tracker getTrackerForActivity(Activity activity) {
        return ((SheketApplication) activity.getApplication()).getTracker();
    }

    public static void setScreenName(Activity activity, String screenName) {
        Tracker tracker = getTrackerForActivity(activity);
        if (tracker == null) return;

        tracker.setScreenName(screenName);
    }

    public static void sendTrackingData(Activity activity, Map<String, String> trackingData) {
        Tracker tracker = getTrackerForActivity(activity);
        if (tracker == null) return;

        tracker.send(trackingData);
    }
}
