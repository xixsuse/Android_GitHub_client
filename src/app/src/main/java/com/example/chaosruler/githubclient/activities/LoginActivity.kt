package com.example.chaosruler.githubclient.activities

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView

import android.Manifest.permission.READ_CONTACTS
import android.annotation.SuppressLint
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import com.example.chaosruler.githubclient.R
import com.example.chaosruler.githubclient.SQLite.user_database_helper
import com.example.chaosruler.githubclient.activities.Settings.SettingsActivity
import com.example.chaosruler.githubclient.dataclasses.User
import com.example.chaosruler.githubclient.services.GitHub_remote_service
import com.example.chaosruler.githubclient.services.themer

import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fragment_user.*
import java.util.*

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var mAuthTask: UserLoginTask? = null
    /**
     * autologin or manual login? true = auto, false= manual
     */
    private var status: Boolean = true
    /**
     * user database to update after successfull login
     * or populate auto login
     */
    private lateinit var db : user_database_helper
    /**
     * populate spinner for auto login
     */
    private var adapter: ArrayAdapter<User>? = null

    /**
     * creates entire logic for login activity
     */
    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(themer.style(baseContext))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = user_database_helper(baseContext) // gets database representation

        val users = db.get_entire_db()

        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, users)
        login_spinner.adapter = adapter
        login_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View?, i: Int, l: Long) {

                if(i<0)
                    return
                email.setText(adapter!!.getItem(i)!!.get__username())
                password.setText(adapter!!.getItem(i)!!.get__password())

            }

            override fun onNothingSelected(adapterView: AdapterView<*>) = Unit
        }




        // Set up the login form.
        populateAutoComplete()
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }

        button.setOnClickListener {
            email.setText( getString(R.string.default_username))
            password.setText( getString(R.string.default_password))
        }
    }

    /**
     * populate contacts read auto complete
     */
    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, null, this)
    }

    /**
     * requests contact permission for contact data autocomplete
     */
    @SuppressLint("ObsoleteSdkInt")
    private fun mayRequestContacts(): Boolean
    {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
        {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED )
        {
            return true
        }
        when {
            shouldShowRequestPermissionRationale(READ_CONTACTS) -> Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            { requestPermissions(arrayOf(READ_CONTACTS,ACCESS_FINE_LOCATION), REQUEST_READ_CONTACTS) })
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> Snackbar.make(email, R.string.permission_rationale2, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            { requestPermissions(arrayOf(READ_CONTACTS,ACCESS_FINE_LOCATION), REQUEST_READ_CONTACTS) })
            else -> requestPermissions(arrayOf(READ_CONTACTS, ACCESS_FINE_LOCATION), REQUEST_READ_CONTACTS)
        }

        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(emailStr, passwordStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    /**
     * verifies user is valid
     * @return always true
     */
    private fun isEmailValid(@Suppress("UNUSED_PARAMETER") email: String): Boolean {
        //return email.contains("@")
        return true
    }

    /**
     * veriffies password is valid
     * @return true if it accepts Github password patterns
     */
    private fun isPasswordValid(password: String): Boolean {

        // length -> 7, one number, one lowercase character
        //val length = password.length
        val PASSWORD_PATTERN = Regex("((?=.*\\d)(?=.*[a-z]).{7,72})")
        return PASSWORD_PATTERN.matches(password)
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    /**
     * create loader for contacts autocomplete
     * @return the cursor of the data loaded
     */
    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    /**
     * pooulate loader when done
     */
    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    /**
     * when loader was reset, empty catch
     */
    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    /**
     * adapter for contacts autocomplete
     */
    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@LoginActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        email.setAdapter(adapter)
    }

    /**
     * profile query for contacts autocomplete
     */
    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY)
        const val ADDRESS = 0
        @Suppress("unused")
        val IS_PRIMARY = 1
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    @SuppressLint("StaticFieldLeak")
    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        /**
         * login done in a different thread
         */
        override fun doInBackground(vararg params: Void): Boolean?
        {
            return GitHub_remote_service.login(mEmail,mPassword)
        }

        /**
         * if login was succeeded, go to main activity with user data, else point to the trouble
         */
        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!)
            {
                if(get_status())
                {
                    db.add_user( mEmail, mPassword)
                }
                startActivity(Intent(this@LoginActivity,MainActivity::class.java))
                finish()
            } else
            {
                password.error = getString(R.string.error_incorrect_password)
                password.requestFocus()
            }
        }

        /**
         * when login operation was canceled
         */
        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    companion object {

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private const val REQUEST_READ_CONTACTS = 0


    }

    /*
        Menu functions
     */

    /**
     * inflates a menu to move between user maintain activity and setting activity
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    /**
     * event listener for item selection on menu
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.login_switch ->
            {
                switch(item)
            }
            R.id.manage_users_db -> {
                val intent = Intent(this@LoginActivity, user_maintain_activity::class.java)
                startActivity(intent)
                finish()
            }
            R.id.settings ->
            {
                val intent = Intent(this@LoginActivity, SettingsActivity::class.java)
                startActivity(intent)

            }
            else -> {
            }
        }
        return true
    }

    /**
     * switch between auto user login and manual complete
     */
    private fun switch(item:MenuItem)
    {
        status = !get_status()
        if (!status)
        // false case -> spinner, email invisible, spinner visible, textview (login) visible password disabled
        {
            login_textview.visibility = View.VISIBLE
            login_spinner.visibility = View.VISIBLE
            email.visibility = View.INVISIBLE
            login_text_input_layout.hint = ""
            password.isEnabled = false
            login_spinner.onItemSelectedListener.onItemSelected(login_spinner,login_spinner.selectedView,login_spinner.selectedItemPosition,login_spinner.selectedItemId)
            item.setTitle(R.string.entry1)
        } else
        // true case -> spinner, textview login invisible, password enabled, email visible
        {
            login_textview.visibility = View.INVISIBLE
            login_spinner.visibility = View.INVISIBLE
            email.visibility = View.VISIBLE
            login_text_input_layout.hint = getString(R.string.prompt_email)
            password.isEnabled = true
            item.setTitle(R.string.entry2)
            email.text.clear()
            password.text.clear()
        }
    }

    /**
     * get the status of current login form (auto or manual?)
     */
    private fun get_status():Boolean
            = this.status
}
