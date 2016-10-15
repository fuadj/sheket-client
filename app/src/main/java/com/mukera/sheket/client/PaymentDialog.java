package com.mukera.sheket.client;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.CompanyEntry;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.network.CompanyAuth;
import com.mukera.sheket.client.network.CompanyID;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.network.VerifyPaymentRequest;
import com.mukera.sheket.client.network.VerifyPaymentResponse;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.PrefUtil;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by fuad on 9/6/16.
 */
public class PaymentDialog extends DialogFragment {
    private TextView mTextReasonDesc;
    private FancyButton mBtnVerify;

    private View mLayoutNum;
    private TextView mTextTitleNum;
    private TextView mTextNum;
    private TextView mTextDescNum;

    private static final String ARG_COMPANY = "arg_company";
    private SCompany mCompany;

    public static PaymentDialog newInstance(SCompany company) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_COMPANY, company);

        PaymentDialog dialog = new PaymentDialog();
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCompany = getArguments().getParcelable(ARG_COMPANY);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().
                inflate(R.layout.dialog_payment, null);

        mTextReasonDesc = (TextView) view.findViewById(R.id.dialog_payment_text_reason_desc);
        mBtnVerify = (FancyButton) view.findViewById(R.id.dialog_payment_btn_verify);
        mLayoutNum = view.findViewById(R.id.dialog_payment_layout_num);

        mTextTitleNum = (TextView) view.findViewById(R.id.dialog_payment_text_title_payment_num);
        mTextNum = (TextView) view.findViewById(R.id.dialog_payment_text_payment_num);

        mTextNum.setText(IdEncoderUtil.encodeAndDelimitId(mCompany.company_id, IdEncoderUtil.ID_TYPE_COMPANY));

        mTextDescNum = (TextView) view.findViewById(R.id.dialog_payment_text_payment_num_desc);

        mBtnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SheketTracker.setScreenName(getActivity(), SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(getActivity(),
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                setAction("verify payment selected").
                                setLabel(mCompany.payment_state == CompanyEntry.PAYMENT_INVALID ? "invalid payment" : "expired payment").
                                build());

                final ProgressDialog verificationProgress = ProgressDialog.show(
                        getActivity(),
                        getString(R.string.dialog_payment_verification_progress_title),
                        getString(R.string.dialog_payment_verification_progress_body),
                        true);

                new Thread() {
                    @Override
                    public void run() {
                        final Pair<Boolean, String> result = verifyPayment(mCompany);
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                verificationProgress.dismiss();
                                final PaymentDialog paymentDialog = PaymentDialog.this;
                                if (result.first == Boolean.TRUE) {
                                    new AlertDialog.Builder(getActivity()).
                                            setIcon(android.R.drawable.ic_dialog_info).
                                            setMessage(R.string.dialog_payment_result_ok).
                                            setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    paymentDialog.dismiss();
                                                }
                                            }).
                                            setOnDismissListener(new DialogInterface.OnDismissListener() {
                                                @Override
                                                public void onDismiss(DialogInterface dialog) {
                                                    paymentDialog.dismiss();
                                                }
                                            }).show();
                                } else {
                                    new AlertDialog.Builder(getActivity()).
                                            setIcon(android.R.drawable.ic_dialog_alert).
                                            setTitle(R.string.dialog_payment_result_error).
                                            setMessage(result.second).show();
                                }
                            }
                        });
                    }
                }.start();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setView(view);

        if (mCompany.payment_state == SheketContract.CompanyEntry.PAYMENT_ENDED) {
            builder.setTitle(R.string.dialog_payment_title_payment_ended);
            mTextReasonDesc.setText(R.string.dialog_payment_body_payment_ended);
        } else if (mCompany.payment_state == SheketContract.CompanyEntry.PAYMENT_INVALID) {
            builder.setTitle(R.string.dialog_payment_title_payment_invalid);
            mTextReasonDesc.setText(R.string.dialog_payment_body_payment_invalid);
        }
        Dialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                SheketTracker.setScreenName(getActivity(), SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(getActivity(),
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_DIALOG).
                                setAction("payment dialog shown").
                                setLabel(mCompany.payment_state == CompanyEntry.PAYMENT_INVALID ? "invalid payment" : "expired payment").
                                build());
            }
        });

        return dialog;
    }

    /**
     * Tries to verify payment. If payment was verified, it will update the company's
     * payment "stuff" in the local db.
     *
     * @return {@code Pair<True, Null>} If it was successful.
     * {@code Pair<False, Error Message>} otherwise
     */
    Pair<Boolean, String> verifyPayment(SCompany company) {
        CompanyAuth companyAuth = CompanyAuth.
                newBuilder().
                setCompanyId(CompanyID.newBuilder().setCompanyId(company.company_id).build()).
                setSheketAuth(
                        SheketAuth.newBuilder().setLoginCookie(
                                PrefUtil.getLoginCookie(getContext())
                        ).build()
                ).build();
        final VerifyPaymentRequest request = VerifyPaymentRequest.newBuilder().
                setCompanyAuth(companyAuth).
                setDeviceId(DeviceId.getUniqueDeviceId(getActivity())).
                setLocalUserTime(String.valueOf(System.currentTimeMillis())).
                build();

        try {
            VerifyPaymentResponse response = new SheketGRPCCall<VerifyPaymentResponse>().runBlockingCall(
                    new SheketGRPCCall.GRPCCallable<VerifyPaymentResponse>() {
                        @Override
                        public VerifyPaymentResponse runGRPCCall() throws Exception {
                            ManagedChannel managedChannel = ManagedChannelBuilder.
                                    forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                                    usePlaintext(true).
                                    build();

                            SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                                    SheketServiceGrpc.newBlockingStub(managedChannel);
                            return blockingStub.verifyPayment(request);
                        }
                    }
            );
            ContentValues updated_values = company.toContentValues();
            updated_values.put(CompanyEntry.COLUMN_PAYMENT_LICENSE, response.getSignedLicense());
            updated_values.put(CompanyEntry.COLUMN_PAYMENT_STATE, CompanyEntry.PAYMENT_VALID);

            // we need to remove it, it creates problems with updating and foreign key reference
            updated_values.remove(CompanyEntry.COLUMN_COMPANY_ID);

            int rows_updated = getContext().getContentResolver().
                    update(CompanyEntry.CONTENT_URI,
                            updated_values,
                            CompanyEntry.COLUMN_COMPANY_ID + " = ?",
                            new String[]{String.valueOf(company.company_id)});
            boolean update_success = rows_updated == 1;
            if (update_success)
                return new Pair<>(Boolean.TRUE, null);
            else
                return new Pair<>(Boolean.FALSE, "Problem updating license");
        } catch (SheketGRPCCall.SheketInvalidLoginException e) {
            LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
                    new Intent(SheketBroadcast.ACTION_SYNC_INVALID_LOGIN_CREDENTIALS)
            );
            return new Pair<>(Boolean.FALSE, e.getMessage());
        } catch (SheketGRPCCall.SheketInternetException e) {
            return new Pair<>(Boolean.FALSE, "Internet problem");
        } catch (SheketGRPCCall.SheketException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }
}
