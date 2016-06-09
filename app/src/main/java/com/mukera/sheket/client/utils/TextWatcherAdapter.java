package com.mukera.sheket.client.utils;

import android.text.Editable;
import android.text.TextWatcher;

/**
 * Created by gamma on 4/2/16.
 */
public class TextWatcherAdapter implements TextWatcher {
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) { }
}


