package com.mukera.sheket.client.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.BranchEntry;
import com.mukera.sheket.client.data.SheketContract.BranchItemEntry;
import com.mukera.sheket.client.data.SheketContract.ChangeTraceable;
import com.mukera.sheket.client.data.SheketContract.ItemEntry;
import com.mukera.sheket.client.data.SheketContract.TransItemEntry;
import com.mukera.sheket.client.data.SheketContract.TransactionEntry;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.PrefUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gamma on 3/28/16.
 */
public class SheketSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = SheketSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in milliseconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    public static final OkHttpClient client = new OkHttpClient();

    public SheketSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        boolean continue_sync = syncUser();
        if (!continue_sync) {
            return;
        }
        syncEntities();
        syncTransactions();
    }

    boolean syncUser() {
        try {
            Log.d(LOG_TAG, "Syncing User started");
            Request.Builder builder = new Request.Builder();
            builder.url(ConfigData.getAddress(getContext()) + "syncuser");
            JSONObject json = new JSONObject();
            json.put(getContext().getString(R.string.sync_json_user_rev),
                    PrefUtil.getUserRevision(getContext()));
            builder.addHeader(getContext().getString(R.string.pref_request_key_cookie),
                    PrefUtil.getLoginCookie(getContext()));
            builder.post(RequestBody.create(MediaType.parse("application/json"),
                    json.toString()));

            Response response = client.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                // todo: check if we need to parse out the error part!
                throw new SyncException("error response");
            }

            JSONObject result = new JSONObject(response.body().string());
            boolean rev_changed = result.getBoolean(
                    getResourceString(R.string.sync_json_user_revision_changed));
            if (!rev_changed) {
                return true;
            }

            ArrayList<ContentProviderOperation> operations = new ArrayList<>();

            int new_user_rev = result.getInt(
                    getResourceString(R.string.sync_json_user_rev));

            final String USER_JSON_COMPANY_ID = "company_id";
            final String USER_JSON_COMPANY_NAME = "company_name";
            final String USER_JSON_COMPANY_PERMISSION = "permission";

            JSONArray companyArr = result.getJSONArray(
                    getResourceString(R.string.sync_json_companies));
            for (int i = 0; i < companyArr.length(); i++) {
                JSONObject companyObj = companyArr.getJSONObject(i);

                long company_id = companyObj.getLong(USER_JSON_COMPANY_ID);
                String company_name = companyObj.getString(USER_JSON_COMPANY_NAME);
                String permission = companyObj.getString(USER_JSON_COMPANY_PERMISSION);

                ContentValues values = new ContentValues();
                values.put(SheketContract.CompanyEntry.COLUMN_ID, company_id);
                values.put(SheketContract.CompanyEntry.COLUMN_NAME, company_name);
                values.put(SheketContract.CompanyEntry.COLUMN_PERMISSION, permission);

                operations.add(ContentProviderOperation.newInsert(SheketContract.CompanyEntry.CONTENT_URI).
                        withValues(DbUtil.setUpdateOnConflict(values)).build());
            }

            getContext().getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operations);

            PrefUtil.setUserRevision(getContext(), new_user_rev);
        } catch (JSONException | IOException |
                RemoteException | OperationApplicationException | SyncException e) {
            Log.w(LOG_TAG, e.getMessage());
            return false;
        }

        return true;
    }

    void parseUserSyncResponse(String response) throws JSONException {

    }

    /**
     * Sync elements like { item | branch | branch-item ...}
     * This prepares the way for the transactions to sync, since
     * it depends on these elements having a "defined" state.
     *
     */
    boolean syncEntities() {
        try {
            Log.d(LOG_TAG, "Syncing Entity started");
            Request.Builder builder = new Request.Builder();
            builder.url(ConfigData.getAddress(getContext()) + "v1/sync/entity");
            JSONObject json = createEntitySyncJSON();
            builder.addHeader(getContext().getString(R.string.pref_header_key_company_id),
                    Long.toString(PrefUtil.getCurrentCompanyId(getContext())));
            builder.addHeader(getContext().getString(R.string.pref_request_key_cookie),
                    PrefUtil.getLoginCookie(getContext()));
            builder.post(RequestBody.create(MediaType.parse("application/json"),
                    json.toString()));

            Response response = client.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                // todo: check if we need to parse out the error part!
                throw new SyncException("error response");
            }

            EntitySyncResponse result = parseEntitySyncResponse(response.body().string());
            applyEntitySync(result);
        } catch (JSONException | IOException | SyncException e) {
            Log.w(LOG_TAG, e.getMessage());
            return false;
        }

        return true;
    }

    ContentValues setChangeStatus(ContentValues values, int change_status) {
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, change_status);
        return values;
    }

    void applyEntitySync(EntitySyncResponse response) throws SyncException {
        long local_max_item_id = PrefUtil.getNewItemId(getContext());
        long local_max_branch_id = PrefUtil.getNewBranchId(getContext());

        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        for (SyncUpdatedElement updated_item : response.updatedItemIds) {
            if (local_max_item_id < updated_item.newId) {
                local_max_item_id = updated_item.newId;
            }

            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_ID, updated_item.newId);
            setChangeStatus(values, ChangeTraceable.CHANGE_STATUS_UPDATED);
            operationList.add(ContentProviderOperation.newUpdate(ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext()))).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", ItemEntry.COLUMN_ITEM_ID),
                            new String[]{Long.toString(updated_item.oldId)}).
                    build());
        }

        for (SyncUpdatedElement updated_branch : response.updatedBranchIds) {
            if (local_max_branch_id < updated_branch.newId) {
                local_max_branch_id = updated_branch.newId;
            }

            ContentValues values = new ContentValues();
            values.put(BranchEntry.COLUMN_BRANCH_ID, updated_branch.newId);
            setChangeStatus(values, ChangeTraceable.CHANGE_STATUS_UPDATED);
            operationList.add(ContentProviderOperation.newUpdate(BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext()))).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", BranchEntry.COLUMN_BRANCH_ID),
                            new String[]{Long.toString(updated_branch.oldId)}).
                    build());
        }

        for (SItem sync_item : response.syncedItems) {
            operationList.add(ContentProviderOperation.newInsert(ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext()))).
                    withValues(
                            setChangeStatus(DbUtil.setUpdateOnConflict(sync_item.toContentValues()),
                                    ChangeTraceable.CHANGE_STATUS_SYNCED)).
                    build());
        }

        for (SBranch sync_branch : response.syncedBranches) {
            operationList.add(ContentProviderOperation.newInsert(BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext()))).
                    withValues(
                            setChangeStatus(DbUtil.setUpdateOnConflict(sync_branch.toContentValues()),
                                    ChangeTraceable.CHANGE_STATUS_SYNCED)).
                    build());
        }

        try {
            getContext().getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);
        } catch (OperationApplicationException | RemoteException e) {
            throw new SyncException(e);
        }
    }

    String getResourceString(int resId) {
        return getContext().getString(resId);
    }

    EntitySyncResponse parseEntitySyncResponse(String server_response) throws JSONException {
        EntitySyncResponse result = new EntitySyncResponse();
        JSONObject rootJson = new JSONObject(server_response);

        result.company_id = rootJson.getLong(getResourceString(R.string.sync_json_company_id));
        result.latest_item_rev = rootJson.getLong(getResourceString(R.string.sync_json_item_rev));
        result.latest_branch_rev = rootJson.getLong(getResourceString(R.string.sync_json_branch_rev));

        result.updatedItemIds = new ArrayList<>();
        result.updatedBranchIds = new ArrayList<>();
        result.syncedItems = new ArrayList<>();
        result.syncedBranches = new ArrayList<>();

        if (rootJson.has(getResourceString(R.string.sync_json_updated_item_ids))) {
            JSONArray updatedArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_updated_item_ids));
            for (int i = 0; i < updatedArray.length(); i++) {
                result.updatedItemIds.add(new SyncUpdatedElement(updatedArray.getJSONObject(i), result.company_id));
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_updated_branch_ids))) {
            JSONArray updatedArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_updated_branch_ids));
            for (int i = 0; i < updatedArray.length(); i++) {
                result.updatedBranchIds.add(new SyncUpdatedElement(updatedArray.getJSONObject(i), result.company_id));
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_items))) {
            JSONArray itemArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_items));


            for (int i = 0; i < itemArray.length(); i++) {
                JSONObject object = itemArray.getJSONObject(i);

                SItem item = new SItem();
                item.item_id = object.getLong(SItem.JSON_ITEM_ID);
                item.model_year = object.getString(SItem.JSON_MODEL_YEAR);
                item.part_number = object.getString(SItem.JSON_PART_NUMBER);
                item.bar_code = object.getString(SItem.JSON_BAR_CODE);
                item.item_code = object.getString(SItem.JSON_ITEM_CODE);
                item.has_bar_code = object.getBoolean(SItem.JSON_HAS_BAR_CODE);
                item.company_id = result.company_id;

                result.syncedItems.add(item);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_branches))) {
            JSONArray branchArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_branches));

            for (int i = 0; i < branchArray.length(); i++) {
                JSONObject object = branchArray.getJSONObject(i);

                SBranch branch = new SBranch();
                branch.branch_id = object.getLong(SBranch.JSON_BRANCH_ID);
                branch.branch_name = object.getString(SBranch.JSON_NAME);
                branch.branch_location = object.getString(SBranch.JSON_LOCATION);
                branch.company_id = result.company_id;

                result.syncedBranches.add(branch);
            }

        }
        return result;
    }

    /**
     * creates the request body JSON used to sync entities.
     *
     * @return
     * @throws JSONException
     */
    JSONObject createEntitySyncJSON() throws JSONException {
        Pair<Boolean, JSONObject> itemChanges = getItemChanges();
        Pair<Boolean, JSONObject> branchChanges = getBranchChanges();
        Pair<Boolean, JSONObject> branchItemChanges = getBranchItemChanges();

        JSONObject syncJson = new JSONObject();
        syncJson.put(getContext().getString(R.string.sync_json_item_rev),
                PrefUtil.getItemRevision(getContext()));
        syncJson.put(getContext().getString(R.string.sync_json_branch_rev),
                PrefUtil.getBranchRevision(getContext()));
        syncJson.put(getContext().getString(R.string.sync_json_branch_item_rev),
                PrefUtil.getBranchItemRevision(getContext()));

        JSONArray types = new JSONArray();

        String item_entity = getContext().getString(R.string.sync_json_entity_type_item);
        String branch_entity = getContext().getString(R.string.sync_json_entity_type_branch);
        String branchItem_entity = getContext().getString(R.string.sync_json_entity_type_branch_item);

        if (itemChanges.first) {
            syncJson.put(item_entity, itemChanges.second);
            types.put(item_entity);
        }
        if (branchChanges.first) {
            syncJson.put(branch_entity, branchChanges.second);
            types.put(branch_entity);
        }
        if (branchItemChanges.first) {
            syncJson.put(branchItem_entity, branchItemChanges.second);
            types.put(branchItem_entity);
        }

        syncJson.put(getContext().getString(R.string.sync_json_entity_types), types);
        return syncJson;
    }

    List<Long> getItemIds(List<SItem> items) {
        List<Long> ids = new ArrayList<>(items.size());
        for (int i = 0; i < items.size(); i++) {
            ids.set(i, items.get(i).item_id);
        }
        return ids;
    }

    String longArrToString(List<Long> longs) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < longs.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(longs.get(i));
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * gathers every changes that happened on items table and returns that's
     * representation in a JSON object. If no changes were found, the Pair's first bool
     * will false.
     */
    Pair<Boolean, JSONObject> getItemChanges() throws JSONException {
        List<SItem> createdItems = new ArrayList<>();
        List<SItem> updatedItems = new ArrayList<>();
        List<SItem> deletedItems = new ArrayList<>();

        String change_selector = String.format("%s != ?", ChangeTraceable.COLUMN_CHANGE_INDICATOR);
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = getContext().getContentResolver().query(
                ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SItem.ITEM_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SItem item = new SItem(cursor);
                switch (item.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdItems.add(item);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedItems.add(item);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedItems.add(item);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdItems.isEmpty() && updatedItems.isEmpty() && deletedItems.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONObject itemsJSON = new JSONObject();

        JSONArray fieldsArray = new JSONArray();

        for (SItem item : createdItems) {
            fieldsArray.put(item.toJsonObject());
        }
        for (SItem item : updatedItems) {
            fieldsArray.put(item.toJsonObject());
        }
        for (SItem item : deletedItems) {
            fieldsArray.put(item.toJsonObject());
        }

        itemsJSON.put(getResourceString(R.string.sync_json_key_create),
                longArrToString(getItemIds(createdItems)));
        itemsJSON.put(getResourceString(R.string.sync_json_key_update),
                longArrToString(getItemIds(updatedItems)));
        itemsJSON.put(getResourceString(R.string.sync_json_key_delete),
                longArrToString(getItemIds(deletedItems)));

        itemsJSON.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);
        return new Pair<>(Boolean.TRUE, itemsJSON);
    }

    List<Long> getBranchIds(List<SBranch> branches) {
        List<Long> ids = new ArrayList<>(branches.size());
        for (int i = 0; i < branches.size(); i++) {
            ids.set(i, branches.get(i).branch_id);
        }
        return ids;
    }

    /**
     * see the {@code getItemChanges()} docs
     *
     * @return
     */
    Pair<Boolean, JSONObject> getBranchChanges() throws JSONException {
        List<SBranch> createdBranches = new ArrayList<>();
        List<SBranch> updatedBranches = new ArrayList<>();
        List<SBranch> deletedBranches = new ArrayList<>();

        String change_selector = String.format("%s != ?", ChangeTraceable.COLUMN_CHANGE_INDICATOR);
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = getContext().getContentResolver().query(
                BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SBranch.BRANCH_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SBranch branch = new SBranch(cursor);
                switch (branch.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdBranches.add(branch);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedBranches.add(branch);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedBranches.add(branch);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdBranches.isEmpty() && updatedBranches.isEmpty() && deletedBranches.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONObject branchesJson = new JSONObject();

        JSONArray fieldsArray = new JSONArray();

        for (SBranch branch : createdBranches) {
            fieldsArray.put(branch.toJsonObject());
        }
        for (SBranch branch : updatedBranches) {
            fieldsArray.put(branch.toJsonObject());
        }
        for (SBranch branch : deletedBranches) {
            fieldsArray.put(branch.toJsonObject());
        }

        branchesJson.put(getResourceString(R.string.sync_json_key_create),
                longArrToString(getBranchIds(createdBranches)));
        branchesJson.put(getResourceString(R.string.sync_json_key_update),
                longArrToString(getBranchIds(updatedBranches)));
        branchesJson.put(getResourceString(R.string.sync_json_key_delete),
                longArrToString(getBranchIds(deletedBranches)));

        branchesJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);
        return new Pair<>(Boolean.TRUE, branchesJson);
    }

    List<String> getBranchItemIds(List<SBranchItem> branchItems) {
        List<String> result = new ArrayList<>(branchItems.size());
        for (SBranchItem branchItem : branchItems) {
            result.add(String.format("%d:%d", branchItem.branch_id, branchItem.item_id));
        }
        return result;
    }

    String stringArrToString(List<String> strings) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        for (int i = 0; i < strings.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(strings.get(i));
        }
        builder.append("]");
        return builder.toString();
    }

    /**
     * Changes in branch items will not include quantity changes, those
     * are only affected by transactions. The other attributes of a branch-item
     * are what is returned in a JSON representation in the Pair's second member.
     *
     * @return
     */
    Pair<Boolean, JSONObject> getBranchItemChanges() throws JSONException {
        List<SBranchItem> createdBranchItems = new ArrayList<>();
        List<SBranchItem> updatedBranchItems = new ArrayList<>();
        List<SBranchItem> deletedBranchItems = new ArrayList<>();

        String change_selector = String.format("%s != ?", ChangeTraceable.COLUMN_CHANGE_INDICATOR);
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = getContext().getContentResolver().query(
                BranchItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SBranchItem.BRANCH_ITEM_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SBranchItem branchItem = new SBranchItem(cursor);
                switch (branchItem.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdBranchItems.add(branchItem);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedBranchItems.add(branchItem);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedBranchItems.add(branchItem);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdBranchItems.isEmpty() && updatedBranchItems.isEmpty() && deletedBranchItems.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONObject branchesItemJson = new JSONObject();

        JSONArray fieldsArray = new JSONArray();

        for (SBranchItem branchItem : createdBranchItems) {
            fieldsArray.put(branchItem.toJsonObject());
        }
        for (SBranchItem branchItem : updatedBranchItems) {
            fieldsArray.put(branchItem.toJsonObject());
        }
        for (SBranchItem branchItem : deletedBranchItems) {
            fieldsArray.put(branchItem.toJsonObject());
        }

        branchesItemJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);

        branchesItemJson.put(getResourceString(R.string.sync_json_key_create),
                stringArrToString(getBranchItemIds(createdBranchItems)));
        branchesItemJson.put(getResourceString(R.string.sync_json_key_update),
                stringArrToString(getBranchItemIds(updatedBranchItems)));
        branchesItemJson.put(getResourceString(R.string.sync_json_key_delete),
                stringArrToString(getBranchItemIds(deletedBranchItems)));

        return new Pair<>(Boolean.TRUE, branchesItemJson);
    }

    void syncTransactions() {
        try {
            JSONObject json = createTransactionSyncJSON();
            Request.Builder builder = new Request.Builder();
            builder.url(ConfigData.getAddress(getContext()) + "v1/sync/transaction");
            builder.addHeader(getContext().getString(R.string.pref_header_key_company_id),
                    Long.toString(PrefUtil.getCurrentCompanyId(getContext())));
            builder.addHeader(getContext().getString(R.string.pref_request_key_cookie),
                    PrefUtil.getLoginCookie(getContext()));
            builder.post(RequestBody.create(MediaType.parse("application/json"),
                    json.toString()));

            Response response = client.newCall(builder.build()).execute();
            if (!response.isSuccessful()) {
                // todo: check if we need to parse out the error part!
                throw new SyncException("error response");
            }

            TransactionSyncResponse result = parseTransactionSyncResponse(
                    response.body().string());
            applyTransactionSync(result);
        } catch (JSONException | IOException | SyncException e) {
            Log.w(LOG_TAG, e.getMessage());
        }
    }

    JSONObject createTransactionSyncJSON() throws JSONException {
        List<STransaction> createdTrans = new ArrayList<>();

        String change_selector = String.format("%s != ?", ChangeTraceable.COLUMN_CHANGE_INDICATOR);
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        // TODO: also fetch the transaction items along,
        // maybe create a query with "fetch items" argument.
        // then join the results
        Cursor cursor = getContext().getContentResolver().query(
                TransactionEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                STransaction.TRANSACTION_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                STransaction transaction = new STransaction(cursor);
                createdTrans.add(transaction);
            } while (cursor.moveToNext());
        }

        JSONObject transactionJson = new JSONObject();
        if (!createdTrans.isEmpty()) {
            JSONArray transArray = new JSONArray();
            for (STransaction transaction : createdTrans) {
                transArray.put(transaction.toJsonObject());
            }

            transactionJson.put(getResourceString(R.string.sync_json_new_transactions),
                    transArray);
        }

        transactionJson.put(getResourceString(R.string.sync_json_trans_rev),
                PrefUtil.getTransactionRevision(getContext()));
        transactionJson.put(getResourceString(R.string.sync_json_branch_item_rev),
                PrefUtil.getBranchItemRevision(getContext()));

        return transactionJson;
    }

    TransactionSyncResponse parseTransactionSyncResponse(String server_response) throws JSONException {
        TransactionSyncResponse result = new TransactionSyncResponse();
        JSONObject rootJson = new JSONObject(server_response);

        result.company_id = rootJson.getLong(getResourceString(R.string.sync_json_company_id));
        result.latest_branch_item_rev = rootJson.getLong(getResourceString(R.string.sync_json_branch_item_rev));
        result.latest_transaction_rev = rootJson.getLong(getResourceString(R.string.sync_json_trans_rev));

        result.updatedTransIds = new ArrayList<>();
        result.syncedBranchItems = new ArrayList<>();
        result.syncedTrans = new ArrayList<>();

        if (rootJson.has(getResourceString(R.string.sync_json_updated_trans_ids))) {
            JSONArray updatedArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_updated_trans_ids));
            for (int i = 0; i < updatedArray.length(); i++) {
                result.updatedTransIds.add(new SyncUpdatedElement(updatedArray.getJSONObject(i), result.company_id));
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_branch_items))) {
            JSONArray branchItemArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_branch_items));

            final String JSON_BRANCH_ID = "branch_id";
            final String JSON_ITEM_ID = "item_id";
            final String JSON_QUANTITY = "quantity";
            final String JSON_LOC = "loc";
            for (int i = 0; i < branchItemArray.length(); i++) {
                JSONObject object = branchItemArray.getJSONObject(i);

                SBranchItem branchItem = new SBranchItem();
                branchItem.company_id = result.company_id;
                branchItem.branch_id = object.getLong(JSON_BRANCH_ID);
                branchItem.item_id = object.getLong(JSON_ITEM_ID);
                branchItem.quantity = object.getDouble(JSON_QUANTITY);
                branchItem.item_location = object.getString(JSON_LOC);

                result.syncedBranchItems.add(branchItem);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_trans))) {
            JSONArray transArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_trans));

            final String TRANS_JSON_TRANS_ID = "trans_id";
            final String TRANS_JSON_USER_ID = "user_id";
            final String TRANS_JSON_BRANCH_ID = "branch_id";
            final String TRANS_JSON_DATE = "date";
            final String TRANS_JSON_ITEMS = "items";

            final String TRANS_ITEM_JSON_TRANS_TYPE = "trans_type";
            final String TRANS_ITEM_JSON_ITEM_ID = "item_id";
            final String TRANS_ITEM_JSON_OTHER_BRANCH_ID = "other_branch";
            final String TRANS_ITEM_JSON_QUANTITY = "quantity";
            for (int i = 0; i < transArray.length(); i++) {
                JSONObject transObject = transArray.getJSONObject(i);
                STransaction transaction = new STransaction();
                transaction.company_id = result.company_id;
                transaction.transaction_id = transObject.getLong(TRANS_JSON_TRANS_ID);
                transaction.user_id = transObject.getLong(TRANS_JSON_USER_ID);
                transaction.branch_id = transObject.getLong(TRANS_JSON_BRANCH_ID);
                transaction.date = transObject.getLong(TRANS_JSON_DATE);

                transaction.transactionItems = new ArrayList<>();

                JSONArray itemsArray = transObject.getJSONArray(TRANS_JSON_ITEMS);
                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject itemObject = itemsArray.getJSONObject(j);
                    STransaction.STransactionItem transactionItem = new
                            STransaction.STransactionItem();
                    transactionItem.company_id = result.company_id;
                    transactionItem.trans_id = transaction.transaction_id;
                    transactionItem.trans_type = itemObject.getInt(TRANS_ITEM_JSON_TRANS_TYPE);
                    transactionItem.item_id = itemObject.getInt(TRANS_ITEM_JSON_ITEM_ID);
                    transactionItem.other_branch_id = itemObject.getInt(TRANS_ITEM_JSON_OTHER_BRANCH_ID);
                    transactionItem.quantity = itemObject.getInt(TRANS_ITEM_JSON_QUANTITY);

                    transaction.transactionItems.add(transactionItem);
                }
            }
        }

        return result;
    }

    void applyTransactionSync(TransactionSyncResponse result) throws SyncException {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        for (SyncUpdatedElement updated_trans : result.updatedTransIds) {
            ContentValues values = new ContentValues();
            values.put(TransactionEntry.COLUMN_TRANS_ID, updated_trans.newId);
            setChangeStatus(values, ChangeTraceable.CHANGE_STATUS_SYNCED);
            operationList.add(ContentProviderOperation.newUpdate(TransactionEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", TransactionEntry.COLUMN_TRANS_ID),
                            new String[]{Long.toString(updated_trans.oldId)}).
                    build());
        }
        for (SBranchItem sync_branch_item : result.syncedBranchItems) {
            operationList.add(ContentProviderOperation.newInsert(BranchItemEntry.buildBaseUri(company_id)).
                    withValues(
                            setChangeStatus(DbUtil.setUpdateOnConflict(sync_branch_item.toContentValues()),
                                    ChangeTraceable.CHANGE_STATUS_SYNCED)).
                    build());
        }
        for (STransaction sync_trans : result.syncedTrans) {
            operationList.add(ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(company_id)).
                    withValues(
                            setChangeStatus(sync_trans.toContentValues(),
                                    ChangeTraceable.CHANGE_STATUS_SYNCED)).
                    build());
            for (STransaction.STransactionItem trans_item : sync_trans.transactionItems) {
                operationList.add(ContentProviderOperation.newInsert(TransItemEntry.buildBaseUri(company_id)).
                        withValues(
                                setChangeStatus(trans_item.toContentValues(),
                                        ChangeTraceable.CHANGE_STATUS_SYNCED)).
                        build());
            }
        }
        try {
            getContext().getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);
        } catch (OperationApplicationException | RemoteException e) {
            throw new SyncException(e);
        }
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).
                    build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }


    static class EntitySyncResponse {
        long company_id;

        long latest_item_rev;
        long latest_branch_rev;

        // empty if the element doesn't exist in the response
        // or if it is empty
        List<SyncUpdatedElement> updatedItemIds;
        List<SyncUpdatedElement> updatedBranchIds;

        List<SItem> syncedItems;
        List<SBranch> syncedBranches;
    }

    static class TransactionSyncResponse {
        long company_id;
        long latest_transaction_rev;
        long latest_branch_item_rev;

        List<SyncUpdatedElement> updatedTransIds;

        List<STransaction> syncedTrans;
        List<SBranchItem> syncedBranchItems;
    }

    public class SyncUpdatedElement {
        public long oldId;
        public long newId;
        public long companyId;

        public static final String JSON_OLD_ID = "o";
        public static final String JSON_NEW_ID = "n";

        public SyncUpdatedElement(JSONObject json, long company_id) throws JSONException {
            oldId = json.getLong(JSON_OLD_ID);
            newId = json.getLong(JSON_NEW_ID);
            this.companyId = company_id;
        }
    }

    private static class SyncException extends Exception {
        public SyncException() {
            super();
        }

        public SyncException(String detailMessage) {
            super(detailMessage);
        }

        public SyncException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public SyncException(Throwable throwable) {
            super(throwable);
        }
    }

}
