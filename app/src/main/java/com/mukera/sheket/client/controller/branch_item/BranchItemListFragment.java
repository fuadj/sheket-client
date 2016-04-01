package com.mukera.sheket.client.controller.branch_item;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.widget.*;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.TransactionActivity;
import com.mukera.sheket.client.controller.items.ItemDetailActivity;
import com.mukera.sheket.client.controller.NewItemActivity;
import com.mukera.sheket.client.data.SheketContract.*;
import com.mukera.sheket.client.models.SBranchItem;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/27/16.
 */
public class BranchItemListFragment extends Fragment implements LoaderCallbacks<Cursor> {
    final int NEW_ITEM_REQUEST = 1;

    private static final String BRANCH_ID_KEY = "branch_id_key";

    private int mBranchId;

    private ListView mBranchItemList;
    private BranchItemCursorAdapter mBranchItemAdapter;

    public static BranchItemListFragment newInstance(int branch_id) {
        Bundle args = new Bundle();

        BranchItemListFragment fragment = new BranchItemListFragment();
        args.putInt(BRANCH_ID_KEY, branch_id);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mBranchId = args.getInt(BRANCH_ID_KEY);
        }
    }

    void startTransactionActivity(int action) {
        Intent intent = new Intent(getActivity(), TransactionActivity.class);
        intent.putExtra(TransactionActivity.LAUNCH_ACTION_KEY, action);
        startActivity(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_ITEM_REQUEST) {
            // Loader handles the adding of new data, we don't need to request
        }
    }

    int getLoaderId() {
        // b/c both are negative, it is a "bigger" number
        return LoaderId.BRANCH_ITEM_LIST_LOADER - mBranchId;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_list, container, false);

        getLoaderManager().restartLoader(getLoaderId(), null, this);

        AppCompatActivity act = (AppCompatActivity) getActivity();
        View v_toolbar = act.findViewById(R.id.toolbar);
        if (v_toolbar != null) {
            Toolbar toolbar = (Toolbar) v_toolbar;
            ImageButton addBtn = (ImageButton) toolbar.findViewById(R.id.toolbar_btn_add);
            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), NewItemActivity.class);
                    startActivityForResult(intent, NEW_ITEM_REQUEST);
                }
            });
        }

        FloatingActionButton buyAction, sellAction;

        buyAction = (FloatingActionButton) rootView.findViewById(R.id.float_btn_item_list_buy);
        buyAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionActivity(TransactionActivity.LAUNCH_TYPE_BUY);
            }
        });
        sellAction = (FloatingActionButton) rootView.findViewById(R.id.float_btn_item_list_sell);
        sellAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTransactionActivity(TransactionActivity.LAUNCH_TYPE_SELL);
            }
        });

        mBranchItemList = (ListView) rootView.findViewById(R.id.list_view_item_list);
        mBranchItemAdapter = new BranchItemCursorAdapter(getActivity());
        mBranchItemList.setAdapter(mBranchItemAdapter);
        mBranchItemList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mBranchItemAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SBranchItem branchItem = new SBranchItem(cursor, true);

                    Intent intent = new Intent(getActivity(), ItemDetailActivity.class);

                    intent.putExtra(ItemDetailActivity.ITEM_ID_KEY, branchItem.item_id);
                    startActivity(intent);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(getLoaderId(), null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(getLoaderId(), null, this);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                BranchItemEntry.buildItemsInBranchUri(mBranchId),
                SBranchItem.BRANCH_ITEM_WITH_DETAIL_COLUMN,
                null, null,
                BranchItemEntry._full(BranchItemEntry.COLUMN_ITEM_ID) + " ASC"
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBranchItemAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mBranchItemAdapter.swapCursor(null);
    }
}
