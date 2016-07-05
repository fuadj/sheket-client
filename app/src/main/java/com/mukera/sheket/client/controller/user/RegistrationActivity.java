package com.mukera.sheket.client.controller.user;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.mukera.sheket.client.R;
import com.mukera.sheket.client.sync.SheketService;
import com.mukera.sheket.client.utils.PrefUtil;

/**
 * Created by gamma on 4/2/16.
 */
public class RegistrationActivity extends AppCompatActivity implements SignupFragment.SignUpListener,
    LoginFragment.LoginListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registration);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(false);

        replaceWithLogin();
    }

    void replaceWithSingup() {
        SignupFragment fragment = new SignupFragment();
        fragment.setListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.register_fragment_container, fragment).
                commit();
    }

    void replaceWithLogin() {
        LoginFragment fragment = new LoginFragment();
        fragment.setListener(this);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.register_fragment_container, fragment).
                commit();
    }

    void syncUserDataOnLogin() {
        Intent intent = new Intent(this, SheketService.class);
        startService(intent);
    }

    @Override
    public void SingUpSuccess() {
        syncUserDataOnLogin();
        this.finish();
    }

    @Override
    public void SingUpFailed(String err_msg) {
    }

    @Override
    public void onBackPressed() {
        /**
         * this activity was started by an intent from main, if we press back
         * we shouldn't go to main without the user login/signup success.
         * So, just ignore the back button.
         */
        if (PrefUtil.isUserSet(this)) {
            super.onBackPressed();
        } else {
            // TODO: quit the application(may be after 2 back presses)
            // just ignore it, or quit the app
        }
    }

    @Override
    public void HaveAccountSelected() {
        replaceWithLogin();
    }

    @Override
    public void LoginSuccess() {
        syncUserDataOnLogin();
        this.finish();
    }

    @Override
    public void LoginFailed(String err_msg) {
    }

    @Override
    public void CreateAccountSelected() {
        replaceWithSingup();
    }
}
