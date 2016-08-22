package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
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
import android.support.v4.app.Fragment;
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
import com.mukera.sheket.client.utils.SheketTextUtils;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Created by gamma on 3/4/16.
 */
public class AllItemsFragment extends SearchableItemFragment {
    private static final String SAVE_KEY_EDIT_MODE = "save_key_edit_mode";
    private static final String ARG_KEY_EDIT_MODE = "arg_key_edit_mode";
    private static final String ARG_KEY_CATEGORY_STACK = "arg_key_category_stack";

    private boolean mIsEditMode = false;

    private FloatingActionButton mPasteBtn, mAddBtn, mDeleteBtn;

    /**
     * We hold on to them for finally applying an operation on them AND ALSO
     * the UI looks at this to figure out which are selected or not.
     */
    private Map<Long, SCategory> mSelectedCategories;

    private Map<Long, SItem> mSelectedItems;

    /**
     * Creates a new fragment with the editing mode set and the category stack
     * so the fragment can start with a certain category "opened" with the option
     * to go through the backstack.
     *
     * @param is_edit_mode   Should it show Edit-Mode or Normal mode.
     * @param category_stack If you want this fragment to start with some category "opened",
     *                       pass in the stack of categories that lead to it starting from the
     *                       root category. If {@code null}, it starts at the root.
     *                       The category at the top of the stack is the one that will be opened.
     * @return
     */
    public static AllItemsFragment newInstance(boolean is_edit_mode, Stack<Long> category_stack) {
        Bundle args = new Bundle();
        args.putBoolean(ARG_KEY_EDIT_MODE, is_edit_mode);

        if (category_stack == null) {
            category_stack = new Stack<>();
        }
        if (category_stack.isEmpty()) {     // add the root category
            category_stack.push(CategoryEntry.ROOT_CATEGORY_ID);
        }

        // Because Stack<Long> can't be put inside a Bundle, convert it to ArrayList<Integer>
        ArrayList<Integer> int_stack = new ArrayList<>();
        for (Long category : category_stack) {
            int_stack.add(category.intValue());
        }

        args.putIntegerArrayList(ARG_KEY_CATEGORY_STACK, int_stack);

        AllItemsFragment fragment = new AllItemsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mIsEditMode = savedInstanceState.getBoolean(SAVE_KEY_EDIT_MODE, false);
        } else {
            Bundle args = getArguments();
            if (args != null) {
                ArrayList<Integer> int_category_stack = args.getIntegerArrayList(ARG_KEY_CATEGORY_STACK);
                if (int_category_stack != null) {
                    Stack<Long> category_stack = new Stack<>();
                    for (Integer category_id : int_category_stack) {
                        category_stack.push(category_id.longValue());
                    }
                    // the top of the stack is current category, pop it
                    super.setCategoryStack(category_stack, category_stack.pop());
                }

                // this defaults to false, so we are good
                mIsEditMode = args.getBoolean(ARG_KEY_EDIT_MODE);
            } else {
                setCurrentCategory(CategoryEntry.ROOT_CATEGORY_ID);
            }
        }

        setHasOptionsMenu(true);

        mSelectedCategories = new HashMap<>();
        mSelectedItems = new HashMap<>();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(SAVE_KEY_EDIT_MODE, mIsEditMode);
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

