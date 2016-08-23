package com.mukera.sheket.client.controller.items;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

/**
 * Created by fuad on 8/23/16.
 */
public class NameCodeDisplayUtil {
    /**
     * A common place to control the display of item name & code in different combinations.
     * If the item doesn't have a code, {@code txtItemCode}'s should be set to View.GONE AND
     * the font of {@code txtItemName} should be adjusted to a bigger font and a darker color.
     * The searching stuff should also be considered to display any matching sub-strings
     * in a "unique" way.
     * @param txtItemName       displays the item name
     * @param txtItemCode       displays item code, will be set to GONE if item code is empty
     * @param sItemName
     * @param sItemCode
     * @param isSearching       should the display be set to search mode. If so, it will display
     *                          the matched {@code search_string} in a different font
     *
     * @param search_string     can be null if either it is not in search mode or otherwise.
     */
    public static void displayItemNameAndCode(Context context,
                                              TextView txtItemName,
                                              TextView txtItemCode,
                                              String sItemName,
                                              String sItemCode,
                                              boolean isSearching,
                                              String search_string) {

        sItemName = sItemName.trim();
        sItemCode = sItemCode.trim();

        int item_name_style = android.R.style.TextAppearance_Small;

        if (sItemCode.isEmpty()) {
            item_name_style = android.R.style.TextAppearance_Large;

            txtItemCode.setVisibility(View.GONE);
        } else {
            txtItemCode.setVisibility(View.VISIBLE);
            if (isSearching)
                showMatchedTextAsBoldItalic(txtItemCode, sItemCode, search_string);
            else
                txtItemCode.setText(sItemCode);
        }

        if (Build.VERSION.SDK_INT < 23) {
            txtItemName.setTextAppearance(context, item_name_style);
        } else {
            txtItemName.setTextAppearance(item_name_style);
        }

        if (isSearching)
            showMatchedTextAsBoldItalic(txtItemName, sItemName, search_string);
        else
            txtItemName.setText(sItemName);
    }

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
