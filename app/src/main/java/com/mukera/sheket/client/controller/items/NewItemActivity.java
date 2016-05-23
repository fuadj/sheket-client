package com.mukera.sheket.client.controller.items;

import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.UnitsOfMeasurement;
import com.mukera.sheket.client.controller.util.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.models.SCategory;
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
                .commit();
    }

    public static class NewItemFragment extends Fragment {
        private EditText mName, mCode, mReorderLevel, mModelYear, mPartNumber;
        private Spinner mUnitsSpinner;
        private CheckBox mHasDerived;
        private EditText mDerivedName, mDerivedFactor, mFormula;
        private TextView mTextBundleName, mTextBundleFactor;

        private Button mCategoryBtn;

        private Button mCancel, mOk;

        private long mSelectedCategoryId = SheketContract.CategoryEntry.ROOT_CATEGORY_ID;
        private SCategory mSelectedCategory = null;

        boolean isEmpty(Editable e) {
            return e.toString().trim().isEmpty();
        }

        void setOkButtonStatus() {
            if (isEmpty(mName.getText())) {
                mOk.setEnabled(false);
                return;
            }

            if (mHasDerived.isChecked()) {
                if (isEmpty(mDerivedName.getText()) || isEmpty(mDerivedFactor.getText())) {
                    mOk.setEnabled(false);
                    return;
                }
            }
            mOk.setEnabled(true);
        }

        String t(String str) {
            return str.trim();
        }

        void updateFormulaDisplay() {
            if (!mHasDerived.isChecked()) return;

            if (isEmpty(mDerivedName.getText()) || isEmpty(mDerivedFactor.getText())) {
                mFormula.setText("");
                return;
            }

            // this has the form " 1 bundle = 120 pcs "
            mFormula.setText(
                    String.format("  1 %s = %s %s  ",
                            mDerivedName.getText().toString().trim(), mDerivedFactor.getText().toString().trim(),
                            UnitsOfMeasurement.getUnitSymbol(mUnitsSpinner.getSelectedItemPosition())));
        }

        void updateDerivedUnitDisplay() {
            boolean is_checked = mHasDerived.isChecked();

            mTextBundleName.setEnabled(is_checked);
            mTextBundleFactor.setEnabled(is_checked);

            mDerivedName.setEnabled(is_checked);
            mDerivedFactor.setEnabled(is_checked);
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

            mUnitsSpinner = (Spinner) rootView.findViewById(R.id.spinner_new_item_unit_selection);
            mUnitsSpinner.setAdapter(new ArrayAdapter(getActivity(),
                    android.R.layout.simple_spinner_dropdown_item,
                    UnitsOfMeasurement.getAllUnits().toArray()));

            mUnitsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    updateFormulaDisplay();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) { }
            });

            mTextBundleName = (TextView) rootView.findViewById(R.id.text_view_new_item_bundle_name);
            mDerivedName = (EditText) rootView.findViewById(R.id.edit_text_new_item_bundle_name);
            mDerivedName.addTextChangedListener(new TextWatcherAdapter(){
                @Override
                public void afterTextChanged(Editable s) {
                    updateFormulaDisplay();
                    setOkButtonStatus();
                }
            });

            mTextBundleFactor = (TextView) rootView.findViewById(R.id.text_view_new_item_bundle_factor);
            mDerivedFactor = (EditText) rootView.findViewById(R.id.edit_text_new_item_bundle_factor);
            mDerivedFactor.addTextChangedListener(new TextWatcherAdapter(){
                @Override
                public void afterTextChanged(Editable s) {
                    updateFormulaDisplay();
                    setOkButtonStatus();
                }
            });

            mFormula = (EditText) rootView.findViewById(R.id.edt_text_new_item_formula);
            mFormula.setEnabled(false);

            mHasDerived = (CheckBox) rootView.findViewById(R.id.check_box_new_item_has_bundle);
            mHasDerived.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateDerivedUnitDisplay();
                    setOkButtonStatus();
                }
            });


            mCode = (EditText) rootView.findViewById(R.id.edit_text_new_item_manual_code);
            mReorderLevel = (EditText) rootView.findViewById(R.id.edit_text_new_item_reorder_level);
            mModelYear = (EditText) rootView.findViewById(R.id.edit_text_new_item_model_year);
            mPartNumber = (EditText) rootView.findViewById(R.id.edit_text_new_item_part_number);

            final AppCompatActivity activity = (AppCompatActivity)getActivity();

            mCategoryBtn = (Button) rootView.findViewById(R.id.btn_new_item_category_selector);
            if (mSelectedCategoryId == SheketContract.CategoryEntry.ROOT_CATEGORY_ID) {
                mCategoryBtn.setText("Not Set");
            } else {
                mCategoryBtn.setText(mSelectedCategory.name);
            }
            mCategoryBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);

                    CategorySelectionFragment fragment = CategorySelectionFragment.
                            newInstance(mSelectedCategoryId);
                    fragment.setListener(new CategorySelectionFragment.SelectionListener() {
                        @Override
                        public void okSelected(long category_id, SCategory category) {
                            mSelectedCategoryId = category_id;
                            mSelectedCategory = category;
                            activity.getSupportFragmentManager().popBackStack();
                        }

                        @Override
                        public void cancelSelected() {
                            activity.getSupportFragmentManager().popBackStack();
                        }
                    });
                    activity.getSupportFragmentManager().beginTransaction()
                            .replace(R.id.new_item_container, fragment)
                            .addToBackStack(null)
                            .commit();
                }
            });

            mOk = (Button) rootView.findViewById(R.id.btn_new_item_ok);
            mOk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final long new_item_id = PrefUtil.getNewItemId(getActivity());
                    PrefUtil.setNewItemId(getActivity(), new_item_id);

                    final String name = t(mName.getText().toString());

                    final int unit = mUnitsSpinner.getSelectedItemPosition();
                    final int has_derived_unit = SheketContract.toInt(
                            mHasDerived.isChecked());
                    final String derived_name = t(mDerivedName.getText().toString());

                    final long category_id = mSelectedCategoryId;

                    String factor = t(mDerivedFactor.getText().toString());
                    final double derived_factor = factor.isEmpty() ? 0 : Double.valueOf(factor);

                    String level = t(mReorderLevel.getText().toString());
                    final double reorder_level = level.isEmpty() ? 0 : Double.valueOf(level);

                    final String model_year = t(mModelYear.getText().toString());
                    final String part_number = t(mPartNumber.getText().toString());
                    final String code = t(mCode.getText().toString());
                    final String bar_code = "";

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();
                            values.put(ItemEntry.COLUMN_ITEM_ID, new_item_id);
                            values.put(ItemEntry.COLUMN_NAME, name);
                            values.put(ItemEntry.COLUMN_CATEGORY_ID, category_id);

                            values.put(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT, unit);
                            values.put(ItemEntry.COLUMN_HAS_DERIVED_UNIT, has_derived_unit);
                            values.put(ItemEntry.COLUMN_DERIVED_UNIT_NAME, derived_name);
                            values.put(ItemEntry.COLUMN_DERIVED_UNIT_FACTOR, derived_factor);

                            values.put(ItemEntry.COLUMN_REORDER_LEVEL, reorder_level);

                            values.put(ItemEntry.COLUMN_BAR_CODE, bar_code);
                            values.put(ItemEntry.COLUMN_MANUAL_CODE, code);
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
                    activity.finish();
                }
            });

            // To start things off
            updateDerivedUnitDisplay();
            setOkButtonStatus();

            return rootView;
        }

    }

    /*
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }
    */
}
