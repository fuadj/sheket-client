package com.mukera.sheket.client.controller.importer;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fuad on 6/9/16.
 *
 * This task goes through the main-ui and a worker thread twice.
 * The lifecycle looks like:
 *
 *          Main Thread                     Worker Thread
 *
 *          MainActivity starts this --->
 *
 *                                          parseFile on worker         -----(1st-stage)
 *
 *          Display ImporterDialog  <----
 *              to user
 *                                  ---->   add data to ContentProvider -----(2nd-stage)
 *
 *          notify main thread on   <----
 *              task finish
 */
public class ImportTask extends AsyncTask<Void, Void, SimpleCSVReader> {
    private SimpleCSVReader mReader;

    private AppCompatActivity mActivity;
    private File mFile;

    public interface ImportListener {
        void importSuccessful();
        void importError(String msg);
        void showImportOptionsDialog(SimpleCSVReader reader);
    }

    private ImportListener mListener;
    public void setListener(ImportListener listener) { mListener = listener; }

    public ImportTask(File file, AppCompatActivity activity) {
        mActivity = activity;
        mFile = file;
    }

    @Override
    protected SimpleCSVReader doInBackground(Void... params) {
        mReader = new SimpleCSVReader(mFile);
        mReader.parseCSV();
        return mReader;
    }

    @Override
    protected void onPostExecute(SimpleCSVReader reader) {
        if (!reader.parsingSuccess()) {
            mListener.importError("Import Error: " + reader.getErrorMessage());
        } else {
            mListener.showImportOptionsDialog(reader);
        }

    }

    public static class ParseResult {
        boolean parsingSuccessful;
        SimpleCSVReader reader;
        Map<Integer, Integer> dataMapping;
    }

    public static class ImportDataTask extends AsyncTask<Void, Void, Pair<Boolean, String>> {
        private SimpleCSVReader mReader;
        private Map<Integer, Integer> mDataMapping;
        private ImportListener mListener;
        private AppCompatActivity mActivity;

        public ImportDataTask(SimpleCSVReader reader, Map<Integer, Integer> mapping, ImportListener listener,
                              AppCompatActivity activity) {
            mReader = reader;
            mDataMapping = mapping;
            mListener = listener;
            mActivity = activity;
        }

        @Override
        protected Pair<Boolean, String> doInBackground(Void... params) {
            ImportData importData = new ImportData();
            importData.mCategoryIds = new HashMap<>();
            importData.mItemIds = new HashMap<>();
            importData.mBranchIds = new HashMap<>();

            importData.operationsList = new ArrayList<>();
            importData.company_id = PrefUtil.getCurrentCompanyId(mActivity);

            if (mDataMapping.get(ImporterDialog.DATA_CATEGORY) !=
                    ImporterDialog.NO_DATA_FOUND) {
                importCategories(importData);
            }

            if (mDataMapping.get(ImporterDialog.DATA_ITEM_CODE) !=
                    ImporterDialog.NO_DATA_FOUND) {
                importItems(importData);
            }

            if (mDataMapping.get(ImporterDialog.DATA_LOCATION) !=
                    ImporterDialog.NO_DATA_FOUND) {
                importBranches(importData);
            }

            if (mDataMapping.get(ImporterDialog.DATA_BALANCE) !=
                    ImporterDialog.NO_DATA_FOUND) {
                addItemsToBranches(importData);
            }

            try {
                mActivity.getContentResolver().
                        applyBatch(SheketContract.CONTENT_AUTHORITY, importData.operationsList);
                return new Pair<>(Boolean.TRUE, null);
            } catch (OperationApplicationException | RemoteException e) {
                return new Pair<>(Boolean.FALSE, e.getMessage());
            }
        }

        @Override
        protected void onPostExecute(Pair<Boolean, String> result) {
            if (result.first == true) mListener.importSuccessful();
            else mListener.importError(result.second);
        }

