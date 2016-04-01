package com.mukera.sheket.client.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by gamma on 3/28/16.
 */
public class SheketSyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static SheketSyncAdapter sSheketSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("SunshineSyncService", "onCreate - SunshineSyncService");
        synchronized (sSyncAdapterLock) {
            if (sSheketSyncAdapter == null) {
                sSheketSyncAdapter = new SheketSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sSheketSyncAdapter.getSyncAdapterBinder();
    }
}
