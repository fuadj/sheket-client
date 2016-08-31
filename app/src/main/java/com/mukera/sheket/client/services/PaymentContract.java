package com.mukera.sheket.client.services;

import org.spongycastle.util.io.pem.PemObject;
import org.spongycastle.util.io.pem.PemReader;

import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

/**
 * Created by fuad on 8/31/16.
 */
public class PaymentContract {
    public String device_id;
    public long user_id;
    public long company_id;
    public String date_issued;
    public String duration;
    public int contract_type;

    // any of the following entities could have the {code UNLIMITED} value.
    public static final int UNLIMITED = -1;
    public int limit_employees;
    public int limit_branches;
    public int limit_items;

    public PaymentContract() {
    }

    // initialize this object from contents of the contract
    public PaymentContract(String contract) {
        String[] subs = contract.split(";");
        if (subs.length != 9)
            return;

        device_id = subs[0];
        user_id = Long.parseLong(subs[1]);
        company_id = Long.parseLong(subs[2]);
        date_issued = subs[3];
        duration = subs[4];
        contract_type = Integer.parseInt(subs[5]);
        limit_employees = Integer.parseInt(subs[6]);
        limit_branches = Integer.parseInt(subs[7]);
        limit_items = Integer.parseInt(subs[8]);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "" +
                        "device_id:%s;" +
                        "user_id:%d;" +
                        "company_id:%d;" +
                        "date_issued:%s;" +
                        "duration:%s;" +
                        "contract_type:%d;" +
                        "employees:%d;" +
                        "branches:%d;" +
                        "items:%d",
                device_id, user_id, company_id,
                date_issued, duration, contract_type,
                limit_employees, limit_branches, limit_items);
    }

    /**
     * Returns true if the signature is valid.
     */
    public boolean isSignatureValid(String proposed_signature) {
        // we couldn't load the public key
        if (sSheketPublicKey == null)
            return false;

        String encoded_contract = toString();

        StringBufferInputStream contractStream = new StringBufferInputStream(encoded_contract);
        StringBufferInputStream signatureStream = new StringBufferInputStream(proposed_signature);

        try {
            final Signature computed_signature = Signature.getInstance("SHA256withRSA");
            computed_signature.initVerify(sSheketPublicKey);

            int read = -1;
            byte[] buffer = new byte[16 * 1024];
            while ((read = contractStream.read(buffer)) != -1) {
                computed_signature.update(buffer, 0, read);
            }

            byte[] signatureBytes = new byte[sSheketPublicKey.getModulus().bitLength() / 8];
            signatureStream.read(signatureBytes);
            return computed_signature.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Breaks up the contract into its components.
     */
    private static final String delimiter = "_||_";

    public static ContractComponents extractContractComponents(String signed_contract) {
        ContractComponents components = new ContractComponents();

        int index = signed_contract.indexOf(delimiter);
        if (index != -1) {
            components.contract = signed_contract.substring(0, index);
            components.signature = signed_contract.substring(index);
        }
        return components;
    }

    public static class ContractComponents {
        String contract;
        String signature;
    }

    private static RSAPublicKey sSheketPublicKey = null;

    static {
        PemReader publicKeyReader = new PemReader(
                new StringReader("" +
                        "-----BEGIN PUBLIC KEY-----\n" +
                        "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx/uST1WerMlMHvmOMfJx\n" +
                        "jE0v/xE5cHXg3QkQn+RxhThQsf+Tk8b30/BkyE6I83RFvqoyNO2v7vFnhSQSansm\n" +
                        "uJdEv+SM9VGEu2vY1KemG3I1/Oae2kwSy53NXEXFYOK3JEYEPhy4aCMLvEk/i1Fa\n" +
                        "iPTONt3FJOyULwSpVweEcs6P0dy/v4732NrwzKMJ6V9RM04jkzI7XVWR3bYYWPQt\n" +
                        "VDkjonXXAb/qVKap0Km7cshEZkOAs5srkw4sVoYgiK3WFpDMbj6mesVaX8nuLrO9\n" +
                        "N385sVItc+3/t9ltrq4KJgH2rcR5JnpUDCopVvmqWmq972Mphs+E4riT7fiyYeDk\n" +
                        "JQIDAQAB\n" +
                        "-----END PUBLIC KEY-----"
                ));
        try {
            final PemObject publicKeyPem = publicKeyReader.readPemObject();
            sSheketPublicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").
                    generatePublic(
                            new X509EncodedKeySpec(
                                    publicKeyPem.getContent()
                            )
                    );
        } catch (Exception e) {
            sSheketPublicKey = null;
        }
    }
}
