package com.mukera.sheket.client.controller.importer;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.UnitsOfMeasurement;
import com.mukera.sheket.client.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by fuad on 6/26/16.
 */
public class ApplyImportOperationsTask extends AsyncTask<Void, Void, Pair<Boolean, String>> {
    private SimpleCSVReader mReader;
    private Map<Integer, Integer> mDataMapping;
    private DuplicateEntities mDuplicateEntities;
    private ImportListener mListener;
    private Context mContext;

    public interface ImportListener {
        void importSuccessful();

        void importError(String msg);
    }

    public ApplyImportOperationsTask(SimpleCSVReader reader,
                                     Map<Integer, Integer> mapping,
                                     DuplicateEntities duplicateEntities,
                                     ImportListener listener,
                                     Context context) {
        mReader = reader;
        mDataMapping = mapping;
        mDuplicateEntities = duplicateEntities;
        mListener = listener;
        mContext = context;
    }

    @Override
    protected Pair<Boolean, String> doInBackground(Void... params) {
        ImportData importData = new ImportData();
        importData.mCategoryIds = new HashMap<>();
        importData.mItemIds = new HashMap<>();
        importData.mBranchIds = new HashMap<>();

        importData.operationsList = new ArrayList<>();
        importData.company_id = PrefUtil.getCurrentCompanyId(mContext);

        if (mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            importCategories(importData);
        }

        if (mDataMapping.get(ColumnMappingDialog.DATA_ITEM_NAME) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            importItems(importData);
        }

        if (mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            importBranches(importData);
        }

        if (mDataMapping.get(ColumnMappingDialog.DATA_QUANTITY) !=
                ColumnMappingDialog.NO_DATA_FOUND) {
            addItemsToBranches(importData);
        }

        // If both branch and categories are defined, add the category to the branch
        if ((mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) !=
                ColumnMappingDialog.NO_DATA_FOUND) &&
                (mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY) !=
                        ColumnMappingDialog.NO_DATA_FOUND)) {
            addCategoriesToBranches(importData);
        }


        try {
            mContext.getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, importData.operationsList);
            return new Pair<>(Boolean.TRUE, null);
        } catch (OperationApplicationException | RemoteException e) {
            return new Pair<>(Boolean.FALSE, e.getMessage());
        }
    }

    @Override
    protected void onPostExecute(Pair<Boolean, String> result) {
        if (result.first) {
            mListener.importSuccessful();
        } else {
            mListener.importError(result.second);
        }
    }

    /**
     * If the name was a duplicate, find the correct replacement chosen by the user.
     * Otherwise, just return it.
     */
    String replaceCategoryNameIfDuplicate(String name) {
        if (mDuplicateEntities.categoryReplacement.containsKey(name))
            return mDuplicateEntities.categoryReplacement.get(name);
        return name;
    }

    String replaceBranchNameIfDuplicate(String name) {
        if (mDuplicateEntities.branchReplacement.containsKey(name))
            return mDuplicateEntities.branchReplacement.get(name);
        return name;
    }

    Cursor _query(Uri uri, String[] projection) {
        return mContext.getContentResolver().query(uri,
                projection,
                null, null, null);
    }

    /**
     * All keys in the maps should pass through this!!!
     */
    String _to_key(String s) {
        return s.toLowerCase();
    }

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

        int category_col = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY);

        for (int i = 0; i < mReader.getNumRows(); i++) {
            String name = mReader.getRowAt(i).get(category_col);

            name = replaceCategoryNameIfDuplicate(name);

            // if the category name is empty, ignore it
            // AND if we've already added it, don't add it again
            if (TextUtils.isEmpty(name) ||
                    importData.mCategoryIds.get(_to_key(name)) != null) {
                continue;
            }

            // this is a new category, add it
            int new_category_id = PrefUtil.getNewCategoryId(mContext);
            PrefUtil.setNewCategoryId(mContext, new_category_id);

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

                importData.mItemIds.put(_to_key(item.item_code), _i);
            } while (cursor.moveToNext());
        }
        if (cursor != null)
            cursor.close();
    }

    void importItems(ImportData importData) {
        _queryItems(importData);

        int item_name_col = mDataMapping.get(ColumnMappingDialog.DATA_ITEM_NAME);
        boolean has_item_code =
                mDataMapping.get(ColumnMappingDialog.DATA_ITEM_CODE) != ColumnMappingDialog.NO_DATA_FOUND;
        int item_code_col = -1;
        if (has_item_code)
            item_code_col = mDataMapping.get(ColumnMappingDialog.DATA_ITEM_CODE);

        boolean has_categories = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY) !=
                ColumnMappingDialog.NO_DATA_FOUND;
        int col_categories = -1;
        if (has_categories)
            col_categories = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY);

        for (int i = 0; i < mReader.getNumRows(); i++) {
            String name = mReader.getRowAt(i).get(item_name_col);
            String code = "";

            if (importData.mItemIds.get(_to_key(name)) != null) {
                continue;
            }

            if (has_item_code) {
                code = mReader.getRowAt(i).get(item_code_col);
            }

            int new_item_id = PrefUtil.getNewItemId(mContext);
            PrefUtil.setNewItemId(mContext, new_item_id);

            _import_item _i = new _import_item();
            _i.new_id = new_item_id;
            _i.is_new = true;
            importData.mItemIds.put(_to_key(mReader.getRowAt(i).get(item_name_col)), _i);

            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_ID, new_item_id);
            values.put(ItemEntry.COLUMN_NAME, name);
            values.put(ItemEntry.COLUMN_ITEM_CODE, code);

            int category_id = CategoryEntry.ROOT_CATEGORY_ID;
            if (has_categories) {
                String category_name = mReader.getRowAt(i).get(col_categories);

                category_name = replaceCategoryNameIfDuplicate(category_name);

                // if the category was empty, just add it to the ROOT
                if (!TextUtils.isEmpty(category_name)) {
                    _import_category _c = importData.mCategoryIds.get(_to_key(category_name));
                    if (_c.is_new) {
                        category_id = _c.new_id;
                    } else {
                        category_id = _c.previousCategory.category_id;
                    }
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

        int branch_col = mDataMapping.get(ColumnMappingDialog.DATA_BRANCH);

        for (int i = 0; i < mReader.getNumRows(); i++) {
            String name = mReader.getRowAt(i).get(branch_col);

            name = replaceBranchNameIfDuplicate(name);

            if (importData.mBranchIds.get(_to_key(name)) != null) {
                continue;
            }

            int new_branch_id = PrefUtil.getNewBranchId(mContext);
            PrefUtil.setNewBranchId(mContext, new_branch_id);

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
        boolean has_branches = mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) != ColumnMappingDialog.NO_DATA_FOUND;
        boolean has_quantity = mDataMapping.get(ColumnMappingDialog.DATA_QUANTITY) != ColumnMappingDialog.NO_DATA_FOUND;

        int col_name = has_branches ? mDataMapping.get(ColumnMappingDialog.DATA_ITEM_NAME) : -1;
        int col_branch = has_branches ? mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) : -1;
        int col_quantity = has_branches ? mDataMapping.get(ColumnMappingDialog.DATA_QUANTITY) : -1;

        // we need both branches and quantity declared to do stuff
        if (!has_branches || !has_quantity) return;

        int company_id = importData.company_id;
        int user_id = PrefUtil.getUserId(mContext);

        /**
         * We want to group imports pertaining to a specific branch into a single transactions.
         * This is done by only creating a new transaction for unseen(new) branches.
         */
        Map<Integer, Integer> seenBranches = new HashMap<>();

        for (int i = 0; i < mReader.getNumRows(); i++) {
            int item_id, branch_id;

            String item_name = _to_key(mReader.getRowAt(i).get(col_name));
            String branch_name = _to_key(replaceBranchNameIfDuplicate(mReader.getRowAt(i).get(col_branch)));

            _import_item _i = importData.mItemIds.get(item_name);
            _import_branch _b = importData.mBranchIds.get(branch_name);

            if (_i == null || _b == null)
                continue;       // TODO: maybe signal an error

            item_id = _i.is_new ? _i.new_id : _i.previousItem.item_id;
            branch_id = _b.is_new ? _b.new_id : _b.previousBranch.branch_id;

            String string_qty = mReader.getRowAt(i).get(col_quantity);
            double quantity = Utils.extractDoubleFromString(string_qty);

            int transaction_id;
            if (seenBranches.containsKey(branch_id)) {
                transaction_id = seenBranches.get(branch_id);
            } else {
                transaction_id = (int) PrefUtil.getNewTransId(mContext);
                PrefUtil.setNewTransId(mContext, transaction_id);

                seenBranches.put(branch_id, transaction_id);

                ContentValues values = new ContentValues();
                values.put(TransactionEntry.COLUMN_TRANS_ID, transaction_id);
                values.put(TransactionEntry.COLUMN_BRANCH_ID, branch_id);
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_CREATED);
                values.put(TransactionEntry.COLUMN_COMPANY_ID, company_id);
                values.put(TransactionEntry.COLUMN_USER_ID, user_id);
                values.put(TransactionEntry.COLUMN_DATE, SheketContract.getDbDateInteger(new Date()));

                values.put(UUIDSyncable.COLUMN_UUID, UUID.randomUUID().toString());
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                        ChangeTraceable.CHANGE_STATUS_CREATED);

                importData.operationsList.add(
                        ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(importData.company_id)).
                                withValues(values).build());
            }

            ContentValues transItemValues = new ContentValues();
            transItemValues.put(TransItemEntry.COLUMN_COMPANY_ID, company_id);
            transItemValues.put(TransItemEntry.COLUMN_ITEM_ID, item_id);
            transItemValues.put(TransItemEntry.COLUMN_OTHER_BRANCH_ID, BranchEntry.DUMMY_BRANCH_ID);
            transItemValues.put(TransItemEntry.COLUMN_QTY, quantity);
            transItemValues.put(TransItemEntry.COLUMN_TRANSACTION_ID, transaction_id);
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

    void addCategoriesToBranches(ImportData importData) {
        boolean has_branches = mDataMapping.get(ColumnMappingDialog.DATA_BRANCH) != ColumnMappingDialog.NO_DATA_FOUND;
        boolean has_categories = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY) != ColumnMappingDialog.NO_DATA_FOUND;

        if (!has_branches || !has_categories) return;

        int col_branch = mDataMapping.get(ColumnMappingDialog.DATA_BRANCH);
        int col_category = mDataMapping.get(ColumnMappingDialog.DATA_CATEGORY);

        int company_id = importData.company_id;

        Map<Integer, Set<Integer>> seenBranchCategories = new HashMap<>();
        for (int i = 0; i < mReader.getNumRows(); i++) {
            String branch_name = replaceBranchNameIfDuplicate(
                    mReader.getRowAt(i).get(col_branch)).trim();
            String category_name = replaceCategoryNameIfDuplicate(
                    mReader.getRowAt(i).get(col_category)).trim();

            if (TextUtils.isEmpty(branch_name) ||
                    TextUtils.isEmpty(category_name))
                continue;

            _import_branch _b = importData.mBranchIds.get(
                    _to_key(branch_name));
            _import_category _c = importData.mCategoryIds.get(
                    _to_key(category_name));
            int branch_id = _b.is_new ? _b.new_id : _b.previousBranch.branch_id;
            int category_id = _c.is_new ? _c.new_id : _c.previousCategory.category_id;

            if (!seenBranchCategories.containsKey(branch_id))
                seenBranchCategories.put(branch_id, new HashSet<Integer>());

            if (seenBranchCategories.get(branch_id).contains(category_id))
                // we've already added the category to the branch
                continue;

            seenBranchCategories.get(branch_id).add(category_id);

            ContentValues values = new ContentValues();
            values.put(BranchCategoryEntry.COLUMN_COMPANY_ID, company_id);
            values.put(BranchCategoryEntry.COLUMN_BRANCH_ID, branch_id);
            values.put(BranchCategoryEntry.COLUMN_CATEGORY_ID, category_id);
            values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR,
                    ChangeTraceable.CHANGE_STATUS_CREATED);
            /**
             * Because we've got an "ON CONFLICT IGNORE" policy on branch categories,
             * there is no worry of possible duplicates or erasing data.
             */
            importData.operationsList.add(
                    ContentProviderOperation.newInsert(
                            BranchCategoryEntry.buildBaseUri(company_id)).
                            withValues(values).build());
        }
    }

    class ImportData {
        Map<String, _import_category> mCategoryIds;
        Map<String, _import_item> mItemIds;
        Map<String, _import_branch> mBranchIds;
        int company_id;

        ArrayList<ContentProviderOperation> operationsList;
    }

    class _import_category {
        int new_id;
        boolean is_new;
        SCategory previousCategory;
    }

    class _import_item {
        int new_id;
        boolean is_new;
        SItem previousItem;
    }

    class _import_branch {
        int new_id;
        boolean is_new;
        SBranch previousBranch;
    }
}

