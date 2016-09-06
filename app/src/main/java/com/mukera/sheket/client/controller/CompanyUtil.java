package com.mukera.sheket.client.controller;

import android.app.Activity;
import android.content.ContentValues;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.mukera.sheket.client.models.SCompany;
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

    public interface LogoutFinishListener {
        void runAfterLogout();

        void logoutError(String msg);
    }

    /**
     * Switches the current company with the replacement company. Saves the current company's
     * state if there the user has selected a company previously. When the switch is finished,
     * the listener will be called in the main thread.
     */
    public static void switchCurrentCompanyInWorkerThread(final Activity context,
                                                          final SCompany switch_company,
                                                          final StateSwitchedListener listener) {
        // if a company hasn't been set previously, we don't have any state to save
        // just do the switching. Otherwise, save the current company's state to the
        // db, then switch.
        if (!PrefUtil.isCompanySet(context)) {
            PrefUtil.setCurrentCompanyId(context, switch_company.company_id);
            PrefUtil.setCurrentCompanyName(context, switch_company.name);
            PrefUtil.setUserPermission(context, switch_company.encoded_permission);
            PrefUtil.restoreStateFromBackup(context, switch_company.state_bkup);

            SPermission.setSingletonPermission(switch_company.encoded_permission);

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

                    PrefUtil.setCurrentCompanyId(context, switch_company.company_id);
                    PrefUtil.setCurrentCompanyName(context, switch_company.name);
                    PrefUtil.setUserPermission(context, switch_company.encoded_permission);
                    PrefUtil.restoreStateFromBackup(context, switch_company.state_bkup);

                    SPermission.setSingletonPermission(switch_company.encoded_permission);

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

    public static void logoutOfCompany(final Activity context,
                                       final LogoutFinishListener listener) {
        // revoke facebook permissions, then remove local stuff related to user
        GraphRequest removePermissionsRequest = new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/permissions/", null, HttpMethod.DELETE,
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse graphResponse) {
                        if (graphResponse == null || (graphResponse.getError() != null)) {
                            // we need to make it an array to subvert the final clause
                            final String[] err_msg = new String[]{"err_msg"};
                            if (graphResponse != null)
                                err_msg[0] = graphResponse.getError().getErrorMessage();

                            context.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listener.logoutError(err_msg[0]);
                                }
                            });
                            //return;
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                long current_company = PrefUtil.getCurrentCompanyId(context);
                                String current_state = PrefUtil.getEncodedStateBackup(context);

                                ContentValues values = new ContentValues();
                                // Yes, It is valid to only include the values you want to update
                                values.put(CompanyEntry.COLUMN_STATE_BACKUP, current_state);

                                context.getContentResolver().
                                        update(
                                                CompanyEntry.CONTENT_URI,
                                                values,
                                                CompanyEntry._full(CompanyEntry.COLUMN_COMPANY_ID) + " = ?",
                                                new String[]{
                                                        String.valueOf(current_company)
                                                }
                                        );

                                // clear local stuff
                                PrefUtil.logoutUser(context);
                                // clear facebook stuff
                                LoginManager.getInstance().logOut();

                                context.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        listener.runAfterLogout();
                                    }
                                });
                            }
                        }).start();
                    }
                });
        removePermissionsRequest.executeAsync();
    }
}
