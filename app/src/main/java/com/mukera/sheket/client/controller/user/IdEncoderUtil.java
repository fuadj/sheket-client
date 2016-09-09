package com.mukera.sheket.client.controller.user;

import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import okio.ByteString;

/**
 * Created by fuad on 7/5/16.
 * Helper methods to manipulate user_ids.
 */
public class IdEncoderUtil {
    private static final long ID_OFFSET = 473;

    public static final String GROUP_DELIMITER = "-";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ID_TYPE_USER, ID_TYPE_COMPANY})
    public @interface ID_TYPE{}

    public static final int ID_TYPE_USER = 1;
    public static final int ID_TYPE_COMPANY = 2;

    /**
     * To make it more readable, the id is delimited as numbers are with commas.
     * @param encoded_id
     * @return
     */
    public static String delimitEncodedId(String encoded_id, int group_size) {
        int delimited_groups = (int)Math.floor(encoded_id.length() / (1.0 * group_size));
        if ((delimited_groups * group_size) == encoded_id.length()) {
            // When encoded length is a multiple of group_size, we won't have
            // any "left-over" bits, it will cut it without any reminder.
            // In that case, there won't be any "left-over" group. So, our group number
            // is smaller by 1.
            delimited_groups -= 1;
            if (delimited_groups < 0)
                delimited_groups = 0;
        }

        StringBuilder delimited = new StringBuilder();
        int last_index = 0;
        for (int group = 0; group <= delimited_groups; group++) {
            if (group > 0) {
                delimited.append(GROUP_DELIMITER);
            }

            int start_index = group * group_size;
            int stop_index = (group + 1) * group_size;
            if (stop_index > encoded_id.length())
                stop_index = encoded_id.length();

            delimited.append(
                    encoded_id.substring(start_index, stop_index));
            last_index += group_size;
        }

        if (last_index < encoded_id.length()) {
            // the last group that didn't make the cut b/c it wasn't a "group length wide"
            delimited.append(encoded_id.substring(last_index));
        }

        return delimited.toString();
    }

    /**
     * Undo the delimiting applied by {code delimitEncodedId}
     * @param delimited_id
     * @return
     */
    public static String removeDelimiterOnEncodedId(String delimited_id) {
        return delimited_id.replaceAll(GROUP_DELIMITER, "");
    }

    private static int num_digits(long number) {
        return (int)Math.floor(Math.log10(number)) + 1;
    }

    private static String flip_string(String s) {
        if (TextUtils.isEmpty(s)) return s;
        StringBuilder flipped = new StringBuilder();
        for (int i = s.length() - 1; i >= 0; i--) {
            flipped.append(s.charAt(i));
        }
        return flipped.toString();
    }

    /**
     * Encodes an id by adding error detection logic to it.
     * This is helpful to prevent invalid/misspelled id. This encodes an id
     * based on its type, so a single id can be encoded differently.
     * NOTE: you should decode an id with the type you encoded it with.
     *      e.g: if you encoded it as a ID_TYPE_USER and try to decode it
     *          as an ID_TYPE_COMPANY, it won't work.
     *
     * The implementation is based on md5 checksum of the id. The generated
     * checksum is then mixed alongside the id to make them more intertwined.
     *
     * Use {@code isValidEncodedUserId} to check if it is a valid encoded id.
     * You can then parse-out the id by {@code decodeEncodedId}
     * @param id
     * @return
     */
    public static String encodeId(long id, @ID_TYPE int id_type) {
        id = id + ID_OFFSET;
        String id_str = Long.toString(id);
        String flipped_id = flip_string(id_str);
        String hash = md5Hex(Long.toString(id));

        int digits = num_digits(id);
        int encoded_hash_length = digits + 1;

        int total_length = digits + encoded_hash_length;

        StringBuilder encoded_id = new StringBuilder();
        for (int i = 0, j = 0, k = 0; i < total_length; i++) {
            if (i % 2 == 0) {
                int hash_index;

                if (id_type == ID_TYPE_USER) {
                    // this will use the right end of the hash string
                    hash_index = hash.length() - (encoded_hash_length - j);
                } else {
                    // use the left end of the hash string
                    hash_index = j;
                }

                j++;
                encoded_id.append(hash.charAt(hash_index));
            } else {
                encoded_id.append(flipped_id.charAt(k));
                k++;
            }
        }

        return encoded_id.toString().toLowerCase();
    }

    /**
     * If decoding wasn't successful, {@code INVALID_ENCODED_ID} will be returned
     */
    public static final long INVALID_ENCODED_ID = -1;

    public static long decodeEncodedId(String encoded_id, @ID_TYPE int id_type) {
        if (encoded_id == null) return INVALID_ENCODED_ID;
        encoded_id = encoded_id.trim().toLowerCase();
        if (TextUtils.isEmpty(encoded_id)) return INVALID_ENCODED_ID;

        if (encoded_id.length() < 3 ||
                // it can't also be even length-ed, b/c it must be of the form 2n + 1
                (encoded_id.length() % 2) == 0) {
            return INVALID_ENCODED_ID;
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

        String flipped_id = flip_string(stored_user_id);

        long extracted_id;
        try {
            extracted_id = Long.parseLong(flipped_id);
        } catch (NumberFormatException e) {
            return INVALID_ENCODED_ID;
        }

        String computed_hash = md5Hex(Long.toString(extracted_id)).toLowerCase();
        if (computed_hash.length() <= stored_hash.length())
            return INVALID_ENCODED_ID;

        switch (id_type) {
            case ID_TYPE_USER: {
                // compare it to the RIGHT end of the computed hash
                int start_index = computed_hash.length() - stored_hash.length();
                if (computed_hash.substring(start_index).compareTo(stored_hash) != 0) {
                    return INVALID_ENCODED_ID;
                }
                break;
            }

            case ID_TYPE_COMPANY: {
                // compare it to the LEFT end of the computed hash
                if (computed_hash.substring(0, stored_hash.length()).
                        compareTo(stored_hash) != 0) {
                    return INVALID_ENCODED_ID;
                }
                break;
            }
        }

        return extracted_id - ID_OFFSET;
    }

    public static boolean isValidEncodedUserId(String encoded_id) {
        return decodeEncodedId(encoded_id, ID_TYPE_USER) != INVALID_ENCODED_ID;
    }

    public static boolean isValidEncodedCompanyId(String encoded_id) {
        return decodeEncodedId(encoded_id, ID_TYPE_COMPANY) != INVALID_ENCODED_ID;
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
