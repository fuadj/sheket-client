package com.mukera.sheket.client.controller.util;

import java.text.DecimalFormat;

/**
 * Created by gamma on 4/17/16.
 */
public class NumberFormatter {
    public static final DecimalFormat sFormatter;
    static {
        sFormatter = new DecimalFormat("#.###");
    }

    public static String formatDoubleForDisplay(double d) {
        return sFormatter.format(d);
    }
}
