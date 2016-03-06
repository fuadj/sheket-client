package com.mukera.sheket.client.b_actions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.input.ManualInputFragment;
import com.mukera.sheket.client.input.ScannerFragment;
import com.mukera.sheket.client.input.ScanResultFragment;
import com.mukera.sheket.client.models.SItem;

import java.util.ArrayList;
import java.util.List;

public class TransactionActivity extends AppCompatActivity implements
        InputSelectionFragment.InputMethodSelectionListener,
        ItemSelectionListener {
    public static final String LAUNCH_ACTION_KEY = "launch_action_key";
    public static final String LAUNCH_ITEM_ID_KEY = "launch_item_id_key";

    private static final int LAUNCH_TYPE_NONE = 0;
    private static final long ITEM_ID_NONE = -1;

    public static final int LAUNCH_TYPE_SEARCH = 1;
    public static final int LAUNCH_TYPE_BUY = 2;
    public static final int LAUNCH_TYPE_SELL = 3;

    private static final String[] sTitles = {"Search", "Buy", "Sell"};

    private int mCurrentSelection = InputSelectionFragment.SELECTION_NONE;
    private int mCurrentAction = LAUNCH_TYPE_NONE;

    public SItem mPassedInItem;
    private List<SItem> mSelectedItemsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        mCurrentAction = getIntent().getIntExtra(LAUNCH_ACTION_KEY, LAUNCH_TYPE_NONE);

        String title = sTitles[mCurrentAction - 1];
        setTitle(title);

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);

        if (savedInstanceState == null) {
            mSelectedItemsList = new ArrayList<>();

            /**
             * If we were provided with an item to start with, grab it and
             * go to the transaction directly.
             */
            long item_id = getIntent().getLongExtra(LAUNCH_ITEM_ID_KEY, ITEM_ID_NONE);
            if (item_id != ITEM_ID_NONE) {
                //TODO: check if there is a better way
                //this.itemSelected(mPassedInItem);
            }

            // If user already has a preferred input method, go with that
            if (mCurrentSelection != InputSelectionFragment.SELECTION_NONE)
                doAction(mCurrentAction);
            else {
                InputSelectionFragment fragment = new InputSelectionFragment();
                fragment.setSelectionListener(this);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.activity_action_container, fragment)
                        .commit();
            }
        }
    }

    private void startFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.activity_action_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    void barcodeResultFound(String result) {
        ScanResultFragment fragment = ScanResultFragment.newInstance(result);
        fragment.setItemSelectedListener(this);
        startFragment(fragment);
    }

    // Accept the qty for the item from user
    void displayQuantityDialog(SItem item) {
        FragmentManager fm = getSupportFragmentManager();
        QtyDialog dialog = new QtyDialog();
        dialog.item = item;
        dialog.show(fm, "dialog");
    }

    @Override
    public void itemSelected(SItem item) {
        mSelectedItemsList.add(item);

        displayQuantityDialog(item);
    }

    @Override
    public void methodSelected(int method) {
        mCurrentSelection = method;
        Fragment fragment = null;
        switch (method) {
            case InputSelectionFragment.SELECTION_BARCODE:
                ScannerFragment scanner = new ScannerFragment();
                scanner.setResultListener(new ScannerFragment.ScanResultListener() {
                    @Override
                    public void resultFound(String result) {
                        barcodeResultFound(result);
                    }
                });
                fragment = scanner;
                break;
            case InputSelectionFragment.SELECTION_MANUAL:
                ManualInputFragment manual = new ManualInputFragment();
                manual.setItemSelectedListener(this);
                fragment = manual;
                break;
        }
        startFragment(fragment);
    }

    void doAction(int action) {
        /*
        int action = getIntent().getIntExtra(LAUNCH_ACTION_KEY, LAUNCH_TYPE_NONE);

        switch (action) {
            case
        }
        */
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        finish();
        return super.onOptionsItemSelected(item);
    }
}
