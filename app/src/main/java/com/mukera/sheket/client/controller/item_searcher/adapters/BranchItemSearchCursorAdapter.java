package com.mukera.sheket.client.controller.item_searcher.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 4/1/16.
 */
public class BranchItemSearchCursorAdapter  extends CursorAdapter {
    private static class ViewHolder {
        TextView item_name;
        TextView item_code;
        TextView qty_remain;

        public ViewHolder(View view) {
            item_name = (TextView) view.findViewById(R.id.list_item_text_view_item_name);
            item_code = (TextView) view.findViewById(R.id.list_item_text_view_item_code);
            qty_remain = (TextView) view.findViewById(R.id.list_item_text_view_item_qty);
        }
    }

    public BranchItemSearchCursorAdapter(Context context) {
        super(context, null);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_search_branch_item, parent, false);
        ViewHolder holder;
        holder = new ViewHolder(view);

        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        SBranchItem branchItem = new SBranchItem(cursor, true);
        SItem item = branchItem.item;

        holder.item_name.setText(item.name);
        String code;
        if (item.has_bar_code) {
            code = item.bar_code;
        } else {
            code = item.manual_code;
        }
        holder.item_code.setText(code);
        holder.qty_remain.setText(String.format("%f", branchItem.quantity));
    }
}
