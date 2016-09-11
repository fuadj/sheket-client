package com.mukera.sheket.client;

import android.test.AndroidTestCase;

import com.mukera.sheket.client.controller.user.IdEncoderUtil;

import java.util.Locale;

/**
 * Created by fuad on 7/5/16.
 */
public class TestIdEncoderUtils extends AndroidTestCase {
    static String _format(String format, Object ...args) {
        return String.format(Locale.US, format, args);
    }

    public void testEncodeDecodeUserId() {
        long test_id = 12784;
        String encoded_id = IdEncoderUtil.encodeId(test_id, IdEncoderUtil.ID_TYPE_USER);
        assertTrue(_format("User Id: %d, Expected %s to be a valid encoded id", test_id, encoded_id),
                IdEncoderUtil.isValidEncodedUserId(encoded_id));
        assertFalse(_format("User Id:%d, not expected to decode as a company id: %s", test_id, encoded_id),
                IdEncoderUtil.isValidEncodedCompanyId(encoded_id));
        assertEquals("Encoded User id doesn't match",
                test_id, IdEncoderUtil.decodeEncodedId(encoded_id, IdEncoderUtil.ID_TYPE_USER));
    }

    public void testEncodeDecodeCompanyId() {
        long test_id = 21312;
        String encoded_id = IdEncoderUtil.encodeId(test_id, IdEncoderUtil.ID_TYPE_COMPANY);
        assertTrue(_format("Company Id: %d, Expected %s to be a valid encoded id", test_id, encoded_id),
                IdEncoderUtil.isValidEncodedCompanyId(encoded_id));
        assertFalse(_format("Company Id:%d, not expected to decode as a user id: %s", test_id, encoded_id),
                IdEncoderUtil.isValidEncodedUserId(encoded_id));
        assertEquals("Encoded Company id doesn't match",
                test_id, IdEncoderUtil.decodeEncodedId(encoded_id, IdEncoderUtil.ID_TYPE_COMPANY));
    }

    public void testDelimitedRecovery() {
        // it is a 2-d array of {String, integer, integer}
        // elements:
        // 1st: (string) the text to delimit
        // 2nd: (int) the delimiting(grouping) size
        // 3rd: (int) the expected number of delimiter
        Object[][] delimit_tests = {
                {"qewafoijf", 3, 2},
                {"abde", 1, 3},
                {"124wqerafadfqwer", 10, 1},
                {"1", 1, 0},
                {"", 1, 0},

                /**
                 * These test when the delimiting size splits the string evenly with not reminders.
                */
                {"kan123", 3, 1},
                {"abcdabcd", 4, 1},
                {"abcdabcdabcd", 2, 5},
                {"abcdedlkja", 1, 9},
        };

        for (Object[] test : delimit_tests) {
            String text = (String) test[0];
            int size = (Integer) test[1];
            int expected_delimiters = (Integer) test[2];

            String delimited = IdEncoderUtil.delimitEncodedId(text, size);

            int num_delimiters = 0;

            int last_index = 0;
            while ((last_index = delimited.indexOf(IdEncoderUtil.GROUP_DELIMITER, last_index)) != -1) {
                last_index++;       // starting at last index keeps us in infinite loop b/c we find it right there
                num_delimiters++;
            }

            assertEquals(
                    String.format(Locale.US, "Test: '%s' delimited '%s'. Expected %d delimiters, found %d",
                            text, delimited, expected_delimiters, num_delimiters),
                    expected_delimiters, num_delimiters
            );

            String recovered = IdEncoderUtil.removeDelimiterOnEncodedId(delimited);

            assertEquals(
                    String.format(Locale.US, "Test: '%s' recovered '%s' doesn't match original. Delimited '%s'",
                            text, recovered, delimited),
                    text, recovered);
        }
    }

