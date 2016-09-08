package com.mukera.sheket.client;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.mukera.sheket.client.controller.CompanyUtil;
import com.mukera.sheket.client.controller.admin.EmployeesFragment;
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
import com.mukera.sheket.client.controller.items.AllItemsFragment;
import com.mukera.sheket.client.controller.navigation.BaseNavigation;
import com.mukera.sheket.client.controller.navigation.LeftNavigation;
import com.mukera.sheket.client.controller.admin.BranchFragment;
import com.mukera.sheket.client.controller.admin.CompanyFragment;
import com.mukera.sheket.client.controller.navigation.RightNavigation;
import com.mukera.sheket.client.controller.user.ProfileFragment;
import com.mukera.sheket.client.controller.user.SettingsFragment;
import com.mukera.sheket.client.data.AndroidDatabaseManager;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.services.AlarmReceiver;
import com.mukera.sheket.client.services.SheketSyncService;
import com.mukera.sheket.client.utils.PrefUtil;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

public class MainActivity extends AppCompatActivity implements
        BaseNavigation.NavigationCallback,
        SPermission.PermissionChangeListener,
        ImportListener,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_FILE_CHOOSER = 1;
    private static final int REQUEST_READ_PHONE_STATE = 2;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

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

    private LeftNavigation mLeftNav;
    private RightNavigation mRightNav;

    private SlidingMenu mNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requireLogin();

        setUserLanguage();
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_app_icon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initSlidingMenuDrawer();

        AlarmReceiver.startPeriodicPaymentAlarm(this);

        syncIfIsLoginFirstTime();
        setTitle(R.string.app_name);

        if (savedInstanceState == null) {
            openNavDrawer();
        }
    }

    void setUserLanguage() {
        Locale locale = new Locale(PrefUtil.getUserLanguageLocale(this));
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getResources().updateConfiguration(
                config, getResources().getDisplayMetrics());
    }

    void initSlidingMenuDrawer() {
        mNavigation = new SlidingMenu(this);
        mNavigation.setMode(SlidingMenu.LEFT_RIGHT);
        mNavigation.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
        mNavigation.setFadeDegree(0.35f);
        mNavigation.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);

        LayoutInflater inflater = LayoutInflater.from(this);

        mLeftNav = new LeftNavigation();
        mLeftNav.setUpNavigation(this, inflater.inflate(R.layout.nav_layout_left, null));

        mRightNav = new RightNavigation();
        mRightNav.setUpNavigation(this, inflater.inflate(R.layout.nav_layout_right, null));

        mNavigation.setMenu(mLeftNav.getRootView());
        mNavigation.setSecondaryMenu(mRightNav.getRootView());

        int width = getResources().getDimensionPixelSize(R.dimen.navdrawer_width);
        mNavigation.setBehindWidth(width);
    }

    /**
     * Sync if we are loggin in, so the user can see his companies right away.
     * Only sync if you have not set any company, we don't want to be here all day!!
     */
    void syncIfIsLoginFirstTime() {
        if (PrefUtil.getShouldSyncOnLogin(this) &&
                !PrefUtil.isCompanySet(this)) {
            Intent intent = new Intent(this, SheketSyncService.class);
            startService(intent);
        }
    }

    void requireLogin() {
        if (!PrefUtil.isUserSet(this)) {
            finish();

            Intent intent = new Intent(this, LoginActivity.class);
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
        return mNavigation.isMenuShowing() || mNavigation.isSecondaryMenuShowing();
    }

    protected boolean isNavDrawerClosed() {
        return !isNavDrawerOpen();
    }

    protected void closeNavDrawer() {
        mNavigation.showContent(true);
    }

    protected void openNavDrawer() {
        /**
         * If we are launching for the first time open either of the navigation drawers.
         * If we don't have any companies, open the left side. Open the right otherwise.
         */
        if (!PrefUtil.isCompanySet(this)) {
            mNavigation.showMenu();
        } else {
            mNavigation.showSecondaryMenu();
        }
    }

    private void toggleDrawerState() {
        if (isNavDrawerOpen())
            closeNavDrawer();
        else
            openNavDrawer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.main_menu_right_nav:
                toggleDrawerState();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void emptyBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
            fm.popBackStack();
        }
    }

    void replaceMainFragment(Fragment fragment, boolean add_to_back_stack) {
        removeCustomActionBarViews();

        emptyBackStack();
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
    public void onCompanySelected(SCompany company) {
        if (company.payment_state != SheketContract.CompanyEntry.PAYMENT_VALID) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                /**
                 * There is a "Screen Overlay" problem in Android M(v23).
                 * We require read_phone_state permissions, which we've already requested
                 * in the manifest but not granted if there is a screen overlay.
                 *
                 * Read http://stackoverflow.com/a/32065680/5753416
                 */
                if (!Settings.canDrawOverlays(this)) {
                    SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                    SheketTracker.sendTrackingData(MainActivity.this,
                            new HitBuilders.EventBuilder().
                                    setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                    setAction("screen overlay disable requested").
                                    build());

                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_READ_PHONE_STATE);
                } else {
                    PaymentDialog.newInstance(company).show(getSupportFragmentManager(), null);
                }
            } else {
                PaymentDialog.newInstance(company).show(getSupportFragmentManager(), null);
            }
            return;
        }

        if (PrefUtil.getCurrentCompanyId(this) == company.company_id) {
            // there is nothing to do, we are already viewing that company
            return;
        }

        CompanyUtil.switchCurrentCompanyInWorkerThread(this, company, new CompanyUtil.StateSwitchedListener() {
            @Override
            public void runAfterSwitchCompleted() {
                SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(MainActivity.this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                setAction("company changed").
                                build());


                LocalBroadcastManager.getInstance(MainActivity.this).
                        sendBroadcast(new Intent(SheketBroadcast.ACTION_CONFIG_CHANGE));
            }
        });
    }

    @Override
    public void onBranchSelected(final SBranch branch) {
        replaceMainFragment(BranchItemFragment.newInstance(branch),
                false);
        closeNavDrawer();
    }

    @Override
    public void onNavigationOptionSelected(int item) {
        closeNavDrawer();
        removeCustomActionBarViews();

        boolean change_title = true;

        String new_screen = "";
        switch (item) {
            case BaseNavigation.StaticNavigationOptions.OPTION_ITEM_LIST:
                new_screen = "Items Fragment";
                replaceMainFragment(new AllItemsFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_IMPORT: {
                // Create the ACTION_GET_CONTENT Intent
                Intent getContentIntent = FileUtils.createGetContentIntent();

                Intent intent = Intent.createChooser(getContentIntent, "Select a file");
                startActivityForResult(intent, REQUEST_FILE_CHOOSER);
                change_title = false;
                SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_NAVIGATION).
                                setAction("importing file").
                                build());
                break;
            }
            case BaseNavigation.StaticNavigationOptions.OPTION_BRANCHES:
                new_screen = "Branches Fragment";
                replaceMainFragment(new BranchFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_COMPANIES:
                new_screen = "Companies Fragment";
                replaceMainFragment(new CompanyFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_EMPLOYEES:
                new_screen = "Employees Fragment";
                replaceMainFragment(new EmployeesFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_SYNC: {
                SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_NAVIGATION).
                                setAction("sync started").
                                build());
                Intent intent = new Intent(this, SheketSyncService.class);
                startService(intent);
                change_title = false;
                break;
            }
            case BaseNavigation.StaticNavigationOptions.OPTION_TRANSACTIONS:
                new_screen = "Transaction Summary";
                replaceMainFragment(new TransactionHistoryFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_SETTINGS:
                new_screen = "Settings";
                replaceMainFragment(new SettingsFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_DEBUG:
                startActivity(new Intent(this, AndroidDatabaseManager.class));
                change_title = false;
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_USER_PROFILE:
                new_screen = "User Profile";
                replaceMainFragment(new ProfileFragment(), false);
                break;
            case BaseNavigation.StaticNavigationOptions.OPTION_LOG_OUT:
                SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                SheketTracker.sendTrackingData(this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_MAIN_NAVIGATION).
                                setAction("logout selected").
                                build());
                logoutUser();
                change_title = false;
                break;
        }

        if (change_title) {
            SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
            SheketTracker.sendTrackingData(this,
                    new HitBuilders.EventBuilder().
                            setCategory(SheketTracker.CATEGORY_MAIN_NAVIGATION).
                            setAction("screen changed").
                            setLabel(new_screen).
                            build());
            setTitle(
                    BaseNavigation.StaticNavigationOptions.getOptionString(item));
        }
    }

    void logoutUser() {
        final ProgressDialog logoutDialog = ProgressDialog.show(this, "Logging out", "Please wait...", true);

        CompanyUtil.logoutOfCompany(this,
                new CompanyUtil.LogoutFinishListener() {
                    @Override
                    public void runAfterLogout() {
                        logoutDialog.dismiss();
                        requireLogin();
                    }

                    @Override
                    public void logoutError(String msg) {
                    }
                });
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

                if (verifyStoragePermissions()) {
                    startImporterTask();
                } else {
                    SheketTracker.setScreenName(MainActivity.this, SheketTracker.SCREEN_NAME_MAIN);
                    SheketTracker.sendTrackingData(this,
                            new HitBuilders.EventBuilder().
                                    setCategory(SheketTracker.CATEGORY_MAIN_CONFIGURATION).
                                    setAction("importing").
                                    setLabel("don't have permission to read file").
                                    build());
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

    private boolean verifyStoragePermissions() {
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
        super.onResume();

        IntentFilter filter = new IntentFilter();

        filter.addAction(SheketBroadcast.ACTION_SYNC_STARTED);
        filter.addAction(SheketBroadcast.ACTION_SYNC_SUCCESS);
        filter.addAction(SheketBroadcast.ACTION_SYNC_INVALID_LOGIN_CREDENTIALS);
        filter.addAction(SheketBroadcast.ACTION_SYNC_SERVER_ERROR);
        filter.addAction(SheketBroadcast.ACTION_SYNC_INTERNET_ERROR);
        filter.addAction(SheketBroadcast.ACTION_SYNC_GENERAL_ERROR);
        filter.addAction(SheketBroadcast.ACTION_CONFIG_CHANGE);
        filter.addAction(SheketBroadcast.ACTION_COMPANY_PERMISSION_CHANGE);
        filter.addAction(SheketBroadcast.ACTION_PAYMENT_REQUIRED);

        LocalBroadcastManager.getInstance(this).
                registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDidResume = false;
        LocalBroadcastManager.getInstance(this).
                unregisterReceiver(mReceiver);
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
        final boolean[] is_categories = new boolean[]{false};

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
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(SheketBroadcast.ACTION_CONFIG_CHANGE));
    }

    void dismissSyncDialog() {
        if (mSyncingProgress != null) {
            mSyncingProgress.dismiss();
            mSyncingProgress = null;
        }
    }

    void displayConfigurationIfFirstTime() {
        // we've already been here
        if (!PrefUtil.getIsFirstTime(this)) return;
        PrefUtil.setIsFirstTime(this, false);

        SettingsFragment.displayConfigurationDialog(this, false);
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Activity activity = MainActivity.this;

            String action = intent.getAction();
            String error_extra = intent.getStringExtra(SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);

            if (action.equals(SheketBroadcast.ACTION_PAYMENT_REQUIRED)) {
                dismissSyncDialog();
                restartMainActivity();
            } else if (action.equals(SheketBroadcast.ACTION_CONFIG_CHANGE)) {
                dismissSyncDialog();
                restartMainActivity();
            } else if (action.equals(SheketBroadcast.ACTION_COMPANY_PERMISSION_CHANGE)) {
                if (mRightNav != null) {
                    mRightNav.userPermissionChanged();
                }
                if (mLeftNav != null) {
                    mLeftNav.userPermissionChanged();
                }
            } else if (action.equals(SheketBroadcast.ACTION_SYNC_INVALID_LOGIN_CREDENTIALS)) {
                dismissSyncDialog();
                logoutUser();
            } else {
                dismissSyncDialog();
                /**
                 * If we are syncing because we just logged in, we don't want
                 * to display the "sync-progress" dialog
                 */
                if (PrefUtil.getShouldSyncOnLogin(MainActivity.this)) {
                    displayConfigurationIfFirstTime();
                } else {
                    if (action.equals(SheketBroadcast.ACTION_SYNC_STARTED)) {
                        mSyncingProgress = ProgressDialog.show(activity,
                                "Syncing", "Please Wait...", true);
                    } else if (action.equals(SheketBroadcast.ACTION_SYNC_SUCCESS)) {
                        new AlertDialog.Builder(MainActivity.this).
                                setTitle("Success").
                                setMessage("You've synced successfully.").show();

                        openNavDrawer();
                    } else {
                        String err_title = "";
                        if (action.equals(SheketBroadcast.ACTION_SYNC_SERVER_ERROR)) {
                            err_title = "Sync error, Try Again...";
                        } else if (action.equals(SheketBroadcast.ACTION_SYNC_INTERNET_ERROR)) {
                            err_title = "Internet error, Try Again...";
                        } else if (action.equals(SheketBroadcast.ACTION_SYNC_GENERAL_ERROR)) {
                            err_title = "Error, Try Again...";
                        }

                        new AlertDialog.Builder(MainActivity.this).
                                setTitle(err_title).setMessage(error_extra).
                                show();
                    }
                }

                // reset the "login-sync" if we're done with that, that only happens
                // after the start-{success|error}. So wait until {success|error}
                if (!action.equals(SheketBroadcast.ACTION_SYNC_STARTED)) {
                    // reset it so the next sync shows the dialogs
                    PrefUtil.setShouldSyncOnLogin(MainActivity.this, false);
                }
            }
        }
    };

    void restartMainActivity() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @Override
            public void run() {
                /**
                 * IMPORTANT: If we are viewing items in either BranchItems for ItemList
                 * and we try to restart MainActivity, the app will crash 'cause of
                 * a google ExpandableListView bug.
                 * So, we remove the fragments before we restart the activity to
                 * clear any references we have to any ExpandableListViewto any ExpandableListViews.
                 */
                emptyBackStack();

                finish();
                startActivity(getIntent());
                /*
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                } else {
                    recreate();
                }
                */
            }
        }, 100);
    }
}
