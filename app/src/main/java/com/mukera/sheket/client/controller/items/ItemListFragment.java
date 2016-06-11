package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

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
import java.util.List;

/**
 * Created by gamma on 3/4/16.
 */
public class ItemListFragment extends EmbeddedCategoryFragment {
    private static final String KEY_CATEGORY_ID = "key_category_id";
    private static final String KEY_SHOW_CARD_TOGGLE_MENU = "key_show_card_toggle_menu";

    private CardViewToggleListener mCardListener;

    private ListView mItemList;
    private ItemDetailAdapter mItemDetailAdapter;

    private long mCategoryId = CategoryEntry.ROOT_CATEGORY_ID;
    private boolean mShowCardToggleMenu;

    public void setCardListener(CardViewToggleListener listener) {
        mCardListener = listener;
    }

    public static ItemListFragment newInstance(long category_id, boolean show_toggle_menu) {
        Bundle args = new Bundle();

        ItemListFragment fragment = new ItemListFragment();
        args.putLong(KEY_CATEGORY_ID, category_id);
        args.putBoolean(KEY_SHOW_CARD_TOGGLE_MENU, show_toggle_menu);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCategoryId = args.getLong(KEY_CATEGORY_ID);
        mShowCardToggleMenu = args.getBoolean(KEY_SHOW_CARD_TOGGLE_MENU);
        setHasOptionsMenu(true);
        setParentCategoryId(mCategoryId);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.all_items, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem toggleCardView = menu.findItem(R.id.all_items_menu_toggle_card_view);
        if (!mShowCardToggleMenu) {
            toggleCardView.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.all_items_menu_add_item:
                Intent intent = new Intent(getActivity(), NewItemActivity.class);
                startActivity(intent);
                return true;
            case R.id.all_items_menu_toggle_card_view:
                mCardListener.onCardOptionSelected(true);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCategorySelected(long category_id) {
        mCategoryId = category_id;
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
        return R.layout.fragment_item_list;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mItemList = (ListView) rootView.findViewById(R.id.item_list_list_view_items);
        mItemDetailAdapter = new ItemDetailAdapter(getActivity());
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

    @Override
    protected Loader<Cursor> onEmbeddedCreateLoader(int id, Bundle args) {
        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        String sortOrder = ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " ASC";

        String selection = null;
        String[] selectionArgs = null;

        if (super.isShowingCategoryTree()) {
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
    protected void onEmbeddedLoadFinished(Loader<Cursor> loader, Cursor data) {
        mItemDetailAdapter.setItemCursor(data);
        ListUtils.setDynamicHeight(mItemList);
    }

    @Override
    protected void onEmbeddedLoadReset(Loader<Cursor> loader) {
        mItemDetailAdapter.setItemCursor(null);
        ListUtils.setDynamicHeight(mItemList);
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
            SItemDetail detail = getItem(position);

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

            return convertView;
        }

        private static class ItemViewHolder {
            TextView item_name;
            TextView item_code;
            TextView total_qty;

            public ItemViewHolder(View view) {
                item_name = (TextView) view.findViewById(R.id.list_item_item_detail_name);
                item_code = (TextView) view.findViewById(R.id.list_item_item_detail_code);
                total_qty = (TextView) view.findViewById(R.id.list_item_item_detail_total_qty);
            }
        }
    }
}
