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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SCategory;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by fuad on 6/2/16.
 */
public class CategoryViewFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private RecyclerView mCategoryList;
    private CategoryViewAdapter mCategoryAdapter;

    private SelectionListener mListener;

    public interface SelectionListener {
        void onCategorySelected(SCategory category);
    }

    public void setListener(SelectionListener listener) {
        mListener = listener;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.MainActivity.CATEGORY_VIEW_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_categories, container, false);

        mCategoryList = (RecyclerView) view.findViewById(R.id.view_category_list);
        mCategoryList.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        mCategoryAdapter = new CategoryViewAdapter(getActivity());
        mCategoryAdapter.mAdapterListener = new CategoryViewAdapter.AdapterSelectionListener() {
            @Override
            public void onClick(int position) {
                Cursor cursor = mCategoryAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SCategory category = new SCategory(cursor);
                    mListener.onCategorySelected(category);
                }
            }
        };
        mCategoryList.setAdapter(mCategoryAdapter);

        return view;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String sortOrder = CategoryEntry._full(CategoryEntry.COLUMN_CATEGORY_ID) + " ASC";

        long company_id = PrefUtil.getCurrentCompanyId(getContext());
        return new CursorLoader(getActivity(),
                CategoryEntry.buildBaseUri(company_id),
                SCategory.CATEGORY_COLUMNS,
                CategoryEntry._full(CategoryEntry.COLUMN_PARENT_ID) + " = ?",
                new String[]{String.valueOf(CategoryEntry.ROOT_CATEGORY_ID)},
                sortOrder);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCategoryAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCategoryAdapter.swapCursor(null);
    }

    private static class CategoryViewAdapter extends CursorRecyclerViewAdapter<CategoryViewAdapter.ViewHolder> {

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

        public CategoryViewAdapter(Context context) {
            super(context, null);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view_category, parent,
                    false);
            return new ViewHolder(v, mAdapterListener);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Cursor cursor, int position) {
            SCategory category = new SCategory(cursor);
            viewHolder.titleTextView.setText(category.name);
            viewHolder.bodyTextView.setText("Test Category");
            viewHolder.card.setCardBackgroundColor(CATEGORY_COLORS[position % CATEGORY_COLORS.length]);
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
