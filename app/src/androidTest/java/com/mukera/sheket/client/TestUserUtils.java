package com.mukera.sheket.client;

import android.test.AndroidTestCase;
import android.test.suitebuilder.annotation.MediumTest;

import com.mukera.sheket.client.controller.user.UserUtil;

/**
 * Created by fuad on 7/5/16.
 */
public class TestUserUtils extends AndroidTestCase {
    @MediumTest
    public void testLoopEncodeDecodeUserId() {
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
}
