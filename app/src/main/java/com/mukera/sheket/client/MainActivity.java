package com.mukera.sheket.client;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.mukera.sheket.client.controller.items.ItemListFragment;
import com.mukera.sheket.client.controller.navigation.NavigationFragment;
import com.mukera.sheket.client.controller.signup.RegistrationActivity;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.sync.SyncUtil;

public class MainActivity extends AppCompatActivity implements NavigationFragment.BranchSelectionCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SyncUtil.isUserSet(this)) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        }

        setContentView(R.layout.activity_main);

        Toolbar tb = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(tb);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_drawer);

        final ActionBar ab = getSupportActionBar();
        //ab.setHomeAsUpIndicator(R.drawable.ic_menu); // set a custom icon for the default home button
        ab.setDisplayShowHomeEnabled(true); // show or hide the default home button
        //ab.setDisplayHomeAsUpEnabled(true);
        ab.setDisplayShowCustomEnabled(true); // enable overriding the default toolbar layout
        //ab.setDisplayShowTitleEnabled(false); // disable the default title element here (for centered title)

        NavigationFragment navigationFragment = new NavigationFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, new ItemListFragment())
                .replace(R.id.main_navigation_container, navigationFragment)
                .commit();
        DrawerLayout drawer = (DrawerLayout)findViewById(R.id.main_drawer_layout);
        navigationFragment.setUp(R.id.main_navigation_container, R.id.main_drawer_layout);
    }

    @Override
    public void onBranchSelected(SBranch branch) {

    }

    @Override
    public void onElementSelected(int item) {

    }
}
