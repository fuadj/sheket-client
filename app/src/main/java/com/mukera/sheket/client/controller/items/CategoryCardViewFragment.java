package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mukera.sheket.client.utils.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.base_adapters.ArrayRecyclerAdapter;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 6/2/16.
 */
public class CategoryCardViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private RecyclerView mCategoryList;
    private CategoryViewAdapter mCategoryAdapter;

    private SelectionListener mListener;
    private CardViewToggleListener mCardListener;

    public interface SelectionListener {
        void onCategorySelected(SCategory category);
    }

    public CategoryCardViewFragment setCategorySelectionListener(SelectionListener listener) {
        mListener = listener;
        return this;
    }

    public CategoryCardViewFragment setCardViewToggleListener(CardViewToggleListener listener) {
        mCardListener = listener;
        return this;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.MainActivity.CATEGORY_VIEW_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.categroy_cards, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.category_card_view_disable) {
            mCardListener.onCardOptionSelected(false);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_categories, container, false);

        mCategoryList = (RecyclerView) view.findViewById(R.id.view_category_list);
        mCategoryList.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mCategoryAdapter = new CategoryViewAdapter(getActivity());
        mCategoryAdapter.setListener(new CategoryViewAdapter.AdapterSelectionListener() {
            @Override
            public void onClick(int position) {
                SCategory category = mCategoryAdapter.getItem(position);
                mListener.onCategorySelected(category);
            }
        });
        mCategoryList.setAdapter(mCategoryAdapter);

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._fullParent(CategoryEntry.COLUMN_NAME) + " ASC";

        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        return new CursorLoader(getActivity(),
                CategoryEntry.buildBaseUri(company_id),
                SCategory.CATEGORY_COLUMNS,
                CategoryEntry._fullParent(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[]{String.valueOf(CategoryEntry.ROOT_CATEGORY_ID)},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCategoryAdapter.setCategoryCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCategoryAdapter.setCategoryCursor(null);
    }

    private static class CategoryViewAdapter extends ArrayRecyclerAdapter<SCategory, CategoryViewAdapter.ViewHolder> {

        static final int[] CATEGORY_COLORS = {
                0xff039BE5,
                //0xff9E9E9E,
                0xff3D51B3,
                0xff689f38,
                //0xff455A64,
                0xff607D8B,
                0xffFD7044
        };

        public interface AdapterSelectionListener {
            void onClick(int position);
        }

        public AdapterSelectionListener mAdapterListener;

        public void setListener(AdapterSelectionListener listener) { mAdapterListener = listener; }

        public CategoryViewAdapter(Context context) {
            super(context);
        }

        public void setCategoryCursor(Cursor cursor) {
            setNotifyOnChange(false);

            clear();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SCategory parent_category = new SCategory(cursor);
                    add(parent_category);
                } while (cursor.moveToNext());
            }

            setNotifyOnChange(true);
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view_category, parent,
                    false);
            return new ViewHolder(v, mAdapterListener);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            SCategory category = getItem(position);
            holder.titleTextView.setText(category.name);
            if (category.childrenCategories.isEmpty()) {
                holder.bodyTextView.setVisibility(View.GONE);
            } else {
                holder.bodyTextView.setVisibility(View.VISIBLE);
                StringBuilder s = new StringBuilder();
                for (int i = 0; i < category.childrenCategories.size(); i++) {
                    if (i != 0) s.append("\n");
                    s.append("\t" + category.childrenCategories.get(i).name);
                }
                holder.bodyTextView.setText(s.toString());
            }
            int n = (int)Math.ceil(Math.random() * (CATEGORY_COLORS.length + 1));
            holder.card.setCardBackgroundColor(CATEGORY_COLORS[n % CATEGORY_COLORS.length]);
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            public TextView titleTextView;
            public TextView bodyTextView;
            public CardView card;

            public ViewHolder(View itemView, final AdapterSelectionListener listener) {
                super(itemView);
                titleTextView = (TextView) itemView.findViewById(R.id.list_item_view_category_title);
                bodyTextView = (TextView) itemView.findViewById(R.id.list_item_view_category_body);

                card = (CardView) itemView.findViewById(R.id.list_item_view_category_card);
                card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onClick(getAdapterPosition());
                    }
                });
            }
        }
    }
}