    public void testEncodedDelimitedUserId() {
        long test_id = 12784;
        String encoded_id = IdEncoderUtil.encodeId(test_id, IdEncoderUtil.ID_TYPE_USER);
        String delimited = IdEncoderUtil.delimitEncodedId(encoded_id, 4);
        String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited);

        // b/c the test id is big enough, it should have a delimiter in it
        assertTrue(String.format(Locale.US,
                "User id delimiting error, delimiter missing. Encoded '%s', delimited '%s'", encoded_id, delimited),
                delimited.contains(IdEncoderUtil.GROUP_DELIMITER));

        assertEquals("The de-delimited user id doesn't match the encoded",
                encoded_id, delimiter_removed);

        long recovered_id = IdEncoderUtil.decodeEncodedId(delimiter_removed, IdEncoderUtil.ID_TYPE_USER);
        assertEquals(String.format(Locale.US, "User id:'%d' doesn't recover after delimiting '%s', recovered '%d' ",
                test_id, delimited, recovered_id),
                test_id, recovered_id);
    }

    public void testEncodedDelimitedCompanyId() {
        long test_id = 23135;

        String encoded_id = IdEncoderUtil.encodeId(test_id, IdEncoderUtil.ID_TYPE_COMPANY);
        String delimited = IdEncoderUtil.delimitEncodedId(encoded_id, 4);
        String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited);

        // b/c the test id is big enough, it should have a delimiter in it
        assertTrue(String.format(Locale.US,
                "User id delimiting error, delimiter missing. Encoded '%s', delimited '%s'", encoded_id, delimited),
                delimited.contains(IdEncoderUtil.GROUP_DELIMITER));

        assertEquals("The de-delimited user id doesn't match the encoded",
                encoded_id, delimiter_removed);

        long recovered_id = IdEncoderUtil.decodeEncodedId(delimiter_removed, IdEncoderUtil.ID_TYPE_COMPANY);
        assertEquals(String.format(Locale.US, "Company id:'%d' doesn't recover after delimiting '%s', recovered '%d' ",
                test_id, delimited, recovered_id),
                test_id, recovered_id);
    }

    /**
     * This isn't a comprehensive test, just to see if a few -ve in the range (-1000, 0) values can be encoded.
     */
    public void testNegativeUserIdEncoding() {
        // USE-LESS test
        /*
        long test_id = -125;
        String encoded_id = IdEncoderUtil.encodeId(test_id);
        String delimited = IdEncoderUtil.delimitEncodedId(encoded_id, 4);
        String delimiter_removed = IdEncoderUtil.removeDelimiterOnEncodedId(delimited);

        // b/c the test id is big enough, it should have a delimiter in it
        assertTrue(String.format(Locale.US,
                "User id delimiting error, delimiter missing. Encoded '%s', delimited '%s'", encoded_id, delimited),
                delimited.contains(IdEncoderUtil.GROUP_DELIMITER));

        assertEquals("The de-delimited user id doesn't match the encoded",
                encoded_id, delimiter_removed);

        long recovered_id = IdEncoderUtil.decodeEncodedId(delimiter_removed);
        assertEquals(String.format(Locale.US, "User id:'%d' doesn't recover after delimiting '%s', recovered '%d' ",
                test_id, delimited, recovered_id),
                test_id, recovered_id);
        */
    }

    /*
    // Uncomment to run this benchmark
    public void testBenchmarkEncodeDecodeUserId() {
        final int num_tests = 10000;
        final int start_id = 12342;
        final int stop_id = num_tests + start_id;

        for (int i = start_id; i < stop_id; i++) {
            long test_id = i;
            String encoded_id = IdEncoderUtil.encodeId(test_id);
            assertTrue(String.format("User Id: %d, Expected %s to be a valid encoded id", test_id, encoded_id),
                    IdEncoderUtil.isValidEncodedUserId(encoded_id));
            assertEquals("Encoded User id doesn't match",
                    test_id, IdEncoderUtil.decodeEncodedId(encoded_id));
        }
    }
    */
}
