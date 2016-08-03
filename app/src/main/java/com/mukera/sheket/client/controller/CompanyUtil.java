package com.mukera.sheket.client.controller;

import android.app.Activity;
import android.content.ContentValues;

import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;

/**
 * Created by fuad on 7/29/16.
 */
public class CompanyUtil {

    public interface StateSwitchedListener {
        // This is guaranteed to be run in the UI thread
        void runAfterSwitchCompleted();
    }

    /**
     * Switches the current company with the replacement company. Saves the current company's
     * state if there the user has selected a company previously. When the switch is finished,
     * the listener will be called in the main thread.
     */
    public static void switchCurrentCompanyInWorkerThread(final Activity context,
                                                          final long new_company_id,
                                                          final String new_company_name,
                                                          final String new_permission,
                                                          final String new_state_bkup,
                                                          final StateSwitchedListener listener) {
        // if a company hasn't been set previously, we don't any state to save
        // just do the switching. Otherwise, save the current company's state to the
        // db, then switch.
        if (!PrefUtil.isCompanySet(context)) {
            PrefUtil.setCurrentCompanyId(context, new_company_id);
            PrefUtil.setCurrentCompanyName(context, new_company_name);
            PrefUtil.setUserPermission(context, new_permission);
            PrefUtil.restoreStateFromBackup(context, new_state_bkup);

            SPermission.setSingletonPermission(new_permission);

            listener.runAfterSwitchCompleted();
        } else {

            // save the current company's state, we are switching!!!
            new Thread(new Runnable() {
                @Override
                public void run() {
                    long current_company = PrefUtil.getCurrentCompanyId(context);
                    String current_state = PrefUtil.getEncodedStateBackup(context);

                    ContentValues values = new ContentValues();
                    // Yes, It is valid to only include the values you want to update
                    values.put(SheketContract.CompanyEntry.COLUMN_STATE_BACKUP, current_state);

                    context.getContentResolver().
                            update(
                                    CompanyEntry.CONTENT_URI,
                                    values,
                                    CompanyEntry._full(SheketContract.CompanyEntry.COLUMN_COMPANY_ID) + " = ?",
                                    new String[]{
                                            String.valueOf(current_company)
                                    }
                            );

                    PrefUtil.setCurrentCompanyId(context, new_company_id);
                    PrefUtil.setCurrentCompanyName(context, new_company_name);
                    PrefUtil.setUserPermission(context, new_permission);
                    PrefUtil.restoreStateFromBackup(context, new_state_bkup);

                    SPermission.setSingletonPermission(new_permission);

                    context.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.runAfterSwitchCompleted();
                        }
                    });
                }
            }).start();
        }
    }
}
