package com.mukera.sheket.client.controller.items.item_searcher.adapters;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public class ItemSearchCursorAdapter extends CursorAdapter {
    private static class ViewHolder {
        TextView item_name;
        TextView item_code;

        public ViewHolder(View view) {
            item_name = (TextView) view.findViewById(R.id.list_item_text_view_item_name);
            item_code = (TextView) view.findViewById(R.id.list_item_text_view_item_code);
        }
    }

    public ItemSearchCursorAdapter(Context context) {
        super(context, null);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_search_item, parent, false);
        ViewHolder holder = new ViewHolder(view);

        view.setTag(holder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder holder = (ViewHolder) view.getTag();
        SItem item = new SItem(cursor);

        holder.item_name.setText(item.name);
        String code;
        if (item.has_bar_code) {
            code = item.bar_code;
        } else {
            code = item.manual_code;
        }
        holder.item_code.setText(code);
    }
}
