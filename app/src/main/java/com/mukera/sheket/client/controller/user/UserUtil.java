package com.mukera.sheket.client.controller.user;

import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okio.ByteString;

/**
 * Created by fuad on 7/5/16.
 * Helper methods to manipulate user_ids.
 */
public class UserUtil {
    private static final long ID_OFFSET = 473;

    private static int num_digits(long number) {
        return (int)Math.floor(Math.log10(number)) + 1;
    }

    public static String encodeUserId(long user_id) {
        user_id = user_id + ID_OFFSET;
        String id_str = Long.toString(user_id);
        String hash = md5Hex(Long.toString(user_id));

        int digits = num_digits(user_id);
        int encoded_hash_length = digits + 1;

        int total_length = digits + encoded_hash_length;

        StringBuilder encoded_id = new StringBuilder();
        for (int i = 0, j = 0, k = 0; i < total_length; i++) {
            if (i % 2 == 0) {
                // this is the hash is stored
                int hash_index = hash.length() - (encoded_hash_length - j);
                j++;
                encoded_id.append(hash.charAt(hash_index));
            } else {
                // this is where the id is stored
                encoded_id.append(id_str.charAt(k));
                k++;
            }
        }

        return encoded_id.toString().toLowerCase();
    }

    /**
     * If decoding wasn't successful, {@code INVALID_USER_ID} will be returned
     */
    public static final long INVALID_USER_ID = -1;

    public static long decodeUserId(String encoded_id) {
        encoded_id = encoded_id.toLowerCase();
        if (encoded_id.length() < 3 ||
                (encoded_id.length() % 2) == 0) {
            return INVALID_USER_ID;
        }

        StringBuilder b_encoded_hash = new StringBuilder();
        StringBuilder b_embedded_id = new StringBuilder();

        for (int i = 0; i < encoded_id.length(); i++) {
            if (i % 2 == 0) { // the hash
                b_encoded_hash.append(encoded_id.charAt(i));
            } else { // the user id
                b_embedded_id.append(encoded_id.charAt(i));
            }
        }

        String stored_user_id = b_embedded_id.toString();
        String stored_hash = b_encoded_hash.toString();

        long user_id;
        try {
            user_id = Long.parseLong(stored_user_id);
        } catch (NumberFormatException e) {
            return INVALID_USER_ID;
        }

        String computed_hash = md5Hex(Long.toString(user_id)).toLowerCase();
        if (computed_hash.length() <= stored_hash.length())
            return INVALID_USER_ID;

        int start_index = computed_hash.length() - stored_hash.length();
        if (computed_hash.substring(start_index).compareTo(stored_hash) != 0) {
            return INVALID_USER_ID;
        }

        return user_id - ID_OFFSET;
    }

    public static boolean isValidEncodedId(String encoded_id) {
        return decodeUserId(encoded_id) != INVALID_USER_ID;
    }

    /** Returns a 32 character string containing an MD5 hash of {@code s}. */
    public static String md5Hex(String s) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] md5bytes = messageDigest.digest(s.getBytes("UTF-8"));
            return ByteString.of(md5bytes).hex();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
