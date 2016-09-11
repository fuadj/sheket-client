package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;

import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by fuad on 8/27/16.
 */
public class PaymentService extends IntentService {
    public PaymentService() {
        super("SheketPaymentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        long prev_time = PrefUtil.getLastSeenTime(this);
        long now = System.currentTimeMillis();
        PrefUtil.setLastSeenTime(this, now);

        boolean invalidate_all_certificates = false;

        // we only accept time that is moving forward. If the current
        // time is "before" the last saved time, that means the clock
        // has been re-wound. So, we invalidate all payments and force
        // user to RE-CONFIRM PAYMENT.
        if (now < prev_time) {
            invalidate_all_certificates = true;
        }

        List<SCompany> companies = getAllCompanies();
        for (SCompany company : companies) {
            if (invalidate_all_certificates) {
                setPaymentState(company, CompanyEntry.PAYMENT_INVALID);
                continue;
            }

            int payment_state = checkPaymentState(company, now);
            switch (payment_state) {
                case CompanyEntry.PAYMENT_INVALID:
                case CompanyEntry.PAYMENT_ENDED:
                    setPaymentState(company, payment_state);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(SheketBroadcast.ACTION_PAYMENT_REQUIRED));
                    break;
            }
        }

        // release the WAKE-LOCK
        AlarmReceiver.completeWakefulIntent(intent);
    }

    /**
     * Checks if payment certificate of the company is valid for this user.
     * This involves comparing the signature received from server with the
     * locally computed value, using the payment public key.
     * It also checks if the payment duration is still valid(not expired).
     */
    int checkPaymentState(SCompany company, long current_time) {
        if (!PaymentContract.isLicenseValidForDeviceAndUser(
                company.payment_license,
                DeviceId.getUniqueDeviceId(this),
                company.user_id,
                company.company_id)) {
            return CompanyEntry.PAYMENT_INVALID;
        }

        PaymentContract contract = new PaymentContract(company.payment_license);

        final long MINUTE = 60 * 1000;      // in milliseconds
        final long HOUR = 60 * MINUTE;
        final long DAY = 24 * HOUR;

        long days_since_payment = (current_time - Long.parseLong(contract.local_date_issued)) / DAY;

        if (days_since_payment > Long.parseLong(contract.duration)) {
            return CompanyEntry.PAYMENT_ENDED;
        }

        return CompanyEntry.PAYMENT_VALID;
    }

    void setPaymentState(SCompany company, int payment_state) {
        company.payment_state = payment_state;
        ContentValues values = company.toContentValues();
        // setting this value fks up the foreign keys by resetting them, check it
        // probably update is short for "delete-insert". And when you
        // delete while being foreign keyed, you remove the referring columns also.
        values.remove(CompanyEntry.COLUMN_COMPANY_ID);
        getContentResolver().update(
                CompanyEntry.buildCompanyUri(company.company_id),
                values,
                String.format(Locale.US, "%s = ?", CompanyEntry.COLUMN_COMPANY_ID),
                new String[]{String.valueOf(company.company_id)});
    }

    List<SCompany> getAllCompanies() {
        List<SCompany> result = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                CompanyEntry.CONTENT_URI,
                null, null, null, null);

        if (cursor != null && !cursor.moveToFirst()) {
            do {
                result.add(new SCompany(cursor));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return result;
    }
}
