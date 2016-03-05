package com.mukera.sheket.client.b_actions;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.mukera.sheket.client.R;

public class ActionActivity extends AppCompatActivity implements InputSelectionFragment.InputMethodSelectionListener {
    public static final String LAUNCH_ACTION_KEY = "launch_action_key";

    public static final int LAUNCH_TYPE_SEARCH = 1;
    public static final int LAUNCH_TYPE_BUY = 2;
    public static final int LAUNCH_TYPE_SELL = 3;

    private static final String[] sTitles = {"Search", "Buy", "Sell"};

    private int mCurrentSelection = InputSelectionFragment.SELECTION_NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item);

        String title = sTitles[Long.valueOf(getIntent().getStringExtra(LAUNCH_ACTION_KEY)).intValue() - 1];
        setTitle(title);
    }

    @Override
    public void methodSelected(int method) {
        mCurrentSelection = method;
        switch (method) {
            case InputSelectionFragment.SELECTION_BARCODE:
                break;
            case InputSelectionFragment.SELECTION_MANUAL:
                break;
        }
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
