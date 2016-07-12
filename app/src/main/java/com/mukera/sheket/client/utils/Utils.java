package com.mukera.sheket.client.utils;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gamma on 4/17/16.
 */
public class Utils {
    public static final DecimalFormat sFormatter;

    static {
        sFormatter = new DecimalFormat("#.###");
    }

    public static String formatDoubleForDisplay(double d) {

        return sFormatter.format(d);
    }

    public static String toTitleCase(String str) {

        if (str == null) {
            return null;
        }

        boolean space = true;
        StringBuilder builder = new StringBuilder(str);
        final int len = builder.length();

        for (int i = 0; i < len; ++i) {
            char c = builder.charAt(i);
            if (space) {
                if (!Character.isWhitespace(c)) {
                    // Convert to title case and switch out of whitespace mode.
                    builder.setCharAt(i, Character.toTitleCase(c));
                    space = false;
                }
            } else if (Character.isWhitespace(c)) {
                space = true;
            } else {
                builder.setCharAt(i, Character.toLowerCase(c));
            }
        }

        return builder.toString();
    }

    private static final Pattern number_extractor;
    static {
        number_extractor = Pattern.compile("(?:\\d{1,3}(?:[,]\\d{3})*|\\d+)(?:[.]\\d+)?");
    }

    public static double extractDoubleFromString(String s) {
        Matcher matcher = number_extractor.matcher(s);
        if (matcher.find()) {
            String number = matcher.group(0);
            number = number.replaceAll(",", "");
            return Double.parseDouble(number);
        }
        return 0.d;
    }

}
