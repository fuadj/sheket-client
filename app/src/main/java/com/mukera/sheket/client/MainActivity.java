package com.mukera.sheket.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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
import com.mukera.sheket.client.controller.importer.ImporterDataMappingDialog;
import com.mukera.sheket.client.controller.importer.SimpleCSVReader;
import com.mukera.sheket.client.controller.items.BranchItemFragment;
import com.mukera.sheket.client.controller.items.CardViewToggleListener;
import com.mukera.sheket.client.controller.items.CategoryCardViewFragment;
import com.mukera.sheket.client.controller.items.ItemListFragment;
import com.mukera.sheket.client.controller.navigation.NavigationFragment;
import com.mukera.sheket.client.controller.admin.BranchFragment;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.admin.SettingsActivity;
import com.mukera.sheket.client.controller.transactions.UnsyncedTranactionHistoryFragment;
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
        ImportTask.ImportListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        SharedPreferences.OnSharedPreferenceChangeListener {

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

    private String mImportPath;

    static final int IMPORT_STATE_NONE = 0;
    static final int IMPORT_STATE_SUCCESS = 1;
    static final int IMPORT_STATE_DISPLAY_OPTIONS = 2;
    static final int IMPORT_STATE_ERROR = 3;

    private int mImportState = IMPORT_STATE_NONE;

    private SimpleCSVReader mReader = null;
    private ProgressDialog mImportProgress = null;
    private String mErrorMsg = null;

    /**
     * When importing, parsing is done on a AsyncTask and we can't
     * issue UI update from a worker thread. We could have posted
     * a {@code Runnable} on UI thread's LoopHandler to display results.
     * But because of AsyncTasks's behaviour, this will cause the app to crash
     * due to the activity not being on a resumed state. To prevent that, we only post to the
     * UI thread if the activity has resumed. So we have {@code mDidResume} for that.
     * If the activity wasn't resumed when we finished parsing, we need
     * to tell it to update the UI after it resumes, so we set {@code mImporting}
     * to true and it will check that to know if it needs to update UI when it wakes up.
     */
    private boolean mImporting = false;
    private boolean mDidResume = false;

    private ProgressDialog mSyncingProgress = null;

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
                    displayItemsORBranchFragment();
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
                mSyncingProgress = ProgressDialog.show(this,
                        "Syncing", "Please Wait...", true);
                PrefUtil.setSyncStatus(this, SheketService.SYNC_STATUS_SYNCING);
                Intent intent = new Intent(this, SheketService.class);
                startService(intent);
                break;
            }
            case NavigationFragment.StaticNavigationAdapter.ENTITY_TRANSACTIONS:
                replaceMainFragment(new UnsyncedTranactionHistoryFragment(), false);
                break;
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

                mImportPath = path;

                if (verityStoragePermissions()) {
                    startImporterTask();
                }

                break;
            }
        }
    }

    void startImporterTask() {
        mImporting = true;
        mImportProgress = ProgressDialog.show(this,
                "Importing Data", "Please Wait...", true);
        ImportTask importTask = new ImportTask(new File(mImportPath), this);
        importTask.setListener(this);
        importTask.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImporterTask();
                }
                break;
        }
    }

    private boolean verityStoragePermissions() {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
            return false;
        }
        return true;
    }

    @Override
    protected void onResume() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    protected void onPause() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
        mDidResume = false;
    }

    void stopImporting(String err_msg) {
        mImporting = false;
        mImportState = IMPORT_STATE_NONE;

        if (mImportProgress != null) {
            mImportProgress.dismiss();
            mImportProgress = null;
        }

        if (err_msg != null) {
            new AlertDialog.Builder(MainActivity.this).
                    setTitle("Import Error").
                    setMessage(err_msg).show();
            Log.e("Sheket MainActivity", err_msg);
        }
    }

    void showImportUpdates() {
        switch (mImportState) {
            case IMPORT_STATE_SUCCESS:
                stopImporting(null);
                break;
            case IMPORT_STATE_DISPLAY_OPTIONS:
                if (mReader.parsingSuccess()) {
                    final ImporterDataMappingDialog dialog = ImporterDataMappingDialog.newInstance(mReader);
                    dialog.setListener(new ImporterDataMappingDialog.OnClickListener() {
                        @Override
                        public void onOkSelected(SimpleCSVReader reader, Map<Integer, Integer> dataMapping) {
                            dialog.dismiss();
                            new ImportTask.ImportDataTask(reader, dataMapping, MainActivity.this, MainActivity.this).execute();
                        }

                        @Override
                        public void onCancelSelected() {
                            stopImporting("Import Dialog Canceled");
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "Import");
                } else {
                    stopImporting("Parsing Error " + mErrorMsg);
                }
                break;
            case IMPORT_STATE_ERROR:
                stopImporting(mErrorMsg);
                break;
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mDidResume = true;
        if (mImporting) {
            showImportUpdates();
        }
    }

    @Override
    public void showImportOptionsDialog(SimpleCSVReader reader) {
        mReader = reader;
        mImportState = IMPORT_STATE_DISPLAY_OPTIONS;
        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void importSuccessful() {
        mImportState = IMPORT_STATE_SUCCESS;
        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void importError(String msg) {
        mErrorMsg = msg;
        mImportState = IMPORT_STATE_ERROR;
        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void userPermissionChanged() {
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(getString(R.string.sync_status)) &&
                PrefUtil.getSyncStatus(this) != SheketService.SYNC_STATUS_SYNCING) {
            if (mSyncingProgress != null)
                mSyncingProgress.dismiss();;
            if (PrefUtil.getSyncStatus(this) == SheketService.SYNC_STATUS_ERROR) {
                new AlertDialog.Builder(this).
                        setTitle("Sync Error, try again").
                        setMessage(PrefUtil.getSyncError(this)).
                        show();
            }
            PrefUtil.setSyncStatus(this, SheketService.SYNC_STATUS_SYNCED);
        }
    }
}
