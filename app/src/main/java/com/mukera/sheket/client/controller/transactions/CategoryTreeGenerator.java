package com.mukera.sheket.client.controller.transactions;

import android.content.Context;
import android.database.Cursor;

import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by fuad on 5/24/16.
 */
public class CategoryTreeGenerator {
    public static class CategoryNode {
        SCategory category;
        int node_depth;
    }

    private static class TreeNode extends CategoryNode {
        List<TreeNode> childCategories;
    }

    private static void enumerateCategories(Context context, List<SCategory> categories) {
        SCategory root_category = new SCategory();
        root_category.category_id = CategoryEntry.ROOT_CATEGORY_ID;
        categories.add(root_category);

        long company_id = PrefUtil.getCurrentCompanyId(context);

        String sortOrder = CategoryEntry._fullParent(CategoryEntry.COLUMN_CATEGORY_ID) + " ASC";
        Cursor cursor = context.getContentResolver().query(
                CategoryEntry.buildBaseUri(company_id), SCategory.CATEGORY_COLUMNS,
                null, null, sortOrder);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                SCategory category = new SCategory(cursor);
                categories.add(category);
            } while (cursor.moveToNext());
        }
    }

    public static void visitNodesInTree(TreeNode node,
                                        List<CategoryNode> result,
                                        int current_depth,
                                        Set<Long> visited) {
        if (visited.contains(node.category.category_id)) {
            // TODO: this will lead to infinite recursion
            // so we stop it
            return;
        }

        visited.add(node.category.category_id);

        node.node_depth = current_depth;
        result.add(node);

        for (TreeNode child : node.childCategories) {
            visitNodesInTree(child, result,
                    current_depth + 1,
                    visited);
        }
    }

    public static List<CategoryNode> createFlatCategoryTree(Context context) {
        List<SCategory> categoryList = new ArrayList<>();

        enumerateCategories(context, categoryList);

        // we want the order to be deterministic, we want to get the SAME result EVERY-TIME
        Map<Long, TreeNode> categoryMap = new LinkedHashMap<>();

        // pre-visit each categoryNode and add it to the id-mapping
        for (SCategory category : categoryList) {
            TreeNode node = new TreeNode();
            node.category = category;
            node.node_depth = 0;
            node.childCategories = new ArrayList<>();

            categoryMap.put(category.category_id, node);
        }

        // build the tree
        for (SCategory category : categoryList) {
            TreeNode parent_node = categoryMap.get(category.parent_id);
            TreeNode child_node = categoryMap.get(category.category_id);

            // If the categoryNode is the root categoryNode, don't bother to link it to its parent
            if (child_node.category.category_id == CategoryEntry.ROOT_CATEGORY_ID)
                continue;

            parent_node.childCategories.add(child_node);
        }

        List<CategoryNode> result = new ArrayList<>();

        TreeNode rootCategory = categoryMap.get(CategoryEntry.ROOT_CATEGORY_ID);
        visitNodesInTree(rootCategory, result, 0, new HashSet<Long>());
        return result;
    }
}