                restartFragmentToApplyEditingChanges();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * <p>
     * Restarts the fragment inside the current category with the set editing mode.
     * This removes the complexity of creating layouts that can support both
     * "normal" and "edit" mode UI. You just need to inflate the UI for the appropriate
     * mode by checking {@link #mIsEditMode}. Then your layouts are simpler as they
     * are designed for only a single mode.
     * </p>
     * <p/>
     * <p>
     * Without this each of your layouts should be capable of displaying in both modes,
     * which means conditionally showing and hiding views.
     * This also creates problems if Views are being recycled
     * like in a ListView where one type of view can wrongly be recycled for another type.
     * This causes the app to crash.
     * </p>
     */
    void restartFragmentToApplyEditingChanges() {
        Stack<Long> current_stack = super.getCurrentStack();
        // add the current category to the top.
        current_stack.push(getCurrentCategory());

        Fragment newFragment = newInstance(mIsEditMode, current_stack);
        getActivity().getSupportFragmentManager().beginTransaction().
                replace(R.id.main_fragment_container, newFragment).
                commit();
    }

    void setUpEditModeUI() {
        if (!mIsEditMode) {
            mAddBtn.setVisibility(View.VISIBLE);
            mPasteBtn.setVisibility(View.GONE);
            mDeleteBtn.setVisibility(View.GONE);
        } else {
            mAddBtn.setVisibility(View.GONE);
            mPasteBtn.setVisibility(View.VISIBLE);
            // TODO: make it visible after you implement delete here and on server
            mDeleteBtn.setVisibility(View.VISIBLE);

            // they both should start disabled only be enabled after some items/categories have been selected
            mPasteBtn.setEnabled(false);
            mDeleteBtn.setEnabled(false);
        }
    }

    @Override
    public void onCategorySelected(long previous_category, long selected_category) {
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
                        moveSelectedEntitiesToCurrentCategory();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mIsEditMode = false;
                                restartFragmentToApplyEditingChanges();
                            }
                        });
                    }
                }).start();
            }
        });

        mDeleteBtn = (FloatingActionButton) rootView.findViewById(R.id.all_items_float_btn_delete);
        mDeleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmCategoryDeletion();
            }
        });

        setUpEditModeUI();

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
        TextView item_name;
        TextView item_code;
        TextView total_qty;

        ImageView item_info;
        // this makes it easier to click on by simulating clicks here
        View code_layout;

        public ItemViewHolder(View view) {
            item_info = (ImageView) view.findViewById(R.id.list_item_item_detail_info);
            code_layout = view.findViewById(R.id.list_item_item_detail_layout_code);

            item_name = (TextView) view.findViewById(R.id.list_item_item_detail_name);
            item_code = (TextView) view.findViewById(R.id.list_item_item_detail_code);
            total_qty = (TextView) view.findViewById(R.id.list_item_item_detail_total_qty);
        }
    }

    private static class EditItemViewHolder {
        TextView item_name, item_code, total_qty;

        CheckBox select_item;
        View layout_select;

        ImageButton delete_item_btn;
        View layout_delete_item;

        public EditItemViewHolder(View view) {
            item_name = (TextView) view.findViewById(R.id.list_item_edit_item_text_view_item_name);
            item_code = (TextView) view.findViewById(R.id.list_item_edit_item_text_view_item_code);
            total_qty = (TextView) view.findViewById(R.id.list_item_edit_item_text_view_total_qty);

            select_item = (CheckBox) view.findViewById(R.id.list_item_edit_item_check_box_select);
            layout_select = view.findViewById(R.id.list_item_edit_item_layout_select);

            delete_item_btn = (ImageButton) view.findViewById(R.id.list_item_edit_item_img_btn_delete_new_item);
            layout_delete_item = view.findViewById(R.id.list_item_edit_item_layout_delete_new_item);
        }
    }

    @Override
    public View newItemView(Context context, ViewGroup parent, Cursor cursor, int position) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view;
        if (mIsEditMode) {
            view = inflater.inflate(R.layout.list_item_all_items_edit_item, parent, false);
            EditItemViewHolder holder = new EditItemViewHolder(view);
            view.setTag(holder);
        } else {
            view = inflater.inflate(R.layout.list_item_all_items, parent, false);
            ItemViewHolder holder = new ItemViewHolder(view);
            view.setTag(holder);
        }
        return view;
    }

    @Override
    public void bindItemView(Context context, Cursor cursor, View view, int position) {
        //cursor.moveToPosition(position);
        // we also want to fetch the branches it exists in, that is the true argument
        final SItem item = new SItem(cursor, true);

        if (mIsEditMode) {
            final EditItemViewHolder holder = (EditItemViewHolder) view.getTag();

            holder.item_code.setVisibility(item.item_code.isEmpty() ? View.GONE : View.VISIBLE);

            if (isSearching()) {
                SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_name, item.name, getSearchText());
                SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_code, item.item_code, getSearchText());
            } else {
                holder.item_name.setText(item.name);
                holder.item_code.setText(item.item_code);
            }

            holder.total_qty.setText(Utils.formatDoubleForDisplay(item.total_quantity));

            /**
             * Because the views are recycled, we don't want it to tell the previous listener
             */
            holder.select_item.setOnCheckedChangeListener(null);
            holder.layout_select.setOnClickListener(null);
            holder.delete_item_btn.setOnClickListener(null);
            holder.layout_delete_item.setOnClickListener(null);

            holder.select_item.setChecked(mSelectedItems.containsKey(item.item_id));
            holder.select_item.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mSelectedItems.put(item.item_id, item);
                    } else {
                        mSelectedItems.remove(item.item_id);
                    }
                    boolean enable_paste_delete = !mSelectedItems.isEmpty() || !mSelectedCategories.isEmpty();
                    mPasteBtn.setEnabled(enable_paste_delete);
                    mDeleteBtn.setEnabled(enable_paste_delete);
                }
            });
            holder.layout_select.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // simulate a click
                    holder.select_item.setChecked(!holder.select_item.isChecked());
                }
            });
            boolean is_newly_created = item.change_status == ChangeTraceable.CHANGE_STATUS_CREATED;
            int show_if_newly_created = is_newly_created ? View.VISIBLE : View.GONE;

            holder.delete_item_btn.setVisibility(show_if_newly_created);
            holder.layout_delete_item.setVisibility(show_if_newly_created);

            if (is_newly_created) {
                View.OnClickListener deleteListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        displayItemDeleteConfirmationDialog(item);
                        // TODO: display confirm to delete, then delete it
                    }
                };
                holder.delete_item_btn.setOnClickListener(deleteListener);
                holder.layout_delete_item.setOnClickListener(deleteListener);
            }
        } else {
            ItemViewHolder holder = (ItemViewHolder) view.getTag();

            holder.item_code.setVisibility(item.item_code.isEmpty() ? View.GONE : View.VISIBLE);

            if (isSearching()) {
                SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_name, item.name, getSearchText());
                SheketTextUtils.showMatchedTextAsBoldItalic(holder.item_code, item.item_code, getSearchText());
            } else {
                holder.item_name.setText(item.name);
                holder.item_code.setText(item.item_code);
            }

            holder.total_qty.setText(Utils.formatDoubleForDisplay(item.total_quantity));
            View.OnClickListener listener = new View.OnClickListener() {
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
                                            dialog.dismiss();
                                            ItemCreateEditDialog.newInstance(getCurrentCategory(), item).
                                                    show(getFragmentManager(), null);
                                        }
                                    }).
                            setNegativeButton("No", null).
                            show();
                }
            };
            holder.item_info.setOnClickListener(listener);
            holder.code_layout.setOnClickListener(listener);
        }
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
            holder.subCount.setText(String.format(Locale.US, "%d", category.childrenCategories.size()));
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
                boolean enable_paste_delete = !mSelectedItems.isEmpty() || !mSelectedCategories.isEmpty();
                mPasteBtn.setEnabled(enable_paste_delete);
                mDeleteBtn.setEnabled(enable_paste_delete);
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

    void displayItemDeleteConfirmationDialog(final SItem item) {
        new AlertDialog.Builder(getActivity()).
                setTitle(R.string.dialog_item_delete_title).
                setMessage(R.string.dialog_item_delete_body).
                setPositiveButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).
                // we've made it at this end to remove accidental deletion
                setNeutralButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        final ProgressDialog deleteProgress = ProgressDialog.show(getActivity(), "Deleting", "Please Wait...", true);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getActivity().getContentResolver().
                                        delete(ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getActivity())),
                                                ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                                                        new String[]{String.valueOf(item.item_id)});
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        deleteProgress.dismiss();
                                    }
                                });
                            }
                        }).start();
                    }
                }).show();
    }

    /**
     * Moves the selected stuff to the current category
     */
    void moveSelectedEntitiesToCurrentCategory() {
        moveCategoriesToCurrentCategory();
        moveItemsToCurrentCategory();

        /**
         * This is performed after every thing has moved and "settled". This is an easier
         * strategy than seeing which is moving and "tracking" it.
         */
        CategoryUtil.updateBranchCategoriesForAllBranches(getActivity());
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
                    withSelection(
                            CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " = ?",
                            new String[]{Long.toString(category.category_id)}
                    ).build());
        }

        try {
            getActivity().getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operation);
        } catch (RemoteException | OperationApplicationException e) {

        }
    }

    void moveItemsToCurrentCategory() {
        if (mSelectedItems.isEmpty()) return;

        long company_id = PrefUtil.getCurrentCompanyId(getActivity());
        long current_category = super.getCurrentCategory();

        ArrayList<ContentProviderOperation> operation = new ArrayList<>();
        for (SItem item : mSelectedItems.values()) {
            item.category = current_category;

            /**
             * If it is only created(not yet synced), you can't set it to updated b/c
             * it hasn't yet been "created" at the server side. So trying to update it
             * will create problems.
             */
            if (item.change_status != SheketContract.ChangeTraceable.CHANGE_STATUS_CREATED)
                item.change_status = SheketContract.ChangeTraceable.CHANGE_STATUS_UPDATED;

            operation.add(ContentProviderOperation.newUpdate(
                    ItemEntry.buildBaseUri(company_id)).
                    withValues(item.toContentValues()).
                    withSelection(
                            ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                            new String[]{Long.toString(item.item_id)}
                    ).build());
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
                setView(options_view);
        final AlertDialog dialog = builder.create();
        option_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();

                ItemCreateEditDialog.newInstance(getCurrentCategory(),
                        // we are passing a null item as we are not editing, but creating
                        null
                ).show(getFragmentManager(), null);
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
     * creates or updates a category on a worker-thread and finally dismisses the dialog on the main thread.
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
                    // TODO: check why removing this is necessary
                    // otherwise the categories children will have the default(root) category assigned
                    values.remove(CategoryEntry.COLUMN_CATEGORY_ID);

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
                    }
                });
            }
        }).start();
    }

    /**
     * Displays a confirmation dialog before deleting the selected categories.
     */
    void confirmCategoryDeletion() {
        // we are converting it to a list because we want to preserve index. THis is because
        // we want to only delete categories that have been confirmed in the multi-choice dialog
        final ArrayList<SCategory> categoryList = new ArrayList<>();
        for (Long category_id : mSelectedCategories.keySet()) {
            categoryList.add(mSelectedCategories.get(category_id));
        }

        String[] nameArray = new String[categoryList.size()];
        for (int i = 0; i < nameArray.length; i++) {
            nameArray[i] = categoryList.get(i).name;
        }

        final boolean[] confirmed_names = new boolean[nameArray.length];
        // we start off with every category approved for deletion, the user can
        // then remove individual categories OR dismiss all.
        for (int i = 0; i < confirmed_names.length; i++) {
            confirmed_names[i] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setMultiChoiceItems(nameArray, confirmed_names,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                confirmed_names[which] = isChecked;
                                boolean at_least_one_confirmed = false;
                                for (Boolean confirm : confirmed_names) {
                                    if (confirm) {
                                        at_least_one_confirmed = true;
                                        break;
                                    }
                                }

                                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).
                                        setEnabled(at_least_one_confirmed);
                            }
                        }).setCancelable(false).
                setTitle("Categories To Delete, Please Confirm").
                setPositiveButton("Delete",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final List<SCategory> confirmedList = new ArrayList<>();
                                for (int i = 0; i < confirmed_names.length; i++) {
                                    // the user has "de-selected" it, so ignore it
                                    if (!confirmed_names[i]) continue;

                                    confirmedList.add(categoryList.get(i));
                                }
                                dialog.dismiss();

                                final ProgressDialog deleteProgress = ProgressDialog.show(getActivity(),
                                        "Deleting categories", "Please wait...", true);

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CategoryUtil.deleteCategoryList(getActivity(), confirmedList);
                                        getActivity().runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                deleteProgress.dismiss();
                                                mIsEditMode = false;
                                                restartFragmentToApplyEditingChanges();
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }).
                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    protected boolean onSearchTextChanged(String newText) {
        restartLoaders();
        return true;
    }

    @Override
    protected boolean onSearchTextViewClosed() {
        restartLoaders();
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
            selectionArgs = new String[]{String.valueOf(mCurrentCategoryId)};
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
        if (!data.isClosed())
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

                // we show the item if it exists in the branch, otherwise skip that branch
                if (pair.first != null)
                    adapter.add(pair);
            }
            branchesList.setAdapter(adapter);

            TextView qty_text_view = (TextView) view.findViewById(R.id.dialog_all_item_text_view_total_quantity);
            qty_text_view.setText(
                    getString(R.string.placeholder_all_items_total_quantity, Utils.formatDoubleForDisplay(mItem.total_quantity)));

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
