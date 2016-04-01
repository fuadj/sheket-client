package com.mukera.sheket.client.controller.item_searcher;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import me.dm7.barcodescanner.zbar.BarcodeFormat;
import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by gamma on 1/13/16.
 */
public class ScanSearchFragment extends Fragment implements ZBarScannerView.ResultHandler {
    public ScanResultListener mResultListener;
    private ZBarScannerView mScannerView;

    public ScanSearchFragment() {
    }

    public void setResultListener(ScanResultListener listener) {
        mResultListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mScannerView = new ZBarScannerView(getActivity());
        List<BarcodeFormat> formatList = new ArrayList<>();
        formatList.add(BarcodeFormat.EAN13);
        formatList.add(BarcodeFormat.EAN8);
        //formatList.add(BarcodeFormat.QRCODE);
        mScannerView.setFormats(formatList);
        return mScannerView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(final Result rawResult) {
        /*
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getActivity().getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            Log.d("ScanSearchFragment", e.getMessage());
        }
        */
        Toast.makeText(getActivity(), "Contents = " + rawResult.getContents() +
                ", Format = " + rawResult.getBarcodeFormat().getName(), Toast.LENGTH_SHORT).show();

        if (mResultListener != null)
            mResultListener.resultFound(rawResult.getContents());
        /*
        (new Handler()).postDelayed(new Runnable() {
            @Override
            public void run() {
                //mScannerView.resumeCameraPreview(ScanSearchFragment.this);
            }
        }, 2000);
        */
    }

    public interface ScanResultListener {
        void resultFound(String result);
    }
}
