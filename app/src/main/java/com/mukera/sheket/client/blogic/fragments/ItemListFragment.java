package com.mukera.sheket.client.blogic.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.blogic.ActionActivity;
import com.mukera.sheket.client.blogic.ItemDetailActivity;
import com.mukera.sheket.client.contentprovider.SheketContract.*;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/4/16.
 */
public class ItemListFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final String CATEGORY_ID_KEY = "category_key";
    private int mCategoryId;

    private ListView mItemList;
    private ItemCursorAdapter mItemAdapter;

    public static ItemListFragment newInstance(int category_id) {
        Bundle args = new Bundle();
        args.putInt(CATEGORY_ID_KEY, category_id);

        ItemListFragment fragment = new ItemListFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mCategoryId = args.getInt(CATEGORY_ID_KEY);
            getLoaderManager().initLoader(mCategoryId, null, this);
        }
    }

    void startActionActivity(int action) {
        Intent intent = new Intent(getActivity(), ActionActivity.class);
        intent.putExtra(ActionActivity.LAUNCH_ACTION_KEY, String.valueOf(action));
        startActivity(intent);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_list, container, false);

        FloatingActionButton buyAction, searchAction, sellAction;

        buyAction = (FloatingActionButton) rootView.findViewById(R.id.main_buy_btn);
        buyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActionActivity(ActionActivity.LAUNCH_TYPE_BUY);
            }
        });
        searchAction = (FloatingActionButton) rootView.findViewById(R.id.main_search_btn);
        searchAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActionActivity(ActionActivity.LAUNCH_TYPE_SEARCH);
            }
        });
        sellAction = (FloatingActionButton) rootView.findViewById(R.id.main_sell_btn);
        sellAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActionActivity(ActionActivity.LAUNCH_TYPE_SELL);
            }
        });

        mItemList = (ListView) rootView.findViewById(R.id.item_list_view);
        mItemAdapter = new ItemCursorAdapter(getActivity());
        mItemList.setAdapter(mItemAdapter);
        mItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mItemAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item = new SItem(cursor);

                    Intent intent = new Intent(getActivity(), ItemDetailActivity.class);

                    intent.putExtra(ItemDetailActivity.ITEM_ID_KEY, String.valueOf(item.id));

                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(mCategoryId, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        String sortOrder = ItemEntry._full(ItemEntry._ID) + " ASC";
        Uri uri;

        if (mCategoryId != CategoryEntry.DEFAULT_CATEGORY_ID)
            uri = ItemEntry.buildItemWithCategoryId(mCategoryId);
        else
            uri = ItemEntry.CONTENT_URI;

        return new CursorLoader(getActivity(),
                uri,
                SItem.ITEM_COLUMNS,
                null, null,
                sortOrder
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mItemAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mItemAdapter.swapCursor(null);
    }
}
