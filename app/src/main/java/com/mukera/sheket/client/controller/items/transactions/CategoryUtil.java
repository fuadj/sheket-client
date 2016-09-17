package com.mukera.sheket.client.controller.items.transactions;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranch;
import com.mukera.sheket.client.models.SBranchCategory;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Created by fuad on 7/13/16.
 */
public class CategoryUtil {
    public static class CategoryNode {
        public long categoryId;
        public CategoryNode parentNode;
    }

    /**
     * Parse's out the flat category stored in the db to a tree structure
     * where every node has a reference to its parent. Use {@code ROOT_CATEGORY_ID}
     * to find the root of the tree.
     *
     * @param context
     * @return
     */
    public static Map<Long, CategoryNode> parseCategoryTree(Context context) {
        Map<Long, CategoryNode> categoryTree = new HashMap<>();

        CategoryNode rootCategory = new CategoryNode();
        rootCategory.categoryId = CategoryEntry.ROOT_CATEGORY_ID;
        rootCategory.parentNode = null;

        categoryTree.put(CategoryEntry.ROOT_CATEGORY_ID, rootCategory);

        long company_id = PrefUtil.getCurrentCompanyId(context);

        String sortOrder = CategoryEntry._fullCurrent(CategoryEntry.COLUMN_CATEGORY_ID) + " ASC";
        Cursor cursor = context.getContentResolver().query(
                CategoryEntry.buildBaseUri(company_id),
                SCategory.CATEGORY_COLUMNS,
                null, null, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SCategory category = new SCategory(cursor);

                long current_id = category.category_id;
                long parent_id = category.parent_id;
                CategoryNode currentNode, parentNode;

                /**
                 * The current node can already exist if it was added as a parent
                 * of a previous category. This happens because when we visited
                 * its child, we hadn't seen this category, so we added the only
                 * information we had about it which is the id. When we finally get
                 * to fetch it, we fill in the rest of the information like its parent.
                 */
                if (categoryTree.containsKey(current_id)) {
                    currentNode = categoryTree.get(current_id);
                } else {
                    currentNode = new CategoryNode();
                    currentNode.categoryId = current_id;
                }

                if (categoryTree.containsKey(parent_id)) {
                    parentNode = categoryTree.get(parent_id);
                } else {
                    parentNode = new CategoryNode();
                    parentNode.categoryId = parent_id;
                    parentNode.parentNode = null;
                    categoryTree.put(parent_id, parentNode);
                }

                currentNode.parentNode = parentNode;
                categoryTree.put(current_id, currentNode);
            } while (cursor.moveToNext());
        }

