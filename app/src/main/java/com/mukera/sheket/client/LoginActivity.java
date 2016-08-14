package com.mukera.sheket.client;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by fuad on 8/14/16.
 */
public class LoginActivity extends AppCompatActivity {
    private LoginButton mFacebookSignInButton;

    private CallbackManager mFacebookCallbackManager;

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

        mFacebookSignInButton = (LoginButton) findViewById(R.id.facebook_sign_in_button);
        mFacebookSignInButton.registerCallback(mFacebookCallbackManager,
                new FacebookCallback<LoginResult>() {
                    @Override
                    public void onSuccess(LoginResult loginResult) {
                        if (loginResult.getAccessToken() != null)
                            startMainActivity();
                    }

                    @Override
                    public void onCancel() {
                        Toast.makeText(getApplicationContext(), "Login Error", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(FacebookException error) {
                        Toast.makeText(getApplicationContext(), "Login Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
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
}
