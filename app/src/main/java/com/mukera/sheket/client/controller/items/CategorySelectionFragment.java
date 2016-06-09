package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.base_adapters.BaseCategoryChildrenAdapter;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.Stack;
import java.util.UUID;

/**
 * Created by fuad on 5/21/16.
 */
public class CategorySelectionFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public interface SelectionListener {
        void okSelected(long category_id, SCategory category);
        void cancelSelected();
    }

    private static final String PREVIOUS_CATEGORY_ID_KEY = "previous_category_id";

    private long mSelectedCategoryId;

    private SCategory mSelectedCategory = null;

    private long mCurrentParentCategoryId;
    private Stack<Long> mParentCategoryBackStack;

    private ListView mCategoryList;
    private CategorySelectionCursorAdapter mCategoryAdapter;

    private SelectionListener mListener;

    public static CategorySelectionFragment newInstance(long previous_selected_category_id) {
        Bundle args = new Bundle();

        CategorySelectionFragment fragment = new CategorySelectionFragment();
        args.putLong(PREVIOUS_CATEGORY_ID_KEY, previous_selected_category_id);
        fragment.setArguments(args);
        return fragment;
    }

    public void setListener(SelectionListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mSelectedCategoryId = args.getLong(PREVIOUS_CATEGORY_ID_KEY);

        // the first level category is the root
        mCurrentParentCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
        mParentCategoryBackStack = new Stack<>();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.TransactionActivity.CATEGORY_SELECTION_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    void restartLoader() {
        getLoaderManager().restartLoader(LoaderId.TransactionActivity.CATEGORY_SELECTION_LOADER, null, this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_category_selection, container, false);

        mCategoryList = (ListView) rootView.findViewById(R.id.category_selection_list_view_categories);
        mCategoryAdapter = new CategorySelectionCursorAdapter(getActivity());
        mCategoryAdapter.mAdapterListener = new CategorySelectionCursorAdapter.AdapterSelectionListener() {
            @Override
            public void categorySelected(boolean newly_selected, long category_id, SCategory category) {
                if (newly_selected) {
                    if (category_id == mSelectedCategoryId) {   // user is selecting again to "un-check" it
                        mSelectedCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
                        mCategoryAdapter.mPreviousSelectedCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
                        mSelectedCategory = null;
                    } else {
                        mSelectedCategoryId = category_id;
                        mSelectedCategory = category;
                        mCategoryAdapter.mPreviousSelectedCategoryId = category_id;
                    }
                } else {
                    // this needs to be saved if only for the user
                    // (gets to selection fragment with already selected category) -> (then directly pressed OK)
                    // the category needs to be passed in even-though it isn't selected in the current "session"
                    mSelectedCategoryId = category_id;
                    mSelectedCategory = category;
                }
            }
        };
        mCategoryAdapter.mPreviousSelectedCategoryId = mSelectedCategoryId;
        mCategoryList.setAdapter(mCategoryAdapter);
        mCategoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SCategory category = mCategoryAdapter.getItem(position);
                mParentCategoryBackStack.push(mCurrentParentCategoryId);
                mCurrentParentCategoryId = category.category_id;
                restartLoader();
            }
        });

        Button cancel_btn, ok_btn;
        cancel_btn = (Button) rootView.findViewById(R.id.category_selection_btn_cancel);
        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.cancelSelected();
                }
            }
        });

        ok_btn = (Button) rootView.findViewById(R.id.category_selection_btn_ok);
        ok_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    if (mSelectedCategoryId == CategoryEntry.ROOT_CATEGORY_ID)
                        mSelectedCategory = null;
                    mListener.okSelected(mSelectedCategoryId, mSelectedCategory);
                }
            }
        });

        View add_category_view = rootView.findViewById(R.id.category_selection_add_new_category);
        add_category_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                CategoryCreateDialog dialog = new CategoryCreateDialog();
                dialog.parent_category_id = mCurrentParentCategoryId;

                dialog.show(fm, "Category");
            }
        });

        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mParentCategoryBackStack.isEmpty()) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        mCurrentParentCategoryId = mParentCategoryBackStack.pop();
                        restartLoader();
                    }
                    return true;
                }

                return false;
            }
        });
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " ASC";

        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        return new CursorLoader(getActivity(),
                CategoryEntry.buildBaseUri(company_id),
                SCategory.CATEGORY_COLUMNS,
                CategoryEntry._full(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[]{String.valueOf(mCurrentParentCategoryId)},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCategoryAdapter.setCategoryCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mCategoryAdapter.setCategoryCursor(null);
    }

    public static class CategorySelectionCursorAdapter extends BaseCategoryChildrenAdapter {
        public interface AdapterSelectionListener {
            void categorySelected(boolean newly_selected, long category_id, SCategory category);
        }

        private static class ViewHolder {
            CheckBox mSelectCategory;
            TextView mCategoryName, mSubCategoryCount;

            public ViewHolder(View view) {
                mSelectCategory = (CheckBox) view.findViewById(R.id.list_item_item_category_check_box_select);
                mCategoryName = (TextView) view.findViewById(R.id.list_item_item_category_category_name);
                mSubCategoryCount = (TextView) view.findViewById(R.id.list_item_item_category_sub_category_count);
            }
        }

        public AdapterSelectionListener mAdapterListener;
        public long mPreviousSelectedCategoryId;

        public CategorySelectionCursorAdapter(Context context) {
            super(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final SCategory category = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).
                        inflate(R.layout.list_item_item_category_selection, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final long category_id = category.category_id;
            holder.mCategoryName.setText(category.name);
            holder.mSubCategoryCount.setText("" + category.childrenCategories.size());
            boolean is_selected = mPreviousSelectedCategoryId == category_id;
            holder.mSelectCategory.setChecked(is_selected);
            if (is_selected)
                mAdapterListener.categorySelected(false, category_id, category);

            holder.mSelectCategory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAdapterListener.categorySelected(true, category_id, category);
                    notifyDataSetChanged();
                }
            });

            return convertView;
        }
    }

    public static class CategoryCreateDialog extends DialogFragment {
        public long parent_category_id;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            View view = getActivity().getLayoutInflater().
                    inflate(R.layout.dialog_new_category, null);

            final EditText editCategoryName = (EditText) view.findViewById(R.id.dialog_new_category_edit_text_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Create Category").setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    getDialog().dismiss();
                }
            }).setPositiveButton("Create", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Activity activity = getActivity();
                    final String category_name = editCategoryName.getText().toString().trim();

                    final long new_category_id = PrefUtil.getNewCategoryId(activity);
                    PrefUtil.setNewCategoryId(activity, new_category_id);

                    final long company_id = PrefUtil.getCurrentCompanyId(activity);

                    final Dialog category_dialog = getDialog();

                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            ContentValues values = new ContentValues();

                            values.put(CategoryEntry.COLUMN_CATEGORY_ID, new_category_id);
                            values.put(CategoryEntry.COLUMN_NAME, category_name);
                            values.put(CategoryEntry.COLUMN_PARENT_ID, parent_category_id);
                            values.put(CategoryEntry.COLUMN_COMPANY_ID, company_id);

                            values.put(UUIDSyncable.COLUMN_UUID,
                                    UUID.randomUUID().toString());
                            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                                    ChangeTraceable.CHANGE_STATUS_CREATED);

                            activity.getContentResolver().insert(
                                    CategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(activity)), values);

                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    category_dialog.dismiss();
                                }
                            });
                        }
                    };
                    t.start();
                }
            });
            final AlertDialog dialog = builder.setView(view).create();
            editCategoryName.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(!TextUtils.isEmpty(s.toString()));
                }
            });
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(
                            !TextUtils.isEmpty(editCategoryName.getText().toString().trim()));
                }
            });

            dialog.setCanceledOnTouchOutside(true);
            return dialog;
        }
    }
}
