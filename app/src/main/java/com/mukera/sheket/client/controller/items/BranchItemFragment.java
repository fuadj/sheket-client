package com.mukera.sheket.client.controller.items;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.Utils;
import com.mukera.sheket.client.controller.transactions.TransactionActivity;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by gamma on 3/27/16.
 */
public class BranchItemFragment extends EmbeddedCategoryFragment {
    private static final String KEY_CATEGORY_ID = "key_category_id";
    private static final String KEY_BRANCH_ID = "key_branch_id";
    private static final String KEY_SHOW_CARD_TOGGLE_MENU = "key_show_card_toggle_menu";

    private CardViewToggleListener mCardListener;

    private long mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
    private long mBranchId;
    private boolean mShowCardToggleMenu;

    private ListView mBranchItemList;
    private BranchItemCursorAdapter mBranchItemAdapter;

    public BranchItemFragment setCardViewToggleListener(CardViewToggleListener listener) {
        mCardListener = listener;
        return this;
    }

    public static BranchItemFragment newInstance(long category_id, long branch_id, boolean show_toggle_menu) {
        Bundle args = new Bundle();

        BranchItemFragment fragment = new BranchItemFragment();
        args.putLong(KEY_CATEGORY_ID, category_id);
        args.putLong(KEY_BRANCH_ID, branch_id);
        args.putBoolean(KEY_SHOW_CARD_TOGGLE_MENU, show_toggle_menu);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCategoryId = args.getLong(KEY_CATEGORY_ID);
        mBranchId = args.getLong(KEY_BRANCH_ID);
        mShowCardToggleMenu = args.getBoolean(KEY_SHOW_CARD_TOGGLE_MENU);

        setParentCategoryId(mCategoryId);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.branch_items, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem toggleCardView = menu.findItem(R.id.branch_item_menu_toggle_card_view);
        if (!mShowCardToggleMenu) {
            toggleCardView.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.branch_item_menu_toggle_card_view:
                mCardListener.onCardOptionSelected(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void startTransactionActivity(int action, long branch_id) {
        Intent intent = new Intent(getActivity(), TransactionActivity.class);
        intent.putExtra(TransactionActivity.LAUNCH_ACTION_KEY, action);
        intent.putExtra(TransactionActivity.LAUNCH_BRANCH_ID_KEY, branch_id);
        startActivity(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        FloatingActionButton buyAction, sellAction;

        buyAction = (FloatingActionButton) rootView.findViewById(R.id.float_btn_item_list_buy);
        buyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionActivity(TransactionActivity.LAUNCH_TYPE_BUY, mBranchId);
            }
        });
        sellAction = (FloatingActionButton) rootView.findViewById(R.id.float_btn_item_list_sell);
        sellAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionActivity(TransactionActivity.LAUNCH_TYPE_SELL, mBranchId);
            }
        });

        mBranchItemList = (ListView) rootView.findViewById(R.id.branch_item_list_view_items);
        mBranchItemAdapter = new BranchItemCursorAdapter(getActivity());
        final AppCompatActivity activity = (AppCompatActivity) getActivity();
        mBranchItemAdapter.setListener(new BranchItemCursorAdapter.ItemSelectionListener() {
            @Override
            public void editItemLocationSelected(final SBranchItem branchItem) {
                displayItemLocationDialog(branchItem);
            }
        });
        mBranchItemList.setAdapter(mBranchItemAdapter);

        return rootView;
    }

    void displayItemLocationDialog(final SBranchItem branchItem) {
        final EditText editText = new EditText(getActivity());

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).
                setTitle("Set Item Location").
                setMessage(branchItem.item.name).
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String location = editText.getText().toString().trim();

                        // the text didn't change, ignore it
                        if (TextUtils.equals(branchItem.item_location, location)) {
                            dialog.dismiss();
                            return;
                        }

                        setItemLocation(dialog, branchItem, location);
                    }
                }).setNeutralButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setCancelable(false);

        final AlertDialog dialog = builder.create();

        if (!TextUtils.isEmpty(branchItem.item_location))
            editText.setText(branchItem.item_location);

        dialog.show();
    }

    void setItemLocation(final DialogInterface dialog,
                         final SBranchItem branchItem,
                         final String location) {
        Thread t = new Thread() {
            @Override
            public void run() {
                ContentValues values = new ContentValues();
                values.put(BranchItemEntry.COLUMN_ITEM_LOCATION, location);

                /**
                 * If the branch item was in created state, we don't want to change it until
                 * we sync with server. If we change it to updated state, it will create problems
                 * as the server still doesn't know about it and the update will fail.
                 */
                if (branchItem.change_status != ChangeTraceable.CHANGE_STATUS_CREATED) {
                    values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_UPDATED);
                }
                getContext().getContentResolver().update(
                        BranchItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                        values,
                        String.format("%s = ? AND %s = ?",
                                BranchItemEntry.COLUMN_BRANCH_ID, BranchItemEntry.COLUMN_ITEM_ID),
                        new String[]{
                                String.valueOf(branchItem.branch_id),
                                String.valueOf(branchItem.item_id)
                        }
                );
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                    }
                });
            }
        };
        t.start();
    }

    @Override
    protected void onCategoryTreeViewToggled(boolean show_tree_view) {
        if (!show_tree_view)
            mCardListener.onCardOptionSelected(false);
    }

    @Override
    protected int getCategoryLoaderId() {
        return LoaderId.MainActivity.BRANCH_ITEM_CATEGORY_LOADER;
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_branch_item;
    }

    @Override
    public void onInitLoader() {
        getLoaderManager().initLoader(LoaderId.MainActivity.BRANCH_ITEM_LOADER, null, this);
    }

    @Override
    public void onRestartLoader() {
        getLoaderManager().restartLoader(LoaderId.MainActivity.BRANCH_ITEM_LOADER, null, this);
    }

    @Override
    public void onCategorySelected(long category_id) {
        mCategoryId = category_id;
    }

    @Override
    protected Loader<Cursor> onEmbeddedCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getContext());

        String selection = null;
        String[] selectionArgs = null;

        if (super.isShowingCategoryTree()) {
            selection = ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?";
            selectionArgs = new String[]{String.valueOf(mCategoryId)};
        }

        return new CursorLoader(getActivity(),
                BranchItemEntry.buildAllItemsInBranchUri(company_id, mBranchId),
                SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                selection,
                selectionArgs,
                ItemEntry._full(ItemEntry.COLUMN_ITEM_CODE) + " ASC"
        );
    }

    @Override
    protected void onEmbeddedLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBranchItemAdapter.swapCursor(data);
        ListUtils.setDynamicHeight(mBranchItemList);
    }

    @Override
    protected void onEmbeddedLoadReset(Loader<Cursor> loader) {
        mBranchItemAdapter.swapCursor(null);
        ListUtils.setDynamicHeight(mBranchItemList);
    }

    public static class BranchItemCursorAdapter extends android.support.v4.widget.CursorAdapter {
        public interface ItemSelectionListener {
            void editItemLocationSelected(SBranchItem branchItem);
        }

        private static class ViewHolder {
            TextView item_name;
            TextView item_code;
            TextView qty_remain;
            TextView item_loc;
            ImageButton edit_loc;

            public ViewHolder(View view) {
                item_name = (TextView) view.findViewById(R.id.list_item_text_view_b_item_name);
                item_code = (TextView) view.findViewById(R.id.list_item_text_view_b_item_code);
                qty_remain = (TextView) view.findViewById(R.id.list_item_text_view_b_item_qty);
                item_loc = (TextView) view.findViewById(R.id.list_item_text_view_b_item_loc);
                edit_loc = (ImageButton) view.findViewById(R.id.list_item_img_btn_b_edit_location);
            }
        }

        public ItemSelectionListener mListener;

        public void setListener(ItemSelectionListener listener) {
            mListener = listener;
        }

        public BranchItemCursorAdapter(Context context) {
            super(context, null);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_branch_item, parent, false);
            ViewHolder holder = new ViewHolder(view);

            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            final SBranchItem branchItem = new SBranchItem(cursor, true);
            SItem item = branchItem.item;

            holder.item_name.setText(item.name);
            String code;
            if (item.has_bar_code) {
                code = item.bar_code;
            } else {
                code = item.item_code;
            }
            holder.item_code.setText(code);
            if (branchItem.item_location == null ||
                    branchItem.item_location.isEmpty()) {
                holder.item_loc.setVisibility(View.GONE);
            } else {
                holder.item_loc.setVisibility(View.VISIBLE);
                holder.item_loc.setText(branchItem.item_location);
            }
            holder.edit_loc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.editItemLocationSelected(branchItem);
                }
            });
            holder.qty_remain.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
        }
    }
}
