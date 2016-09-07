package com.mukera.sheket.client.utils;

import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by fuad on 7/10/16.
 *
 * Helper methods related to server responses.
 */
public class SheketNetworkUtil {
    public static String getErrorMessage(Response response) {
        final String JSON_ERROR_MSG = "error_message";
        try {
            JSONObject object = new JSONObject(response.body().string());
            return object.getString(JSON_ERROR_MSG);
        } catch (JSONException | IOException e) {
        }
        return "";
    }
}
