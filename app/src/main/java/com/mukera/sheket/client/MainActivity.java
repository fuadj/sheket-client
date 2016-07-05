package com.mukera.sheket.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
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
import com.mukera.sheket.client.controller.importer.DuplicateEntities;
import com.mukera.sheket.client.controller.importer.DuplicateReplacementDialog;
import com.mukera.sheket.client.controller.importer.DuplicateFinderTask;
import com.mukera.sheket.client.controller.importer.ImportDataMappingDialog;
import com.mukera.sheket.client.controller.importer.ImportDataTask;
import com.mukera.sheket.client.controller.importer.ImportListener;
import com.mukera.sheket.client.controller.importer.ParseFileTask;
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
import java.util.Set;
import java.util.Vector;


public class MainActivity extends AppCompatActivity implements
        NavigationFragment.BranchSelectionCallback,
        SPermission.PermissionChangeListener,
        ImportListener,
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
    static final int IMPORT_STATE_DISPLAY_DATA_MAPPING_DIALOG = 2;
    static final int IMPORT_STATE_DISPLAY_REPLACEMENT_DIALOG = 3;
    static final int IMPORT_STATE_ERROR = 4;

    private int mImportState = IMPORT_STATE_NONE;

    private SimpleCSVReader mReader = null;

    private Map<Integer, Integer> mImportDataMapping = null;
    private DuplicateEntities mDuplicateEntities = null;

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
                new AlertDialog.Builder(this).
                        setTitle("Are You Sure?").
                        setMessage("This will delete all un-synced data, are you sure?").
                        setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                deleteAllUnSyncedData();
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
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
                logoutUser();
                break;
        }
        closeNavDrawer();
    }

    void logoutUser() {
        final ProgressDialog savingDialog = ProgressDialog.show(this, "Logging out", "Please wait...", true);
        final Context context = MainActivity.this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                long current_company = PrefUtil.getCurrentCompanyId(context);
                String current_state = PrefUtil.getEncodedStateBackup(context);

                ContentValues values = new ContentValues();
                // Yes, It is valid to only include the values you want to update
                values.put(CompanyEntry.COLUMN_STATE_BACKUP, current_state);

                context.getContentResolver().
                        update(
                                CompanyEntry.CONTENT_URI,
                                values,
                                CompanyEntry._full(CompanyEntry.COLUMN_ID) + " = ?",
                                new String[]{
                                        String.valueOf(current_company)
                                }
                        );
                PrefUtil.logoutUser(MainActivity.this);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        savingDialog.dismiss();
                        requireLogin();
                    }
                });
            }
        }).start();
    }

    String unsyncedSelector(String column) {
        return column + " != " + ChangeTraceable.CHANGE_STATUS_SYNCED;
    }

    void deleteAllUnSyncedData() {
        long company_id = PrefUtil.getCurrentCompanyId(this);
        getContentResolver().delete(
                TransItemEntry.buildBaseUri(company_id),
                unsyncedSelector(TransItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
        getContentResolver().delete(
                TransactionEntry.buildBaseUri(company_id),
                unsyncedSelector(TransactionEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
        getContentResolver().delete(
                BranchItemEntry.buildBaseUri(company_id),
                unsyncedSelector(BranchItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
        getContentResolver().delete(
                CategoryEntry.buildBaseUri(company_id),
                unsyncedSelector(CategoryEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
        getContentResolver().delete(
                ItemEntry.buildBaseUri(company_id),
                unsyncedSelector(ItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
        getContentResolver().delete(
                BranchEntry.buildBaseUri(company_id),
                unsyncedSelector(BranchEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                null
        );
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
        ParseFileTask parseFileTask = new ParseFileTask(new File(mImportPath));
        parseFileTask.setListener(this);
        parseFileTask.execute();
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
            case IMPORT_STATE_DISPLAY_DATA_MAPPING_DIALOG:
                if (mReader.parsingSuccess()) {
                    final ImportDataMappingDialog dialog = ImportDataMappingDialog.newInstance(mReader);
                    dialog.setListener(new ImportDataMappingDialog.OnClickListener() {
                        @Override
                        public void onOkSelected(SimpleCSVReader reader, Map<Integer, Integer> dataMapping) {
                            dialog.dismiss();
                            new DuplicateFinderTask(reader, dataMapping, MainActivity.this).execute();
                        }

                        @Override
                        public void onCancelSelected() {
                            dialog.dismiss();
                            stopImporting("Import Dialog Canceled");
                        }
                    });
                    dialog.show(getSupportFragmentManager(), "Import");
                } else {
                    stopImporting("Parsing Error " + mErrorMsg);
                }
                break;
            case IMPORT_STATE_DISPLAY_REPLACEMENT_DIALOG:
                chooseReplacementForDuplicates();
                break;
            case IMPORT_STATE_ERROR:
                stopImporting(mErrorMsg);
                break;
        }
    }

    void chooseReplacementForDuplicates() {
        boolean found_duplicates = false;
        Vector<String> duplicates = null;

        // this can't be a single variable b/c it is final
        final boolean []is_categories = new boolean[]{false};

        if (!mDuplicateEntities.categoryDuplicates.isEmpty()) {
            found_duplicates = true;
            is_categories[0] = true;
            duplicates = mDuplicateEntities.categoryDuplicates.remove(0);
        } else if (!mDuplicateEntities.branchDuplicates.isEmpty()) {
            found_duplicates = true;
            is_categories[0] = false;
            duplicates = mDuplicateEntities.branchDuplicates.remove(0);
        }

        if (found_duplicates) {
            final DuplicateReplacementDialog dialog = DuplicateReplacementDialog.newInstance(duplicates,
                    is_categories[0] ? "Categories" : "Branches");
            dialog.setListener(new DuplicateReplacementDialog.ReplacementListener() {
                @Override
                public void noDuplicatesFound() {
                    dialog.dismiss();

                    // recursive for the next
                    chooseReplacementForDuplicates();
                }

                @Override
                public void duplicatesFound(Set<String> nonDuplicates, DuplicateReplacementDialog.Replacement replacement) {
                    dialog.dismiss();
                    /**
                     * for each replacement word, make a mapping for it to the "correct word".
                     * we use this mapping when we actually do the importing to replace out the
                     * duplicates with the correct ones.
                     */

                    // doing the checking outside is more efficient
                    if (is_categories[0]) {
                        for (String duplicateCategory : replacement.duplicates) {
                            mDuplicateEntities.categoryReplacement.put(duplicateCategory, replacement.correctWord);
                        }
                    } else {
                        for (String duplicateBranch : replacement.duplicates) {
                            mDuplicateEntities.branchReplacement.put(duplicateBranch, replacement.correctWord);
                        }
                    }

                    // recursive for the next
                    chooseReplacementForDuplicates();
                }
            });
            dialog.show(getSupportFragmentManager(), "Duplicate " + (is_categories[0] ? "Categories" : "Branches"));
        } else {
            // This means we've gone through all the categories and branches,
            // time to do the actual importing
            new ImportDataTask(mReader, mImportDataMapping, mDuplicateEntities, this, this).execute();
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
    public void displayDataMappingDialog(SimpleCSVReader reader) {
        mReader = reader;
        mImportState = IMPORT_STATE_DISPLAY_DATA_MAPPING_DIALOG;
        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void displayReplacementDialog(SimpleCSVReader reader, Map<Integer, Integer> mapping, DuplicateEntities duplicateEntities) {
        mImportState = IMPORT_STATE_DISPLAY_REPLACEMENT_DIALOG;

        mImportDataMapping = mapping;
        mDuplicateEntities = duplicateEntities;

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
        if (!key.equals(getString(R.string.sync_status)) &&
                PrefUtil.getSyncStatus(this) == SheketService.SYNC_STATUS_SYNCING) {
            return;
        }
        if (mSyncingProgress != null) {
            mSyncingProgress.dismiss();
            mSyncingProgress = null;
        }

        boolean is_error = true;
        String err_title = null, err_msg = null;
        switch (PrefUtil.getSyncStatus(this)) {
            case SheketService.SYNC_STATUS_SUCCESSFUL:
                is_error = false;
                break;
            case SheketService.SYNC_STATUS_SYNC_ERROR:
                err_title = "Sync Error, try again";
                err_msg = PrefUtil.getSyncErrorMessage(this);
                break;
            case SheketService.SYNC_STATUS_INTERNET_ERROR:
                err_title = "Internet Problem";
                err_msg = "Try Again";
                break;
            case SheketService.SYNC_STATUS_GENERAL_ERROR:
                // TODO: don't know how to display it, just print it for now
                err_title = "Error, Try again";
                err_msg = PrefUtil.getSyncErrorMessage(this);
                break;
        }

        AlertDialog dialog = null;
        if (is_error) {
            dialog = new AlertDialog.Builder(this).
                    setTitle(err_title).
                    setMessage(err_msg).
                    setIcon(android.R.drawable.ic_dialog_alert).create();
        } else {
            if (PrefUtil.isCompanySet(this)) {
                dialog = new AlertDialog.Builder(this).
                        setTitle("Success").
                        setMessage("You've synced successfully").create();
            }
        }

        if (dialog != null) {
            dialog.show();
        }

        // reset it to synced state
        PrefUtil.setSyncStatus(this, SheketService.SYNC_STATUS_SYNCED);
    }
}
