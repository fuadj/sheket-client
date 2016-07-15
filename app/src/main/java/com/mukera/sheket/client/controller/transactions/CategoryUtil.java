package com.mukera.sheket.client.controller.transactions;

import android.content.Context;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.HashMap;
import java.util.Map;

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
}
