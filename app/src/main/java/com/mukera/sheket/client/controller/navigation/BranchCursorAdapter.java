package com.mukera.sheket.client.controller.navigation;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SBranch;

/**
 * Created by gamma on 3/27/16.
 */
public class BranchCursorAdapter extends CursorAdapter {

    public BranchCursorAdapter(Context context) {
        super(context, null);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(
                R.layout.list_item_navigation, parent, false);
        NavigationViewHolder holder = new NavigationViewHolder(view);
        view.setTag(holder);

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        NavigationViewHolder holder = (NavigationViewHolder) view.getTag();
        SBranch branch = new SBranch(cursor);
        holder.elementName.setText(branch.branch_name);
    }
}
