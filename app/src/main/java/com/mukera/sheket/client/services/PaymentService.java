package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

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

    final long MINUTE = 60 * 1000;      // in milliseconds
    final long HOUR = 60 * MINUTE;
    final long DAY = 24 * HOUR;

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
        if ((now + 24.0 * HOUR) < prev_time) {
            invalidate_all_certificates = true;
            Log.d("PaymentService", "Time was reset, resetting all companies");
        }

        int current_company = PrefUtil.getCurrentCompanyId(this);
        List<SCompany> companies = getAllCompanies();
        for (SCompany company : companies) {
            if (invalidate_all_certificates) {
                setPaymentState(company, CompanyEntry.PAYMENT_INVALID);
                if (current_company == company.company_id) {
                    LocalBroadcastManager.getInstance(this).
                            sendBroadcast(new Intent(SheketBroadcast.ACTION_PAYMENT_REQUIRED));
                }
                continue;
            }

            int payment_state = checkPaymentState(company, now);
            switch (payment_state) {
                case CompanyEntry.PAYMENT_INVALID:
                case CompanyEntry.PAYMENT_ENDED:
                    setPaymentState(company, payment_state);
                    if (current_company == company.company_id) {
                        LocalBroadcastManager.getInstance(this).
                                sendBroadcast(new Intent(SheketBroadcast.ACTION_PAYMENT_REQUIRED));
                    }
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
        PaymentContract contract = new PaymentContract(company.payment_license);

        String device_id = "";
        if (contract.parse_success && !contract.is_free_license) {
            device_id = DeviceId.getUniqueDeviceId(this);
        }

        if (!PaymentContract.isLicenseValidForDeviceAndUser(
                company.payment_license,
                device_id,
                company.user_id,
                company.company_id)) {
            return CompanyEntry.PAYMENT_INVALID;
        }

        long contract_issued_date;
        if (contract.is_free_license)
            contract_issued_date = PrefUtil.getLocalCompanyPaymentDate(this);
        else {
            try {
                contract_issued_date = Long.parseLong(contract.local_date_issued);
            } catch (NumberFormatException e) {
                return CompanyEntry.PAYMENT_INVALID;
            }
        }

        long days_since_payment = (current_time - contract_issued_date) / DAY;

        if (days_since_payment > Long.parseLong(contract.duration)) {
            return CompanyEntry.PAYMENT_ENDED;
        }

        return CompanyEntry.PAYMENT_VALID;
    }

    void setPaymentState(SCompany company, int payment_state) {
        company.payment_state = payment_state;

        // if we're setting payment to a non-valid state, remove the license
        if (payment_state != CompanyEntry.PAYMENT_VALID)
            company.payment_license = "";

        ContentValues values = company.toContentValues();
        // setting this value fks up the foreign keys by resetting them, check it
        // probably update is short for "delete-insert". And when you
        // delete while being foreign keyed, you remove the referring columns also.
        values.remove(CompanyEntry.COLUMN_COMPANY_ID);

        boolean did_update =
                1 == getContentResolver().update(
                CompanyEntry.CONTENT_URI,
                values,
                String.format(Locale.US, "%s = ?", CompanyEntry.COLUMN_COMPANY_ID),
                new String[]{String.valueOf(company.company_id)});

        if (!did_update)
            Log.d("PaymentService",
                    String.format("Payment Update Error %d payment state", company.company_id));
    }

    List<SCompany> getAllCompanies() {
        List<SCompany> result = new ArrayList<>();
        Cursor cursor = getContentResolver().query(
                CompanyEntry.CONTENT_URI,
                null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                result.add(new SCompany(cursor));
            } while (cursor.moveToNext());

            cursor.close();
        }

        return result;
    }
}
