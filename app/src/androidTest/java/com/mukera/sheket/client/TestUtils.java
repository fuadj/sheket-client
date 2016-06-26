package com.mukera.sheket.client;

import android.test.AndroidTestCase;

import com.mukera.sheket.client.utils.DuplicateFinder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Created by fuad on 6/24/16.
 */
public class TestUtils extends AndroidTestCase {
    @Override
    protected void setUp() throws Exception {
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
