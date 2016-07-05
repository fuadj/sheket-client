package com.mukera.sheket.client;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.mukera.sheket.client.controller.user.UserUtil;

import java.util.Locale;

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
        String[] delimit_tests = {
                "qewafoijf",
                "abde",
                "124wqerafadfqwer",
                "kan123",
                "1",
                "",
        };

        for (String test : delimit_tests) {
            int size = (int)(Math.random() * 5.0);
            String delimited = UserUtil.delimitEncodedUserId(test, size);

            String recovered = UserUtil.removeDelimiterOnEncodedId(delimited);

            assertEquals(String.format(Locale.US, "Test: '%s' recovered '%s' doesn't match original. Delimited '%s'",
                    test, recovered, delimited),
                    test, recovered);
        }
    }

    public void testEncodedDelimitedUserId() {
        long test_id = 12784;
        String encoded_id = UserUtil.encodeUserId(test_id);
        String delimited = UserUtil.delimitEncodedUserId(encoded_id, 4);
        String delimiter_removed = UserUtil.removeDelimiterOnEncodedId(delimited);

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
