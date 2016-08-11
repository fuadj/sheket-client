package com.mukera.sheket.client.utils;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.widget.TextView;

/**
 * Created by fuad on 8/11/16.
 */
public class SheketTextUtils {

    /**
     * Searches for the {@code needle} in the {@code hay_stack} and if it finds it, it will make it bold.
     * Otherwise, just show the whole of {@code hay_stack} as normal
     */
    public static void showMatchedTextAsBoldItalic(TextView textView, String hay_stack, String needle) {
        int match_index = hay_stack.toLowerCase().indexOf(needle.toLowerCase());
        if (match_index == -1) {
            textView.setText(hay_stack);
            return;
        }

        SpannableStringBuilder sb = new SpannableStringBuilder(hay_stack);
        StyleSpan b = new StyleSpan(Typeface.BOLD_ITALIC);

        sb.setSpan(b, match_index, match_index + needle.length(),
                Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
        textView.setText(sb);
    }
}
