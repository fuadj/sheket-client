package com.mukera.sheket.client;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.PrefUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import mehdi.sakout.fancybuttons.FancyButton;

/**
 * Created by fuad on 8/14/16.
 */
public class LoginActivity extends AppCompatActivity {
    //private LoginButton mFacebookSignInButton;
    private FancyButton mFacebookButton;

    private ProgressDialog mProgress = null;
    private CallbackManager mFacebookCallbackManager;

    public static final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        // the user has logged in, start MainActivity
        if (AccessToken.getCurrentAccessToken() != null) {
            startMainActivity();
            return;
        }

        mFacebookCallbackManager = CallbackManager.Factory.create();

        setContentView(R.layout.activity_login);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setHomeAsUpIndicator(R.mipmap.ic_app_icon);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        LoginManager.getInstance().registerCallback(mFacebookCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                if (loginResult.getAccessToken() == null)
                    return;

                mFacebookButton.setVisibility(View.GONE);
                mProgress = ProgressDialog.show(LoginActivity.this,
                        "Logging in", "Please Wait", true);
                new SignInTask(loginResult.getAccessToken().getToken()).execute();
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), "Login Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        mFacebookButton = (FancyButton) findViewById(R.id.facebook_login);
        mFacebookButton.setVisibility(View.VISIBLE);
        mProgress = null;
        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                        Arrays.asList("public_profile"));
            }
        });
        updateFacebookButtonUI();
    }

    void updateFacebookButtonUI() {
        if (AccessToken.getCurrentAccessToken() != null) {
            mFacebookButton.setText("Logout");
        } else {
            mFacebookButton.setText("Login with Facebook");
        }
    }

    void startMainActivity() {
        // We should sync as soon as login is successful
        PrefUtil.setShouldSyncOnLogin(this, true);

        this.finish();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mFacebookCallbackManager.onActivityResult(requestCode, resultCode, data);
    }

    class SignInTask extends AsyncTask<Void, Void, Boolean> {
        public static final String REQUEST_TOKEN = "token";
        public static final String RESPONSE_USERNAME = "username";
        public static final String RESPONSE_USER_ID = "user_id";

        private String mToken;
        private String errMsg;

        public SignInTask(String token) {
            super();

            mToken = token;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                Context context = LoginActivity.this;

                Request.Builder builder = new Request.Builder();
                builder.url(ConfigData.getAddress(context) + "v1/signin/facebook");
                builder.post(RequestBody.create(MediaType.parse("application/json"),
                        new JSONObject().put(REQUEST_TOKEN, mToken).toString()));
                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    JSONObject err = new JSONObject(response.body().string());
                    errMsg = err.getString(context.getString(R.string.json_err_message));
                    return false;
                }

                String login_cookie =
                        response.header(context.getString(R.string.pref_response_key_cookie));

                JSONObject result = new JSONObject(response.body().string());

                long user_id = result.getLong(RESPONSE_USER_ID);
                String username = result.getString(RESPONSE_USERNAME);

                PrefUtil.setUserName(context, username);
                PrefUtil.setUserId(context, user_id);
                PrefUtil.setLoginCookie(context, login_cookie);
            } catch (JSONException | IOException e) {
                errMsg = e.getMessage();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (mProgress != null) {
                mProgress.dismiss();
                mProgress = null;
            }

            if (!success) {
                // remove any-facebook "logged-in" stuff
                Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                LoginManager.getInstance().logOut();
                mFacebookButton.setVisibility(View.VISIBLE);
                return;
            }

            // if all goes well, start main activity
            startMainActivity();
        }

        @Override
        protected void onCancelled() {
            LoginManager.getInstance().logOut();
        }
    }
}
