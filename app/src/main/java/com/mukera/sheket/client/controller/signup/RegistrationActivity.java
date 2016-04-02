package com.mukera.sheket.client.controller.signup;

import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.mukera.sheket.client.R;

/**
 * Created by gamma on 4/2/16.
 */
public class RegistrationActivity extends AppCompatActivity implements SignupFragment.SignUpListener,
    LoginFragment.LoginListener{

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        ActionBar ab = getSupportActionBar();

        ab.setDisplayShowHomeEnabled(false); // show or hide the default home button
        ab.setDisplayHomeAsUpEnabled(false);

        if (savedInstanceState == null) {
            replaceWithSingup();
        }
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

    @Override
    public void SingUpSuccess() {
        this.finish();
    }

    @Override
    public void SingUpFailed(String err_msg) {
    }

    @Override
    public void HaveAccountSelected() {
        replaceWithLogin();
    }

    @Override
    public void LoginSuccess() {
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
