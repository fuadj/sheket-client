package com.mukera.sheket.client;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.IntDef;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.services.PaymentContract;
import com.mukera.sheket.client.utils.PrefUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

/**
 * Created by fuad on 9/21/16.
 * <p/>
 * Common place to check if some operation is supported under the current license.
 * Because the check might involve some computation, we shouldn't run it in UI thread.
 * This class helps us handle that boilerplate.
 */
public class OperationSupport {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({OPERATION_ADD_EMPLOYEE, OPERATION_ADD_ITEM, OPERATION_ADD_CATEGORY, OPERATION_ADD_BRANCH})
    public @interface Operation {
    }

    public static final int OPERATION_ADD_EMPLOYEE = 1;
    public static final int OPERATION_ADD_ITEM = 2;
    public static final int OPERATION_ADD_CATEGORY = 3;
    public static final int OPERATION_ADD_BRANCH = 4;

    /**
     * This will be used to fill in the place holder in R.string.dialog_payment_operation_not_supported_body.
     * <p/>
     * NOTE: the phrases should describe the action being performed.
     */
    private static final HashMap<Integer, String> sOperationErrorMessages;

    static {
        sOperationErrorMessages = new HashMap<>();
        sOperationErrorMessages.put(OPERATION_ADD_EMPLOYEE, "Adding Employees");
        sOperationErrorMessages.put(OPERATION_ADD_ITEM, "Adding Items");
        sOperationErrorMessages.put(OPERATION_ADD_CATEGORY, "Adding Categories");
        sOperationErrorMessages.put(OPERATION_ADD_BRANCH, "Adding Branches");
    }

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
     * <p/>
     * if the button needs to check if the license allows this type of operation
     * <p/>
     * button.setOnClickListener(new OnClickListener() {
     *
     * @Override public void onClick(...) {
     * <p/>
     * OperationSupport.checkPaymentSupportsOperation(
     * getActivity(),
     * operation_code,
     * new OperationSupportListener() {
     * @Override public void operationSupported() {
     * // Continue with the operation
     * }
     * @Override public void operationNotSupported() {
     * }
     * });
     * }
     * }
     */
    public static void checkPaymentSupportsOperation(final Activity activity,
                                                     @Operation final int operation,
                                                     final OperationSupportListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean is_supported = isOperationSupported(activity, operation);

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (is_supported) {
                            listener.operationSupported();
                        } else {
                            String err_msg = activity.getString(
                                    R.string.dialog_payment_operation_not_supported_body,
                                    sOperationErrorMessages.get(operation));

                            new AlertDialog.Builder(activity).
                                    setTitle(R.string.dialog_payment_operation_not_supported_title).
                                    setMessage(err_msg).
                                    show();

                            listener.operationNotSupported();
                        }
                    }
                });
            }
        }).start();
    }

    private static boolean isOperationSupported(Activity activity,
                                                              @Operation int operation) {

        if (true)
            return true;

        int company_id = PrefUtil.getCurrentCompanyId(activity);
        Cursor cursor = activity.getContentResolver().
                query(SheketContract.CompanyEntry.buildCompanyUri(company_id),
                        SCompany.COMPANY_COLUMNS,
                        null, null,
                        null);
        if (cursor == null || !cursor.moveToFirst())
            return false;

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

        return true;
    }
}
