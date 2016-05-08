package com.mukera.sheket.client.controller.user;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.NumberFormatter;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by gamma on 4/3/16.
 */
public class ProfileFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView name = (TextView) rootView.findViewById(R.id.profile_text_view_name);
        TextView id = (TextView) rootView.findViewById(R.id.profile_text_view_id);

        name.setText(PrefUtil.getUsername(getContext()));
        id.setText(String.valueOf(PrefUtil.getUserId(getContext())));

        return rootView;
    }

}