        Cursor _query(Uri uri, String[] projection) {
            return mActivity.getContentResolver().query(uri,
                    projection,
                    null, null, null);
        }

        /**
         * All keys in the maps should pass through this!!!
         */
        String _to_key(String s) { return s.toLowerCase(); }

        void _queryCategories(ImportData importData) {
            Cursor cursor = _query(CategoryEntry.buildBaseUri(importData.company_id), SCategory.CATEGORY_COLUMNS);

            // fetch categories that already exist, then we will check if it is duplicate
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SCategory category = new SCategory(cursor);
                    _import_category _c = new _import_category();
                    _c.is_new = false;
                    _c.previousCategory = category;

                    importData.mCategoryIds.put(_to_key(category.name), _c);
                } while (cursor.moveToNext());
            }
            if (cursor != null)
                cursor.close();
        }

        void importCategories(ImportData importData) {
            _queryCategories(importData);

            int category_col = mDataMapping.get(ImporterDialog.DATA_CATEGORY);

            for (int i = 0; i < mReader.getNumRows(); i++) {
                String name = mReader.getRowAt(i).get(category_col);

                if (importData.mCategoryIds.get(_to_key(name)) != null) {
                    continue;       // we've already seen it
                }

                // this is a new category, add it
                long new_category_id = PrefUtil.getNewCategoryId(mActivity);
                PrefUtil.setNewCategoryId(mActivity, new_category_id);

                _import_category _c = new _import_category();
                _c.new_id = new_category_id;
                _c.is_new = true;
                importData.mCategoryIds.put(_to_key(name), _c);

                ContentValues values = new ContentValues();

                values.put(CategoryEntry.COLUMN_CATEGORY_ID, new_category_id);
                values.put(CategoryEntry.COLUMN_NAME, name);
                values.put(CategoryEntry.COLUMN_PARENT_ID, CategoryEntry.ROOT_CATEGORY_ID);
                values.put(CategoryEntry.COLUMN_COMPANY_ID, importData.company_id);

                values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(CategoryEntry.buildBaseUri(importData.company_id)).
                                withValues(values).build());
            }
        }

        void _queryItems(ImportData importData) {
            Cursor cursor = _query(ItemEntry.buildBaseUri(importData.company_id), SItem.ITEM_COLUMNS);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SItem item = new SItem(cursor);

                    _import_item _i = new _import_item();
                    _i.is_new = false;
                    _i.previousItem = item;

                    importData.mItemIds.put(_to_key(item.manual_code), _i);
                } while (cursor.moveToNext());
            }
            if (cursor != null)
                cursor.close();
        }

        void importItems(ImportData importData) {
            _queryItems(importData);

            int item_code_col = mDataMapping.get(ImporterDialog.DATA_ITEM_CODE);
            boolean has_item_name =
                    mDataMapping.get(ImporterDialog.DATA_ITEM_NAME) != ImporterDialog.NO_DATA_FOUND;
            int item_name_col = -1;
            if (has_item_name)
                item_name_col = mDataMapping.get(ImporterDialog.DATA_ITEM_NAME);

            boolean has_categories = mDataMapping.get(ImporterDialog.DATA_CATEGORY) !=
                    ImporterDialog.NO_DATA_FOUND;
            int col_categories = -1;
            if (has_categories)
                col_categories = mDataMapping.get(ImporterDialog.DATA_CATEGORY);

            for (int i = 0; i < mReader.getNumRows(); i++) {
                String code = mReader.getRowAt(i).get(item_code_col);
                String name;

                if (importData.mItemIds.get(_to_key(code)) != null) {
                    continue;
                }

                if (has_item_name) {
                    name = mReader.getRowAt(i).get(item_name_col);
                } else {
                    name = code;
                    code = "";
                }

                long new_item_id = PrefUtil.getNewItemId(mActivity);
                PrefUtil.setNewItemId(mActivity, new_item_id);

                _import_item _i = new _import_item();
                _i.new_id = new_item_id;
                _i.is_new = true;
                importData.mItemIds.put(_to_key(mReader.getRowAt(i).get(item_code_col)), _i);

                ContentValues values = new ContentValues();
                values.put(ItemEntry.COLUMN_ITEM_ID, new_item_id);
                values.put(ItemEntry.COLUMN_NAME, name);
                values.put(ItemEntry.COLUMN_MANUAL_CODE, code);

                long category_id = CategoryEntry.ROOT_CATEGORY_ID;
                if (has_categories) {
                    String category_name = mReader.getRowAt(i).get(col_categories);
                    _import_category _c = importData.mCategoryIds.get(_to_key(category_name));
                    if (_c.is_new) {
                        category_id = _c.new_id;
                    } else {
                        category_id = _c.previousCategory.category_id;
                    }
                }
                values.put(ItemEntry.COLUMN_CATEGORY_ID, category_id);
                values.put(ItemEntry.COLUMN_COMPANY_ID, importData.company_id);

                // PCS is the DEFAULT unit of measurement
                values.put(ItemEntry.COLUMN_UNIT_OF_MEASUREMENT, UnitsOfMeasurement.UNIT_PCS);
                values.put(ItemEntry.COLUMN_HAS_DERIVED_UNIT, SheketContract.FALSE);

                values.put(ItemEntry.COLUMN_HAS_BAR_CODE, SheketContract.FALSE);

                values.put(UUIDSyncable.COLUMN_UUID,
                        UUID.randomUUID().toString());
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(ItemEntry.buildBaseUri(importData.company_id)).
                                withValues(values).build());
            }
        }

        void _queryBranches(ImportData importData) {
            Cursor cursor = _query(BranchEntry.buildBaseUri(importData.company_id), SBranch.BRANCH_COLUMNS);

            // fetch categories that already exist, then we will check if it is duplicate
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SBranch branch = new SBranch(cursor);
                    _import_branch _b = new _import_branch();
                    _b.is_new = false;
                    _b.previousBranch = branch;

                    importData.mBranchIds.put(_to_key(branch.branch_name), _b);
                } while (cursor.moveToNext());
            }
            if (cursor != null)
                cursor.close();
        }

        void importBranches(ImportData importData) {
            _queryBranches(importData);

            int branch_col = mDataMapping.get(ImporterDialog.DATA_LOCATION);

            for (int i = 0; i < mReader.getNumRows(); i++) {
                String name = mReader.getRowAt(i).get(branch_col);

                if (importData.mBranchIds.get(_to_key(name)) != null) {
                    continue;
                }

                long new_branch_id = PrefUtil.getNewBranchId(mActivity);
                PrefUtil.setNewBranchId(mActivity, new_branch_id);

                _import_branch _b = new _import_branch();
                _b.new_id = new_branch_id;
                _b.is_new = true;
                importData.mBranchIds.put(_to_key(name), _b);

                ContentValues values = new ContentValues();

                values.put(BranchEntry.COLUMN_NAME, name);
                values.put(BranchEntry.COLUMN_BRANCH_ID, new_branch_id);
                values.put(BranchEntry.COLUMN_COMPANY_ID, importData.company_id);

                values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(BranchEntry.buildBaseUri(importData.company_id)).
                                withValues(values).build());
            }
        }

        void addItemsToBranches(ImportData importData) {
            boolean has_branches = mDataMapping.get(ImporterDialog.DATA_LOCATION) != ImporterDialog.NO_DATA_FOUND;
            boolean has_quantity = mDataMapping.get(ImporterDialog.DATA_BALANCE) != ImporterDialog.NO_DATA_FOUND;

            int col_code = has_branches ? mDataMapping.get(ImporterDialog.DATA_ITEM_CODE) : -1;
            int col_branch = has_branches ? mDataMapping.get(ImporterDialog.DATA_LOCATION) : -1;
            int col_quantity = has_branches ? mDataMapping.get(ImporterDialog.DATA_BALANCE) : -1;

            Pattern number_extractor = Pattern.compile("\\d+(?:[.]\\d+)*");

            // we need both branches and quantity declared to do stuff
            if (!has_branches || !has_quantity) return;

            long company_id = importData.company_id;
            long user_id = PrefUtil.getUserId(mActivity);

            for (int i = 0; i < mReader.getNumRows(); i++) {
                long item_id, branch_id;

                String item_code = _to_key(mReader.getRowAt(i).get(col_code));
                String branch_name = _to_key(mReader.getRowAt(i).get(col_branch));

                _import_item _i = importData.mItemIds.get(item_code);
                _import_branch _b = importData.mBranchIds.get(branch_name);

                if (_i == null || _b == null)
                    return;     // this is an error

                item_id = _i.is_new ? _i.new_id : _i.previousItem.item_id;
                branch_id = _b.is_new ? _b.new_id : _b.previousBranch.branch_id;

                double quantity = 0;
                String string_qty = mReader.getRowAt(i).get(col_quantity);
                Matcher matcher = number_extractor.matcher(string_qty);
                if (matcher.find()) {
                    quantity = Double.parseDouble(matcher.group(0));
                }

                long new_trans_id = PrefUtil.getNewTransId(mActivity);
                PrefUtil.setNewTransId(mActivity, new_trans_id);

                ContentValues values = new ContentValues();
                values.put(TransactionEntry.COLUMN_TRANS_ID, new_trans_id);
                values.put(TransactionEntry.COLUMN_BRANCH_ID, branch_id);
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_CREATED);
                values.put(TransactionEntry.COLUMN_COMPANY_ID, company_id);
                values.put(TransactionEntry.COLUMN_USER_ID, user_id);
                values.put(TransactionEntry.COLUMN_DATE, System.currentTimeMillis());

                values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(importData.company_id)).
                                withValues(values).build());

                ContentValues transItemValues = new ContentValues();
                transItemValues.put(TransItemEntry.COLUMN_COMPANY_ID, company_id);
                transItemValues.put(TransItemEntry.COLUMN_ITEM_ID, item_id);
                transItemValues.put(TransItemEntry.COLUMN_OTHER_BRANCH_ID, BranchEntry.DUMMY_BRANCH_ID);
                transItemValues.put(TransItemEntry.COLUMN_QTY, quantity);
                transItemValues.put(TransItemEntry.COLUMN_TRANSACTION_ID, new_trans_id);
                transItemValues.put(TransItemEntry.COLUMN_TRANSACTION_TYPE, TransItemEntry.TYPE_INCREASE_PURCHASE);
                transItemValues.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(TransItemEntry.buildBaseUri(importData.company_id)).
                                withValues(transItemValues).build());

                ContentValues branchItemValues = new ContentValues();
                branchItemValues.put(BranchItemEntry.COLUMN_COMPANY_ID, company_id);
                branchItemValues.put(BranchItemEntry.COLUMN_BRANCH_ID, branch_id);
                branchItemValues.put(BranchItemEntry.COLUMN_ITEM_ID, item_id);
                branchItemValues.put(BranchItemEntry.COLUMN_QUANTITY, quantity);
                branchItemValues.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(BranchItemEntry.buildBaseUri(importData.company_id)).
                                withValues(branchItemValues).build());
            }
        }

        class ImportData {
            Map<String, _import_category> mCategoryIds;
            Map<String, _import_item> mItemIds;
            Map<String, _import_branch> mBranchIds;
            long company_id;

            ArrayList<ContentProviderOperation> operationsList;
        }

        static class _import_category {
            long new_id;
            boolean is_new;
            SCategory previousCategory;
        }

        static class _import_item {
            long new_id;
            boolean is_new;
            SItem previousItem;
        }

        static class _import_branch {
            long new_id;
            boolean is_new;
            SBranch previousBranch;
        }
    }
}
