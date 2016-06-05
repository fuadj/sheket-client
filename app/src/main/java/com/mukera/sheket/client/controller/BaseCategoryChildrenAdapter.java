package com.mukera.sheket.client.controller;

import android.content.Context;
import android.database.Cursor;
import android.widget.ArrayAdapter;

import com.mukera.sheket.client.models.SCategory;

/**
 * Created by fuad on 6/5/16.
 */
public abstract class BaseCategoryChildrenAdapter extends ArrayAdapter<SCategory> {
    public BaseCategoryChildrenAdapter(Context context) {
        super(context, 0);
    }

    public void setCategoryCursor(Cursor cursor) {
        setNotifyOnChange(false);

        clear();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SCategory parent_category = new SCategory(cursor);
                super.add(parent_category);
            } while (cursor.moveToNext());
        }
        notifyDataSetChanged();
    }

}
