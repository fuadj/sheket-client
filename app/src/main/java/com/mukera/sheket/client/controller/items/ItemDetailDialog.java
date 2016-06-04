package com.mukera.sheket.client.controller.items;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.util.Utils;
import com.mukera.sheket.client.data.SheketContract.BranchItemEntry;
import com.mukera.sheket.client.data.SheketContract.ChangeTraceable;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by fuad on 6/4/16.
 */
public class ItemDetailDialog extends DialogFragment {
    public ItemListFragment.SItemDetail mItemDetail;

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
        qty_text_view.setText("Total Qty: " + Utils.formatDoubleForDisplay(mItemDetail.total_quantity));

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
            itemQty.setText(Utils.formatDoubleForDisplay(branchItem.quantity));
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

