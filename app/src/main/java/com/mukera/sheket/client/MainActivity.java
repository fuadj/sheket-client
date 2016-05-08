package com.mukera.sheket.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;

import com.mukera.sheket.client.controller.admin.MembersFragment;
import com.mukera.sheket.client.controller.admin.TransactionHistoryFragment;
import com.mukera.sheket.client.controller.items.BranchItemFragment;
import com.mukera.sheket.client.controller.items.ItemListFragment;
import com.mukera.sheket.client.controller.navigation.NavigationFragment;
import com.mukera.sheket.client.controller.admin.BranchFragment;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.admin.SettingsActivity;
import com.mukera.sheket.client.controller.user.ProfileFragment;
import com.mukera.sheket.client.controller.user.RegistrationActivity;
import com.mukera.sheket.client.data.AndroidDatabaseManager;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.sync.SheketService;
import com.mukera.sheket.client.utility.PrefUtil;


public class MainActivity extends AppCompatActivity implements
        NavigationFragment.BranchSelectionCallback, SPermission.PermissionChangeListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireLogin();

        //if (savedInstanceState == null) {
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
        //}

        // This is out of the if stmt because we want it to run every-time
        // Any settings changes should be reflected
        NavigationFragment navigationFragment = new NavigationFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_navigation_container, navigationFragment)
                .commit();
        navigationFragment.setUp(R.id.main_navigation_container, R.id.main_drawer_layout);

        hideToolBarAddBtn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            recreate();
        }
    }

    void requireLogin() {
        if (!PrefUtil.isUserSet(this)) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onBranchSelected(SBranch branch) {
        replaceMainFragment(BranchItemFragment.newInstance(branch.branch_id));
    }

    void replaceMainFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    void hideToolBarAddBtn() {
        View v_toolbar = this.findViewById(R.id.toolbar);
        if (v_toolbar != null) {
            Toolbar toolbar = (Toolbar) v_toolbar;
            ImageButton addBtn = (ImageButton) toolbar.findViewById(R.id.toolbar_btn_add);
            addBtn.setVisibility(View.GONE);
        }
    }

    @Override
    public void onElementSelected(int item) {
        hideToolBarAddBtn();

        switch (item) {
            case NavigationFragment.StaticNavigationAdapter.ENTITY_ALL_ITEMS:
                replaceMainFragment(new ItemListFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_BRANCHES:
                replaceMainFragment(new BranchFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_COMPANIES:
                replaceMainFragment(new CompanyFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_HISTORY:
                replaceMainFragment(new TransactionHistoryFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_MEMBERS:
                replaceMainFragment(new MembersFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_SYNC:
                Intent intent = new Intent(this, SheketService.class);
                startService(intent);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_DEBUG:
                startActivity(new Intent(this, AndroidDatabaseManager.class));
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_USER_PROFILE:
                replaceMainFragment(new ProfileFragment());
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_LOG_OUT:
                PrefUtil.logoutUser(this);
                requireLogin();
                break;
        }
    }

    @Override
    public void userPermissionChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            recreate();
        }
    }
}
