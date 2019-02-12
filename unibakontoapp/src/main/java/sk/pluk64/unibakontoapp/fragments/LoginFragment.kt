package sk.pluk64.unibakontoapp.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_login.view.*
import sk.pluk64.unibakonto.IsUnibaKonto
import sk.pluk64.unibakonto.UnibaKonto
import sk.pluk64.unibakonto.Util
import sk.pluk64.unibakontoapp.MainActivity
import sk.pluk64.unibakontoapp.R
import sk.pluk64.unibakontoapp.Utils
import java.lang.ref.WeakReference

class LoginFragment : Fragment() {
    private var mAuthTask: UserLoginTask? = null

    private lateinit var activity: MainActivity
    private val preferences by lazy { activity.getPreferences(Context.MODE_PRIVATE) }
    private lateinit var mView: View

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is MainActivity) {
            activity = context
        }
    }

    var onSuccess: () -> Unit = {}

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)
        mView = view

        view.username.imeOptions = EditorInfo.IME_ACTION_NEXT

        view.password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == R.integer.loginImeActionId || id == EditorInfo.IME_ACTION_DONE) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        val credentials = EwalletAndMenusFragment.Credentials(preferences)
        view.username.setText(credentials.username)
        view.password.setText(credentials.password)

        val mSignInButton = view.findViewById<Button>(R.id.email_sign_in_button)
        mSignInButton.setOnClickListener { attemptLogin() }

        return view

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    class UserLoginTask(myActivity: MainActivity, loginFragment: LoginFragment, username: String, password: String) : AsyncTask<Void, Void, Boolean>() {
        private val unibaKonto: IsUnibaKonto
        private val activityReference: WeakReference<MainActivity>
        private val fragmentReference: WeakReference<LoginFragment>
        private var noInternet = false

        init {
            unibaKonto = UnibaKonto(username, password)
            activityReference = WeakReference(myActivity)
            fragmentReference = WeakReference(loginFragment)
        }

        override fun doInBackground(vararg params: Void): Boolean {
            try {
                unibaKonto.login()
            } catch (e: Util.ConnectionFailedException) {
                noInternet = true
                val activity = activityReference.get()
                activity?.runOnUiThread { Utils.showNoInternetConnection(activity.applicationContext) }
            }

            return unibaKonto.isLoggedIn
        }

        override fun onPostExecute(success: Boolean) {
            val fragment = fragmentReference.get()
            fragment?.onLoginFinished(success, noInternet, unibaKonto)
        }

        override fun onCancelled() {
            val fragment = fragmentReference.get()
            fragment?.onLoginCancelled()
        }
    }

    private fun onLoginCancelled() {
        mAuthTask = null
        showProgress(false)
    }

    private fun onLoginFinished(success: Boolean, noInternet: Boolean, unibaKonto: IsUnibaKonto) {
        mAuthTask = null

        showProgress(false)
        if (success) {
            activity.isLoggedIn = true
            activity.saveLoginDetails(unibaKonto.username, unibaKonto.password)
            onSuccess()
        } else if (!noInternet) {
            mView.password.error = getString(R.string.error_incorrect_username_or_password)
            mView.password.requestFocus()
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)

        mView.login_form.visibility = if (show) View.GONE else View.VISIBLE
        mView.login_form.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mView.login_form.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        mView.login_progress.visibility = if (show) View.VISIBLE else View.GONE
        mView.login_progress.animate().setDuration(shortAnimTime.toLong()).alpha(
            (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                mView.login_progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mView.username.windowToken, 0)
        imm.hideSoftInputFromWindow(mView.password.windowToken, 0)
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        mView.username.error = null
        mView.password.error = null

        // Store values at the time of the login attempt.
        val username = mView.username.text.toString()
        val password = mView.password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            mView.username.error = getString(R.string.error_field_required)
            focusView = mView.username
            cancel = true
        } else if (TextUtils.isEmpty(password)) {
            mView.username.error = getString(R.string.error_field_required)
            focusView = mView.password
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            activity.invalidateUnibaKonto()
            val authTask = UserLoginTask(activity, this, username, password)
            authTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            mAuthTask = authTask
        }
    }
}
