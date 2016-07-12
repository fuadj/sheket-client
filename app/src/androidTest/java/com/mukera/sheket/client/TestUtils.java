package com.mukera.sheket.client;

import android.test.AndroidTestCase;

import com.mukera.sheket.client.utils.DuplicateFinder;
import com.mukera.sheket.client.utils.Utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

/**
 * Created by fuad on 6/24/16.
 */
public class TestUtils extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
    }

    private static boolean doublesMatch(double a, double b) {
        return Math.abs(a - b) < 0.0001;
    }

    public void testDoubleExtractor() {
        // an array of {(string) test_string, (boolean) should match, (double) expected value}
        Object[][] tests = {
                {"4,352,123.91283", true, 4352123.91283},
                {"1.01", true, 1.01},
                {"fldkaj", false, 1.01},
                {"100.21m2", true, 100.21},
                {" 90", true, 90.d},
                {" 0.001", true, 0.001},
                {" 12,134", true, 12134.d},
        };
        for (int i = 0; i < tests.length; i++) {
            String str = (String)tests[i][0];
            boolean should_match = (Boolean)tests[i][1];
            double expected = (Double)tests[i][2];

            double parsed = Utils.extractDoubleFromString(str);
            assertTrue(
                    String.format(Locale.US,
                            "double extract failed, test string '%s', extracted %f",
                            str, parsed),
                    should_match == doublesMatch(expected, parsed)
            );
        }

    }

    public void testDistance() {
        String a = "test".toLowerCase();
        String b = "teet".toLowerCase();

        assertEquals(DuplicateFinder.computeDistance(a, b), 1);
        assertEquals(DuplicateFinder.computeDistance(b, a), 1);
    }

    public void testDistanceIgnoringSpace() {
        String a = "this is a test string".trim();
        String b = "    this iis a    test   string      ".trim();
        String c = "this        is a       test      string".trim();

        assertEquals(DuplicateFinder.computeDistanceIgnoringSpaces(a, b), 1);
        assertEquals(DuplicateFinder.computeDistanceIgnoringSpaces(b, a), 1);

        assertEquals(DuplicateFinder.computeDistanceIgnoringSpaces(a, c), 0);
        assertEquals(DuplicateFinder.computeDistanceIgnoringSpaces(c, a), 0);
    }

    public void testFindDuplicates() {
        String[] word_list = {
                "test   ", "  this    is a test ",
                "       this    is a tsst       ",
                "     tset  ",
                "abcd", "abcd"
        };

        Set<String> words = new HashSet<>();
        Collections.addAll(words, word_list);

        Vector<Vector<String>> duplicates = DuplicateFinder.findDuplicates(words,
                DuplicateFinder.DISTANCE_2_COMPARATOR);
        assertEquals(duplicates.size(), 3);
    }
}
