package com.mukera.sheket.client.controller.signup;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mukera.sheket.client.ConfigData;
import com.mukera.sheket.client.R;
import com.mukera.sheket.client.controller.TextWatcherAdapter;
import com.mukera.sheket.client.sync.SyncUtil;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by gamma on 4/2/16.
 */
public class SignupFragment extends Fragment {
    private EditText mUserName, mPassword, mConfirmPass;
    private Button mBtnSingup;
    private ProgressBar mProgressView;
    private View signupForm;
    private TextView mLoginChoice;

    private SignUpListener mListener;
    public static final OkHttpClient client = new OkHttpClient();

    public void setListener(SignUpListener listener) {
        mListener = listener;
    }

    String getUsername() {
        return mUserName.getText().toString().trim();
    }

    String getPassword() {
        // we don't trim this b/c it space may be part of password
        return mPassword.getText().toString();
    }

    String getConfirmPassword() {
        return mConfirmPass.getText().toString();
    }

    boolean doesPasswordMatch() {
        return getPassword().equals(getConfirmPassword());
    }

    void setSingupButtonStatus() {
        mBtnSingup.setEnabled(!TextUtils.isEmpty(getUsername()) &&
                !TextUtils.isEmpty(getPassword()) && doesPasswordMatch());
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_signup, container, false);

        TextWatcherAdapter singupBtnStatusSetter = new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                setSingupButtonStatus();
            }
        };

        mLoginChoice = (TextView) rootView.findViewById(R.id.signup_text_view_login);
        mLoginChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.HaveAccountSelected();
                }
            }
        });

        mUserName = (EditText) rootView.findViewById(R.id.signup_edit_username);
        mPassword = (EditText) rootView.findViewById(R.id.signup_edit_password);
        mConfirmPass = (EditText) rootView.findViewById(R.id.signup_edit_confirm_password);

        mUserName.addTextChangedListener(singupBtnStatusSetter);
        mPassword.addTextChangedListener(singupBtnStatusSetter);
        mConfirmPass.addTextChangedListener(new TextWatcherAdapter(){
            @Override
            public void afterTextChanged(Editable s) {
                setSingupButtonStatus();
                mConfirmPass.setError(null);
                if (getConfirmPassword().length() >= getPassword().length()) {
                    if (!doesPasswordMatch()) {
                        mConfirmPass.setError("Passwords don't match");
                        mConfirmPass.requestFocus();
                    }
                }
            }
        });

        mProgressView = (ProgressBar) rootView.findViewById(R.id.signup_progress);
        signupForm = rootView.findViewById(R.id.signup_form);

        mBtnSingup = (Button) rootView.findViewById(R.id.signup_btn_signup);
        mBtnSingup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptSingup(getUsername(), getPassword());
            }
        });

        setSingupButtonStatus();
        showProgress(false);

        return rootView;
    }

    void attemptSingup(String username, String password) {
        showProgress(true);
        UserSingupTask task = new UserSingupTask(username, password);
        task.execute();
    }

    void showProgress(boolean show) {
        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        signupForm.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    public class UserSingupTask extends AsyncTask<Void, Void, Boolean> {
        public static final String USER_SIGNUP_KEY_USERNAME = "username";
        public static final String USER_SIGNUP_KEY_PASSWORD = "password";
        public static final String USER_SIGNUP_KEY_NEW_USER_ID = "new_user_id";
        public static final String USER_SIGNUP_KEY_ERROR = "error";

        private String mUsername, mPassword;
        private String errMsg;

        public UserSingupTask(String username, String password) {
            super();
            mUsername = username;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                JSONObject json = new JSONObject();
                json.put(USER_SIGNUP_KEY_USERNAME, mUsername);
                json.put(USER_SIGNUP_KEY_PASSWORD, mPassword);

                Request.Builder builder = new Request.Builder();
                builder.url(ConfigData.getAddress(getActivity()) + "signup");
                builder.post(RequestBody.create(MediaType.parse("application/json"),
                        json.toString()));
                Response response = client.newCall(builder.build()).execute();
                if (!response.isSuccessful()) {
                    JSONObject err = new JSONObject(response.body().toString());
                    errMsg = err.getString(USER_SIGNUP_KEY_ERROR);
                    return false;
                }

                String body = response.body().string();
                Log.i("signup", body);
                JSONObject result = new JSONObject(body);
                long user_id = result.getLong(USER_SIGNUP_KEY_NEW_USER_ID);
                SyncUtil.setUserId(getContext(), user_id);
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
                    mListener.SingUpSuccess();
                }
            } else {
                Toast.makeText(getContext(), errMsg, Toast.LENGTH_LONG).show();
                if (mListener != null) {
                    mListener.SingUpFailed(errMsg);
                }
            }
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }

    public interface SignUpListener {
        void SingUpSuccess();
        void SingUpFailed(String err_msg);
        void HaveAccountSelected();
    }
}
