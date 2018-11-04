package sk.pluk64.unibakontoapp.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import sk.pluk64.unibakonto.UnibaKonto;
import sk.pluk64.unibakonto.Util;
import sk.pluk64.unibakontoapp.R;
import sk.pluk64.unibakontoapp.TabbedActivity;
import sk.pluk64.unibakontoapp.Utils;

public class LoginFragment extends Fragment {
    private AutoCompleteTextView mUsernameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mProgressView;
    private UserLoginTask mAuthTask;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);
        mUsernameView = view.findViewById(R.id.username);
        mUsernameView.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        mPasswordView = view.findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        TabbedActivity.UsernamePassword up = getMyActivity().getUsernamePassword();
        if (up.username != null) {
            mUsernameView.setText(up.username);
        }
        if (up.password != null) {
            mPasswordView.setText(up.password);
        }

        Button mSignInButton = view.findViewById(R.id.email_sign_in_button);
        mSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = view.findViewById(R.id.login_form);
        mProgressView = view.findViewById(R.id.login_progress);
        return view;

    }

    private TabbedActivity getMyActivity() {
        return ((TabbedActivity) getActivity());
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public static class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        private final UnibaKonto unibaKonto;
        private final WeakReference<TabbedActivity> activityReference;
        private final WeakReference<LoginFragment> fragmentReference;
        private boolean noInternet = false;

        public UserLoginTask(TabbedActivity myActivity, LoginFragment loginFragment, String username, String password) {
            unibaKonto = new UnibaKonto(username, password);
            activityReference = new WeakReference<>(myActivity);
            fragmentReference = new WeakReference<>(loginFragment);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            try {
                unibaKonto.login();
            } catch (Util.ConnectionFailedException e) {
                noInternet = true;
                final TabbedActivity activity = activityReference.get();
                if (activity != null) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Utils.showNoInternetConnection(activity.getApplicationContext());
                        }
                    });
                }
            }
            return unibaKonto.isLoggedIn();
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            LoginFragment fragment = fragmentReference.get();
            if (fragment != null) {
                fragment.onLoginFinished(success, noInternet, unibaKonto);
            }
        }

        @Override
        protected void onCancelled() {
            LoginFragment fragment = fragmentReference.get();
            if (fragment != null) {
                fragment.onLoginCancelled();
            }
        }
    }

    private void onLoginCancelled() {
        mAuthTask = null;
        if (getActivity() != null) {
            showProgress(false);
        }
    }

    private void onLoginFinished(Boolean success, boolean noInternet, UnibaKonto unibaKonto) {
        mAuthTask = null;
        TabbedActivity activity = getMyActivity();
        if (activity == null) {
            return;
        }

        showProgress(false);
        if (success) {
            activity.setIsLoggedIn(true);
            activity.saveLoginDetails(unibaKonto.username, unibaKonto.password);
            activity.setForceRefresh(true);
            activity.removeFragment(LoginFragment.this);
        } else if (!noInternet) {
            mPasswordView.setError(getString(R.string.error_incorrect_username_or_password));
            mPasswordView.requestFocus();
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mUsernameView.getWindowToken(), 0);
                imm.hideSoftInputFromWindow(mPasswordView.getWindowToken(), 0);
            }
        }
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        } else if (TextUtils.isEmpty(password)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            getMyActivity().invalidateUnibaKonto();
            mAuthTask = new UserLoginTask(getMyActivity(), this, username, password);
            mAuthTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }
}
