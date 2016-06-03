package com.mukera.sheket.client;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.mukera.sheket.client.controller.admin.MembersFragment;
import com.mukera.sheket.client.controller.admin.TransactionHistoryFragment;
import com.mukera.sheket.client.controller.items.BranchItemFragment;
import com.mukera.sheket.client.controller.items.CategoryViewFragment;
import com.mukera.sheket.client.controller.items.ItemListFragment;
import com.mukera.sheket.client.controller.navigation.NavigationFragment;
import com.mukera.sheket.client.controller.admin.BranchFragment;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.admin.SettingsActivity;
import com.mukera.sheket.client.controller.user.ProfileFragment;
import com.mukera.sheket.client.controller.user.RegistrationActivity;
import com.mukera.sheket.client.data.AndroidDatabaseManager;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.sync.SheketService;
import com.mukera.sheket.client.utility.PrefUtil;


public class MainActivity extends AppCompatActivity implements
        NavigationFragment.BranchSelectionCallback, SPermission.PermissionChangeListener {

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    //private NavigationView mNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireLogin();

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_drawer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initNavigationDrawer();
    }

    void initNavigationDrawer() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.main_drawer_layout);
        //mNavigationView = (NavigationView) findViewById(R.id.navigation_view);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_open, R.string.drawer_close);
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        getSupportFragmentManager().beginTransaction().
                replace(R.id.main_navigation_container, new NavigationFragment()).
                commit();
        openNavDrawer();
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
    public void onBackPressed() {
        if (isNavDrawerOpen()) {
            closeNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    protected void openNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            /**
             * IMPORTANT:
             * home is also the "burger stack" for the navigation drawer, so without overloading the
             * menu inflater, you can't click the navigation "burger" to slide out the drawer
             */
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBranchSelected(final SBranch branch) {
        CategoryViewFragment fragment = new CategoryViewFragment();
        fragment.setListener(new CategoryViewFragment.SelectionListener() {
            @Override
            public void onCategorySelected(SCategory category) {
                getSupportFragmentManager().beginTransaction().
                        replace(R.id.main_fragment_container,
                                BranchItemFragment.newInstance(category.category_id, branch.branch_id)).
                        addToBackStack(null).commit();
            }
        });
        replaceMainFragment(fragment);
        closeNavDrawer();
    }

    void replaceMainFragment(Fragment fragment) {
        FragmentManager fm = getSupportFragmentManager();
        for(int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
    }

    @Override
    public void onElementSelected(int item) {
        switch (item) {
            case NavigationFragment.StaticNavigationAdapter.ENTITY_ALL_ITEMS:
                CategoryViewFragment fragment = new CategoryViewFragment();
                fragment.setListener(new CategoryViewFragment.SelectionListener() {
                    @Override
                    public void onCategorySelected(SCategory category) {
                        getSupportFragmentManager().beginTransaction().
                                replace(R.id.main_fragment_container,
                                        ItemListFragment.newInstance(category.category_id)).
                                addToBackStack(null).commit();
                    }
                });
                replaceMainFragment(fragment);
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
        closeNavDrawer();
    }

    @Override
    public void userPermissionChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            recreate();
        }
    }
}
