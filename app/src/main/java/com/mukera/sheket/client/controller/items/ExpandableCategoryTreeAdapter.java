package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ExpandableListView;

import com.mukera.sheket.client.R;

/**
 * Created by fuad on 7/19/16.
 *
 * An adapter that supports 2 cursors to be displayed. The first is used for categoryNavigation. The second
 * is used to populate items.
 */
public class ExpandableCategoryTreeAdapter extends CursorTreeAdapter {
    public static final int GROUP_CATEGORY = 0;
    public static final int GROUP_ITEMS = 1;

    /**
     * This is a hack to workaround the limitations of {@link CursorTreeAdapter} providing
     * us with only 1-set of child views. We override the {@code getChildTypeCount}
     * to return 2 and switch between category and item mode when we create/bind views.
     *
     * This is expected to work b/c everything is being called in a single thread,
     * so the states we save on {@code mViewType} can be trusted.
     */
    private static final int VIEW_TYPE_NONE = -1;
    private static final int VIEW_TYPE_CATEGORY = 0;
    private static final int VIEW_TYPE_ITEM = 1;
    private static final int VIEW_TYPE_COUNT = 2;

    private int mViewType = VIEW_TYPE_NONE;
    private int mChildPosition = -1;

    private ExpandableCategoryTreeListener mListener;
    public interface ExpandableCategoryTreeListener {
        /**
         * Create a new category view for the cursor at the position.
         */
        View newCategoryView(Context context, ViewGroup parent, Cursor cursor, int position);
        View newItemView(Context context, ViewGroup parent, Cursor cursor, int position);

        /**
         * Bind the view at the position to the data in the cursor.
         */
        void bindCategoryView(Context context, Cursor cursor, View view, int position);
        void bindItemView(Context context, Cursor cursor, View view, int position);
    }

    private ExpandableCategoryTreeAdapter setListener(ExpandableCategoryTreeListener listener) {
        mListener = listener;
        return this;
    }

    /**
     * This is the "constructor" for this adapter.
     */
    public static ExpandableCategoryTreeAdapter newAdapter(Context context,
                                                           ExpandableCategoryTreeListener listener) {
        // we have to name it "_id" for the CursorAdapter to work
        MatrixCursor groupCursor = new MatrixCursor(new String[]{"Group Name", "_id"});

        groupCursor.newRow().add("Category").add(GROUP_CATEGORY);
        groupCursor.newRow().add("Items").add(GROUP_ITEMS);

        return new ExpandableCategoryTreeAdapter(groupCursor, context).setListener(listener);
    }

    private ExpandableCategoryTreeAdapter(Cursor cursor, Context context) {
        super(cursor, context);
    }

    /**
     * Used to populate the category tree UI.
     */
    public void setCategoryCursor(Cursor cursor) {
        setChildrenCursor(GROUP_CATEGORY, cursor);
    }

    /**
     * Used to display the item list.
     */
    public void setItemsCursor(Cursor cursor) {
        setChildrenCursor(GROUP_ITEMS, cursor);
    }

    @Override
    protected Cursor getChildrenCursor(Cursor groupCursor) {
        // we return nil now so when the data is ready(e.g: after onLoaderFinished)
        // it can be set to the valid cursor
        return null;
    }

    @Override
    protected View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.list_item_category_group_view_separator, parent, false);
        // start all expanded, then the rest can do as they like
        ((ExpandableListView)parent).expandGroup(cursor.getPosition());
        return view;
    }

    @Override
    protected void bindGroupView(View view, Context context, Cursor cursor, boolean isExpanded) {
    }

    /**
     * We've overrode this to simulate having multiple view types.
     */
    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        switch (groupPosition) {
            case GROUP_CATEGORY: mViewType = VIEW_TYPE_CATEGORY; break;
            case GROUP_ITEMS: mViewType = VIEW_TYPE_ITEM; break;
            default: mViewType = VIEW_TYPE_NONE; break;
        }

        mChildPosition = childPosition;
        //
        /**
         * Let the super class worry about fetching the appropriate cursor for the group and stuff.
         * This will call {@code newChildView} if it can't recycle the old views. Then it calls
         * {@code bindChildView}. We use the {@link mViewType} to decide which set of view it is.
         */
        View view = super.getChildView(groupPosition, childPosition, isLastChild,
                convertView, parent);
        mViewType = VIEW_TYPE_NONE;
        return view;
    }

    @Override
    protected View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
        switch (mViewType) {
            case VIEW_TYPE_CATEGORY:
                return mListener.newCategoryView(context, parent, cursor, mChildPosition);
            case VIEW_TYPE_ITEM:
                return mListener.newItemView(context, parent, cursor, mChildPosition);
            default:
                throw new IllegalStateException("newChildView The ViewType isn't a type we defined: " + mViewType);
        }
    }

    @Override
    protected void bindChildView(View view, Context context, Cursor cursor, boolean isLastChild) {
        switch (mViewType) {
            case VIEW_TYPE_CATEGORY:
                mListener.bindCategoryView(context, cursor, view, mChildPosition);
                break;
            case VIEW_TYPE_ITEM:
                mListener.bindItemView(context, cursor, view, mChildPosition);
                break;
            default:
                throw new IllegalStateException("bindChildView: The ViewType isn't a type we defined: " + mViewType);
        }
    }

    @Override
    public int getChildTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    public int getChildType(int groupPosition, int childPosition) {
        switch (groupPosition) {
            case 0: return VIEW_TYPE_CATEGORY;
            case 1: return VIEW_TYPE_ITEM;
            default:
                throw new IllegalStateException("getChildType only supported for 2 types: " + groupPosition);
        }
    }
}
