package com.mukera.sheket.client.controller.user;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mukera.sheket.client.utils.ConfigData;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.utils.TextWatcherAdapter;
import com.mukera.sheket.client.utils.PrefUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by gamma on 3/27/16.
 */
public class LoginFragment extends Fragment {
    private EditText mUserName, mPassword;
    private Button mBtnLogin;

    private View loginForm;
    private ProgressBar mProgressView;

    private LoginListener mListener;

    public static final OkHttpClient client = new OkHttpClient();

    public void setListener(LoginListener listener) {
        mListener = listener;
    }

    String getUsername() {
        return mUserName.getText().toString().trim();
    }

    String getPassword() {
        // we don't trim this b/c it space may be part of password
        return mPassword.getText().toString();
    }
    void setLoginButtonStatus() {
        mBtnLogin.setEnabled(!getUsername().isEmpty() && !getPassword().isEmpty());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        TextView createAccount = (TextView) rootView.findViewById(R.id.login_text_view_signup);
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.CreateAccountSelected();
                }
            }
        });

        /*
        EditText ip_address = (EditText) rootView.findViewById(R.id.login_edit_ip_address);
        ip_address.addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                String address = s.toString().trim();
                if (address.isEmpty()) return;

                PrefUtil.setIpAddress(getContext(), address);
            }
        });
        */

        TextWatcherAdapter loginBtnStatusSetter = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                setLoginButtonStatus();
            }
        };

        mUserName = (EditText) rootView.findViewById(R.id.login_edit_username);
        mPassword = (EditText) rootView.findViewById(R.id.login_edit_password);

        mUserName.addTextChangedListener(loginBtnStatusSetter);
        mPassword.addTextChangedListener(loginBtnStatusSetter);

        mBtnLogin = (Button) rootView.findViewById(R.id.login_btn_login);
        mBtnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptLogin(getUsername(), getPassword());
            }
        });

        mProgressView = (ProgressBar) rootView.findViewById(R.id.login_progress);
        loginForm = rootView.findViewById(R.id.login_form);

        setLoginButtonStatus();
        showProgress(false);

        return rootView;
    }

    void attemptLogin(String username, String password) {
        showProgress(true);
        UserLoginTask task = new UserLoginTask(username, password);
        task.execute();
    }

    void showProgress(boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        loginForm.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        public static final String USER_LOGIN_KEY_USERNAME = "username";
        public static final String USER_LOGIN_KEY_PASSWORD = "password";
        public static final String USER_LOGIN_KEY_USER_ID = "user_id";

        private String mUsername, mPassword;

        private String errMsg;

        public UserLoginTask(String username, String password) {
            super();
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONObject json = new JSONObject();
                json.put(USER_LOGIN_KEY_USERNAME, mUsername);
                json.put(USER_LOGIN_KEY_PASSWORD, mPassword);

                Request.Builder builder = new Request.Builder();
                builder.url(ConfigData.getAddress(getActivity()) + "v1/signin");
                builder.post(RequestBody.create(MediaType.parse("application/json"),
                        json.toString()));
                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    JSONObject err = new JSONObject(response.body().string());
                    errMsg = err.getString(getContext().getString(R.string.json_err_message));
                    return false;
                }

                String login_cookie =
                        response.header(getContext().getString(R.string.pref_response_key_cookie));

                JSONObject result = new JSONObject(response.body().string());

                long user_id = result.getLong(USER_LOGIN_KEY_USER_ID);

                PrefUtil.setUserName(getActivity(), mUsername);
                PrefUtil.setUserId(getActivity(), user_id);
                PrefUtil.setLoginCookie(getActivity(), login_cookie);
            } catch (JSONException | IOException e) {
                errMsg = e.getMessage();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            showProgress(false);
            if (success) {
                if (mListener != null) {
                    mListener.LoginSuccess();
                }
            } else {
                Toast.makeText(getContext(), errMsg, Toast.LENGTH_LONG).show();
                if (mListener != null) {
                    mListener.LoginFailed(errMsg);
                }
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    public interface LoginListener {
        void LoginSuccess();
        void LoginFailed(String err_msg);
        void CreateAccountSelected();
    }
}
