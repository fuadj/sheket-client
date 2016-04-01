package com.mukera.sheket.client.controller.navigation;

import android.view.View;
import android.widget.TextView;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 3/27/16.
 */
public class NavigationViewHolder {
    TextView elementName;

    public NavigationViewHolder(View view) {
        elementName = (TextView) view.findViewById(R.id.text_view_list_item_branch_name);
        view.setTag(this);
    }
}

