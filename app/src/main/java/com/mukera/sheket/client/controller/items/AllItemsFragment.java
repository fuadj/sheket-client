package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.controller.items.transactions.CategoryUtil;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by gamma on 3/4/16.
 */
public class AllItemsFragment extends SearchableItemFragment {
    private static final String KEY_SAVE_EDIT_MODE = "key_save_edit_mode";
    private boolean mIsEditMode = false;

    private View mViewSelectAll;
    private CheckBox mCheckBoxSelectAll;

    private FloatingActionButton mPasteBtn, mAddBtn, mDeleteBtn;

    private long mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;

    /**
     * We hold on to them for finally applying an operation on them AND ALSO
     * the UI looks at this to figure out which are selected or not.
     */
    private Map<Long, SCategory> mSelectedCategories;

    /**
     * This holds any parent category of selected categories/items.
     * Depending on the size this set, the paste/delete button with either
     * be enabled/disabled when editing.
     */
    private Set<Long> mParentCategories;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsEditMode = savedInstanceState.getBoolean(KEY_SAVE_EDIT_MODE, false);
        }
        mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
        setHasOptionsMenu(true);
        setCurrentCategory(mCategoryId);

        mSelectedCategories = new HashMap<>();
        mParentCategories = new HashSet<>();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_SAVE_EDIT_MODE, mIsEditMode);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.all_items, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.all_items_menu_toggle_editing:
                mIsEditMode = !mIsEditMode;

                // for the next round, we start fresh
                mSelectedCategories.clear();
                updateEditingUI();
                /**
                 * this will ensure that the previous types of views
                 * won't be given to us as "converted-recycled-views".
                 * This sometimes creates a problem if you are displaying two
                 * types of views. There will be problems trying to access
                 * view elements that won't exist and stuff. So, invalidate
                 * the whole listview and start feeding it new views.
                 */
                invalidateListView();

                restartLoader();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateEditingUI() {
        //((CategorySelectionEditionAdapter)getCategoryAdapter()).setEditMode(mIsEditMode);
        //mViewSelectAll.setVisibility(mIsEditMode ? View.VISIBLE : View.GONE);
        if (!mIsEditMode) {
            mAddBtn.setVisibility(View.VISIBLE);
            mPasteBtn.setVisibility(View.GONE);
            mDeleteBtn.setVisibility(View.GONE);
        } else {
            mAddBtn.setVisibility(View.GONE);
            mPasteBtn.setVisibility(View.VISIBLE);
            mDeleteBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCategorySelected(long previous_category, long selected_category) {
        mCategoryId = selected_category;
    }

    @Override
    public void onInitLoader() {
        getLoaderManager().initLoader(LoaderId.MainActivity.ITEM_LIST_LOADER, null, this);
    }

    @Override
    public void onRestartLoader() {
        getLoaderManager().restartLoader(LoaderId.MainActivity.ITEM_LIST_LOADER, null, this);
    }

    @Override
    protected int getCategoryLoaderId() {
        return LoaderId.MainActivity.ITEM_LIST_CATEGORY_LOADER;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_all_item;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mAddBtn = (FloatingActionButton) rootView.findViewById(R.id.all_items_float_btn_add);
        mAddBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayAddOptionDialog();
            }
        });
        mPasteBtn = (FloatingActionButton) rootView.findViewById(R.id.all_items_float_btn_paste);
        mPasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        moveCategoriesToCurrentCategory();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsEditMode = false;
                                updateEditingUI();

                                // we are invalidating the view b/c we are leaving "edit-mode" for
                                // normal mode, and the two views are different.
                                invalidateListView();

                                restartLoader();
                            }
                        });
                    }
                }).start();
            }
        });

        mDeleteBtn = (FloatingActionButton) rootView.findViewById(R.id.all_items_float_btn_delete);

        return rootView;
    }

    @Override
    protected boolean onEntitySelected(Cursor cursor) {
        FragmentManager fm = getActivity().getSupportFragmentManager();

        SItem item = new SItem(cursor, true);
        ItemDetailDialog dialog = new ItemDetailDialog();
        dialog.mItem = item;
        dialog.show(fm, null);
        return true;
    }

    private static class ItemViewHolder {
        ImageView item_info;
        TextView item_name;
        TextView item_code;
        TextView total_qty;

        public ItemViewHolder(View view) {
            item_info = (ImageView) view.findViewById(R.id.list_item_item_detail_info);
            item_name = (TextView) view.findViewById(R.id.list_item_item_detail_name);
            item_code = (TextView) view.findViewById(R.id.list_item_item_detail_code);
            total_qty = (TextView) view.findViewById(R.id.list_item_item_detail_total_qty);
        }
    }

    @Override
    public View newItemView(Context context, ViewGroup parent, Cursor cursor, int position) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.list_item_all_items, parent, false);
        ItemViewHolder holder = new ItemViewHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public void bindItemView(Context context, Cursor cursor, View view, int position) {
        cursor.moveToPosition(position);
        // we also want to fetch the branches it exists in, that is the true argument
        final SItem item = new SItem(cursor, true);

        ItemViewHolder holder = (ItemViewHolder) view.getTag();

        holder.item_name.setText(item.name);
        boolean has_code = item.has_bar_code || !item.item_code.isEmpty();
        if (has_code) {
            holder.item_code.setVisibility(View.VISIBLE);
            holder.item_code.setText(
                    item.has_bar_code ? item.bar_code : item.item_code);
        } else {
            holder.item_code.setVisibility(View.GONE);
        }
        holder.total_qty.setText(Utils.formatDoubleForDisplay(item.total_quantity));
        holder.item_info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity activity = getActivity();
                new AlertDialog.Builder(activity).
                        setTitle("Edit Item").
                        setMessage("Do you want to edit item attributes?").
                        setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        startActivity(ItemCreateEditActivity.createIntent(getActivity(), true, item));
                                    }
                                }).
                        setNegativeButton("No", null).
                        show();
            }
        });
    }

    /**
     * We are overriding the category view stuff to implement the checkbox selection for
     * editing purposes.
     */
    @Override
    public View newCategoryView(Context context, ViewGroup parent, Cursor cursor, int position) {
        // we only want to override it when we are in "edit-mode"
        if (!mIsEditMode)
            return super.newCategoryView(context, parent, cursor, position);

        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.list_item_all_items_edit_category, parent, false);
        final CategoryViewHolder holder = new CategoryViewHolder(view);

        view.setTag(holder);
        return view;
    }

    @Override
    public void bindCategoryView(Context context, Cursor cursor, View view, int position) {
        // we only want to override it when we are in "edit-mode"
        if (!mIsEditMode) {
            super.bindCategoryView(context, cursor, view, position);
            return;
        }

        final SCategory category = new SCategory(cursor, true);

        final CategoryViewHolder holder = (CategoryViewHolder) view.getTag();

        holder.categoryName.setText(category.name);
        if (category.childrenCategories.isEmpty()) {
            holder.subCount.setVisibility(View.GONE);
        } else {
            holder.subCount.setVisibility(View.VISIBLE);
            holder.subCount.setText("" + category.childrenCategories.size());
        }

        /**
         * since the adapter recycles the list item UI, the checkbox probably has
         * a listener assigned by its previous "owner". So, we should reset it to null
         * because calling {@code selectCheck.setChecked} will invoke that listener
         * and weird shit happens.
         */
        holder.selectCheck.setOnCheckedChangeListener(null);
        holder.selectFrameLayout.setOnClickListener(null);
        holder.editBtn.setOnClickListener(null);
        holder.editFrameLayout.setOnClickListener(null);

        holder.selectCheck.setChecked(mSelectedCategories.containsKey(category.category_id));
        holder.selectCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    mSelectedCategories.put(category.category_id, category);
                } else {
                    mSelectedCategories.remove(category.category_id);
                }
            }
        });
        /**
         * It is hard to click the check box, we intercept the enclosing view and simulate a
         * click on the checkbox also. If we don't do this, then the list-view will receive the
         * click event and that is not the desired behaviour.
         */
        holder.selectFrameLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.selectCheck.setChecked(!holder.selectCheck.isChecked());
            }
        });

        View.OnClickListener editListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displayCategoryAddEditDialog(category);
            }
        };
        holder.editBtn.setOnClickListener(editListener);
        holder.editFrameLayout.setOnClickListener(editListener);
    }

    private static class CategoryViewHolder {
        TextView categoryName, subCount;
        CheckBox selectCheck;
        View selectFrameLayout, editFrameLayout;
        ImageView editBtn;

        public CategoryViewHolder(View view) {
            categoryName = (TextView) view.findViewById(R.id.list_item_all_items_category_text_view_category_name);
            subCount = (TextView) view.findViewById(R.id.list_item_all_items_category_text_view_sub_count);
            selectCheck = (CheckBox) view.findViewById(R.id.list_item_all_items_category_check_box_select);
            selectFrameLayout = view.findViewById(R.id.list_item_all_items_category_layout_select);
            editBtn = (ImageView) view.findViewById(R.id.list_item_all_items_category_btn_edit);
            editFrameLayout = view.findViewById(R.id.list_item_all_items_category_layout_edit);
        }
    }

    /**
     * Moves the selected categories to the current category. Updates them so they
     * point to this category as their parent.
     */
    void moveCategoriesToCurrentCategory() {
        if (mSelectedCategories.isEmpty()) return;

        long company_id = PrefUtil.getCurrentCompanyId(getActivity());
        long current_category = super.getCurrentCategory();

        ArrayList<ContentProviderOperation> operation = new ArrayList<>();
        for (SCategory category : mSelectedCategories.values()) {
            category.parent_id = current_category;

            /**
             * If it is only created(not yet synced), you can't set it to updated b/c
             * it hasn't yet been "created" at the server side. So trying to update it
             * will create problems.
             */
            if (category.change_status != SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED)
                category.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_UPDATED;

            operation.add(ContentProviderOperation.newUpdate(
                    CategoryEntry.buildBaseUri(company_id)).
                    withValues(category.toContentValues()).
                    withSelection(CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " = ?",
                            new String[]{Long.toString(category.category_id)}).build());
        }

        try {
            getActivity().getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operation);

            /**
             * This needs to be performed after the categories have moved. This is because we
             * can't efficiently know what the category ancestry of the items will be until the
             * movement has finished.
             */
            CategoryUtil.updateBranchCategoriesForAllBranches(getActivity());
        } catch (RemoteException | OperationApplicationException e) {

        }
    }

    void displayAddOptionDialog() {
        View options_view = getActivity().getLayoutInflater().
                inflate(R.layout.all_items_floating_btn_add_options, null);
        final ImageButton option_item = (ImageButton) options_view.findViewById(R.id.all_items_option_add_item);
        final ImageButton option_category = (ImageButton) options_view.findViewById(R.id.all_items_option_add_category);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setView(options_view);
        final AlertDialog dialog = builder.create();
        option_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                startActivity(ItemCreateEditActivity.createIntent(getActivity(), false, null));
            }
        });
        option_category.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                displayCategoryAddEditDialog(null);
            }
        });
        dialog.show();
    }

    /**
     * Displays a dialog for setting the name of a category.
     * This is used for both creating and editing category names.
     *
     * @param category if of null, it is then editing and the changed name
     *                 will be set to the category.
     *                 if null, it is creating a new category.
     */
    void displayCategoryAddEditDialog(final SCategory category) {
        final EditText editText = new EditText(getActivity());

        final boolean is_editing = category != null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setTitle(is_editing ? "Edit Category" : "New Category").
                setMessage(is_editing ? category.name : "").
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String name = editText.getText().toString().trim();

                        createOrEditCategory(dialog, category, is_editing, name);
                    }
                }).setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false);

        final AlertDialog dialog = builder.create();

        if (is_editing)
            editText.setText(category.name);

        dialog.show();
    }

    /**
     * creates or updates a category on a worker-thread and finally dismisses the dialog on the math thread.
     */
    void createOrEditCategory(final DialogInterface dialog,
                              final SCategory category,
                              final boolean is_editing,
                              final String category_name) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                long company_id = PrefUtil.getCurrentCompanyId(getActivity());

                ContentValues values;
                if (is_editing) {
                    category.name = category_name;
                    if (category.change_status != SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED)
                        category.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_UPDATED;

                    values = category.toContentValues();

                    getActivity().getContentResolver().update(
                            CategoryEntry.buildBaseUri(company_id),
                            values,
                            String.format(Locale.US, "%s = ?", CategoryEntry.COLUMN_CATEGORY_ID),
                            new String[]{String.valueOf(category.category_id)});
                } else {
                    final long new_category_id = PrefUtil.getNewCategoryId(getActivity());
                    PrefUtil.setNewCategoryId(getActivity(), new_category_id);

                    long current_category = AllItemsFragment.this.getCurrentCategory();

                    values = new ContentValues();
                    values.put(CategoryEntry.COLUMN_CATEGORY_ID, new_category_id);
                    values.put(CategoryEntry.COLUMN_NAME, category_name);
                    values.put(CategoryEntry.COLUMN_PARENT_ID, current_category);
                    values.put(CategoryEntry.COLUMN_COMPANY_ID, company_id);
                    values.put(SheketContract.UUIDSyncable.COLUMN_UUID,
                            UUID.randomUUID().toString());
                    values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                            ChangeTraceable.CHANGE_STATUS_CREATED);

                    getActivity().getContentResolver().insert(CategoryEntry.buildBaseUri(company_id), values);
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        // TODO: it is strange that if we create a new category, then the item list disappears
                        // and we have to restart the loader just for it, fix it!!
                        restartLoader();
                    }
                });
            }
        }).start();
    }

    @Override
    protected boolean onSearchTextChanged(String newText) {
        restartLoader();
        return true;
    }

    @Override
    protected boolean onSearchTextViewClosed() {
        restartLoader();
        return true;
    }

    @Override
    protected Loader<Cursor> onEntityCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        String sortOrder = ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " ASC";

        String selection = null;
        String[] selectionArgs = null;

        if (super.isSearching()) {
            String search_text = super.getSearchText();
            selection = "(" + ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " LIKE '%" + search_text + "%' OR " +
                    ItemEntry._full(ItemEntry.COLUMN_NAME) + " LIKE '%" + search_text + "%' ) ";
        } else if (super.isShowingCategoryTree()) {
            selection = ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?";
            selectionArgs = new String[]{String.valueOf(mCategoryId)};
        }

        return new CursorLoader(getActivity(),
                ItemEntry.buildBaseUriWithBranches(company_id),
                SItem.ITEM_WITH_BRANCH_DETAIL_COLUMNS,
                selection,
                selectionArgs,
                sortOrder
        );
    }

    @Override
    protected boolean shouldShowCategoryNavigation() {
        /**
         * If we are searching, we don't what the categories to be displayed!!!
         */
        return !super.isSearching();
    }

    @Override
    protected void onEntityLoaderFinished(Loader<Cursor> loader, Cursor data) {
        setEntityCursor(new SItem.ItemWithAvailableBranchesCursor(data));
    }

    @Override
    protected void onEntityLoaderReset(Loader<Cursor> loader) {
        setEntityCursor(null);
    }

    /**
     * Shows which branches the items exist in and the location in each.
     */
    public static class ItemDetailDialog extends DialogFragment {
        public SItem mItem;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_all_item_detail, null);

            ListView branchesList = (ListView) view.findViewById(R.id.dialog_all_item_list_view_branches);
            DetailDialogAdapter adapter = new DetailDialogAdapter(getActivity());
            for (Pair<SBranchItem, SBranch> pair : mItem.available_branches) {
                adapter.add(pair);
            }
            branchesList.setAdapter(adapter);

            TextView qty_text_view = (TextView) view.findViewById(R.id.dialog_all_item_text_view_total_quantity);
            qty_text_view.setText("Total Qty: " + Utils.formatDoubleForDisplay(mItem.total_quantity));

            return builder.setTitle(mItem.name).setView(view).create();
        }

        private static class DetailDialogAdapter extends ArrayAdapter<Pair<SBranchItem, SBranch>> {
            public DetailDialogAdapter(Context context) {
                super(context, 0);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final Pair<SBranchItem, SBranch> pair = getItem(position);

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.list_item_all_item_detail_dialog, parent, false);
                }

                TextView branchName, itemLoc, itemQty;

                branchName = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_branch_name);
                itemLoc = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_location);
                itemQty = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_quantity);

                branchName.setText(pair.second.branch_name);
                SBranchItem branchItem = pair.first;
                itemQty.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
                if (branchItem.item_location != null && !branchItem.item_location.isEmpty()) {
                    itemLoc.setText(branchItem.item_location);
                    itemLoc.setVisibility(View.VISIBLE);
                } else {
                    itemLoc.setVisibility(View.GONE);
                }

                return convertView;
            }
        }
    }
}
