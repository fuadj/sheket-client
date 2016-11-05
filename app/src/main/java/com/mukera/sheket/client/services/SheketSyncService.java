package com.mukera.sheket.client.services;

import android.app.IntentService;
import android.content.Intent;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Log;

import com.mukera.sheket.client.SheketBroadcast;
import com.mukera.sheket.client.SheketGRPCCall;
import com.mukera.sheket.client.controller.user.IdEncoderUtil;
import com.mukera.sheket.client.models.SBranchCategory;
import com.mukera.sheket.client.models.SCompany;
import com.mukera.sheket.client.network.Company;
import com.mukera.sheket.client.network.CompanyAuth;
import com.mukera.sheket.client.network.CompanyID;
import com.mukera.sheket.client.network.CompanyList;
import com.mukera.sheket.client.network.EntityRequest;
import com.mukera.sheket.client.network.EntityResponse;
import com.mukera.sheket.client.network.SheketAuth;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.network.SyncCompanyRequest;
import com.mukera.sheket.client.network.TransactionRequest;
import com.mukera.sheket.client.network.TransactionResponse;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.models.SMember;
import com.mukera.sheket.client.models.STransaction;
import com.mukera.sheket.client.utils.DbUtil;
import com.mukera.sheket.client.utils.DeviceId;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by gamma on 4/13/16.
 */
public class SheketSyncService extends IntentService {
    private final String LOG_TAG = SheketSyncService.class.getSimpleName();

    public SheketSyncService() {
        super(SheketSyncService.class.getSimpleName());
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
                    forAddress(ConfigData.getServerIP(SheketSyncService.this), ConfigData.getServerPort()).
                    usePlaintext(true).
                    build();

            SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                    SheketServiceGrpc.newBlockingStub(managedChannel);

            Set<Integer> previous_companies = getLocalCompanyIds();

            Pair<Set<Integer>, Boolean> pair = syncCompanyList(blockingStub);

            /**
             * Remove the companies that previously existed locally but now don't appear when syncing.
             * IMPORTANT: if the company is the currently selected company, we also need to force
             * the UI to reset with the company not being visible thereafter.
             *
             * NOTE: {@code removeAll()} is a set subtraction.
             * i.e:     companies_to_remove = previously_exist - currently_synced;
             */
            Set<Integer> companies_to_delete = previous_companies;
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
                    syncEntities(blockingStub);
                    syncTransactions(blockingStub);
                }

                sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_SUCCESS);
            }

            // we've finished syncing, so set it here
            // so the payment service doesn't assume we are still syncing
            PrefUtil.setIsSyncRunning(this, false);

            startService(new Intent(this, PaymentService.class));

        } catch (SheketGRPCCall.SheketInvalidLoginException e) {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_INVALID_LOGIN_CREDENTIALS);
        } catch (SheketGRPCCall.SheketInternetException e) {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_INTERNET_ERROR,
                    "Internet problem",
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        } catch (SheketGRPCCall.SheketException e) {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_SERVER_ERROR,
                    e.getMessage(),
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        } catch (Exception e) {
            sendSheketBroadcast(SheketBroadcast.ACTION_SYNC_GENERAL_ERROR,
                    e.getMessage(),
                    SheketBroadcast.ACTION_SYNC_EXTRA_ERROR_MSG);
        }

        PrefUtil.setIsSyncRunning(this, false);
    }

    Set<Integer> getLocalCompanyIds() throws Exception {
        Set<Integer> company_ids = new HashSet<>();
        Cursor cursor = getContentResolver().query(CompanyEntry.CONTENT_URI,
                SCompany.COMPANY_COLUMNS, null, null, null);
        if (cursor == null)
            throw new Exception("can't enumerate local companies");
        if (cursor.moveToFirst()) {
            do {
                SCompany company = new SCompany(cursor);
                company_ids.add(company.company_id);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return company_ids;
    }

    void deleteRemovedCompanies(Set<Integer> removed_companies) throws Exception {
        ArrayList<ContentProviderOperation> deleteOperations = new ArrayList<>();

        for (Integer company_id : removed_companies) {
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
    Pair<Set<Integer>, Boolean> syncCompanyList(final SheketServiceGrpc.SheketServiceBlockingStub blockingStub) throws Exception {
        final SyncCompanyRequest request = SyncCompanyRequest.
                newBuilder().
                setUserRev(PrefUtil.getUserRevision(this)).
                setDeviceId(DeviceId.getUniqueDeviceId(this)).
                setLocalUserTime(
                        String.valueOf(System.currentTimeMillis())).
                setAuth(SheketAuth.newBuilder().
                        setLoginCookie(PrefUtil.getLoginCookie(this)).build()).
                build();

        CompanyList companies = new SheketGRPCCall<CompanyList>().runBlockingCall(
                new SheketGRPCCall.GRPCCallable<CompanyList>() {
                    @Override
                    public CompanyList runGRPCCall() throws Exception {
                        return blockingStub.syncCompanies(request);
                    }
                }
        );

        long user_id = PrefUtil.getUserId(this);
        long current_company_id = PrefUtil.getCurrentCompanyId(this);

        boolean is_current_company_license_valid = false;

        Set<Integer> sync_company_ids = new HashSet<>();

        List<Company> companyList = companies.getCompaniesList();

        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        for (Company company : companyList) {
            int company_id = company.getCompanyId();

            sync_company_ids.add(company_id);

            String name = company.getCompanyName();
            String new_permission = company.getPermission();
            String license = company.getSignedLicense();
            //String payment_id = company.getPaymentId();
            String payment_id = IdEncoderUtil.encodeAndDelimitId(company_id, IdEncoderUtil.ID_TYPE_COMPANY);

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
    void syncEntities(final SheketServiceGrpc.SheketServiceBlockingStub blockingStub) throws SheketGRPCCall.SheketException {
        final EntityRequest.Builder entity_request = EntityRequest.newBuilder();
        CompanyAuth companyAuth = CompanyAuth.
                newBuilder().
                setCompanyId(
                        CompanyID.newBuilder().setCompanyId(
                                PrefUtil.getCurrentCompanyId(this)
                        ).build()
                ).
                setSheketAuth(
                        SheketAuth.newBuilder().setLoginCookie(
                                PrefUtil.getLoginCookie(this)
                        ).build()
                ).build();
        entity_request.setCompanyAuth(companyAuth);

        buildEntityRequest(entity_request);

        EntityResponse response = new SheketGRPCCall<EntityResponse>().runBlockingCall(
                new SheketGRPCCall.GRPCCallable<EntityResponse>() {
                    @Override
                    public EntityResponse runGRPCCall() throws Exception {
                        return blockingStub.syncEntity(entity_request.build());
                    }
                }
        );
        applyEntityResponse(response);
    }

    ContentValues setStatusSynced(ContentValues values) {
        values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_SYNCED);
        return values;
    }

    void applyEntityResponse(EntityResponse response) {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        int company_id = PrefUtil.getCurrentCompanyId(this);

        for (EntityResponse.UpdatedId updatedId : response.getUpdatedCategoryIdsList()) {
            ContentValues values = new ContentValues();
            values.put(CategoryEntry.COLUMN_CATEGORY_ID, updatedId.getNewId());
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(CategoryEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", CategoryEntry.COLUMN_CATEGORY_ID),
                            new String[]{Long.toString(updatedId.getOldId())}
                    ).build());
        }
        for (EntityResponse.UpdatedId updatedId : response.getUpdatedItemIdsList()) {
            ContentValues values = new ContentValues();
            values.put(ItemEntry.COLUMN_ITEM_ID, updatedId.getNewId());
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(ItemEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", ItemEntry.COLUMN_ITEM_ID),
                            new String[]{Long.toString(updatedId.getOldId())}).
                    build());
        }

        for (EntityResponse.UpdatedId updatedId : response.getUpdatedItemIdsList()) {
            ContentValues values = new ContentValues();
            values.put(BranchEntry.COLUMN_BRANCH_ID, updatedId.getNewId());
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(BranchEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", BranchEntry.COLUMN_BRANCH_ID),
                            new String[]{Long.toString(updatedId.getOldId())}).
                    build());
        }

        for (EntityResponse.SyncCategory syncCategory : response.getCategoriesList()) {
            if (syncCategory.getState() == EntityResponse.SyncState.REMOVED) {
                operationList.add(ContentProviderOperation.newDelete(
                        CategoryEntry.buildBaseUri(company_id)).
                        withSelection(CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " = ?",
                                new String[]{String.valueOf(syncCategory.getCategory().getCategoryId())}).
                        build());
            } else {
                SCategory category = new SCategory(syncCategory.getCategory());
                category.company_id = company_id;
                operationList.add(ContentProviderOperation.newInsert(CategoryEntry.buildBaseUri(company_id)).
                        withValues(setStatusSynced(DbUtil.setUpdateOnConflict(
                                category.toContentValues()
                        ))).build());
            }
        }

        for (EntityResponse.SyncItem syncItem : response.getItemsList()) {
            if (syncItem.getState() == EntityResponse.SyncState.REMOVED) {
                operationList.add(ContentProviderOperation.newDelete(
                        CategoryEntry.buildBaseUri(company_id)).
                        withSelection(ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                                new String[]{String.valueOf(syncItem.getItem().getItemId())}).
                        build());
            } else {
                SItem item = new SItem(syncItem.getItem());
                item.company_id = company_id;
                operationList.add(ContentProviderOperation.newInsert(ItemEntry.buildBaseUri(company_id)).
                        withValues(
                                setStatusSynced(DbUtil.setUpdateOnConflict(
                                        item.toContentValues()
                                ))).build());
            }
        }

        for (EntityResponse.SyncBranch syncBranch : response.getBranchesList()) {
            if (syncBranch.getState() == EntityResponse.SyncState.REMOVED) {
                operationList.add(ContentProviderOperation.newDelete(
                        BranchEntry.buildBaseUri(company_id)).
                        withSelection(BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " = ?",
                                new String[]{String.valueOf(syncBranch.getBranch().getBranchId())}).
                        build());
            } else {
                SBranch branch = new SBranch(syncBranch.getBranch());
                branch.company_id = company_id;
                operationList.add(ContentProviderOperation.newInsert(BranchEntry.buildBaseUri(company_id)).
                        withValues(
                                setStatusSynced(DbUtil.setUpdateOnConflict(
                                        branch.toContentValues()
                                ))).build());
            }
        }

        for (EntityResponse.SyncBranchCategory syncBranchCategory : response.getBranchCategoriesList()) {
            if (syncBranchCategory.getState() == EntityResponse.SyncState.REMOVED) {
                String selection = BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_BRANCH_ID) + " = ? AND " +
                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID) + " = ?";
                String[] selectionArgs = new String[]{
                        String.valueOf(syncBranchCategory.getBranchCategory().getBranchId()),
                        String.valueOf(syncBranchCategory.getBranchCategory().getCategoryId())
                };
                operationList.add(ContentProviderOperation.newDelete(
                        BranchCategoryEntry.buildBaseUri(company_id)).
                        withSelection(selection, selectionArgs).
                        build());
            } else {
                SBranchCategory branchCategory = new SBranchCategory(syncBranchCategory.getBranchCategory());
                branchCategory.company_id = company_id;
                operationList.add(ContentProviderOperation.newInsert(BranchCategoryEntry.buildBaseUri(company_id)).
                        withValues(
                                setStatusSynced(DbUtil.setUpdateOnConflict(branchCategory.toContentValues()))).
                        build());
            }
        }

        for (EntityResponse.SyncEmployee syncEmployee : response.getEmployeesList()) {
            if (syncEmployee.getState() == EntityResponse.SyncState.REMOVED) {
                operationList.add(ContentProviderOperation.newDelete(
                        MemberEntry.buildBaseUri(company_id)).
                        withSelection(MemberEntry._full(MemberEntry.COLUMN_MEMBER_ID) + " = ?",
                                new String[]{String.valueOf(syncEmployee.getEmployee().getEmployeeId())}).
                        build());
            } else {
                SMember employee = new SMember(syncEmployee.getEmployee());
                employee.company_id = company_id;
                operationList.add(ContentProviderOperation.newInsert(MemberEntry.buildBaseUri(company_id)).
                        withValues(
                                setStatusSynced(DbUtil.setUpdateOnConflict(employee.toContentValues()))).
                        build());
            }
        }

        try {
            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);
            PrefUtil.setCategoryRevision(this, (int) response.getNewCategoryRev());
            PrefUtil.setItemRevision(this, (int) response.getNewItemRev());
            PrefUtil.setBranchRevision(this, (int) response.getNewBranchRev());
            PrefUtil.setMemberRevision(this, (int) response.getNewMemberRev());
            PrefUtil.setBranchCategoryRevision(this, (int) response.getNewBranchCategoryRev());
        } catch (OperationApplicationException | RemoteException e) {
            e.printStackTrace();
            // TODO: handle exception
            //throw e;
        }
    }

    interface EntityBuilder {
        void buildEntity(Cursor cursor, EntityRequest.Builder request_builder);
    }

    EntityRequest.Action toSyncRequestAction(int change_status) {
        switch (change_status) {
            case ChangeTraceable.CHANGE_STATUS_CREATED:
                return EntityRequest.Action.CREATE;
            case ChangeTraceable.CHANGE_STATUS_UPDATED:
                return EntityRequest.Action.UPDATE;
            case ChangeTraceable.CHANGE_STATUS_DELETED:
            default:
                return EntityRequest.Action.DELETE;
        }
    }

    void buildEntityRequest(EntityRequest.Builder request_builder) {
        request_builder.
                setOldCategoryRev(PrefUtil.getCategoryRevision(this)).
                setOldItemRev(PrefUtil.getItemRevision(this)).
                setOldBranchRev(PrefUtil.getBranchRevision(this)).
                setOldBranchItemRev(PrefUtil.getBranchItemRevision(this)).
                setOldMemberRev(PrefUtil.getMemberRevision(this)).
                setOldBranchCategoryRev(PrefUtil.getBranchCategoryRevision(this));

        // build categories
        genericEntityRequestBuilder(request_builder,
                CategoryEntry.buildBaseUriWithNoChildren(PrefUtil.getCurrentCompanyId(this)),
                CategoryEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SCategory.CATEGORY_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        Log.d("SheketSync", "adding category");
                        SCategory category = new SCategory(cursor);

                        request_builder.addCategories(
                                EntityRequest.RequestCategory.newBuilder().
                                        setCategory(category.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(category.change_status)
                                        ));
                    }
                });

        // build items
        genericEntityRequestBuilder(request_builder,
                ItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                ItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SItem.ITEM_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        SItem item = new SItem(cursor);
                        request_builder.addItems(
                                EntityRequest.RequestItem.newBuilder().
                                        setItem(item.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(item.change_status)
                                        ));
                    }
                }
        );

        // build branches
        genericEntityRequestBuilder(request_builder,
                BranchEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                BranchEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SBranch.BRANCH_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        SBranch branch = new SBranch(cursor);

                        request_builder.addBranches(
                                EntityRequest.RequestBranch.newBuilder().
                                        setBranch(branch.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(branch.change_status)
                                        ));
                    }
                }
        );

        // build branch_item
        genericEntityRequestBuilder(request_builder,
                BranchItemEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                BranchItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SBranchItem.BRANCH_ITEM_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        SBranchItem branchItem = new SBranchItem(cursor);

                        request_builder.addBranchItems(
                                EntityRequest.RequestBranchItem.newBuilder().
                                        setBranchItem(branchItem.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(branchItem.change_status)
                                        ));
                    }
                }
        );

        // build employee
        genericEntityRequestBuilder(request_builder,
                MemberEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                MemberEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SMember.MEMBER_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        SMember employee = new SMember(cursor);

                        request_builder.addEmployees(
                                EntityRequest.RequestEmployee.newBuilder().
                                        setEmployee(employee.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(employee.change_status)
                                        ));
                    }
                }
        );

        // build branch_category
        genericEntityRequestBuilder(request_builder,
                BranchCategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(this)),
                BranchCategoryEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR),
                SBranchCategory.BRANCH_CATEGORY_COLUMNS,
                new EntityBuilder() {
                    @Override
                    public void buildEntity(Cursor cursor, EntityRequest.Builder request_builder) {
                        SBranchCategory branchCategory = new SBranchCategory(cursor);

                        request_builder.addBranchCategories(
                                EntityRequest.RequestBranchCategory.newBuilder().
                                        setBranchCategory(branchCategory.toGRPCBuilder()).
                                        setAction(
                                                toSyncRequestAction(branchCategory.change_status)
                                        ));
                    }
                }
        );
    }

    void genericEntityRequestBuilder(EntityRequest.Builder request_builder,
                                     Uri query_uri,
                                     String change_column,
                                     String[] projection,
                                     EntityBuilder entityBuilder) {
        Cursor cursor = this.getContentResolver().query(
                query_uri,
                projection,
                String.format("%s != ?", change_column),
                new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)},
                null);

        if (cursor == null) {
            // TODO: maybe throw an Exception?
            return;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        do {
            entityBuilder.buildEntity(cursor, request_builder);
        } while (cursor.moveToNext());

        cursor.close();
    }


    void syncTransactions(final SheketServiceGrpc.SheketServiceBlockingStub blockingStub) throws Exception {
        final TransactionRequest.Builder transaction_request = TransactionRequest.newBuilder();
        CompanyAuth companyAuth = CompanyAuth.
                newBuilder().
                setCompanyId(
                        CompanyID.newBuilder().setCompanyId(
                                PrefUtil.getCurrentCompanyId(this)
                        ).build()
                ).
                setSheketAuth(
                        SheketAuth.newBuilder().setLoginCookie(
                                PrefUtil.getLoginCookie(this)
                        ).build()
                ).build();
        transaction_request.setCompanyAuth(companyAuth);

        buildTransactionRequest(transaction_request);

        TransactionResponse response = new SheketGRPCCall<TransactionResponse>().runBlockingCall(
                new SheketGRPCCall.GRPCCallable<TransactionResponse>() {
                    @Override
                    public TransactionResponse runGRPCCall() throws Exception {
                        return blockingStub.syncTransaction(transaction_request.build());
                    }
                }
        );
        applyTransactionResponse(response);
    }

    void buildTransactionRequest(TransactionRequest.Builder request_builder) {
        request_builder.
                setOldBranchItemRev(PrefUtil.getBranchItemRevision(this)).
                setOldTransRev(PrefUtil.getTransactionRevision(this));

        Cursor cursor = this.getContentResolver().query(
                // the TransItemEntry.NO_TRANS_ID_SET is to search ALL transactions
                TransItemEntry.buildTransactionItemsUri(PrefUtil.getCurrentCompanyId(this), TransItemEntry.NO_TRANS_ID_SET),
                STransaction.TRANSACTION_JOIN_ITEMS_COLUMNS,
                String.format("%s != ?", TransItemEntry._full(ChangeTraceable.COLUMN_CHANGE_INDICATOR)),
                new String[]{Integer.toString(ChangeTraceable.CHANGE_STATUS_SYNCED)},
                null);
        if (cursor == null) {
            // TODO: maybe throw an exception?
            return;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return;
        }

        do {
            request_builder.addTransactions(new STransaction(cursor, true).toGRPCBuilder());
        } while (cursor.moveToNext());

        cursor.close();
    }

    void applyTransactionResponse(TransactionResponse response) throws Exception {
        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        int company_id = PrefUtil.getCurrentCompanyId(this);

        for (TransactionResponse.UpdatedTransId updatedId : response.getUpdatedTransactionIdsList()) {
            ContentValues values = new ContentValues();
            values.put(TransactionEntry.COLUMN_TRANS_ID, updatedId.getNewId());
            setStatusSynced(values);
            operationList.add(ContentProviderOperation.newUpdate(TransactionEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(
                            String.format("%s = ?", TransactionEntry.COLUMN_TRANS_ID),
                            new String[]{Long.toString(updatedId.getOldId())}).
                    build());
        }

        for (TransactionResponse.SyncBranchItem sync_branch_item : response.getBranchItemsList()) {
            SBranchItem branchItem = new SBranchItem(sync_branch_item.getBranchItem());
            branchItem.company_id = company_id;
            operationList.add(ContentProviderOperation.newInsert(BranchItemEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(DbUtil.setUpdateOnConflict(branchItem.toContentValues()))).
                    build());
        }

        for (TransactionResponse.SyncTransaction sync_trans : response.getTransactionsList()) {
            STransaction transaction = new STransaction(sync_trans, company_id);
            operationList.add(ContentProviderOperation.newInsert(TransactionEntry.buildBaseUri(company_id)).
                    withValues(
                            setStatusSynced(transaction.toContentValues())).
                    build());
            for (STransaction.STransactionItem trans_item : transaction.transactionItems) {
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
            /**
             * Update the transaction item to have an updated state. Check if we can do that
             * in a single ContentProviderOperation's List. The issue is which transaction id
             * to use. If we use the "un-synced" values, what will happen to the update
             * since we are also changing them. If we try to use the new ids, there might
             * be a "FOREIGN-KEY" violation as the transactions might not have updated their ids.
             */

            for (TransactionResponse.UpdatedTransId updated_trans : response.getUpdatedTransactionIdsList()) {
                operationList.add(ContentProviderOperation.newUpdate(TransItemEntry.buildBaseUri(company_id)).
                        withValues(setStatusSynced(new ContentValues())).
                        withSelection(
                                String.format("%s = ?", TransItemEntry.COLUMN_TRANSACTION_ID),
                                new String[]{Long.toString(updated_trans.getNewId())}).build());
            }

            this.getContentResolver().applyBatch(
                    SheketContract.CONTENT_AUTHORITY, operationList);

            PrefUtil.setTransactionRevision(this, (int) response.getNewTransRev());
            PrefUtil.setBranchItemRevision(this, (int) response.getNewBranchItemRev());
        } catch (OperationApplicationException | RemoteException e) {
            throw e;
        }
    }
}
