package com.mukera.sheket.client.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by gamma on 3/28/16.
 */
public class SheketAuthenticatorService extends Service {
    private SheketAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new SheketAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
