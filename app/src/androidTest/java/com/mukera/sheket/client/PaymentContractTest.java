package com.mukera.sheket.client;

import android.test.AndroidTestCase;

import com.mukera.sheket.client.services.PaymentContract;

/**
 * Created by fuad on 8/31/16.
 */
public class PaymentContractTest extends AndroidTestCase {
    private static final String LOG_TAG = PaymentContractTest.class.getSimpleName();

    private static final String TEST_CONTRACT = "device_id:123lj1k23lkj;user_id:29;company_id:2;server_date_issued:12-3-2015;duration:30;contract_type:5;employees:2;branches:-1;items:-1";

    // a signature for {@code TEST_CONTRACT} signed by the corresponding private key
    private static final String TEST_SIGNATURE = "Iw5J2AlhBTZABEtueQr38X2s0IxpCP5v1jTKF/ZpitAI4VGmG9ziSdpUfvyDPR1sqcv0v8ccmvQA0cvTYQI2t55xLv3BeIHQ9ZMJ5MbgxoI1mHMqa1DpR35qAKBx2iRTEaTGm1N/l85OGMpWe8bdYafe04HtIbFeKkYG4ZvGzOUC3+k9km157zgQ0pfr8+bXxtyN/nscuIoLIgXRV3FzQWsyh0yHexbDniAbaClkoEzxjgHkuvbhPrSLKxySIq1TLs8PV2h8VU5P71w+aGomuD1zwpf6VeY/pmfFvLzgpYEQTAK3VkB2ci40wUAqyKXNz1S7+k5H36Ojk++tzes6lw==";

    public void testSignatureVerification() {
        assertTrue("Signature verification failure",
                PaymentContract.isMessageSignatureValid(TEST_CONTRACT, TEST_SIGNATURE));
    }

    public void testContractEncoding() {
        PaymentContract contract = new PaymentContract();

        contract.device_id = "123lj1k23lkj";
        contract.user_id = 29;
        contract.company_id = 2;
        contract.server_date_issued = "12-3-2015";
        contract.duration = "30";
        contract.contract_type = 5;
        contract.limit_employees = 2;
        contract.limit_branches = PaymentContract.UNLIMITED;
        contract.limit_items = PaymentContract.UNLIMITED;

        assertEquals("Contract encoding error", TEST_CONTRACT, contract.toString());
    }
}
