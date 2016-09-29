package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.models.SBranchCategory;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.network.Company;
import com.mukera.sheket.client.network.CompanyList;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.network.SyncCompanyRequest;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.SMember;
import com.mukera.sheket.client.models.SPermission;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.SheketNetworkUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by gamma on 4/13/16.
 */
public class SheketSyncService extends IntentService {
    private final String LOG_TAG = SheketSyncService.class.getSimpleName();

    public static final int SYNC_ROOT_CATEGORY_ID = -1;

    public static final OkHttpClient client = new OkHttpClient();

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SYNC_STATUS_SYNCED, SYNC_STATUS_SYNCING, SYNC_STATUS_SUCCESSFUL,
            SYNC_STATUS_SYNC_ERROR, SYNC_STATUS_INTERNET_ERROR, SYNC_STATUS_GENERAL_ERROR})
    public @interface SyncStatus {
    }

    public static final int SYNC_STATUS_SYNCED = 0;
    public static final int SYNC_STATUS_SYNCING = 1;
    public static final int SYNC_STATUS_SUCCESSFUL = 2;
    public static final int SYNC_STATUS_SYNC_ERROR = 3;
    public static final int SYNC_STATUS_INTERNET_ERROR = 4;
    public static final int SYNC_STATUS_GENERAL_ERROR = 5;

    private String error_msg;

    public SheketSyncService() {
        super("Sheket");
    }

    /**
     * Sends the action as a broadcast through {@code LocalBroadcastManager}.
     * If an {@code extra_msg} is specified, it will send that also using
     * the {@code extra_key}.
     */
    void sendSheketBroadcast(String action, String extra_msg, String extra_key) {
        Intent intent = new Intent(action);
        if (extra_msg != null) {
            intent.putExtra(extra_key, extra_msg);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * Send a local broadcast without any extras
     */
    void sendSheketBroadcast(String action) {
        sendSheketBroadcast(action, null, null);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //client.setConnectTimeout(10, TimeUnit.SECONDS);
        PrefUtil.setIsSyncRunning(this, true);
        try {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_STARTED);

            ManagedChannel managedChannel = ManagedChannelBuilder.
                    forAddress(ConfigData.getServerIP(), ConfigData.getServerPort()).
                    usePlaintext(true).
                    build();

            SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                    SheketServiceGrpc.newBlockingStub(managedChannel);

            Set<Long> previous_companies = getLocalCompanyIds();

            Pair<Set<Long>, Boolean> pair = syncCompanyList(blockingStub);

            /**
             * Remove the companies that previously existed locally but now don't appear when syncing.
             * IMPORTANT: if the company is the currently selected company, we also need to force
             * the UI to reset with the company not being visible thereafter.
             *
             * NOTE: {@code removeAll()} is a set subtraction.
             * i.e:     companies_to_remove = previously_exist - currently_synced;
             */
            Set<Long> companies_to_delete = previous_companies;
            companies_to_delete.removeAll(pair.first);

            Boolean is_current_company_payment_valid = pair.second;

            boolean did_remove_current_company = companies_to_delete.
                    contains(PrefUtil.getCurrentCompanyId(this));

            deleteRemovedCompanies(companies_to_delete);

            /**
             * If either the current company got removed OR the license of the current company isn't
             * legit, we need to "force-out" the user from the company.
             */
            if (did_remove_current_company ||
                    (PrefUtil.isCompanySet(this) && !is_current_company_payment_valid)) {
                sendSheketBroadcast(SheketBroadcast.ACTION_COMPANY_RESET);
            } else {
                // can only sync if there is a company selected
                if (PrefUtil.isCompanySet(this)) {
                    syncEntities();
                    syncTransactions();
                }

                sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_SUCCESS);
            }

            // we've finished syncing, so set it here
            // so the payment service doesn't assume we are still syncing
            PrefUtil.setIsSyncRunning(this, false);

            startService(new Intent(this, PaymentService.class));
        } catch (InvalidLoginCredentialException e) {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_INVALID_LOGIN_CREDENTIALS);
        } catch (SyncException e) {
            Log.e(LOG_TAG, "Sync Error", e);
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_SERVER_ERROR,
                    e.getMessage(),
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Internet Problem", e);
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_INTERNET_ERROR,
                    "Internet problem",
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        } catch (Exception e) {
            e.printStackTrace();
            String err = e.getMessage();
            // it usually has the format package.exception_class: error message,
            // so do the crude "parsing" of the error message
            int index = err.indexOf("Exception");
            String err_msg = err;
            if (index != -1)
                err_msg = err.substring(index + "Exception".length());

            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_GENERAL_ERROR,
                    "Error " + err_msg,
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        }
        PrefUtil.setIsSyncRunning(this, false);
    }

    Set<Long> getLocalCompanyIds() throws Exception {
        Set<Long> company_ids = new HashSet<>();
        Cursor cursor = getContentResolver().query(CompanyEntry.CONTENT_URI,
                SCompany.COMPANY_COLUMNS, null, null, null);
        if (cursor == null)
            throw new SyncException("can't enumerate local companies");
        if (cursor.moveToFirst()) {
            do {
                SCompany company = new SCompany(cursor);
                company_ids.add(company.company_id);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return company_ids;
    }

    void deleteRemovedCompanies(Set<Long> removed_companies) throws Exception {
        ArrayList<ContentProviderOperation> deleteOperations = new ArrayList<>();

        for (Long company_id : removed_companies) {
            deleteOperations.add(ContentProviderOperation.newDelete(CompanyEntry.CONTENT_URI).
                    withSelection(CompanyEntry.COLUMN_COMPANY_ID + " = ?",
                            new String[]{String.valueOf(company_id)}).build());
        }

        if (!deleteOperations.isEmpty())
            getContentResolver().applyBatch(SheketContract.CONTENT_AUTHORITY, deleteOperations);
    }

    /**
     * <p/>
     * Fetches the companies the user belongs in. Also checks if the payment license for the
     * current company is still valid. If it isn't, the caller needs to "force-out" the user
     * so he can pay.
     * <p/>
     *
     * @return a Pair<Set, Boolean>
     * The {@code Set<Long>} holds the ids of companies in the current sync. * The {@code Boolean} is true if the current company's payment license is valid.
     * </p>
     */
    Pair<Set<Long>, Boolean> syncCompanyList(SheketServiceGrpc.SheketServiceBlockingStub blockingStub) throws Exception {
        SyncCompanyRequest request = SyncCompanyRequest.
                newBuilder().
                setUserRev(PrefUtil.getUserRevision(this)).
                setDeviceId(DeviceId.getUniqueDeviceId(this)).
                setLocalUserTime(
                        String.valueOf(System.currentTimeMillis())).
                setAuth(SheketAuth.newBuilder().
                        setLoginCookie(PrefUtil.getLoginCookie(this)).build()).
                build();

        CompanyList companies = blockingStub.syncCompanies(request);

        long user_id = PrefUtil.getUserId(this);
        long current_company_id = PrefUtil.getCurrentCompanyId(this);

        boolean is_current_company_license_valid = false;

        Set<Long> sync_company_ids = new HashSet<>();

        List<Company> companyList = companies.getCompaniesList();

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Company company : companyList) {
            long company_id = company.getCompanyId();

            sync_company_ids.add(company_id);

            String name = company.getCompanyName();
            String new_permission = company.getPermission();
            String license = company.getSignedLicense();
            // TODO: get correct payment id
            String payment_id = "";

            /**
             * The server will only return a non-empty license if payment is made.
             * So checking if the license is empty or not is LEGIT. We don't have to
             * verify the signature.
             */
            boolean is_license_valid = !license.trim().isEmpty();

            if (company_id == current_company_id) {
                is_current_company_license_valid = is_license_valid;
            }

            ContentValues values = new ContentValues();
            values.put(CompanyEntry.COLUMN_COMPANY_ID, company_id);
            // tie this company to the current user calling the sync
            values.put(CompanyEntry.COLUMN_USER_ID, user_id);
            values.put(CompanyEntry.COLUMN_NAME, name);
            values.put(CompanyEntry.COLUMN_PERMISSION, new_permission);
            values.put(CompanyEntry.COLUMN_PAYMENT_ID, payment_id);
            values.put(CompanyEntry.COLUMN_PAYMENT_LICENSE, license);
            // if we've got a license, then there is payment available, otherwise payment has ended.
            values.put(CompanyEntry.COLUMN_PAYMENT_STATE,
                    is_license_valid ? CompanyEntry.PAYMENT_VALID : CompanyEntry.PAYMENT_ENDED);

            operations.add(ContentProviderOperation.newInsert(SheketContract.CompanyEntry.CONTENT_URI).
                    withValues(values).build());

            ContentValues updateValues = new ContentValues(values);
            // if the user already has this company, we can't insert b/c of the "ON CONFLICT IGNORE"
            // but we can update it, so we try our luck
            updateValues.remove(CompanyEntry.COLUMN_COMPANY_ID);
            operations.add(ContentProviderOperation.newUpdate(CompanyEntry.CONTENT_URI).
                    withValues(updateValues).
                    withSelection(CompanyEntry.COLUMN_COMPANY_ID + " = ?", new String[]{
                            String.valueOf(company_id)
                    }).build());

            String previous_permission = PrefUtil.getEncodedUserPermission(this);

            // if there is a permission change, send a broadcast to UI can update accordingly
            if (PrefUtil.getCurrentCompanyId(this) == company_id &&
                    !previous_permission.equals(new_permission)) {
                PrefUtil.setUserPermission(this, new_permission);
                sendSheketBroadcast(SheketBroadcast.ACTION_COMPANY_PERMISSION_CHANGE);
            }
        }

        if (!operations.isEmpty())
            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operations);

        return new Pair<>(sync_company_ids, is_current_company_license_valid);
    }

    /**
     * Sync elements like { item | branch | branch-item ...}
     * This prepares the way for the transactions to sync, since
     * it depends on these elements having a "defined" state.
     */
    void syncEntities() throws Exception {
        Request.Builder builder = new Request.Builder();
        builder.url(ConfigData.getAddress(this) + "v1/sync/entity");
        JSONObject json = createEntitySyncJSON();
        builder.addHeader(this.getString(R.string.pref_header_key_company_id),
                Long.toString(PrefUtil.getCurrentCompanyId(this)));
        builder.addHeader(this.getString(R.string.pref_request_key_cookie),
                PrefUtil.getLoginCookie(this));
        builder.post(RequestBody.create(MediaType.parse("application/json"),
                json.toString()));

        Response response = client.newCall(builder.build()).execute();
        if (!response.isSuccessful()) {
            throwAppropriateException(response);
        }

        EntitySyncResponse result = parseEntitySyncResponse(response.body().string());
        applyEntitySync(result);
    }

    ContentValues setStatusSynced(ContentValues values) {
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_SYNCED);
        return values;
    }

    void applyEntitySync(EntitySyncResponse response) throws Exception {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        long company_id = PrefUtil.getCurrentCompanyId(this);
        for (SyncUpdatedElement updated_category : response.updatedCategoryIds) {
            ContentValues values = new ContentValues();
            values.put(CategoryEntry.COLUMN_CATEGORY_ID, updated_category.newId);
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(CategoryEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", CategoryEntry.COLUMN_CATEGORY_ID),
                            new String[]{Long.toString(updated_category.oldId)}
                    ).build());
        }

        for (SyncUpdatedElement updated_item : response.updatedItemIds) {
            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_ID, updated_item.newId);
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(ItemEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", ItemEntry.COLUMN_ITEM_ID),
                            new String[]{Long.toString(updated_item.oldId)}).
                    build());
        }

        for (SyncUpdatedElement updated_branch : response.updatedBranchIds) {
            ContentValues values = new ContentValues();
            values.put(BranchEntry.COLUMN_BRANCH_ID, updated_branch.newId);
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(BranchEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", BranchEntry.COLUMN_BRANCH_ID),
                            new String[]{Long.toString(updated_branch.oldId)}).
                    build());
        }

        for (SCategory sync_category : response.syncedCategories) {
            operationList.add(ContentProviderOperation.newInsert(CategoryEntry.buildBaseUri(company_id)).
                    withValues(setStatusSynced(DbUtil.setUpdateOnConflict(sync_category.toContentValues()))).
                    build());
        }

        for (SItem sync_item : response.syncedItems) {
            operationList.add(ContentProviderOperation.newInsert(ItemEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(DbUtil.setUpdateOnConflict(sync_item.toContentValues()))).
                    build());
        }

        for (SBranch sync_branch : response.syncedBranches) {
            operationList.add(ContentProviderOperation.newInsert(BranchEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(DbUtil.setUpdateOnConflict(sync_branch.toContentValues()))).
                    build());
        }

        for (SMember sync_member : response.syncedMembers) {
            operationList.add(ContentProviderOperation.newInsert(MemberEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(DbUtil.setUpdateOnConflict(sync_member.toContentValues()))).
                    build());
        }

        for (SBranchCategory branch_category : response.syncedBranchCategories) {
            operationList.add(ContentProviderOperation.newInsert(BranchCategoryEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(DbUtil.setUpdateOnConflict(branch_category.toContentValues()))).
                    build());
        }

        for (Long category_id : response.deletedCategories) {
            operationList.add(ContentProviderOperation.newDelete(
                    CategoryEntry.buildBaseUri(company_id)).
                    withSelection(CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " = ?",
                            new String[]{String.valueOf(category_id)}).
                    build());
        }

        for (Pair<Long, Long> branch_category_pair : response.deletedBranchCategories) {
            String selection = BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_BRANCH_ID) + " = ? AND " +
                    BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID) + " = ?";
            String[] selectionArgs = new String[]{
                    String.valueOf(branch_category_pair.first),
                    String.valueOf(branch_category_pair.second)
            };
            operationList.add(ContentProviderOperation.newDelete(
                    BranchCategoryEntry.buildBaseUri(company_id)).
                    withSelection(selection, selectionArgs).
                    build());
        }

        for (Long member_id : response.deletedMembers) {
            operationList.add(ContentProviderOperation.newDelete(
                    MemberEntry.buildBaseUri(company_id)).
                    withSelection(MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID) + " = ?",
                            new String[]{String.valueOf(member_id)}).
                    build());
        }

        try {
            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);
            PrefUtil.setCategoryRevision(this, response.latest_category_rev);
            PrefUtil.setItemRevision(this, response.latest_item_rev);
            PrefUtil.setBranchRevision(this, response.latest_branch_rev);
            PrefUtil.setMemberRevision(this, response.latest_member_rev);
            PrefUtil.setBranchCategoryRevision(this, response.latest_branch_category_rev);
        } catch (OperationApplicationException | RemoteException e) {
            throw e;
        }
    }

    String getResourceString(int resId) {
        return this.getString(resId);
    }

    EntitySyncResponse parseEntitySyncResponse(String server_response) throws JSONException {
        EntitySyncResponse result = new EntitySyncResponse();
        JSONObject rootJson = new JSONObject(server_response);

        result.company_id = rootJson.getLong(getResourceString(R.string.pref_header_key_company_id));

        result.latest_category_rev = rootJson.getInt(getResourceString(R.string.sync_json_category_rev));
        result.latest_item_rev = rootJson.getInt(getResourceString(R.string.sync_json_item_rev));
        result.latest_branch_rev = rootJson.getInt(getResourceString(R.string.sync_json_branch_rev));
        result.latest_member_rev = rootJson.getInt(getResourceString(R.string.sync_json_member_rev));
        result.latest_branch_category_rev = rootJson.getInt(getResourceString(R.string.sync_json_branch_category_rev));

        result.updatedCategoryIds = new ArrayList<>();
        result.updatedItemIds = new ArrayList<>();
        result.updatedBranchIds = new ArrayList<>();

        result.syncedCategories = new ArrayList<>();
        result.syncedItems = new ArrayList<>();
        result.syncedBranches = new ArrayList<>();
        result.syncedMembers = new ArrayList<>();
        result.syncedBranchCategories = new ArrayList<>();

        result.deletedCategories = new ArrayList<>();
        result.deletedBranchCategories = new ArrayList<>();
        result.deletedMembers = new ArrayList<>();

        if (rootJson.has(getResourceString(R.string.sync_json_updated_category_ids))) {
            JSONArray updatedArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_updated_category_ids));
            for (int i = 0; i < updatedArray.length(); i++) {
                result.updatedCategoryIds.add(new SyncUpdatedElement(updatedArray.getJSONObject(i), result.company_id));
            }
        }

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

        if (rootJson.has(getResourceString(R.string.sync_json_sync_categories))) {
            JSONArray categoryArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_categories));

            for (int i = 0; i < categoryArray.length(); i++) {
                JSONObject object = categoryArray.getJSONObject(i);

                SCategory category = new SCategory();
                category.company_id = result.company_id;

                category.category_id = object.getLong(SCategory.JSON_CATEGORY_ID);
                category.name = object.getString(SCategory.JSON_NAME);
                category.client_uuid = object.getString(SCategory.JSON_CATEGORY_UUID);

                category.parent_id = object.getLong(SCategory.JSON_PARENT_ID);
                if (category.parent_id == SYNC_ROOT_CATEGORY_ID) {
                    category.parent_id = CategoryEntry.ROOT_CATEGORY_ID;
                }

                result.syncedCategories.add(category);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_items))) {
            JSONArray itemArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_items));

            for (int i = 0; i < itemArray.length(); i++) {
                JSONObject object = itemArray.getJSONObject(i);

                SItem item = new SItem();
                item.company_id = result.company_id;

                item.item_id = object.getLong(SItem.JSON_ITEM_ID);
                item.name = object.getString(SItem.JSON_ITEM_NAME);
                item.item_code = object.getString(SItem.JSON_ITEM_CODE);
                item.client_uuid = object.getString(SItem.JSON_ITEM_UUID);

                long category = object.getLong(SItem.JSON_ITEM_CATEGORY);
                if (category == SYNC_ROOT_CATEGORY_ID) {
                    category = CategoryEntry.ROOT_CATEGORY_ID;
                }
                item.category = category;

                item.unit_of_measurement = object.getInt(SItem.JSON_UNIT_OF_MEASUREMENT);
                item.has_derived_unit = object.getBoolean(SItem.JSON_HAS_DERIVED_UNIT);
                item.derived_name = object.getString(SItem.JSON_DERIVED_NAME);
                item.derived_factor = object.getDouble(SItem.JSON_DERIVED_FACTOR);
                item.reorder_level = object.getDouble(SItem.JSON_REORDER_LEVEL);

                item.model_year = object.getString(SItem.JSON_MODEL_YEAR);
                item.part_number = object.getString(SItem.JSON_PART_NUMBER);
                item.bar_code = object.getString(SItem.JSON_BAR_CODE);
                item.has_bar_code = object.getBoolean(SItem.JSON_HAS_BAR_CODE);

                item.status_flag = object.getInt(ItemEntry.JSON_STATUS_FLAG);
                result.syncedItems.add(item);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_branches))) {
            JSONArray branchArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_branches));

            for (int i = 0; i < branchArray.length(); i++) {
                JSONObject object = branchArray.getJSONObject(i);

                SBranch branch = new SBranch();
                branch.company_id = result.company_id;

                branch.branch_id = object.getLong(SBranch.JSON_BRANCH_ID);
                branch.client_uuid = object.getString(SBranch.JSON_BRANCH_UUID);
                branch.branch_name = object.getString(SBranch.JSON_NAME);
                branch.branch_location = object.getString(SBranch.JSON_LOCATION);

                branch.status_flag = object.getInt(BranchEntry.JSON_STATUS_FLAG);

                result.syncedBranches.add(branch);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_members))) {
            JSONArray memberArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_members));

            for (int i = 0; i < memberArray.length(); i++) {
                JSONObject object = memberArray.getJSONObject(i);

                SMember member = new SMember();
                member.company_id = result.company_id;

                member.member_id = object.getLong(SMember.JSON_MEMBER_ID);
                member.member_name = object.getString(SMember.JSON_MEMBER_NAME);
                member.member_permission = SPermission.Decode(object.getString(SMember.JSON_MEMBER_PERMISSION));

                result.syncedMembers.add(member);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_sync_branch_categories))) {
            JSONArray branchCategoryArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_sync_branch_categories));

            for (int i = 0; i < branchCategoryArray.length(); i++) {
                JSONObject object = branchCategoryArray.getJSONObject(i);

                SBranchCategory branchCategory = new SBranchCategory();
                branchCategory.company_id = result.company_id;

                branchCategory.branch_id = object.getLong(SBranchCategory.JSON_BRANCH_ID);
                branchCategory.category_id = object.getLong(SBranchCategory.JSON_CATEGORY_ID);

                result.syncedBranchCategories.add(branchCategory);
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_delete_categories))) {
            JSONArray deletedCategoryIdArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_delete_categories));

            for (int i = 0; i < deletedCategoryIdArray.length(); i++) {
                result.deletedCategories.add((long) deletedCategoryIdArray.getInt(i));
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_delete_branch_categories))) {
            JSONArray deletedBranchCategoryIdArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_delete_branch_categories));

            for (int i = 0; i < deletedBranchCategoryIdArray.length(); i++) {
                String branch_category_id = deletedBranchCategoryIdArray.getString(i);
                long branch_id = -1, category_id = -1;
                try {
                    int colon_index = branch_category_id.indexOf(":");
                    if (colon_index != -1) {
                        branch_id = Long.parseLong(branch_category_id.substring(0, colon_index));
                        category_id = Long.parseLong(branch_category_id.substring(colon_index + 1));
                    }
                } catch (NumberFormatException e) {
                    branch_id = -1;
                    category_id = -1;
                }
                if (branch_id != -1 && category_id != -1) {
                    result.deletedBranchCategories.add(new Pair<>(branch_id, category_id));
                }
            }
        }

        if (rootJson.has(getResourceString(R.string.sync_json_delete_members))) {
            JSONArray deletedMemberIdArray = rootJson.getJSONArray(getResourceString(R.string.sync_json_delete_members));

            for (int i = 0; i < deletedMemberIdArray.length(); i++) {
                result.deletedMembers.add((long) deletedMemberIdArray.getInt(i));
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
        Pair<Boolean, JSONObject> categoryChanges = getCategoryChanges();
        Pair<Boolean, JSONObject> itemChanges = getItemChanges();
        Pair<Boolean, JSONObject> branchChanges = getBranchChanges();
        Pair<Boolean, JSONObject> branchItemChanges = getBranchItemChanges();
        Pair<Boolean, JSONObject> memberChanges = getMemberChanges();
        Pair<Boolean, JSONObject> branchCategoryChanges = getBranchCategoryChanges();

        JSONObject syncJson = new JSONObject();
        syncJson.put(this.getString(R.string.sync_json_category_rev),
                PrefUtil.getCategoryRevision(this));
        syncJson.put(this.getString(R.string.sync_json_item_rev),
                PrefUtil.getItemRevision(this));
        syncJson.put(this.getString(R.string.sync_json_branch_rev),
                PrefUtil.getBranchRevision(this));
        syncJson.put(this.getString(R.string.sync_json_branch_item_rev),
                PrefUtil.getBranchItemRevision(this));
        syncJson.put(this.getString(R.string.sync_json_member_rev),
                PrefUtil.getMemberRevision(this));
        syncJson.put(this.getString(R.string.sync_json_branch_category_rev),
                PrefUtil.getBranchCategoryRevision(this));

        JSONArray types = new JSONArray();

        String category_entity = this.getString(R.string.sync_json_entity_type_category);
        String item_entity = this.getString(R.string.sync_json_entity_type_item);
        String branch_entity = this.getString(R.string.sync_json_entity_type_branch);
        String branchItem_entity = this.getString(R.string.sync_json_entity_type_branch_item);
        String member_entity = this.getString(R.string.sync_json_entity_type_member);
        String branchCategory_entity = this.getString(R.string.sync_json_entity_type_branch_category);

        if (categoryChanges.first) {
            syncJson.put(category_entity, categoryChanges.second);
            types.put(category_entity);
        }
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
        if (memberChanges.first) {
            syncJson.put(member_entity, memberChanges.second);
            types.put(member_entity);
        }
        if (branchCategoryChanges.first) {
            syncJson.put(branchCategory_entity, branchCategoryChanges.second);
            types.put(branchCategory_entity);
        }

        syncJson.put(this.getString(R.string.sync_json_entity_types), types);
        return syncJson;
    }

    List<Long> getItemIds(List<SItem> items) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ids.add(items.get(i).item_id);
        }
        return ids;
    }

    JSONArray longArrToJson(List<Long> longs) {
        JSONArray array = new JSONArray();
        for (Long l : longs) {
            array.put(l);
        }
        return array;
    }

    List<Long> getCategoryIds(List<SCategory> categories) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < categories.size(); i++) {
            ids.add(categories.get(i).category_id);
        }
        return ids;
    }

    /**
     * gathers every changes that happened on items table and returns that's
     * representation in a JSON object. If no changes were found, the Pair's first bool
     * will false.
     */
    Pair<Boolean, JSONObject> getCategoryChanges() throws JSONException {
        List<SCategory> createdCategories = new ArrayList<>();
        List<SCategory> updatedCategories = new ArrayList<>();
        List<SCategory> deletedCategories = new ArrayList<>();

        String change_selector = String.format("%s != ?", CategoryEntry._fullCurrent(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                CategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                SCategory.CATEGORY_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SCategory category = new SCategory(cursor);

                if (category.parent_id == CategoryEntry.ROOT_CATEGORY_ID) {
                    category.parent_id = SYNC_ROOT_CATEGORY_ID;
                }
                switch (category.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdCategories.add(category);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedCategories.add(category);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedCategories.add(category);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdCategories.isEmpty() && updatedCategories.isEmpty() && deletedCategories.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONObject categoryJson = new JSONObject();
        JSONArray fieldsArray = new JSONArray();

        for (SCategory category : createdCategories) {
            fieldsArray.put(category.toJsonObject());
        }
        for (SCategory category : updatedCategories) {
            fieldsArray.put(category.toJsonObject());
        }
        for (SCategory category : deletedCategories) {
            fieldsArray.put(category.toJsonObject());
        }

        categoryJson.put(getResourceString(R.string.sync_json_key_create),
                longArrToJson(getCategoryIds(createdCategories)));
        categoryJson.put(getResourceString(R.string.sync_json_key_update),
                longArrToJson(getCategoryIds(updatedCategories)));
        categoryJson.put(getResourceString(R.string.sync_json_key_delete),
                longArrToJson(getCategoryIds(deletedCategories)));

        categoryJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);
        return new Pair<>(Boolean.TRUE, categoryJson);
    }

    /**
     * see the {@code getCategoryChanges()} docs
     */
    Pair<Boolean, JSONObject> getItemChanges() throws JSONException {
        List<SItem> createdItems = new ArrayList<>();
        List<SItem> updatedItems = new ArrayList<>();
        List<SItem> deletedItems = new ArrayList<>();

        String change_selector = String.format("%s != ?", ItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                SItem.ITEM_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SItem item = new SItem(cursor);
                /**
                 * Convert from the local root category id to the one the server expects
                 */
                if (item.category == CategoryEntry.ROOT_CATEGORY_ID) {
                    item.category = SYNC_ROOT_CATEGORY_ID;
                }
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
                longArrToJson(getItemIds(createdItems)));
        itemsJSON.put(getResourceString(R.string.sync_json_key_update),
                longArrToJson(getItemIds(updatedItems)));
        itemsJSON.put(getResourceString(R.string.sync_json_key_delete),
                longArrToJson(getItemIds(deletedItems)));

        itemsJSON.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);
        return new Pair<>(Boolean.TRUE, itemsJSON);
    }

    List<Long> getBranchIds(List<SBranch> branches) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < branches.size(); i++) {
            ids.add(branches.get(i).branch_id);
        }
        return ids;
    }

    /**
     * see the {@code getCategoryChanges()} docs
     */
    Pair<Boolean, JSONObject> getBranchChanges() throws JSONException {
        List<SBranch> createdBranches = new ArrayList<>();
        List<SBranch> updatedBranches = new ArrayList<>();
        List<SBranch> deletedBranches = new ArrayList<>();

        String change_selector = String.format("%s != ?", BranchEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
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
                longArrToJson(getBranchIds(createdBranches)));
        branchesJson.put(getResourceString(R.string.sync_json_key_update),
                longArrToJson(getBranchIds(updatedBranches)));
        branchesJson.put(getResourceString(R.string.sync_json_key_delete),
                longArrToJson(getBranchIds(deletedBranches)));

        branchesJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);
        return new Pair<>(Boolean.TRUE, branchesJson);
    }

    List<String> getBranchItemIds(List<SBranchItem> branchItems) {
        List<String> result = new ArrayList<>(branchItems.size());
        for (SBranchItem branchItem : branchItems) {
            result.add(String.format(Locale.US, "%d:%d", branchItem.branch_id, branchItem.item_id));
        }
        return result;
    }

    JSONArray stringArrToJson(List<String> strings) {
        JSONArray arr = new JSONArray();
        for (String s : strings) {
            arr.put(s);
        }
        return arr;
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

        String change_selector = String.format("%s != ?", BranchItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                BranchItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
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
                stringArrToJson(getBranchItemIds(createdBranchItems)));
        branchesItemJson.put(getResourceString(R.string.sync_json_key_update),
                stringArrToJson(getBranchItemIds(updatedBranchItems)));
        branchesItemJson.put(getResourceString(R.string.sync_json_key_delete),
                stringArrToJson(getBranchItemIds(deletedBranchItems)));

        return new Pair<>(Boolean.TRUE, branchesItemJson);
    }

    List<String> getBranchCategoryIds(List<SBranchCategory> branchCategories) {
        List<String> result = new ArrayList<>(branchCategories.size());
        for (SBranchCategory branchCategory : branchCategories) {
            result.add(String.format(Locale.US, "%d:%d", branchCategory.branch_id, branchCategory.category_id));
        }
        return result;
    }

    Pair<Boolean, JSONObject> getBranchCategoryChanges() throws JSONException {
        List<SBranchCategory> createdBranchCategories = new ArrayList<>();
        List<SBranchCategory> updatedBranchCategories = new ArrayList<>();
        List<SBranchCategory> deletedBranchCategories = new ArrayList<>();

        String change_selector = String.format(Locale.US, "%s != ?", BranchCategoryEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{String.valueOf(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                BranchCategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                SBranchCategory.BRANCH_CATEGORY_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                SBranchCategory branchCategory = new SBranchCategory(cursor);
                switch (branchCategory.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdBranchCategories.add(branchCategory);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedBranchCategories.add(branchCategory);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedBranchCategories.add(branchCategory);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdBranchCategories.isEmpty() && updatedBranchCategories.isEmpty() && deletedBranchCategories.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONArray fieldsArray = new JSONArray();

        for (SBranchCategory branchCategory : createdBranchCategories) {
            fieldsArray.put(branchCategory.toJsonObject());
        }

        for (SBranchCategory branchCategory : updatedBranchCategories) {
            fieldsArray.put(branchCategory.toJsonObject());
        }

        for (SBranchCategory branchCategory : deletedBranchCategories) {
            fieldsArray.put(branchCategory.toJsonObject());
        }

        JSONObject branchCategoryJson = new JSONObject();

        branchCategoryJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArray);

        branchCategoryJson.put(getResourceString(R.string.sync_json_key_create),
                stringArrToJson(getBranchCategoryIds(createdBranchCategories)));
        branchCategoryJson.put(getResourceString(R.string.sync_json_key_update),
                stringArrToJson(getBranchCategoryIds(updatedBranchCategories)));
        branchCategoryJson.put(getResourceString(R.string.sync_json_key_delete),
                stringArrToJson(getBranchCategoryIds(deletedBranchCategories)));

        return new Pair<>(Boolean.TRUE, branchCategoryJson);
    }

    List<Long> getMemberIds(List<SMember> members) {
        List<Long> result = new ArrayList<>(members.size());
        for (SMember member : members) {
            result.add(member.member_id);
        }
        return result;
    }

    Pair<Boolean, JSONObject> getMemberChanges() throws JSONException {
        List<SMember> createdMembers = new ArrayList<>();
        List<SMember> updatedMembers = new ArrayList<>();
        List<SMember> deletedMembers = new ArrayList<>();

        String change_selector = String.format("%s != ?", MemberEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = getContentResolver().query(
                MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                SMember.MEMBER_COLUMNS,
                change_selector,
                args, null);

        if (cursor.moveToFirst()) {
            do {
                SMember member = new SMember(cursor);
                switch (member.change_status) {
                    case ChangeTraceable.CHANGE_STATUS_CREATED:
                        createdMembers.add(member);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_UPDATED:
                        updatedMembers.add(member);
                        break;
                    case ChangeTraceable.CHANGE_STATUS_DELETED:
                        deletedMembers.add(member);
                        break;
                }
            } while (cursor.moveToNext());
        }

        if (createdMembers.isEmpty() && updatedMembers.isEmpty() && deletedMembers.isEmpty()) {
            return new Pair<>(Boolean.FALSE, null);
        }

        JSONObject memberJson = new JSONObject();
        JSONArray fieldsArr = new JSONArray();

        for (SMember member : createdMembers) {
            fieldsArr.put(member.toJsonObject());
        }

        for (SMember member : updatedMembers) {
            fieldsArr.put(member.toJsonObject());
        }

        for (SMember member : deletedMembers) {
            fieldsArr.put(member.toJsonObject());
        }

        memberJson.put(getResourceString(R.string.sync_json_key_fields),
                fieldsArr);

        memberJson.put(getResourceString(R.string.sync_json_key_create),
                longArrToJson(getMemberIds(createdMembers)));
        memberJson.put(getResourceString(R.string.sync_json_key_update),
                longArrToJson(getMemberIds(updatedMembers)));
        memberJson.put(getResourceString(R.string.sync_json_key_delete),
                longArrToJson(getMemberIds(deletedMembers)));

        return new Pair<>(Boolean.TRUE, memberJson);
    }

    void syncTransactions() throws Exception {
        JSONObject json = createTransactionSyncJSON();
        Request.Builder builder = new Request.Builder();
        builder.url(ConfigData.getAddress(this) + "v1/sync/transaction");
        builder.addHeader(this.getString(R.string.pref_header_key_company_id),
                Long.toString(PrefUtil.getCurrentCompanyId(this)));
        builder.addHeader(this.getString(R.string.pref_request_key_cookie),
                PrefUtil.getLoginCookie(this));
        builder.post(RequestBody.create(MediaType.parse("application/json"),
                json.toString()));

        Response response = client.newCall(builder.build()).execute();
        if (!response.isSuccessful()) {
            throwAppropriateException(response);
        }

        TransactionSyncResponse result = parseTransactionSyncResponse(
                response.body().string());
        applyTransactionSync(result);
    }

    JSONObject createTransactionSyncJSON() throws JSONException {
        List<STransaction> unsyncedTrans = new ArrayList<>();

        String change_selector = String.format("%s != ?", TransItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR));
        String[] args = new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)};
        Cursor cursor = this.getContentResolver().query(
                // the TransItemEntry.NO_TRANS_ID_SET is to search ALL transactions
                TransItemEntry.buildTransactionItemsUri(PrefUtil.getCurrentCompanyId(this), TransItemEntry.NO_TRANS_ID_SET),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS,
                change_selector,
                args,
                null);
        if (cursor.moveToFirst()) {
            do {
                STransaction transaction = new STransaction(cursor, true);
                unsyncedTrans.add(transaction);
            } while (cursor.moveToNext());
        }

        JSONObject transactionJson = new JSONObject();
        if (!unsyncedTrans.isEmpty()) {
            JSONArray transArray = new JSONArray();
            for (STransaction transaction : unsyncedTrans) {
                transArray.put(transaction.toJsonObject());
            }

            transactionJson.put(getResourceString(R.string.sync_json_new_transactions),
                    transArray);
        }

        transactionJson.put(getResourceString(R.string.sync_json_trans_rev),
                PrefUtil.getTransactionRevision(this));
        transactionJson.put(getResourceString(R.string.sync_json_branch_item_rev),
                PrefUtil.getBranchItemRevision(this));

        return transactionJson;
    }

    TransactionSyncResponse parseTransactionSyncResponse(String server_response) throws JSONException {
        TransactionSyncResponse result = new TransactionSyncResponse();
        JSONObject rootJson = new JSONObject(server_response);

        result.company_id = rootJson.getLong(getResourceString(R.string.pref_header_key_company_id));
        result.latest_branch_item_rev = rootJson.getInt(getResourceString(R.string.sync_json_branch_item_rev));
        result.latest_transaction_rev = rootJson.getInt(getResourceString(R.string.sync_json_trans_rev));

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
            final String TRANS_JSON_UUID = "client_uuid";
            final String TRANS_JSON_USER_ID = "user_id";
            final String TRANS_JSON_BRANCH_ID = "branch_id";
            final String TRANS_JSON_DATE = "date";
            final String TRANS_JSON_TRANS_NOTE = "trans_note";
            final String TRANS_JSON_ITEMS = "items";

            final String TRANS_ITEM_JSON_TRANS_TYPE = "trans_type";
            final String TRANS_ITEM_JSON_ITEM_ID = "item_id";
            final String TRANS_ITEM_JSON_OTHER_BRANCH_ID = "other_branch";
            final String TRANS_ITEM_JSON_QUANTITY = "quantity";
            final String TRANS_ITEM_JSON_ITEM_NOTE = "item_note";

            for (int i = 0; i < transArray.length(); i++) {
                JSONObject transObject = transArray.getJSONObject(i);
                STransaction transaction = new STransaction();
                transaction.company_id = result.company_id;
                transaction.client_uuid = transObject.getString(TRANS_JSON_UUID);
                transaction.transaction_id = transObject.getLong(TRANS_JSON_TRANS_ID);
                transaction.user_id = transObject.getLong(TRANS_JSON_USER_ID);
                transaction.branch_id = transObject.getLong(TRANS_JSON_BRANCH_ID);
                transaction.date = transObject.getLong(TRANS_JSON_DATE);
                transaction.transactionNote = transObject.getString(TRANS_JSON_TRANS_NOTE);

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
                    transactionItem.item_note = itemObject.getString(TRANS_ITEM_JSON_ITEM_NOTE);

                    transaction.transactionItems.add(transactionItem);
                }

                result.syncedTrans.add(transaction);
            }
        }

        return result;
    }

    void applyTransactionSync(TransactionSyncResponse result) throws Exception {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        long company_id = PrefUtil.getCurrentCompanyId(this);
        for (SyncUpdatedElement updated_trans : result.updatedTransIds) {
            ContentValues values = new ContentValues();
            values.put(TransactionEntry.COLUMN_TRANS_ID, updated_trans.newId);
            setStatusSynced(values);
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
                            setStatusSynced(DbUtil.setUpdateOnConflict(sync_branch_item.toContentValues()))).
                    build());
        }
        for (STransaction sync_trans : result.syncedTrans) {
            operationList.add(ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(sync_trans.toContentValues())).
                    build());
            for (STransaction.STransactionItem trans_item : sync_trans.transactionItems) {
                operationList.add(ContentProviderOperation.newInsert(TransItemEntry.buildBaseUri(company_id)).
                        withValues(
                                setStatusSynced(trans_item.toContentValues())).
                        build());
            }
        }
        try {
            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);
            operationList = new ArrayList<>();
            // also update the transaction items
            // TODO: check if this can be done in the previous contentprovider operations list
            for (SyncUpdatedElement updated_trans : result.updatedTransIds) {
                operationList.add(ContentProviderOperation.newUpdate(TransItemEntry.buildBaseUri(company_id)).
                        withValues(setStatusSynced(new ContentValues())).
                        withSelection(
                                String.format("%s = ?", TransItemEntry.COLUMN_TRANSACTION_ID),
                                new String[]{Long.toString(updated_trans.newId)}).build());
            }
            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);

            PrefUtil.setTransactionRevision(this, result.latest_transaction_rev);
            PrefUtil.setBranchItemRevision(this, result.latest_branch_item_rev);
        } catch (OperationApplicationException | RemoteException e) {
            throw e;
        }
    }

    /**
     * Checks the response codes and throws the exception for it.
     */
    void throwAppropriateException(Response response) throws InvalidLoginCredentialException, SyncException {
        // TODO: don't hardcode these constants, use a standard library
        switch (response.code()) {
            case 200:
                return;

            // un-authorized, they don't have valid login credentials
            case 401:
                throw new InvalidLoginCredentialException("Invalid login credentials");

            default:
                throw new SyncException(SheketNetworkUtil.getErrorMessage(response));
        }
    }

    static class EntitySyncResponse {
        long company_id;

        int latest_category_rev;
        int latest_item_rev;
        int latest_branch_rev;
        int latest_member_rev;
        int latest_branch_category_rev;

        // empty if the element doesn't exist in the response
        // or if it is empty
        List<SyncUpdatedElement> updatedCategoryIds;
        List<SyncUpdatedElement> updatedItemIds;
        List<SyncUpdatedElement> updatedBranchIds;

        List<SCategory> syncedCategories;
        List<SItem> syncedItems;
        List<SBranch> syncedBranches;
        List<SMember> syncedMembers;
        List<SBranchCategory> syncedBranchCategories;

        List<Long> deletedCategories;
        List<Pair<Long, Long>> deletedBranchCategories;
        List<Long> deletedMembers;
    }

    static class TransactionSyncResponse {
        long company_id;
        int latest_transaction_rev;
        int latest_branch_item_rev;

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

    public static class InvalidLoginCredentialException extends Exception {
        public InvalidLoginCredentialException() {
            super();
        }

        public InvalidLoginCredentialException(String detailMessage) {
            super(detailMessage);
        }

        public InvalidLoginCredentialException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
        }

        public InvalidLoginCredentialException(Throwable throwable) {
            super(throwable);
        }
    }
}
