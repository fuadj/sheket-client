package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.base_adapters.BaseCategoryChildrenAdapter;
import com.mukera.sheket.client.controller.ListUtils;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

import java.util.Stack;

/**
 * Created by fuad on 6/4/16.
 */
public abstract class EmbeddedCategoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    protected abstract int getCategoryLoaderId();
    protected abstract int getLayoutResId();

    protected long mCurrentParentCategoryId;
    protected Stack<Long> mParentCategoryBackStack;

    private ListView mCategoryList;
    private CategoryAdapter mAdapter;
    private SwitchCompat mToggleCategoryView;

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
     * Subclasses can override this to get notified when a category is selected.
     * NOTE: This is called before any changes are done so subclasses can
     * prepare for the change.(E.g: if a category is selected to view its subcategories,
     * this will be called before the LoaderManager is Restarted. This ensures that
     * subclasses have prepared for their Loader's to restart by setting any
     * necessary internal variables to the appropriate state.)
     */
    public void onCategorySelected(long category_id) { }

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

        // The root category is the DEFAULT
        setParentCategoryId(CategoryEntry.ROOT_CATEGORY_ID);
        mParentCategoryBackStack = new Stack<>();
    }

    protected void setParentCategoryId(long category_id) {
        mCurrentParentCategoryId = category_id;
    }

    protected void addCategoryToStack(long category_id) {
        mParentCategoryBackStack.push(mCurrentParentCategoryId);
        mCurrentParentCategoryId = category_id;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        initLoader();
        super.onActivityCreated(savedInstanceState);
    }

    protected BaseAdapter getCategoryAdapter() {
        return mAdapter;
    }

    public boolean isShowingCategoryTree() {
        return PrefUtil.showCategoryTree(getActivity());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResId(), container, false);

        ActionBar actionBar = ((AppCompatActivity)getActivity()).
                getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowCustomEnabled(true);

            LayoutInflater toggleInfaltor = (LayoutInflater)getActivity().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View v = toggleInfaltor.inflate(R.layout.action_bar_category_toggle, null);
            actionBar.setCustomView(v);
            mToggleCategoryView = (SwitchCompat) v.findViewById(R.id.action_bar_toggle_category_layout);

            // set the current preference
            mToggleCategoryView.setChecked(PrefUtil.showCategoryTree(getActivity()));
            mToggleCategoryView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    PrefUtil.setShowCategoryTree(getActivity(), isChecked);
                    setCategoryListVisibility(isChecked);
                    long category_id;
                    if (!isChecked) {
                        category_id = CategoryEntry.ROOT_CATEGORY_ID;
                    } else {
                        category_id = mCurrentParentCategoryId;
                    }
                    onCategorySelected(category_id);
                    onRestartLoader();
                }
            });
        }
        mCategoryList = (ListView) rootView.findViewById(R.id.embedded_category_list_list_view);
        mAdapter = new CategoryAdapter(getActivity());
        mCategoryList.setAdapter(mAdapter);
        mCategoryList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                SCategory category = mAdapter.getItem(position);

                addCategoryToStack(category.category_id);

                onCategorySelected(category.category_id);
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
                    if (mParentCategoryBackStack.isEmpty()) {
                        /**
                         * If we are not inside a sub-category and we press the back button,
                         * it should naturally do what is mostly expected which is move back to the
                         * previous fragment.
                         */
                        // invalidate the action-bar stuff added by this

                        ActionBar actionBar = ((AppCompatActivity)getActivity()).
                                getSupportActionBar();
                        if (actionBar != null && (actionBar.getCustomView() != null))
                            actionBar.getCustomView().setVisibility(View.GONE);
                        getActivity().invalidateOptionsMenu();
                        getActivity().onBackPressed();
                        //getActivity().getSupportFragmentManager().popBackStack();
                    } else {
                        /**
                         * If we were inside a sub-category, we should move back to the parent
                         * and notify of that.
                         */
                        mCurrentParentCategoryId = mParentCategoryBackStack.pop();
                        onCategorySelected(mCurrentParentCategoryId);
                        restartLoader();
                    }
                    return true;
                }

                return false;
            }
        });

        return rootView;
    }

    protected abstract Loader<Cursor> onEmbeddedCreateLoader(int id, Bundle args);
    protected abstract void onEmbeddedLoadFinished(Loader<Cursor> loader, Cursor data);
    protected abstract void onEmbeddedLoadReset(Loader<Cursor> loader);

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != getCategoryLoaderId())
            return onEmbeddedCreateLoader(id, args);

        String sortOrder = CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " ASC";

        return new CursorLoader(getActivity(),
                CategoryEntry.buildBaseUri(PrefUtil.getCurrentCompanyId(getContext())),
                SCategory.CATEGORY_COLUMNS,
                CategoryEntry._full(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[]{String.valueOf(mCurrentParentCategoryId)},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() == getCategoryLoaderId()) {
            mAdapter.setCategoryCursor(data);
            setCategoryListVisibility(true);
        } else {
            onEmbeddedLoadFinished(loader, data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (loader.getId() != getCategoryLoaderId()) {
            mAdapter.setCategoryCursor(null);
            ListUtils.setDynamicHeight(mCategoryList);
            mDividerView.setVisibility(View.GONE);
        } else {
            onEmbeddedLoadReset(loader);
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

    public static class CategoryAdapter extends BaseCategoryChildrenAdapter {
        public CategoryAdapter(Context context) {
            super(context);
        }

        private static class ViewHolder {
            TextView categoryName, childrenCount;

            public ViewHolder(View view) {
                categoryName = (TextView) view.findViewById(R.id.list_item_select_category_name);
                childrenCount = (TextView) view.findViewById(R.id.list_item_select_category_sub_count);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            SCategory category = getItem(position);

            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.list_item_select_category, parent, false);
                holder = new ViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.categoryName.setText(category.name);
            holder.childrenCount.setText("" + category.childrenCategories.size());
            return convertView;
        }
    }
}
