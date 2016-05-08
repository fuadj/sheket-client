package com.mukera.sheket.client.controller.items;

import android.net.Uri;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.data.SheketContract;
import com.mukera.sheket.client.models.SItem;
import com.mukera.sheket.client.utility.PrefUtil;

/**
 * Created by gamma on 3/5/16.
 */
public class ItemDetailActivity extends AppCompatActivity {
    private static final String LOG_TAG = ItemDetailActivity.class.getSimpleName();

    public static final String ITEM_ID_KEY = "ITEM_ID_KEY";
    public static final long INVALID_ITEM_ID = -1L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        if (savedInstanceState == null) {
            ItemDetailFragment fragment = new ItemDetailFragment();
            Bundle args = new Bundle();
            args.putLong(ITEM_ID_KEY, getIntent().getLongExtra(ITEM_ID_KEY, INVALID_ITEM_ID));
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.item_detail_container, fragment)
                    .commit();
        }
    }

    /**
     * Created by gamma on 3/5/16.
     */
    public static class ItemDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private long mItemId;

        public static ItemDetailFragment newInstance(int item_id) {
            Bundle args = new Bundle();
            args.putInt(ITEM_ID_KEY, item_id);

            ItemDetailFragment fragment = new ItemDetailFragment();
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState == null) {
                Bundle args = getArguments();
                mItemId = args.getLong(ITEM_ID_KEY,
                        INVALID_ITEM_ID);
                getLoaderManager().initLoader(LoaderId.ITEM_DETAIL_LOADER, null, this);
            }
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_item_list, container, false);

            FloatingActionButton buyAction, sellAction;

            /*
            buyAction = (FloatingActionButton) rootView.findViewById(R.id.main_buy_btn);
            buyAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startTransactionActivity(TransactionActivity.LAUNCH_TYPE_BUY);
                }
            });
            sellAction = (FloatingActionButton) rootView.findViewById(R.id.main_sell_btn);
            sellAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startTransactionActivity(TransactionActivity.LAUNCH_TYPE_SELL);
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

                        Intent intent = new Intent(getActivity(), TransactionActivity.class);

                        intent.putExtra(TransactionActivity.LAUNCH_ACTION_KEY, TransactionActivity.LAUNCH_TYPE_ITEM_DETAIL);
                        intent.putExtra(TransactionActivity.LAUNCH_ITEM_ID_KEY, item.id);

                        startActivity(intent);
                    }
                }
            });

            */
            return rootView;
        }

        @Override
        public Loader onCreateLoader(int id, Bundle args) {
            if (mItemId == INVALID_ITEM_ID) return null;

            long company_id = PrefUtil.getCurrentCompanyId(getContext());
            Uri uri = SheketContract.ItemEntry.buildItemUri(company_id, mItemId);
            return new CursorLoader(getActivity(),
                    uri,
                    SItem.ITEM_COLUMNS,
                    null, null,
                    null
            );
        }

        void displayErrorMsg() {

        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null && data.moveToNext()) {
                SItem item = new SItem(data);
            } else {
                displayErrorMsg();
            }
        }

        @Override
        public void onLoaderReset(Loader loader) {
            displayErrorMsg();
        }
    }
}
