package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.widget.*;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.util.NumberFormatter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utility.PrefUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/4/16.
 */
public class ItemListFragment extends Fragment implements LoaderCallbacks<Cursor> {
    private static final String KEY_CATEGORY_ID = "key_category_id";

    private ListView mItemList;
    private ItemDetailAdapter mItemDetailAdapter;

    private long mCategoryId;

    public static ItemListFragment newInstance(long cateogry_id) {
        Bundle args = new Bundle();

        ItemListFragment fragment = new ItemListFragment();
        args.putLong(KEY_CATEGORY_ID, cateogry_id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCategoryId = args.getLong(KEY_CATEGORY_ID);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.item_list_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_list_menu_add_item:
                Intent intent = new Intent(getActivity(), NewItemActivity.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_list, container, false);

        mItemList = (ListView) rootView.findViewById(R.id.all_item_list_view);
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

    void resetAdapter() {
        getLoaderManager().initLoader(LoaderId.MainActivity.ALL_ITEM_LOADER, null, this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.MainActivity.ALL_ITEM_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        String sortOrder = ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " ASC";

        return new CursorLoader(getActivity(),
                ItemEntry.buildBaseUriWithBranches(PrefUtil.getCurrentCompanyId(getContext())),
                SItem.ITEM_WITH_BRANCH_DETAIL_COLUMNS,
                ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?",
                new String[]{String.valueOf(mCategoryId)},
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mItemDetailAdapter.setItemCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mItemDetailAdapter.setItemCursor(null);
    }

    static class SItemDetail {
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

        private static final int[] colors = new int[] {
                0xffffffff,
                0xfff2f7ff
        };

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
            boolean has_code = detail.item.has_bar_code || !detail.item.manual_code.isEmpty();
            if (has_code) {
                holder.item_code.setVisibility(View.VISIBLE);
                holder.item_code.setText(
                        detail.item.has_bar_code ? detail.item.bar_code : detail.item.manual_code);
            } else {
                holder.item_code.setVisibility(View.GONE);
            }
            holder.total_qty.setText(NumberFormatter.formatDoubleForDisplay(detail.total_quantity));

            convertView.setBackgroundColor(colors[position%2]);

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
            adapter.setListener(new DetailDialogAdapter.BranchItemSelectionListener() {
                @Override
                public void editItemLocationSelected(final SBranchItem branchItem) {
                    FragmentManager fm = getActivity().getSupportFragmentManager();

                    final ItemLocationDialog dialog = new ItemLocationDialog();
                    final Activity activity = getActivity();
                    dialog.setBranchItem(branchItem);
                    dialog.setListener(new ItemLocationDialog.ItemLocationListener() {
                        @Override
                        public void cancelSelected() {
                            dialog.dismiss();
                        }

                        @Override
                        public void locationSelected(final String location) {
                            // the text didn't change, ignore it
                            if (TextUtils.equals(branchItem.item_location, location))
                                return;

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
                                    activity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
                                        }
                                    });
                                }
                            };
                            t.start();
                        }
                    });
                    dialog.show(fm, "Set Location");
                }
            });
            for (Pair<SBranch, SBranchItem> pair : mItemDetail.available_branches) {
                adapter.add(pair);
            }
            branchesList.setAdapter(adapter);

            TextView qty_text_view = (TextView) view.findViewById(R.id.dialog_all_item_text_view_total_quantity);
            qty_text_view.setText("Total Qty: " + NumberFormatter.formatDoubleForDisplay(mItemDetail.total_quantity));

            return builder.setTitle(mItemDetail.item.name).
                    setView(view).create();
        }

        public static class DetailDialogAdapter extends ArrayAdapter<Pair<SBranch, SBranchItem>> {
            interface BranchItemSelectionListener {
                void editItemLocationSelected(SBranchItem branchItem);
            }

            public BranchItemSelectionListener mListener;

            public void setListener(BranchItemSelectionListener listener) {
                mListener = listener;
            }

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
                itemQty.setText(NumberFormatter.formatDoubleForDisplay(branchItem.quantity));
                if (branchItem.item_location != null && !branchItem.item_location.isEmpty()) {
                    itemLoc.setText(pair.second.item_location);
                    itemLoc.setVisibility(View.VISIBLE);
                } else {
                    itemLoc.setVisibility(View.GONE);
                }

                ImageButton imgLocation = (ImageButton) convertView.findViewById(R.id.list_item_img_btn_all_item_detail_dialog);
                imgLocation.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.editItemLocationSelected(pair.second);
                    }
                });

                return convertView;
            }
        }
    }

}
