package com.mukera.sheket.client;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.mukera.sheket.client.controller.user.UserUtil;

import java.util.Locale;
import java.util.Objects;

/**
 * Created by fuad on 7/5/16.
 */
public class TestUserUtils extends AndroidTestCase {
    public void testEncodeDecodeUserId() {
        long test_id = 12784;
        String encoded_id = UserUtil.encodeUserId(test_id);
        assertTrue(String.format("User Id: %d, Expected %s to be a valid encoded id", test_id, encoded_id),
                UserUtil.isValidEncodedId(encoded_id));
        assertEquals("Encoded User id doesn't match",
                test_id, UserUtil.decodeUserId(encoded_id));
    }

    public void testDelimitedRecovery() {
        // it is a 2-d array of {String, integer}
        // the string is the text to delimit and the int is the delimiting size
        Object[][] delimit_tests = {
                {"qewafoijf", 3},
                {"abde", 1},
                {"124wqerafadfqwer", 10},
                {"1", 1},
                {"", 1},

                /**
                 * These test when the delimiting size splits the string evenly with not reminders.
                 * IMPORTANT: It is a corner case.
                */
                {"kan123", 3},
                {"abcdabcd", 4},
                {"abcdabcdabcd", 2},
                {"abcdedlkja", 1},
        };

        for (Object[] test : delimit_tests) {
            String text = (String) test[0];
            int size = (Integer) test[1];

            String delimited = UserUtil.delimitEncodedUserId(text, size);

            String recovered = UserUtil.removeDelimiterOnEncodedId(delimited);

            assertEquals(
                    String.format(Locale.US, "Test: '%s' recovered '%s' doesn't match original. Delimited '%s'",
                            text, recovered, delimited),
                    text, recovered);
        }
    }

    public void testEncodedDelimitedUserId() {
        long test_id = 12784;
        String encoded_id = UserUtil.encodeUserId(test_id);
        String delimited = UserUtil.delimitEncodedUserId(encoded_id, 4);
        String delimiter_removed = UserUtil.removeDelimiterOnEncodedId(delimited);

        // b/c the test id is big enough, it should have a delimiter in it
        assertTrue(String.format(Locale.US,
                "User id delimiting error, delimiter missing. Encoded '%s', delimited '%s'", encoded_id, delimited),
                delimited.contains(UserUtil.GROUP_DELIMITER));

        assertEquals("The de-delimited user id doesn't match the encoded",
                encoded_id, delimiter_removed);

        long recovered_id = UserUtil.decodeUserId(delimiter_removed);
        assertEquals(String.format(Locale.US, "User id:'%d' doesn't recover after delimiting '%s', recovered '%d' ",
                test_id, delimited, recovered_id),
                test_id, recovered_id);
    }

    /**
     * This isn't a comprehensive test, just to see if a few -ve in the range (-1000, 0) values can be encoded.
     */
    public void testNegativeUserIdEncoding() {
        long test_id = -125;
        String encoded_id = UserUtil.encodeUserId(test_id);
        String delimited = UserUtil.delimitEncodedUserId(encoded_id, 4);
        String delimiter_removed = UserUtil.removeDelimiterOnEncodedId(delimited);

        // b/c the test id is big enough, it should have a delimiter in it
        assertTrue(String.format(Locale.US,
                "User id delimiting error, delimiter missing. Encoded '%s', delimited '%s'", encoded_id, delimited),
                delimited.contains(UserUtil.GROUP_DELIMITER));

        assertEquals("The de-delimited user id doesn't match the encoded",
                encoded_id, delimiter_removed);

        long recovered_id = UserUtil.decodeUserId(delimiter_removed);
        assertEquals(String.format(Locale.US, "User id:'%d' doesn't recover after delimiting '%s', recovered '%d' ",
                test_id, delimited, recovered_id),
                test_id, recovered_id);
    }

    /*
    // Uncomment to run this benchmark
    public void testBenchmarkEncodeDecodeUserId() {
        final int num_tests = 10000;
        final int start_id = 12342;
        final int stop_id = num_tests + start_id;

        for (int i = start_id; i < stop_id; i++) {
            long test_id = i;
            String encoded_id = UserUtil.encodeUserId(test_id);
            assertTrue(String.format("User Id: %d, Expected %s to be a valid encoded id", test_id, encoded_id),
                    UserUtil.isValidEncodedId(encoded_id));
            assertEquals("Encoded User id doesn't match",
                    test_id, UserUtil.decodeUserId(encoded_id));
        }
    }
    */
}
