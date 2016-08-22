package com.mukera.sheket.client.controller.user;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.MainActivity;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.navigation.BaseNavigation;
import com.mukera.sheket.client.data.AndroidDatabaseManager;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 7/30/16.
 */
public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        ListView listSettings = (ListView) rootView.findViewById(R.id.setting_list_view_main);
        final SettingsAdapter adapter = new SettingsAdapter(getContext());
        listSettings.setAdapter(adapter);
        adapter.add(BaseNavigation.StaticNavigationOptions.OPTION_COMPANIES);
        adapter.add(BaseNavigation.StaticNavigationOptions.OPTION_USER_PROFILE);
        adapter.add(BaseNavigation.StaticNavigationOptions.OPTION_LANGUAGES);
        //adapter.add(BaseNavigation.StaticNavigationOptions.OPTION_DEBUG);

        listSettings.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Integer item = adapter.getItem(position);
                Fragment fragment = null;
                switch (item) {
                    case BaseNavigation.StaticNavigationOptions.OPTION_COMPANIES:
                        fragment = new CompanyFragment();
                        break;
                    case BaseNavigation.StaticNavigationOptions.OPTION_USER_PROFILE:
                        fragment = new ProfileFragment();
                        break;
                    case BaseNavigation.StaticNavigationOptions.OPTION_DEBUG:
                        startActivity(new Intent(getContext(), AndroidDatabaseManager.class));
                        break;
                    case BaseNavigation.StaticNavigationOptions.OPTION_LANGUAGES:
                        displayConfigurationDialog(getActivity(), true);
                        break;
                }
                if (fragment != null) {
                    getActivity().getSupportFragmentManager().beginTransaction().
                            replace(R.id.main_fragment_container, fragment).
                            addToBackStack(null).
                            commit();
                }
            }
        });
        ListUtils.setDynamicHeight(listSettings);

        View logoutView = rootView.findViewById(R.id.settings_log_out);
        logoutView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity activity = (MainActivity)getActivity();
                activity.onNavigationOptionSelected(BaseNavigation.StaticNavigationOptions.OPTION_LOG_OUT);
            }
        });

        getActivity().setTitle("Settings");

        return rootView;
    }

    static class SettingsAdapter extends ArrayAdapter<Integer> {
        public SettingsAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Integer item = getItem(position);

            SettingViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_settings, parent, false);
                holder = new SettingViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (SettingViewHolder) convertView.getTag();
            }

            holder.name.setText(BaseNavigation.StaticNavigationOptions.sEntityAndIcon.get(item).first);
            holder.icon.setImageResource(BaseNavigation.StaticNavigationOptions.sEntityAndIcon.get(item).second);

            return convertView;
        }

        static class SettingViewHolder {
            TextView name;
            ImageView icon;

            public SettingViewHolder(View view) {
                name = (TextView) view.findViewById(R.id.list_item_settings_text);
                icon = (ImageView) view.findViewById(R.id.list_item_settings_icon);
                view.setTag(this);
            }
        }
    }

    public static void displayConfigurationDialog(final Context context, boolean is_cancellable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_first_time_configuration, null);
        builder.setView(view);
        final Button btnEnglish = (Button) view.findViewById(R.id.dialog_config_btn_english);
        final Button btnAmharic = (Button) view.findViewById(R.id.dialog_config_btn_amharic);

        builder.setTitle("Choose Language");
        builder.setCancelable(is_cancellable);
        final AlertDialog dialog = builder.create();
        if (!is_cancellable)
            dialog.setCanceledOnTouchOutside(false);
        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selected_lang = -1;
                if (v.getId() == btnEnglish.getId()) {
                    selected_lang = PrefUtil.LANGUAGE_ENGLISH;
                } else if (v.getId() == btnAmharic.getId()) {
                    selected_lang = PrefUtil.LANGUAGE_AMHARIC;
                }

                dialog.dismiss();
                if (selected_lang == -1 ||
                        selected_lang == PrefUtil.getUserLanguageId(context)) {
                    return;
                }

                PrefUtil.setUserLanguage(context, selected_lang);
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(SheketBroadcast.ACTION_CONFIG_CHANGE));
            }
        };

        btnEnglish.setOnClickListener(listener);
        btnAmharic.setOnClickListener(listener);
        dialog.show();
    }
}
