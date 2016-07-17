package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentProviderOperation;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.data.SheketContract.CategoryEntry;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by gamma on 3/4/16.
 */
public class AllItemsFragment extends SearchableItemFragment {
    private ListView mItemList;
    private ItemDetailAdapter mItemDetailAdapter;
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

                restartLoader();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void updateEditingUI() {
        ((CategorySelectionEditionAdapter)getCategoryAdapter()).setEditMode(mIsEditMode);
        mViewSelectAll.setVisibility(mIsEditMode ? View.VISIBLE : View.GONE);
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

    void displayOptionToEditItem(final SItemDetail itemDetail) {
        final Activity activity = getActivity();
        new AlertDialog.Builder(activity).
                setTitle("Edit Item").
                setMessage("Do you want to edit item attributes?").
                setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(ItemCreateEditActivity.createIntent(getActivity(), true, itemDetail.item));
                    }
                }).setNegativeButton("No", null).show();
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
                                // update the UI, we aren't using {@code restartLoader} b/c that
                                // queries the provider again. We shouldn't query again b/c it
                                // is only a UI update.
                                updateEditingUI();
                                initLoader();
                            }
                        });
                    }
                }).start();
            }
        });

        mDeleteBtn = (FloatingActionButton) rootView.findViewById(R.id.all_items_float_btn_delete);

        mViewSelectAll = rootView.findViewById(R.id.all_items_select_all_layout);
        mViewSelectAll.setVisibility(View.GONE);
        mCheckBoxSelectAll = (CheckBox) rootView.findViewById(R.id.all_items_select_all_check_box);
        mCheckBoxSelectAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

            }
        });

        mItemList = (ListView) rootView.findViewById(R.id.all_items_list_view_items);
        mItemDetailAdapter = new ItemDetailAdapter(getActivity());
        mItemDetailAdapter.setListener(new ItemDetailAdapter.ItemDetailSelectionListener() {
            @Override
            public void itemInfoSelected(SItemDetail itemDetail) {
                displayOptionToEditItem(itemDetail);
            }
        });
        mItemList.setAdapter(mItemDetailAdapter);
        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                FragmentManager fm = getActivity().getSupportFragmentManager();

                SItemDetail itemDetail = mItemDetailAdapter.getItem(position);
                ItemDetailDialog dialog = new ItemDetailDialog();
                dialog.mItemDetail = itemDetail;
                dialog.show(fm, "Detail");
            }
        });

        return rootView;
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
        } catch (RemoteException | OperationApplicationException e) {

        }
    }

    void displayAddOptionDialog() {
        View options_view = getActivity().getLayoutInflater().
                inflate(R.layout.all_items_floating_btn_add_options, null);
        final ImageButton option_item = (ImageButton) options_view.findViewById(R.id.all_items_option_add_item);
        final ImageButton option_category = (ImageButton) options_view.findViewById(R.id.all_items_option_add_category);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setTitle("What are you adding?").
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
            }
        });
        dialog.show();
    }

    @Override
    protected CategoryAdapter getCategoryAdapter() {
        if (mCategoryAdapter == null) {
            CategorySelectionEditionAdapter adapter =
                    new CategorySelectionEditionAdapter(getActivity());
            adapter.setListener(new CategorySelectionEditionAdapter.SelectionEditionListener() {
                @Override
                public boolean isCategorySelected(SCategory category) {
                    return mSelectedCategories.containsKey(category.category_id);
                }

                @Override
                public void categorySelected(SCategory category, boolean state) {
                    if (state == true) {
                        mSelectedCategories.put(category.category_id, category);
                    } else {
                        mSelectedCategories.remove(category.category_id);
                    }
                }

                @Override
                public void editCategorySelected(SCategory category) {

                }
            });
            mCategoryAdapter = adapter;
        }
        return mCategoryAdapter;
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
    protected Loader<Cursor> onCategoryTreeCreateLoader(int id, Bundle args) {
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
    protected boolean showCategoryNavigation() {
        /**
         * If we are searching, we don't what the categories to be displayed!!!
         */
        return !super.isSearching();
    }

    @Override
    protected void onCategoryTreeLoaderFinished(Loader<Cursor> loader, Cursor data) {
        mItemDetailAdapter.setItemCursor(data);
        ListUtils.setDynamicHeight(mItemList);
        mViewSelectAll.setVisibility(mIsEditMode ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onCategoryTreeLoaderReset(Loader<Cursor> loader) {
        mItemDetailAdapter.setItemCursor(null);
        ListUtils.setDynamicHeight(mItemList);
        mViewSelectAll.setVisibility(mIsEditMode ? View.VISIBLE : View.GONE);
    }

    public static class SItemDetail {
        public SItem item;
        public double total_quantity;
        public List<Pair<SBranch, SBranchItem>> available_branches;
    }

    public static class ItemDetailAdapter extends ArrayAdapter<SItemDetail> {
        public ItemDetailAdapter(Context context) {
            super(context, 0);
        }

        public interface ItemDetailSelectionListener {
            void itemInfoSelected(SItemDetail itemDetail);
        }

        private ItemDetailSelectionListener mListener;
        public void setListener(ItemDetailSelectionListener listener) {
            mListener = listener;
        }

        /**
         * This assumes the items are in ascending order by item id,
         * that is how is differentiates where one item ends and another starts.
         */
        public void setItemCursor(Cursor cursor) {
            setNotifyOnChange(false);

            clear();
            if (cursor != null && cursor.moveToFirst()) {
                long prev_item_id = -1;
                SItemDetail detail = null;
                do {
                    long item_id = cursor.getLong(SItem.COL_ITEM_ID);
                    if (item_id != prev_item_id) {
                        if (prev_item_id != -1) {
                            // add it to the ArrayAdapter
                            super.add(detail);
                        }
                        prev_item_id = item_id;
                        detail = new SItemDetail();
                        detail.item = new SItem(cursor);
                        detail.total_quantity = 0;
                        detail.available_branches = new ArrayList<>();
                    }
                    SBranchItem branchItem = new SBranchItem(cursor, SItem.COL_LAST);
                    branchItem.item = detail.item;      // they all refer to this item
                    SBranch branch = new SBranch(cursor, SItem.COL_LAST + SBranchItem.COL_LAST);

                    if (branchItem.branch_id != 0 && branch.branch_id != 0) {
                        detail.available_branches.add(new Pair<>(branch, branchItem));
                    }
                    detail.total_quantity += branchItem.quantity;
                } while (cursor.moveToNext());
                if (detail != null) {       // add the last item
                    super.add(detail);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final SItemDetail detail = getItem(position);

            ItemViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_all_items, parent, false);
                holder = new ItemViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ItemViewHolder) convertView.getTag();
            }

            holder.item_name.setText(detail.item.name);
            boolean has_code = detail.item.has_bar_code || !detail.item.item_code.isEmpty();
            if (has_code) {
                holder.item_code.setVisibility(View.VISIBLE);
                holder.item_code.setText(
                        detail.item.has_bar_code ? detail.item.bar_code : detail.item.item_code);
            } else {
                holder.item_code.setVisibility(View.GONE);
            }
            holder.total_qty.setText(Utils.formatDoubleForDisplay(detail.total_quantity));
            holder.item_info.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.itemInfoSelected(detail);
                }
            });

            return convertView;
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
    }

    /**
     * Shows which branches the items exist in and the location in each.
     */
    public static class ItemDetailDialog extends DialogFragment {
        public SItemDetail mItemDetail;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            LayoutInflater inflater = getActivity().getLayoutInflater();

            View view = inflater.inflate(R.layout.dialog_all_item_detail, null);

            ListView branchesList = (ListView) view.findViewById(R.id.dialog_all_item_list_view_branches);
            DetailDialogAdapter adapter = new DetailDialogAdapter(getActivity());
            for (Pair<SBranch, SBranchItem> pair : mItemDetail.available_branches) {
                adapter.add(pair);
            }
            branchesList.setAdapter(adapter);

            TextView qty_text_view = (TextView) view.findViewById(R.id.dialog_all_item_text_view_total_quantity);
            qty_text_view.setText("Total Qty: " + Utils.formatDoubleForDisplay(mItemDetail.total_quantity));

            return builder.setTitle(mItemDetail.item.name).
                    setView(view).create();
        }

        public static class DetailDialogAdapter extends ArrayAdapter<Pair<SBranch, SBranchItem>> {
            public DetailDialogAdapter(Context context) {
                super(context, 0);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final Pair<SBranch, SBranchItem> pair = getItem(position);

                if (convertView == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    convertView = inflater.inflate(R.layout.list_item_all_item_detail_dialog, parent, false);
                }

                TextView branchName, itemLoc, itemQty;

                branchName = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_branch_name);
                itemLoc = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_location);
                itemQty = (TextView) convertView.findViewById(R.id.list_item_all_item_item_detail_text_view_quantity);

                branchName.setText(pair.first.branch_name);
                SBranchItem branchItem = pair.second;
                itemQty.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
                if (branchItem.item_location != null && !branchItem.item_location.isEmpty()) {
                    itemLoc.setText(pair.second.item_location);
                    itemLoc.setVisibility(View.VISIBLE);
                } else {
                    itemLoc.setVisibility(View.GONE);
                }

                return convertView;
            }
        }
    }

    /**
     * This adapter allows selecting categories by a {@code CheckBox} and notifies about the event.
     * It also allows editing a category by providing a UI for that and notifies of the event.
     *
     * The adapter can also be used as a normal "viewing" more where the editing options are disabled.
     */
    public static class CategorySelectionEditionAdapter extends CategoryTreeNavigationFragment.CategoryChildrenArrayAdapter {
        public interface SelectionEditionListener {
            /**
             * Checks this to set the UI to appropriate state.
             * @return true if the category needs to be selected.
             */
            boolean isCategorySelected(SCategory category);

            /**
             * This is called for both selection and de-selection.
             * Use {@code state} to decide the state.
             *
             * @param category
             * @param state    If true, it is being selected, if false it is de-selecting.
             */
            void categorySelected(SCategory category, boolean state);

            void editCategorySelected(SCategory category);
        }
        private SelectionEditionListener mListener;
        public void setListener(SelectionEditionListener listener) {
            mListener = listener;
        }

        public CategorySelectionEditionAdapter(Context context) {
            super(context);
            // NOT-EDITING is the default
            mIsEditMode = false;
        }

        private boolean mIsEditMode;
        public void setEditMode(boolean editMode) {
            mIsEditMode = editMode;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final SCategory category = getItem(position);

            final ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_all_items_edit_category, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.categoryName.setText(category.name);
            if (category.childrenCategories.isEmpty()) {
                holder.subCount.setVisibility(View.GONE);
            } else {
                holder.subCount.setVisibility(View.VISIBLE);
                holder.subCount.setText("" + category.childrenCategories.size());
            }

            if (mIsEditMode) {
                holder.selectCheck.setVisibility(View.VISIBLE);
                holder.editBtn.setVisibility(View.VISIBLE);

                // we should overwrite the previously assigned listener because we
                // are recycling them it things gets messed up when we call {@code selectCheck.setChecked}
                holder.selectCheck.setOnCheckedChangeListener(null);
                holder.selectCheck.setChecked(mListener.isCategorySelected(category));
                holder.selectCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        mListener.categorySelected(category, isChecked);
                    }
                });
                /**
                 * It is hard to click the check box, we intercept the enclosing view and simulate a
                 * click on the checkbox also. If we don't do this, then the list-view will receive the
                 * click event and that is not the desired behaviour.
                 */
                holder.selectCheckBoxEnclosingView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.selectCheck.setChecked(!holder.selectCheck.isChecked());
                    }
                });

                holder.editBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.editCategorySelected(category);
                    }
                });
            } else {
                /**
                 * We don't make the visibility gone b/c we want its place to be still
                 * held by it for the different layout to look the same in both modes.
                 */
                //
                holder.selectCheck.setVisibility(View.INVISIBLE);

                holder.editBtn.setVisibility(View.GONE);
            }
            return convertView;
        }

        private static class ViewHolder {
            TextView categoryName, subCount;
            CheckBox selectCheck;
            View selectCheckBoxEnclosingView;
            ImageView editBtn;

            public ViewHolder(View view) {
                categoryName = (TextView) view.findViewById(R.id.list_item_all_items_category_text_view_category_name);
                subCount = (TextView) view.findViewById(R.id.list_item_all_items_category_text_view_sub_count);
                selectCheck = (CheckBox) view.findViewById(R.id.list_item_all_items_category_check_box_select);
                selectCheckBoxEnclosingView = view.findViewById(R.id.list_item_all_items_category_layout_select);
                editBtn = (ImageView) view.findViewById(R.id.list_item_all_items_category_btn_edit);
            }
        }
    }
}
