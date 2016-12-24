package com.mukera.sheket.client;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.analytics.HitBuilders;
import com.mukera.sheket.client.network.SheketServiceGrpc;
import com.mukera.sheket.client.network.SignupResponse;
import com.mukera.sheket.client.network.SingupRequest;
import com.mukera.sheket.client.services.AlarmReceiver;
import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.utils.PrefUtil;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.squareup.okhttp.OkHttpClient;

import java.util.Locale;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
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

    // used to measure how long it takes to login with facebook
    private long mStartTime = 0;

    private boolean createLocalUser() {
        // the user has logged in, start MainActivity
        if (PrefUtil.isUserSet(this)) {
            SheketTracker.setScreenName(this, SheketTracker.SCREEN_NAME_LOGIN);
            SheketTracker.sendTrackingData(this,
                    new HitBuilders.EventBuilder().
                            setCategory(SheketTracker.CATEGORY_LOGIN).
                            setAction("User already logged in").
                            build());

            startMainActivity(false);
            return true;
        } else {
            if (PrefUtil.isUserLanguageSet(this))
                displayNewUserDialog();
            else {
                LanguageSelectionDialog.displayLanguageConfigurationDialog(this, false, new Runnable() {
                    @Override
                    public void run() {
                        // restart the activity so the language applies
                        finish();
                        startActivity(getIntent());
                    }
                });
            }
        }
        return true;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());

        AlarmReceiver.startPaymentCheckNow(this);

        if (PrefUtil.isUserLanguageSet(this)) {
            Locale locale = new Locale(PrefUtil.getUserLanguageLocale(this));
            Locale.setDefault(locale);
            Configuration config = new Configuration();
            config.locale = locale;
            getResources().updateConfiguration(
                    config, getResources().getDisplayMetrics());
        }

        if (createLocalUser()) {
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

                //mFacebookButton.setVisibility(View.GONE);
                mProgress = ProgressDialog.show(LoginActivity.this,
                        "Logging in", "Please Wait", true);
                mStartTime = System.nanoTime();
                //new SignInTask(loginResult.getAccessToken().getToken()).execute();
            }

            @Override
            public void onCancel() {
                SheketTracker.setScreenName(LoginActivity.this, SheketTracker.SCREEN_NAME_LOGIN);
                SheketTracker.sendTrackingData(LoginActivity.this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_LOGIN).
                                setAction("Login cancelled").
                                build());
            }

            @Override
            public void onError(FacebookException error) {
                Toast.makeText(getApplicationContext(), "Login Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                SheketTracker.setScreenName(LoginActivity.this, SheketTracker.SCREEN_NAME_LOGIN);
                SheketTracker.sendTrackingData(LoginActivity.this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_LOGIN).
                                setAction("Facebook login error").
                                setLabel(error.toString()).
                                build());
            }
        });
        mFacebookButton = (FancyButton) findViewById(R.id.facebook_login);
        mFacebookButton.setText("Login with Facebook");
        //mFacebookButton.setVisibility(View.VISIBLE);
        mProgress = null;
        setTitle(R.string.app_name);
        mFacebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                LoginManager.getInstance().logInWithReadPermissions(LoginActivity.this,
                        Arrays.asList("public_profile"));
                        */
            }
        });

        checkReadPhoneStatePermissionGranted();
    }

    void displayNewUserDialog() {
        PrefUtil.setUserId(LoginActivity.this, getResources().getInteger(R.integer.local_user_id));
        PrefUtil.setUserName(LoginActivity.this, "");
        LocalBroadcastManager.getInstance(LoginActivity.this).
                sendBroadcast(new Intent(SheketBroadcast.ACTION_USER_CONFIG_CHANGE));
        startMainActivity(false);
        /*
        final EditText editText = new EditText(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this).
                setTitle(R.string.dialog_new_user_name).
                setView(editText).
                setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, int which) {
                        final String new_name = editText.getText().toString().trim();

                        PrefUtil.setUserId(LoginActivity.this, getResources().getInteger(R.integer.local_user_id));
                        PrefUtil.setUserName(LoginActivity.this, new_name);
                        LocalBroadcastManager.getInstance(LoginActivity.this).
                                sendBroadcast(new Intent(SheketBroadcast.ACTION_USER_CONFIG_CHANGE));
                        startMainActivity(false);
                    }
                }).
                setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LoginActivity.this.finish();
                        dialog.dismiss();
                    }
                });

        final AlertDialog dialog = builder.create();

        editText.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                String new_name = s.toString().trim();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).
                        setVisibility(!new_name.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // initially don't show the "Ok" button b/c the name hasn't changed
                ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE).setVisibility(View.GONE);
            }
        });

        dialog.show();
        */
    }

    /**
     * Because we will sync if login is successful and because read_phone_state is required for sync,
     * it is better if we confirm the permission is granted before trying to sync.
     */
    void checkReadPhoneStatePermissionGranted() {
        // there is a bug in android M, declaring the permission in the manifest isn't enough
        // see: http://stackoverflow.com/a/38782876/5753416
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                final int REQUEST_READ_PHONE_STATE = 1;

                /**
                 * Even though we are requesting the permission, we don't actually check if
                 * it has been granted. We leave that to the user. If the user chooses to deny
                 * the permission, it will be again requested when trying to perform a sync.
                 */
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        REQUEST_READ_PHONE_STATE);
            }
        }
    }

    void startMainActivity(boolean sync_on_login) {
        if (sync_on_login)
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
                ManagedChannel managedChannel = ManagedChannelBuilder.
                        forAddress(ConfigData.getServerIP(LoginActivity.this), ConfigData.getServerPort()).
                        usePlaintext(true).
                        build();

                SheketServiceGrpc.SheketServiceBlockingStub blockingStub =
                        SheketServiceGrpc.newBlockingStub(managedChannel);

                SingupRequest request = SingupRequest.newBuilder().
                        setToken(mToken).build();
                SignupResponse response = blockingStub.userSignup(request);

                Context context = LoginActivity.this;

                PrefUtil.setUserName(context, response.getUsername());
                PrefUtil.setUserId(context, response.getUserId());
                PrefUtil.setLoginCookie(context, response.getLoginCookie());
            } catch (StatusRuntimeException e) {
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
                SheketTracker.setScreenName(LoginActivity.this, SheketTracker.SCREEN_NAME_LOGIN);
                SheketTracker.sendTrackingData(LoginActivity.this,
                        new HitBuilders.EventBuilder().
                                setCategory(SheketTracker.CATEGORY_LOGIN).
                                setAction("Login Un-successful").
                                setLabel(errMsg).
                                build());
                // remove any-facebook "logged-in" stuff
                Toast.makeText(LoginActivity.this, errMsg, Toast.LENGTH_LONG).show();
                LoginManager.getInstance().logOut();
                mFacebookButton.setVisibility(View.VISIBLE);
                return;
            }

            long stop_time = System.nanoTime();
            long second_duration = (stop_time - mStartTime) / 1000000000;

            SheketTracker.setScreenName(LoginActivity.this, SheketTracker.SCREEN_NAME_LOGIN);
            SheketTracker.sendTrackingData(LoginActivity.this,
                    new HitBuilders.TimingBuilder().
                            setCategory(SheketTracker.CATEGORY_LOGIN).
                            setValue(second_duration).
                            setLabel("login duration").
                            setVariable("facebook login").
                            build());
            // if all goes well, start main activity
            startMainActivity(true);
        }

        @Override
        protected void onCancelled() {
            LoginManager.getInstance().logOut();
        }
    }
}
