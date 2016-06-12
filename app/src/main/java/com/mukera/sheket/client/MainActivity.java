package com.mukera.sheket.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mukera.sheket.client.controller.admin.MembersFragment;
import com.mukera.sheket.client.controller.admin.TransactionHistoryFragment;
import com.mukera.sheket.client.controller.importer.ImportTask;
import com.mukera.sheket.client.controller.importer.ImporterDialog;
import com.mukera.sheket.client.controller.importer.SimpleCSVReader;
import com.mukera.sheket.client.controller.items.BranchItemFragment;
import com.mukera.sheket.client.controller.items.CardViewToggleListener;
import com.mukera.sheket.client.controller.items.CategoryCardViewFragment;
import com.mukera.sheket.client.controller.items.ItemListFragment;
import com.mukera.sheket.client.controller.navigation.NavigationFragment;
import com.mukera.sheket.client.controller.admin.BranchFragment;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.admin.SettingsActivity;
import com.mukera.sheket.client.controller.user.ProfileFragment;
import com.mukera.sheket.client.controller.user.RegistrationActivity;
import com.mukera.sheket.client.data.AndroidDatabaseManager;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.sync.SheketService;
import com.mukera.sheket.client.utils.PrefUtil;

import java.io.File;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements
        NavigationFragment.BranchSelectionCallback,
        SPermission.PermissionChangeListener,
        ImportTask.ImportListener {

    private static final int REQUEST_FILE_CHOOSER = 1234;

    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    //private NavigationView mNavigationView;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private boolean mIsBranchSelected = false;
    private long mSelectedBranchId;

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

    void requireLogin() {
        if (!PrefUtil.isUserSet(this)) {
            Intent intent = new Intent(this, RegistrationActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        // If there are fragments, do what is normally done
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            super.onBackPressed();
            return;
        }

        if (isNavDrawerClosed()) {
            openNavDrawer();
        } else {
            super.onBackPressed();
        }
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    protected boolean isNavDrawerClosed() {
        return mDrawerLayout != null && !mDrawerLayout.isDrawerOpen(GravityCompat.START);
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
        mIsBranchSelected = true;
        mSelectedBranchId = branch.branch_id;
        displayItemsORBranchFragment();
        closeNavDrawer();
    }

    void displayItemsORBranchFragment() {
        removeCustomActionBarViews();
        boolean show_category_card = PrefUtil.showCategoryCards(this);

        if (show_category_card) {
            CategoryCardViewFragment fragment = new CategoryCardViewFragment();
            fragment.setCategorySelectionListener(new CategoryCardViewFragment.SelectionListener() {
                @Override
                public void onCategorySelected(SCategory category) {
                    CardViewToggleListener listener = new CardViewToggleListener() {
                        @Override
                        public void onCardOptionSelected(boolean enable_card_view) {
                            displayItemsORBranchFragment();
                        }
                    };
                    Fragment fragment;
                    if (mIsBranchSelected) {
                        fragment = BranchItemFragment.
                                newInstance(category.category_id, mSelectedBranchId, false).
                                setCardViewToggleListener(listener);
                    } else {
                        fragment = ItemListFragment.
                                newInstance(category.category_id, false).
                                setCardViewToggleListener(listener);
                    }
                    replaceMainFragment(fragment, true);
                }
            }).setCardViewToggleListener(new CardViewToggleListener() {
                @Override
                public void onCardOptionSelected(boolean enable_card_view) {
                    PrefUtil.setCategoryCardShow(MainActivity.this,
                            false);
                    CardViewToggleListener listener = new CardViewToggleListener() {
                        @Override
                        public void onCardOptionSelected(boolean enable_card_view) {
                            PrefUtil.setCategoryCardShow(MainActivity.this,
                                    true);
                            displayItemsORBranchFragment();
                        }
                    };
                    Fragment fragment;
                    if (mIsBranchSelected) {
                        fragment = BranchItemFragment.
                                newInstance(CategoryEntry.ROOT_CATEGORY_ID,
                                        mSelectedBranchId, true).setCardViewToggleListener(listener);
                    } else {
                        fragment = ItemListFragment.
                                newInstance(CategoryEntry.ROOT_CATEGORY_ID,
                                        true).setCardViewToggleListener(listener);
                    }
                    replaceMainFragment(fragment, false);
                }
            });
            replaceMainFragment(fragment, false);
        } else {
            CardViewToggleListener listener = new CardViewToggleListener() {
                @Override
                public void onCardOptionSelected(boolean enable_card_view) {
                    PrefUtil.setCategoryCardShow(MainActivity.this,
                            enable_card_view);
                    displayItemsORBranchFragment();
                }
            };
            Fragment fragment;
            if (mIsBranchSelected) {
                fragment = BranchItemFragment.
                        newInstance(CategoryEntry.ROOT_CATEGORY_ID,
                                mSelectedBranchId, true).
                        setCardViewToggleListener(listener);
            } else {
                fragment = ItemListFragment.
                        newInstance(CategoryEntry.ROOT_CATEGORY_ID, true).
                        setCardViewToggleListener(listener);
            }
            replaceMainFragment(fragment, false);
        }
    }

    void replaceMainFragment(Fragment fragment, boolean add_to_back_stack) {
        removeCustomActionBarViews();

        FragmentManager fm = getSupportFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().
                replace(R.id.main_fragment_container, fragment);
        if (add_to_back_stack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }

    void removeCustomActionBarViews() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null && (actionBar.getCustomView() != null))
            actionBar.getCustomView().setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    @Override
    public void onElementSelected(int item) {
        removeCustomActionBarViews();
        switch (item) {
            case NavigationFragment.StaticNavigationAdapter.ENTITY_ALL_ITEMS:
                mIsBranchSelected = false;
                displayItemsORBranchFragment();
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_IMPORT: {
                // Create the ACTION_GET_CONTENT Intent
                Intent getContentIntent = FileUtils.createGetContentIntent();

                Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                startActivityForResult(intent, REQUEST_FILE_CHOOSER);
                break;
            }
            case NavigationFragment.StaticNavigationAdapter.ENTITY_DELETE: {
                deleteAllRecords();
                break;
            }
            case NavigationFragment.StaticNavigationAdapter.ENTITY_BRANCHES:
                replaceMainFragment(new BranchFragment(), false);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_COMPANIES:
                replaceMainFragment(new CompanyFragment(), false);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_HISTORY:
                replaceMainFragment(new TransactionHistoryFragment(), false);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_MEMBERS:
                replaceMainFragment(new MembersFragment(), false);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_SYNC: {
                Intent intent = new Intent(this, SheketService.class);
                startService(intent);
                break;
            }
            case NavigationFragment.StaticNavigationAdapter.ENTITY_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_DEBUG:
                startActivity(new Intent(this, AndroidDatabaseManager.class));
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_USER_PROFILE:
                replaceMainFragment(new ProfileFragment(), false);
                break;
            case NavigationFragment.StaticNavigationAdapter.ENTITY_LOG_OUT:
                PrefUtil.logoutUser(this);
                requireLogin();
                break;
        }
        closeNavDrawer();
    }

    void deleteAllRecords() {
        long company_id = PrefUtil.getCurrentCompanyId(this);
        getContentResolver().delete(
                TransItemEntry.buildBaseUri(company_id),
                null,
                null
        );
        getContentResolver().delete(
                TransactionEntry.buildBaseUri(company_id),
                null,
                null
        );
        getContentResolver().delete(
                BranchItemEntry.buildBaseUri(company_id),
                null,
                null
        );
        getContentResolver().delete(
                CategoryEntry.buildBaseUri(company_id),
                CategoryEntry.COLUMN_CATEGORY_ID + " != '" + CategoryEntry.ROOT_CATEGORY_ID +
                        "' AND  " +
                        CategoryEntry.COLUMN_CATEGORY_ID + " != '" + CategoryEntry._ROOT_CATEGORY_PARENT_ID + "' ",
                null
        );
        getContentResolver().delete(
                ItemEntry.buildBaseUri(company_id),
                null,
                null
        );
        getContentResolver().delete(
                BranchEntry.buildBaseUri(company_id),
                BranchEntry.COLUMN_BRANCH_ID + " != ' " + BranchEntry.DUMMY_BRANCH_ID + " ' ",
                null
        );
        PrefUtil.resetAllRevisionNumbers(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_FILE_CHOOSER: {
                if (resultCode != RESULT_OK) return;
                final Uri uri = data.getData();

                // Get the File path from the Uri
                String path = FileUtils.getPath(this, uri);

                if (path == null || !FileUtils.isLocal(path)) return;

                verityStoragePermissions();

                mImportProgress = ProgressDialog.show(this,
                        "Importing Data", "Please Wait...", true);
                ImportTask importTask = new ImportTask(new File(path), this);
                importTask.setListener(this);
                importTask.execute();
                break;
            }
        }
    }

    private void verityStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private boolean mShowImportOptionsDialog = false;
    private SimpleCSVReader mReader = null;
    private boolean mErrorOccurred = false;
    private boolean mImportSuccessful = false;
    private ProgressDialog mImportProgress = null;
    private String mErrorMsg = null;
    private boolean mImporting = false;

    private boolean mDidResume = false;

    @Override
    protected void onPause() {
        super.onPause();
        mDidResume = false;
    }

    void showImportingStuff() {
        if (mErrorOccurred) {
            mImporting = false;
            mErrorOccurred = false;

            if (mImportProgress != null)
                mImportProgress.dismiss();

            displayErrorDialog(mErrorMsg);
            return;
        }

        if (mShowImportOptionsDialog) {
            mShowImportOptionsDialog = false;
            if (mReader.parsingSuccess()) {
                final ImporterDialog dialog = ImporterDialog.newInstance(mReader);
                dialog.setListener(new ImporterDialog.OnClickListener() {
                    @Override
                    public void onOkSelected(SimpleCSVReader reader, Map<Integer, Integer> dataMapping) {
                        dialog.dismiss();
                        new ImportTask.ImportDataTask(reader, dataMapping, MainActivity.this, MainActivity.this).execute();
                    }

                    @Override
                    public void onCancelSelected() {
                        mErrorOccurred = true;
                        mErrorMsg = "Options Dialog Canceled";
                    }
                });
                dialog.show(getSupportFragmentManager(), "Import");
            } else {
                displayErrorDialog("Parsing Error " + mErrorMsg);
            }
        } else if (mImportSuccessful) {
            mImportSuccessful = false;
            mImporting = false;
            if (mImportProgress != null)
                mImportProgress.dismiss();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mDidResume = true;
        if (mImporting) {
            showImportingStuff();
        }
    }

    void displayErrorDialog(String err_msg) {
        if (err_msg != null) {
            new AlertDialog.Builder(MainActivity.this).
                    setTitle("Import Error").
                    setMessage(err_msg).show();
        }
        Log.e("Sheket MainActivity", (err_msg != null) ? err_msg : "");
    }

    @Override
    public void showImportOptionsDialog(SimpleCSVReader reader) {
        mImporting = true;
        mReader = reader;
        mShowImportOptionsDialog = true;
        if (mDidResume) {
            showImportingStuff();
        }
    }

    @Override
    public void importSuccessful() {
        mImporting = true;
        mImportSuccessful = true;
        if (mDidResume) {
            showImportingStuff();
        }
    }

    @Override
    public void importError(String msg) {
        mErrorOccurred = true;
        mErrorMsg = msg;
        if (mDidResume) {
            showImportingStuff();
        }
    }

    @Override
    public void userPermissionChanged() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        finish();
                        startActivity(getIntent());
                    } else {
                        recreate();
                    }
                }
            }, 1);
        }
    }
}
