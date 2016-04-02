package com.mukera.sheket.client.controller.navigation;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.mukera.sheket.client.LoaderId;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.models.SBranch;

/**
 * Created by gamma on 3/27/16.
 */
public class NavigationFragment extends NavigationDrawerFragment implements LoaderCallbacks<Cursor> {
    private BranchSelectionCallback mCallback;

    private ListView mBranchListView;
    private BranchCursorAdapter mBranchCursorAdapter;
    private TextView mSeparator1TextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_navigation, container, false);

        View all_item_view = rootView.findViewById(R.id.all_items);
        NavigationViewHolder holder = new NavigationViewHolder(all_item_view);
        holder.elementName.setText("All Items");
        all_item_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDrawerState(false);
                mCallback.onElementSelected(0);
            }
        });

        mBranchListView = (ListView) rootView.findViewById(R.id.list_view_branches);
        mBranchCursorAdapter = new BranchCursorAdapter(getContext());
        mBranchListView.setAdapter(mBranchCursorAdapter);
        mBranchListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = mBranchCursorAdapter.getCursor();
                if (cursor != null && cursor.moveToPosition(position)) {
                    setDrawerState(false);
                    SBranch branch = new SBranch(cursor);
                    mCallback.onBranchSelected(branch);
                }
            }
        });

        View separator1 = rootView.findViewById(R.id.separator_1);
        mSeparator1TextView = (TextView) separator1.findViewById(R.id.text_view_separator);
        mSeparator1TextView.setBackgroundColor(getContext().getResources().getColor(R.color.section_separator_color));
        mSeparator1TextView.setText("Categories");

        setDrawerState(false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(LoaderId.BRANCH_LIST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mBranchCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mBranchCursorAdapter.swapCursor(null);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (BranchSelectionCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement BranchSelectionCallback.");
        }
    }

    public interface BranchSelectionCallback {
        void onBranchSelected(SBranch branch);
        void onElementSelected(int item);
    }

}
