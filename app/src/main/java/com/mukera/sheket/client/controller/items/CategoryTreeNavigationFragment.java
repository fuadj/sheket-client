package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.Stack;

/**
 * Created by fuad on 6/4/16.
 *
 * <p>This fragment enables traversing category ancestry tree in the UI.
 * By extending this class and overriding its methods, sub classes can be notified
 * of the various states of the traversal. The traversal starts at the root category.
 * When the user selects a category, it will update the UI to look into the category.
 * This fragment keeps a stack of the categories visited so going back is possible.</p>
 */
public abstract class CategoryTreeNavigationFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    protected long mCurrentCategoryId;
    protected Stack<Long> mCategoryBackstack;

    private ListView mCategoryList;
    protected CategoryAdapter mCategoryAdapter = null;

    private View mDividerView;

    protected void initLoader() {
        getLoaderManager().initLoader(getCategoryLoaderId(), null, this);
        onInitLoader();
    }

    protected void restartLoader() {
        getLoaderManager().restartLoader(getCategoryLoaderId(), null, this);
        onRestartLoader();
    }

    /**
     * Since sub-classes inflate their own UI, they should provide the resolve layout id
     * to be inflated.
     * NOTE: the inflated UI should should embed the layout {@code R.layout.embedded_category_tree_navigation},
     * which contains the category navigation list and a divider view.
     * @return
     */
    protected abstract int getLayoutResId();

    protected abstract int getCategoryLoaderId();


    /**
     * Subclasses can override this to get notified when a category is selected.
     * NOTE: This is called before any changes are done so subclasses can
     * prepare for the change.(E.g: if a category is selected to view its subcategories,
     * this will be called before the LoaderManager is Restarted. This ensures that
     * subclasses have prepared for their Loader's to restart by setting any
     * necessary internal variables to the appropriate state.)
     *
     * @param previous_category     This this the last category visited before selecting the category
     * @param selected_category
     */
    public void onCategorySelected(long previous_category, long selected_category) { }

    /**
     * Subclasses can override this to get notified on loader initialization
     */
    public void onInitLoader() { }

    /**
     * Subclasses can override this to get notified on loader restart
     */
    public void onRestartLoader() { }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCategoryBackstack = new Stack<>();
        // we start at the root
        mCurrentCategoryId = CategoryEntry.ROOT_CATEGORY_ID;

        setHasOptionsMenu(true);
    }

    /**
     * Sets the category as the current and pushed the previous on the backstack.
     *
     * @param category_id
     * @return  the previous category
     */
    protected long setCurrentCategory(long category_id) {
        // the root category is only added to the "bottom" of the stack
        if (category_id == CategoryEntry.ROOT_CATEGORY_ID)
            return mCurrentCategoryId;

        long previous_category = mCurrentCategoryId;
        mCategoryBackstack.push(mCurrentCategoryId);
        mCurrentCategoryId = category_id;
        return previous_category;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        initLoader();
        super.onActivityCreated(savedInstanceState);
    }

    /**
     * Override this to create your own adapter for the category list.
     */
    protected CategoryAdapter getCategoryAdapter() {
        if (mCategoryAdapter == null) {
            mCategoryAdapter = new CategoryChildrenArrayAdapter(getActivity());
        }
        return mCategoryAdapter;
    }

    /**
     * Sub-classes can override this and define their own rules. This is then used
     * to determine whether to show the category navigation list view or not.
     * @return
     */
    protected boolean isShowingCategoryTree() {
        return PrefUtil.getShowCategoryTreeState(getActivity());
    }

    protected void onCategoryTreeViewToggled(boolean show_tree_view) {
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (displayCategoryToggleActionBarOption()) {
            inflater.inflate(R.menu.category_navigation_menu, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_category_navigation_toggle: {
                boolean state = PrefUtil.getShowCategoryTreeState(getActivity());
                state = !state;
                PrefUtil.setShowCategoryTreeState(getActivity(), state);

                onCategoryTreeViewToggled(state);

                if (state) {
                    onCategorySelected(mCurrentCategoryId, mCurrentCategoryId);
                } else {
                    onCategorySelected(CategoryEntry.ROOT_CATEGORY_ID, CategoryEntry.ROOT_CATEGORY_ID);
                }
                restartLoader();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Override this to disable the toggle view.
     * @return
     */
    protected boolean displayCategoryToggleActionBarOption() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResId(), container, false);

        mCategoryList = (ListView) rootView.findViewById(R.id.category_tree_navigation_list_view);
        mCategoryList.setAdapter(getCategoryAdapter());
        mCategoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SCategory category = getCategoryAdapter().getCategoryAt(position);

                long previous_category = setCurrentCategory(category.category_id);

                onCategorySelected(previous_category, category.category_id);
                restartLoader();
            }
        });

        mDividerView = rootView.findViewById(R.id.embedded_category_list_separator_divider);

        /**
         * This handles the "back" button key. If we are in a sub-category
         * and press back, we will move up to the parent category
         */
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mCategoryBackstack.isEmpty()) {
                        /**
                         * If we are not inside a sub-category and we press the back button,
                         * it should naturally do what is mostly expected which is move back to the
                         * previous fragment.
                         */
                        getActivity().onBackPressed();
                    } else {
                        /**
                         * If we were inside a sub-category, we should move back to the parent
                         * and notify of that.
                         */
                        long previous_category = mCategoryBackstack.peek();
                        mCurrentCategoryId = mCategoryBackstack.pop();
                        onCategorySelected(previous_category, mCurrentCategoryId);
                        restartLoader();
                    }
                    return true;
                }

                return false;
            }
        });

        return rootView;
    }

    /**
     * Override these 3 methods to load your data.
     */
    protected abstract Loader<Cursor> onCategoryTreeCreateLoader(int id, Bundle args);
    protected abstract void onCategoryTreeLoaderFinished(Loader<Cursor> loader, Cursor data);
    protected abstract void onCategoryTreeLoaderReset(Loader<Cursor> loader);

    /**
     * Override this to create another loader.
     */
    protected Loader<Cursor> getCategoryTreeLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._fullCurrent(CategoryEntry.COLUMN_NAME) + " ASC";

        return new CursorLoader(getActivity(),
                CategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SCategory.CATEGORY_WITH_CHILDREN_COLUMNS,
                CategoryEntry._fullCurrent(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[]{String.valueOf(mCurrentCategoryId)},
                sortOrder);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != getCategoryLoaderId())
            return onCategoryTreeCreateLoader(id, args);

        return getCategoryTreeLoader(id, args);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == getCategoryLoaderId()) {
            mCategoryAdapter.setCategoryCursor(data);
            setCategoryListVisibility(isShowingCategoryTree());
        } else {
            onCategoryTreeLoaderFinished(loader, data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != getCategoryLoaderId()) {
            mCategoryAdapter.setCategoryCursor(null);
            setCategoryListVisibility(false);
        } else {
            onCategoryTreeLoaderReset(loader);
        }
    }

    void setCategoryListVisibility(boolean show_list) {
        if (show_list) {
            mCategoryList.setVisibility(View.VISIBLE);
            ListUtils.setDynamicHeight(mCategoryList);
            mDividerView.setVisibility(mCategoryList.getAdapter().getCount() > 0 ? View.VISIBLE : View.GONE);
        } else {
            mCategoryList.setVisibility(View.GONE);
            mDividerView.setVisibility(View.GONE);
        }
    }

    /**
     * The adapter the {@code CategoryTreeNavigationFragment} uses to load
     * the category data should implement this. The underlying implementation can
     * be {@code CursorAdapter}, {@code ArrayAdapter} OR what ever.
     */
    public interface CategoryAdapter extends ListAdapter {
        SCategory getCategoryAt(int position);

        /**
         * The data is populated through this method.
         */
        void setCategoryCursor(Cursor cursor);
    }

    /**
     * This adapter loads the categories 2-level, so the parent are loaded along with their children.
     */
    public static class CategoryChildrenArrayAdapter extends ArrayAdapter<SCategory> implements CategoryAdapter {
        private static class ViewHolder {
            TextView categoryName, childrenCount;

            public ViewHolder(View view) {
                categoryName = (TextView) view.findViewById(R.id.list_item_category_tree_text_view_name);
                childrenCount = (TextView) view.findViewById(R.id.list_item_category_tree_text_view_sub_count);
            }
        }

        public CategoryChildrenArrayAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public SCategory getCategoryAt(int position) {
            return super.getItem(position);
        }

        @Override
        public void setCategoryCursor(Cursor cursor) {
            setNotifyOnChange(false);

            clear();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SCategory parent_category = new SCategory(cursor, true);
                    super.add(parent_category);
                } while (cursor.moveToNext());
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SCategory category = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_category_tree_navigation, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.categoryName.setText(category.name);
            if (category.childrenCategories.isEmpty()) {
                holder.childrenCount.setVisibility(View.GONE);
            } else {
                holder.childrenCount.setVisibility(View.VISIBLE);
                holder.childrenCount.setText("" + category.childrenCategories.size());
            }
            return convertView;
        }
    }
}
