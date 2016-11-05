package com.mukera.sheket.client.controller.importer;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.ipaulpro.afilechooser.utils.FileUtils;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.SheketTracker;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

public class ImporterActivity extends AppCompatActivity implements
        ApplyImportOperationsTask.ImportListener,
        LoaderManager.LoaderCallbacks<Cursor>,
        DuplicateSearcherTask.SearchFinishedListener {
    public static final int REQUEST_FILE_CHOOSER = 1;

    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private String mImportPath;

    static final int IMPORT_SUCCESS = 1;
    static final int IMPORT_DISPLAY_COLUMN_MAPPING = 2;
    static final int IMPORT_DISPLAY_REPLACEMENT = 3;
    static final int IMPORT_ERROR = 4;

    private int mImportState;

    private SimpleCSVReader mReader = null;

    private Map<Integer, Integer> mColumnMapping = null;
    private DuplicateEntities mDuplicateEntities = null;

    private ProgressDialog mImportProgress = null;
    private String mErrorMsg = null;

    /**
     * If a branch was not specified but initial quantity for the items was set,
     * give an option for the user to specify to what branch those quantities should
     * apply, even give option to create new branch. If the user chooses to create the
     * branch, save the branch id and apply the importing to that branch.
     * Otherwise, ignore the quantities as we don't have any branch to put them in.
     */
    private boolean mDidChooseBranchForImporting = false;
    private int mChosenBranchId = -1;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_importer);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (savedInstanceState != null)
            return;

        // Create the ACTION_GET_CONTENT Intent
        Intent getContentIntent = FileUtils.createGetContentIntent();

        Intent intent = Intent.createChooser(getContentIntent, "Select a file");
        startActivityForResult(intent, REQUEST_FILE_CHOOSER);
        SheketTracker.setScreenName(ImporterActivity.this, SheketTracker.SCREEN_NAME_MAIN);
        SheketTracker.sendTrackingData(this,
                new HitBuilders.EventBuilder().
                        setCategory(SheketTracker.CATEGORY_MAIN_NAVIGATION).
                        setAction("importing file").
                        build());
    }

    void displayToastAndFinishActivity(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3500);
                } catch (Exception e) {
                } finally {
                    finish();
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_FILE_CHOOSER: {
                if (resultCode != RESULT_OK) {
                    // user has dismissed the file chooser!!
                    finish();
                    return;
                }

                final Uri uri = data.getData();

                // Get the File path from the Uri
                String path = FileUtils.getPath(this, uri);

                if (path == null || !FileUtils.isLocal(path)) {
                    String err_msg = "";
                    if (path == null) {
                        err_msg = "Invalid path";
                    } else if (!FileUtils.isLocal(path)) {
                        err_msg = "Please Select a file on your local phone";
                    }
                    displayToastAndFinishActivity(err_msg);
                    return;
                }

                String extension = FileUtils.getExtension(path);
                if (extension == null || !extension.trim().toLowerCase().equals(".csv")) {
                    displayToastAndFinishActivity(
                            getResources().getString(
                                    R.string.toast_file_extension_must_be_csv));
                    return;
                }
                mImportPath = path;

                if (verifyStoragePermissions()) {
                    startCSVImporter();
                } else {
                    SheketTracker.setScreenName(ImporterActivity.this, SheketTracker.SCREEN_NAME_MAIN);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCSVImporter();
                } else {
                    // TODO: user denied permission
                    finish();
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

    void startCSVImporter() {
        mImporting = true;
        mImportProgress = ProgressDialog.show(this,
                "Importing Data", "Please Wait...", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SimpleCSVReader reader = new SimpleCSVReader(new File(mImportPath));
                reader.parseCSV();

                ImporterActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (reader.parsingSuccess()) {
                            mReader = reader;
                            mImportState = IMPORT_DISPLAY_COLUMN_MAPPING;
                            if (mDidResume) {
                                showImportUpdates();
                            }
                        } else {
                            importError("Import Error: " + reader.getErrorMessage());
                        }
                    }
                });
            }
        }).start();
    }


    @Override
    protected void onPause() {
        super.onPause();
        mDidResume = false;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        mDidResume = true;
        if (mImporting) {
            showImportUpdates();
        }
    }

    void stopImporting(String err_msg) {
        mImporting = false;

        if (mImportProgress != null) {
            mImportProgress.dismiss();
            mImportProgress = null;
        }

        AlertDialog.Builder builder = new AlertDialog.
                Builder(ImporterActivity.this).
                setTitle(
                        err_msg != null ? "Import Error" : "Import Success"
                ).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).
                setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        dialog.dismiss();
                        finish();
                    }
                });
        if (err_msg != null) {
            builder.setMessage(err_msg);
        }
        builder.show();
    }

    void showImportUpdates() {
        switch (mImportState) {
            case IMPORT_SUCCESS:
                stopImporting(null);
                break;
            case IMPORT_DISPLAY_COLUMN_MAPPING:
                // a nice handy way of implementing "closure" to wrap this code in,
                // so we don't have to define it as an external function
                final Runnable importDialogPresenter = new Runnable() {
                    @Override
                    public void run() {
                        final ColumnMappingDialog dialog = ColumnMappingDialog.newInstance(mReader);
                        dialog.setListener(new ColumnMappingDialog.OnClickListener() {
                            @Override
                            public void onColumnMappingDone(Map<Integer, Integer> columnMapping) {
                                dialog.dismiss();

                                mColumnMapping = columnMapping;

                                boolean quantity_set = columnMapping.get(ColumnMappingDialog.DATA_QUANTITY) !=
                                        ColumnMappingDialog.NO_DATA_FOUND;
                                boolean branch_set = columnMapping.get(ColumnMappingDialog.DATA_BRANCH) !=
                                        ColumnMappingDialog.NO_DATA_FOUND;
                                if (quantity_set && !branch_set) {
                                    getSupportLoaderManager().
                                            initLoader(LoaderId.ImporterActivity.BRANCH_LIST_LOADER, null,
                                                    ImporterActivity.this);
                                } else {
                                    startDuplicateFinderTask();
                                }
                            }

                            @Override
                            public void onCancelSelected() {
                                dialog.dismiss();
                                stopImporting("Import Dialog Canceled");
                            }
                        });
                        dialog.show(getSupportFragmentManager(), null);
                    }
                };

                if (mReader.getNumSkippedLines() > 0) {
                    String msg = String.format(Locale.US, "%d lines skipped\n" +
                                    "%d lines had fewer columns than header\n" +
                                    "%d lines had more columns than header.",
                            mReader.getNumSkippedLines(),
                            mReader.getNumLinesWithFewerColumnsThanHeader(),
                            mReader.getNumLinesWithMoreColumnsThanHeader());

                    new AlertDialog.Builder(ImporterActivity.this).
                            setTitle("Some lines were skipped").
                            setMessage(msg).
                            setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    importDialogPresenter.run();
                                }
                            }).
                            setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    importDialogPresenter.run();
                                }
                            }).show();
                } else {
                    importDialogPresenter.run();
                }

                break;
            case IMPORT_DISPLAY_REPLACEMENT:
                chooseReplacementForDuplicates();
                break;
            case IMPORT_ERROR:
                stopImporting(mErrorMsg);
                break;
        }
    }

    void startDuplicateFinderTask() {
        new DuplicateSearcherTask(mReader, mColumnMapping, ImporterActivity.this).execute();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";

        /**
         * filter-out branches who've got their status_flag's set to INVISIBLE
         */
        String selection = String.format(Locale.US,
                "%s != %d",
                BranchEntry._full(BranchEntry.COLUMN_STATUS_FLAG),
                BranchEntry.STATUS_INVISIBLE);

        return new CursorLoader(this,
                BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                SBranch.BRANCH_COLUMNS,
                selection,
                null, sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data != null)
            displayImportBranchSelectionDialog(data);
        else
            importError(null);
    }

    void displayImportBranchSelectionDialog(Cursor data) {
        final ArrayList<SBranch> branches = new ArrayList<>();
        ArrayList<String> branchNames = new ArrayList<>();

        if (data.moveToFirst()) {
            do {
                SBranch branch = new SBranch(data);
                branches.add(branch);
                branchNames.add(branch.branch_name);
            } while (data.moveToNext());
        }

        final int[] selectedBranchIndex = new int[]{-1};

        View view = getLayoutInflater().inflate(R.layout.dialog_import_select_branch_custom_title_view, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCustomTitle(view);
        if (!branches.isEmpty()) {
            String[] branchNameArray = new String[branchNames.size()];
            branchNames.toArray(branchNameArray);

            builder.setSingleChoiceItems(
                    branchNameArray, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedBranchIndex[0] = which;

                            ((AlertDialog) dialog).
                                    getButton(DialogInterface.BUTTON_POSITIVE).
                                    setVisibility(View.VISIBLE);
                        }
                    });
        }

        AlertDialog dialog = builder.
                setPositiveButton(R.string.dialog_import_select_branch_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (((AlertDialog) dialog).
                                getButton(DialogInterface.BUTTON_POSITIVE).getVisibility() == View.INVISIBLE) {
                            return;
                        }
                        dialog.dismiss();
                        mDidChooseBranchForImporting = true;
                        mChosenBranchId = branches.get(selectedBranchIndex[0]).branch_id;
                        startDuplicateFinderTask();
                    }
                }).
                setNegativeButton(R.string.dialog_import_select_branch_create_new_branch, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        showCreateNewBranchDialog();
                    }
                }).
                setNeutralButton(R.string.dialog_import_select_branch_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        mDidChooseBranchForImporting = false;
                        startDuplicateFinderTask();
                    }
                }).
                setCancelable(false).
                create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setVisibility(View.INVISIBLE);
            }
        });
        dialog.show();
    }

    void showCreateNewBranchDialog() {
        final EditText editText = new EditText(ImporterActivity.this);

        AlertDialog.Builder builder = new AlertDialog.Builder(ImporterActivity.this).
                setTitle("Create Branch").
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String branch_name = editText.getText().toString().trim();
                        final Activity activity = ImporterActivity.this;
                        final String location = "";
                        final long company_id = PrefUtil.getCurrentCompanyId(activity);

                        Thread t = new Thread() {
                            @Override
                            public void run() {
                                ContentValues values = new ContentValues();
                                values.put(BranchEntry.COLUMN_NAME, branch_name);
                                values.put(BranchEntry.COLUMN_LOCATION, location);
                                values.put(BranchEntry.COLUMN_COMPANY_ID, company_id);
                                values.put(SheketContract.UUIDSyncable.COLUMN_UUID,
                                        UUID.randomUUID().toString());
                                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                                        ChangeTraceable.CHANGE_STATUS_CREATED);

                                int branch_id = PrefUtil.getNewBranchId(activity);
                                PrefUtil.setNewBranchId(activity, branch_id);

                                values.put(BranchEntry.COLUMN_BRANCH_ID, branch_id);
                                activity.getContentResolver().insert(
                                        BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(activity)), values);

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        dialog.dismiss();
                                        // We don't need to call initLoader as we did on cancel
                                        // b/c that will get called automatically when the branch gets
                                        // created
                                    }
                                });
                            }
                        };
                        t.start();
                    }
                }).
                setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();

                        // recall the loader so the branch selection dialog can be "re-shown"
                        // it is easier just to re-create it rather than to undo the transaction
                        // as dialogs don't put transactions on the backstack to be un-done.
                        getSupportLoaderManager().
                                initLoader(LoaderId.ImporterActivity.BRANCH_LIST_LOADER, null,
                                        ImporterActivity.this);
                    }
                });

        final AlertDialog dialog = builder.setCancelable(false).create();

        editText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                String branchName = s.toString().trim();

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).
                        setVisibility(!branchName.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // initially don't show the "Ok" button b/c the name hasn't changed
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        });

        dialog.show();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO: couldn't load branch list, probably should exist the activity altogether
        importError(null);
    }

    void chooseReplacementForDuplicates() {
        Vector<String> duplicates = null;

        // this can't be a single variable b/c it is final
        final boolean[] is_categories = new boolean[]{false};

        boolean found_duplicates = true;

        if (!mDuplicateEntities.categoryDuplicates.isEmpty()) {
            is_categories[0] = true;
            // "pick-off" the first category "batch" to find correct word
            duplicates = mDuplicateEntities.categoryDuplicates.remove(0);
        } else if (!mDuplicateEntities.branchDuplicates.isEmpty()) {
            is_categories[0] = false;
            // "pick-off" the first branch "batch" to find correct word
            duplicates = mDuplicateEntities.branchDuplicates.remove(0);
        } else {
            found_duplicates = false;
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
            dialog.show(getSupportFragmentManager(), null);
        } else {
            // This means we've gone through all the categories and branches,
            // time to do the actual importing
            new ApplyImportOperationsTask(
                    mReader,
                    mColumnMapping,
                    mDuplicateEntities,
                    mDidChooseBranchForImporting,
                    mChosenBranchId,
                    this,
                    this).execute();
        }
    }

    @Override
    public void duplicateSearchFinished(DuplicateEntities duplicateEntities) {
        mImportState = IMPORT_DISPLAY_REPLACEMENT;

        mDuplicateEntities = duplicateEntities;

        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void importSuccessful() {
        mImportState = IMPORT_SUCCESS;
        if (mDidResume) {
            showImportUpdates();
        }
    }

    @Override
    public void importError(String msg) {
        mErrorMsg = msg;
        mImportState = IMPORT_ERROR;
        if (mDidResume) {
            showImportUpdates();
        }
    }
}
