package com.mukera.sheket.client.controller.items;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.mukera.sheket.client.R;

/**
 * Created by fuad on 7/17/16.
 *
 * Since java doesn't support multiple inheritance,
 * we have to extend {@code CategoryTreeNavigationFragment}.
 */
public abstract class SearchableItemFragment extends CategoryTreeNavigationFragment {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.item_searcher, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem menuItem = menu.findItem(R.id.search_item);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menuItem);
        Log.d("SearchableItemFragment", "Address: " + searchView);
        /*
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return onSearchTextSubmitted(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return onSearchTextChanged(newText);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                return onSearchTextViewClosed();
            }
        });
        */
        searchView.setSubmitButtonEnabled(false);
        searchView.setIconifiedByDefault(true);
    }

    /**
     * Override this to implement your own behaviour
     * @param newText
     * @return
     */
    protected boolean onSearchTextChanged(String newText) {
        return true;
    }

    protected boolean onSearchTextSubmitted(String query) {
        return false;
    }

    protected boolean onSearchTextViewClosed() {
        return true;
    }
}
