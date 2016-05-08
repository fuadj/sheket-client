package com.mukera.sheket.client.controller.items;

import android.content.ContentValues;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.TextWatcherAdapter;
import com.mukera.sheket.client.controller.item_searcher.ScanSearchFragment;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.UUID;

/**
 * Created by gamma on 3/6/16.
 */
public class NewItemActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_item);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.new_item_container, new NewItemFragment())
                .addToBackStack(null)
                .commit();
    }

    public static class NewItemFragment extends Fragment {
        private EditText mName, mModelYear, mPartNumber, mManualCode;
        private TextView mBarcodeLabel;
        private ImageButton mBarcodeBtn;

        private Button mCancel, mOk;

        private String mSavedBarcode;

        void setOkButtonStatus() {
            /*
            boolean code_entered = !mBarcodeLabel.getText().toString().isEmpty()
                    || !mManualCode.getText().toString().trim().isEmpty();
            mOk.setEnabled(code_entered && !mName.getText().toString().toString().isEmpty());
            */
            mOk.setEnabled(!mName.getText().toString().toString().isEmpty());
        }

        String t(String str) {
            return str.trim();
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_new_item, container, false);

            TextWatcherAdapter okButtonStatusChecker = new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    setOkButtonStatus();
                }
            };

            mName = (EditText) rootView.findViewById(R.id.edit_text_new_item_name);
            mName.addTextChangedListener(okButtonStatusChecker);

            mModelYear = (EditText) rootView.findViewById(R.id.edit_text_new_item_model_year);
            mPartNumber = (EditText) rootView.findViewById(R.id.edit_text_new_item_part_number);

            mManualCode = (EditText) rootView.findViewById(R.id.edit_text_new_item_manual_code);
            mManualCode.addTextChangedListener(okButtonStatusChecker);

            mBarcodeLabel = (TextView) rootView.findViewById(R.id.text_view_new_item_bar_code);

            final AppCompatActivity activity = (AppCompatActivity)getActivity();

            mOk = (Button) rootView.findViewById(R.id.btn_new_item_ok);
            mOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String name = t(mName.getText().toString());
                    final String model_year = t(mModelYear.getText().toString());
                    final String part_number = t(mPartNumber.getText().toString());
                    final String manual_code = t(mManualCode.getText().toString());
                    final String bar_code = t(mBarcodeLabel.getText().toString());
                    final long new_item_id = PrefUtil.getNewItemId(getActivity());
                    PrefUtil.setNewItemId(getActivity(), new_item_id);

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(ItemEntry.COLUMN_ITEM_ID, new_item_id);
                            values.put(ItemEntry.COLUMN_NAME, name);
                            values.put(ItemEntry.COLUMN_BAR_CODE, bar_code);
                            values.put(ItemEntry.COLUMN_MANUAL_CODE, manual_code);
                            values.put(ItemEntry.COLUMN_HAS_BAR_CODE, !bar_code.isEmpty());
                            values.put(ItemEntry.COLUMN_MODEL_YEAR, model_year);
                            values.put(ItemEntry.COLUMN_PART_NUMBER, part_number);
                            values.put(ItemEntry.COLUMN_COMPANY_ID, PrefUtil.getCurrentCompanyId(getActivity()));
                            values.put(SheketContract.UUIDSyncable.COLUMN_UUID,
                                    UUID.randomUUID().toString());
                            values.put(SheketContract.ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                                    SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED);

                            activity.getContentResolver().insert(
                                    ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())), values);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // TODO: notify that we created successfully
                                    activity.finish();
                                }
                            });
                        }
                    };
                    t.start();
                }
            });
            mCancel = (Button) rootView.findViewById(R.id.btn_new_item_cancel);
            mCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO: notify cancel pressed
                    activity.finish();
                }
            });

            if (mSavedBarcode != null && !mSavedBarcode.isEmpty())
                mBarcodeLabel.setText(mSavedBarcode);
            mBarcodeBtn = (ImageButton) rootView.findViewById(R.id.img_btn_new_item_bar_code);
            mBarcodeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ScanSearchFragment fragment = new ScanSearchFragment();
                    fragment.setResultListener(new ScanSearchFragment.ScanResultListener() {
                        @Override
                        public void resultFound(String result) {
                            result = result.trim();
                            mSavedBarcode = result;
                            setOkButtonStatus();
                            activity.getSupportFragmentManager().popBackStack();
                        }
                    });
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.new_item_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            // To start things off
            setOkButtonStatus();

            return rootView;
        }

    }

}
