package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.List;

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
                revokePaymentCertificate(company, PAYMENT_INVALID);
                continue;
            }

            int payment_state = checkPaymentState(company, now);
            switch (payment_state) {
                case PAYMENT_ENDED:
                case PAYMENT_INVALID:
                    revokePaymentCertificate(company, payment_state);
                    break;
            }
        }

        // release the WAKE-LOCK
        AlarmReceiver.completeWakefulIntent(intent);
    }

    // user has a valid payment, can continue using the pp
    public static final int PAYMENT_VALID = 1;
    // user had a valid payment, but it has expired
    public static final int PAYMENT_ENDED = 2;
    // there is a problem with the payment signature, force user to RE-CONFIRM
    public static final int PAYMENT_INVALID = 3;
    /**
     * Checks if payment certificate of the company is valid for this user.
     * This involves comparing the signature received from server with the
     * locally computed value, using the payment public key.
     * It also checks if the payment duration is still valid(not expired).
     *
     * @return one of the PAYMENT_* constants
     */
    int checkPaymentState(SCompany company, long current_time) {
        PaymentContract.ContractComponents contractAndSignature =
                PaymentContract.extractContractComponents(company.payment_certificate);

        /**
         * Load in the contract, then replace the device and user related fields and
         * check if the signature is valid. This is necessary b/c the signature could
         * be valid but could have been issued for a different {device|user}. So make
         * sure it is for the current device's user.
         */
        PaymentContract contract = new PaymentContract(contractAndSignature.contract);

        contract.device_id = DeviceId.getUniqueDeviceId(this);
        contract.user_id = company.user_id;
        contract.company_id = company.company_id;

        boolean is_signature_valid = PaymentContract.
                isSignatureValid(contract.toString(), contractAndSignature.signature);
        if (!is_signature_valid)
            return PAYMENT_INVALID;

        final long MINUTE = 60 * 1000;      // in milliseconds
        final long HOUR = 60 * MINUTE;
        final long DAY = 24 * HOUR;

        long days_since_payment = (current_time - Long.parseLong(contract.local_date_issued)) / DAY;

        if (days_since_payment > Long.parseLong(contract.duration)) {
            return PAYMENT_ENDED;
        }

        return PAYMENT_VALID;
    }

    /**
     * Revokes the payment certificate for the company.
     */
    void revokePaymentCertificate(SCompany company, int revokation) {

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
