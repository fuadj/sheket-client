package com.mukera.sheket.client.input;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v4.app.LoaderManager.LoaderCallbacks;

import com.mukera.sheket.client.b_actions.ItemSelectionListener;
import com.mukera.sheket.client.contentprovider.SheketContract.ItemEntry;
import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SItem;

/**
 * Created by gamma on 3/5/16.
 */
public class ScanResultFragment extends Fragment implements LoaderCallbacks<Cursor> {
    public static final String BARCODE_KEY = "barcode_key";
    private String mBarcode;

    private ListView mResultList;
    private SearchCursorAdapter mScanAdapter;

    private ItemSelectionListener mListener;

    public static ScanResultFragment newInstance(String barcode) {
        Bundle args = new Bundle();
        args.putString(BARCODE_KEY, barcode);

        ScanResultFragment fragment = new ScanResultFragment();
        fragment.setArguments(args);

        return fragment;
    }

    public void setItemSelectedListener(ItemSelectionListener listener) {
        mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            Bundle args = getArguments();
            mBarcode = args.getString(BARCODE_KEY);
            getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_scan_result, container, false);

        mResultList = (ListView) rootView.findViewById(R.id.list_view_scan_result);
        mScanAdapter = new SearchCursorAdapter(getActivity());
        mResultList.setAdapter(mScanAdapter);
        mResultList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mScanAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    SItem item = new SItem(cursor);

                    if (mListener != null)
                        mListener.itemSelected(item);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.SEARCH_RESULT_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        String sortOrder = ItemEntry._full(ItemEntry._ID) + " ASC";
        Uri uri;

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
        mScanAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader loader) {
        mScanAdapter.swapCursor(null);
    }
}
