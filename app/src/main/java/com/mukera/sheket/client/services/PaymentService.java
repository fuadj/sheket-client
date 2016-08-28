package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by fuad on 8/27/16.
 */
public class PaymentService extends IntentService {
    public PaymentService() {
        super("SheketPaymentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        // release the WAKE-LOCK
        AlarmReceiver.completeWakefulIntent(intent);
    }
}
