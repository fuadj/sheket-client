package com.mukera.sheket.client;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.IntDef;
import android.support.v4.util.Pair;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.services.PaymentContract;
import com.mukera.sheket.client.utils.PrefUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by fuad on 9/21/16.
 *
 * Common place to check if some operation is supported under the current license.
 * Because the check might involve some computation, we shouldn't run it in UI thread.
 * This class helps us handle that boilerplate.
 */
public class OperationSupport {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OPERATION_ADD_EMPLOYEE, OPERATION_ADD_ITEM})
    public @interface Operation {
    }

    public static final int OPERATION_ADD_EMPLOYEE = 1;
    public static final int OPERATION_ADD_ITEM = 2;

    /**
     * Depending on the payment level, different operations can be
     * supported or not.
     */
    public interface OperationSupportListener {
        void operationSupported();

        void operationNotSupported();
    }

    /**
     * Checks if the current company payment supports the {@code operation}.
     * Notifies the result through the listener on UI thread.
     * example usage:
     *
     *      if the button needs to check if the license allows this type of operation
     *
     *      button.setOnClickListener(new OnClickListener() {
     *          @Override
     *          public void onClick(...) {
     *
     *              OperationSupport.doesPaymentSupportOperation(
     *                      getActivity(),
     *                      operation_code,
     *                      new OperationSupportListener() {
     *                          @Override
     *                          public void operationSupported() {
     *                              // Continue with the operation
     *                          }
     *
     *                          @Override
     *                          public void operationNotSupported() {
     *                          }
     *                      });
     *          }
     *      }
     */
    public static void doesPaymentSupportOperation(final Activity activity,
                                                   @Operation final int operation,
                                                   final OperationSupportListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Pair<Boolean, String> result = checkOperationIsSupported(activity, operation);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (result.first == Boolean.TRUE) {
                            listener.operationSupported();
                        } else {
                            listener.operationNotSupported();
                        }
                    }
                });
            }
        }).start();
    }

    private static Pair<Boolean, String> checkOperationIsSupported(Activity activity,
                                                                   @Operation int operation) {
        long company_id = PrefUtil.getCurrentCompanyId(activity);
        Cursor cursor = activity.getContentResolver().
                query(SheketContract.CompanyEntry.buildCompanyUri(company_id),
                        SCompany.COMPANY_COLUMNS,
                        null, null,
                        null);
        if (cursor == null || !cursor.moveToFirst())
            return new Pair<>(Boolean.FALSE, "company fetch error");

        SCompany company = new SCompany(cursor);
        cursor.close();

        PaymentContract contract = new PaymentContract(company.payment_license);

        switch (operation) {
            case OPERATION_ADD_EMPLOYEE:
                // TODO: check allowed
                break;
            case OPERATION_ADD_ITEM:
                // TODO: check allowed
                break;
        }

        return null;
    }
}