        return categoryTree;
    }

    /**
     * Deletes the given categories and moves their "contents" to their respective parent categories.
     * This has 2 types of behaviour.
     * <p/>
     * 1) If the category is a newly created one, it hasn't yet been synced, it can safely be
     * removed locally. This is ok because the server doesn't know any thing about it and
     * no-body else either.
     * <p/>
     * 2) If the category is a synced one, it should only be marked as deleted and should only
     * be totally removed after a sync has been performed. If we locally remove it and
     * don't tell the server about the removal, it will still exist in other people's databases.
     */
    public static void deleteCategoryList(Context context, List<SCategory> categoryList) {
        long company_id = PrefUtil.getCurrentCompanyId(context);

        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();
        for (SCategory category : categoryList) {
            // first "move out" its contents to its parent
            moveOutChildrenCategories(context, operationList, category, company_id);
            moveOutItemsInsideCategory(context, operationList, category, company_id);

            String selection = CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " = ?";
            String[] selectionArgs = new String[]{String.valueOf(category.category_id)};

            // it was only created locally and not yet synced, we can remove altogether
            if (category.change_status == ChangeTraceable.CHANGE_STATUS_CREATED) {
                operationList.add(ContentProviderOperation.newDelete(
                        CategoryEntry.buildBaseUri(company_id)).
                        withSelection(selection, selectionArgs).
                        build());
            } else {
                category.change_status = ChangeTraceable.CHANGE_STATUS_DELETED;
                operationList.add(ContentProviderOperation.newUpdate(
                        CategoryEntry.buildBaseUri(company_id)).
                        withValues(category.toContentValues()).
                        withSelection(selection, selectionArgs).
                        build());
            }

            try {
                context.getContentResolver().
                        applyBatch(SheketContract.CONTENT_AUTHORITY, operationList);
            } catch (OperationApplicationException | RemoteException e) {

            }
        }
    }

    private static void moveOutChildrenCategories(Context context,
                                                  ArrayList<ContentProviderOperation> operations,
                                                  SCategory parent_category,
                                                  long company_id) {
        /**
         * TODO: this is a very inefficient implementation, we are fetching EVERY
         * child category and updating it while what we should be doing is run a single
         * query to update categories that have their parent set to this particular category.
         */
        Cursor cursor = context.getContentResolver().query(CategoryEntry.buildBaseUri(company_id),
                SCategory.CATEGORY_COLUMNS,
                CategoryEntry._fullCurrent(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[] {String.valueOf(parent_category.category_id)},
                null);
        if (cursor == null)
            return;

        if (!cursor.moveToFirst())  {
            cursor.close();
            return;
        }

        do {
            SCategory category = new SCategory(cursor);
            category.category_id = parent_category.parent_id;

            // we don't want to update deleted and new(un-synced) flag
            if (category.change_status != ChangeTraceable.CHANGE_STATUS_DELETED &&
                    category.change_status != ChangeTraceable.CHANGE_STATUS_CREATED) {
                category.change_status = ChangeTraceable.CHANGE_STATUS_UPDATED;
            }
            // we don't want a conflict to arise
            ContentValues values = category.toContentValues();
            values.remove(CategoryEntry.COLUMN_CATEGORY_ID);

            operations.add(ContentProviderOperation.newUpdate(
                    CategoryEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID),
                            new String[]{String.valueOf(category.category_id)}).
                    build());
        } while (cursor.moveToNext());
        cursor.close();
    }

    private static void moveOutItemsInsideCategory(Context context,
                                                   ArrayList<ContentProviderOperation> operations,
                                                  SCategory parent_category,
                                                  long company_id) {
        /**
         * TODO: this is a very inefficient implementation, we are fetching EVERY
         * item in the category and updating it. We should be running a single
         * query to update categories that have their parent set to this particular category.
         */
        Cursor cursor = context.getContentResolver().query(ItemEntry.buildBaseUri(company_id),
                SItem.ITEM_COLUMNS,
                ItemEntry._full(ItemEntry.COLUMN_CATEGORY_ID) + " = ?",
                new String[] {String.valueOf(parent_category.category_id)},
                null);
        if (cursor == null)
            return;

        if (!cursor.moveToFirst())  {
            cursor.close();
            return;
        }

        do {
            SItem item = new SItem(cursor);
            item.category = parent_category.parent_id;

            if (item.change_status != ChangeTraceable.CHANGE_STATUS_DELETED &&
                    item.change_status != ChangeTraceable.CHANGE_STATUS_CREATED) {
                item.change_status = ChangeTraceable.CHANGE_STATUS_UPDATED;
            }
            ContentValues values = item.toContentValues();
            // we don't want a conflict to arise.
            values.remove(ItemEntry.COLUMN_ITEM_ID);

            operations.add(ContentProviderOperation.newUpdate(
                    ItemEntry.buildBaseUri(company_id)).
                    withValues(values).
                    withSelection(ItemEntry._full(ItemEntry.COLUMN_ITEM_ID) + " = ?",
                            new String[]{String.valueOf(item.item_id)}).
                    build());
        } while (cursor.moveToNext());
        cursor.close();
    }

    /**
     * After categories have been moved, we need to update the branch categories to make
     * items visible in the branch, and this is done for all branches as different branches
     * have different items. So, for each item a branch has, its category tree should allow it
     * to access it. It also needs to remove any categories that were "visible" in the branch
     * before category movement happened and now have become "empty" categories with no items.
     */
    public static void updateBranchCategoriesForAllBranches(Context context) {
        Map<Long, CategoryNode> categoryTree = parseCategoryTree(context);
        List<SBranch> branches = getAllBranches(context);

        long company_id = PrefUtil.getCurrentCompanyId(context);

        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        for (SBranch branch : branches) {
            List<SBranchCategory> branchCategories = getBranchCategories(context, branch);
            List<SBranchItem> branchItems = getBranchItems(context, branch);

            Set<Long> itemCategories = new HashSet<>();
            for (SBranchItem branchItem : branchItems) {
                /**
                 * Don't "count" items that are invisible.
                 * IMPORTANT: If a category only contains invisible items, it should be deleted!!!
                 */
                if (branchItem.item.status_flag == ItemEntry.STATUS_INVISIBLE)
                    continue;

                itemCategories.add(branchItem.item.category);
            }

            Set<Long> visited_categories = new HashSet<>();
            for (Long item_category : itemCategories) {
                CategoryNode node = categoryTree.get(item_category);

                while (node.categoryId != CategoryEntry.ROOT_CATEGORY_ID) {
                    if (visited_categories.contains(node.categoryId)) break;
                    visited_categories.add(node.categoryId);

                    // go up the category tree
                    node = node.parentNode;
                }
            }

            Set<Long> previous_categories = new HashSet<>();

            // if we need to delete them, then the un-synced can be TOTALLY deleted LOCALLY.
            Set<Long> un_synced_categories = new HashSet<>();

            // if we had deleted them, but not yet synced the "delete-action", and we then try
            // to add them BACK, that should just ignore the "delete-action" altogether
            Map<Long, SBranchCategory> marked_as_deleted = new HashMap<>();

            for (SBranchCategory branchCategory : branchCategories) {
                previous_categories.add(branchCategory.category_id);

                if (branchCategory.change_status == ChangeTraceable.CHANGE_STATUS_CREATED)
                    un_synced_categories.add(branchCategory.category_id);
                else if (branchCategory.change_status == ChangeTraceable.CHANGE_STATUS_DELETED)
                    marked_as_deleted.put(branchCategory.category_id, branchCategory);
            }

            // these will be added to the branch
            Set<Long> newly_added_categories = setDifference(visited_categories, previous_categories);
            // we should restore them.
            /**
             * We should restore these branch categories. We had marked them as deleted, but we haven't
             * synced the "delete-action". We are now trying to "RE-ADD them". So just restore them
             * to the "un-deleted" state. The un-deleted state is the synced state because if we've marked
             * them as delete and not actually deleted them. That means they had been synced previously and
             * can't be just removed locally. So restoring them as "synced" is the "correct" way.
             */
            Set<Long> categories_to_restore = setIntersection(visited_categories, marked_as_deleted.keySet());

            // these existed before, but are not used now. Will be removed
            Set<Long> unseen_categories = setDifference(previous_categories, visited_categories);

            // add the new categories
            for (Long category_id : newly_added_categories) {
                ContentValues values = new ContentValues();
                values.put(BranchCategoryEntry.COLUMN_COMPANY_ID, company_id);
                values.put(BranchCategoryEntry.COLUMN_BRANCH_ID, branch.branch_id);
                values.put(BranchCategoryEntry.COLUMN_CATEGORY_ID, category_id);
                values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_CREATED);
                operationList.add(ContentProviderOperation.newInsert(
                        BranchCategoryEntry.buildBaseUri(company_id)).
                        withValues(values).build());
            }

            for (Long restore_category_id : categories_to_restore) {
                SBranchCategory branchCategory = marked_as_deleted.get(restore_category_id);
                branchCategory.change_status = ChangeTraceable.CHANGE_STATUS_SYNCED;

                String selection = BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_BRANCH_ID) + " = ? AND " +
                        BranchCategoryEntry._full(BranchCategoryEntry.COLUMN_CATEGORY_ID) + " = ?";
                String[] selectionArgs = new String[]{
                        String.valueOf(branch.branch_id),
                        String.valueOf(restore_category_id)
                };
                operationList.add(ContentProviderOperation.newUpdate(
                        BranchCategoryEntry.buildBaseUri(company_id)).
                        withValues(branchCategory.toContentValues()).
                        withSelection(selection, selectionArgs).build());
            }

            // remove the previously existing, but not currently being used ones
            for (Long category_id : unseen_categories) {
                String selection = String.format(Locale.US, "%s = ? AND %s = ?",
                        BranchCategoryEntry.COLUMN_BRANCH_ID, BranchCategoryEntry.COLUMN_CATEGORY_ID);
                String[] selection_args = new String[]{
                        String.valueOf(branch.branch_id),
                        String.valueOf(category_id)};

                ContentProviderOperation operation;

                // the branch category hasn't been synced, so it can just be deleted locally
                if (un_synced_categories.contains(category_id)) {
                    operation = ContentProviderOperation.newDelete(
                            BranchCategoryEntry.buildBaseUri(company_id)).
                            withSelection(selection, selection_args).build();
                } else {
                    // we need to set the deleted flag
                    ContentValues values = new ContentValues();
                    values.put(BranchCategoryEntry.COLUMN_COMPANY_ID, company_id);
                    values.put(ChangeTraceable.COLUMN_CHANGE_INDICATOR, ChangeTraceable.CHANGE_STATUS_DELETED);
                    operation = ContentProviderOperation.newUpdate(
                            BranchCategoryEntry.buildBaseUri(company_id)).
                            withValues(values).withSelection(selection, selection_args).build();
                }
                operationList.add(operation);
            }
        }

        try {
            context.getContentResolver().
                    applyBatch(SheketContract.CONTENT_AUTHORITY, operationList);
        } catch (OperationApplicationException | RemoteException e) {

        }
    }

    /**
     * Fetches the categories that exist inside the branch
     */
    private static List<SBranchCategory> getBranchCategories(Context context, SBranch branch) {
        List<SBranchCategory> branchCategories = new ArrayList<>();
        Uri uri = BranchCategoryEntry.buildBranchCategoryUri(PrefUtil.getCurrentCompanyId(context),
                branch.branch_id, BranchCategoryEntry.NO_ID_SET);
        Cursor cursor = context.getContentResolver().query(
                uri,
                SBranchCategory.BRANCH_CATEGORY_COLUMNS,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                branchCategories.add(new SBranchCategory(cursor));
            } while (cursor.moveToNext());
        }
        return branchCategories;
    }

    /**
     * Gets the items that exist inside the branch.
     */
    private static List<SBranchItem> getBranchItems(Context context, SBranch branch) {
        List<SBranchItem> branchItems = new ArrayList<>();
        Uri uri = BranchItemEntry.buildAllItemsInBranchUri(PrefUtil.getCurrentCompanyId(context),
                branch.branch_id);
        Cursor cursor = context.getContentResolver().query(
                uri,
                SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMNS,
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                branchItems.add(new SBranchItem(cursor, true));
            } while (cursor.moveToNext());
        }
        return branchItems;
    }

    private static List<SBranch> getAllBranches(Context context) {
        List<SBranch> branches = new ArrayList<>();

        long company_id = PrefUtil.getCurrentCompanyId(context);

        String sortOrder = BranchEntry._full(BranchEntry.COLUMN_BRANCH_ID) + " ASC";
        Cursor cursor = context.getContentResolver().
                query(BranchEntry.buildBaseUri(company_id),
                        SBranch.BRANCH_COLUMNS, null, null, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                branches.add(new SBranch(cursor));
            } while (cursor.moveToNext());
        }
        return branches;
    }

    /**
     * Does left - right
     */
    private static Set<Long> setDifference(Set<Long> left, Set<Long> right) {
        Set<Long> left_copy = new HashSet<>(left);
        left_copy.removeAll(right);
        return left_copy;
    }

    private static Set<Long> setIntersection(Set<Long> left, Set<Long> right) {
        Set<Long> result = new HashSet<>();
        for (Long l : left) {
            if (right.contains(l)) {
                result.add(l);
            }
        }
        return result;
    }

}
