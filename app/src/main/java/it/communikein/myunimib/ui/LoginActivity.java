package it.communikein.myunimib.ui;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;


import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import it.communikein.myunimib.R;
import it.communikein.myunimib.data.User;
import it.communikein.myunimib.accountmanager.AccountUtils;
import it.communikein.myunimib.databinding.ActivityLoginBinding;
import it.communikein.myunimib.data.network.S3Helper;
import it.communikein.myunimib.utilities.NetworkUtils;
import it.communikein.myunimib.utilities.UserUtils;
import it.communikein.myunimib.utilities.Utils;

import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class LoginActivity extends AuthAppCompatActivity implements
        LoaderManager.LoaderCallbacks, EasyPermissions.PermissionCallbacks {

    private ActivityLoginBinding mBinding;

    private AccountManager mAccountManager;
    private String username;
    private static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1001;

    private static final int LOADER_LOGIN_ID = 2100;
    private static final int LOADER_FAKE_LOGIN_ID = 2101;
    private static final int LOADER_CONFIRM_FACULTY_ID = 2102;

    private ProgressDialog progress;

    private int selected = Spinner.INVALID_POSITION;
    private int selectedFaculty = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initUI();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mAccountManager == null) mAccountManager = AccountManager.get(this);

        // Check if the app has the permission to access the accounts saved on the device
        if (EasyPermissions.hasPermissions(this,
                android.Manifest.permission.GET_ACCOUNTS, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if there already is a registered ic_account on the device
            Account[] S3_accounts = mAccountManager.getAccountsByType(AccountUtils.ACCOUNT_TYPE);

            if (S3_accounts.length == 0)
                mBinding.buttonLogin.setOnClickListener(v -> attemptLogin());
            else
                startActivity(new Intent(this, MainActivity.class));
        }
        else
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.error_permission_accounts_necessary),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void initUI() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        mBinding.buttonLogin.setOnClickListener(view -> attemptLogin());

        mBinding.coursesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected = position;

                Log.d("LOGIN_CHOOSE_FACULTY", "New faculty selected: " + position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        progress = new ProgressDialog(this);
        progress.setMessage(getString(R.string.label_logging_in));
        progress.setCancelable(false);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setTitle(R.string.title_login);
    }

    private void handleIntent(Intent intent) {
        String appLinkAction = intent.getAction();
        Uri appLinkData = intent.getData();

        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null){
            username = appLinkData.getLastPathSegment();
            mBinding.usernameTextInputLayout.getEditText().setText(username);
        }
    }

    private void showProgress(final boolean show) {
        if (show) progress.show();
        else if(progress != null && progress.isShowing()) progress.dismiss();
    }


    @SuppressWarnings("unchecked")
    private void handleLoginResults(final Context context, final User user){
        showProgress(false);

        int ris = (int) user.getTag();
        switch (ris){
            // If the user has to choose the faculty
            case S3Helper.ERROR_FACULTY_TO_CHOOSE:
                // Tell the user he needs to choose the faculty
                Snackbar.make(mBinding.loginView, R.string.faculty_to_choose, Snackbar.LENGTH_LONG)
                        .show();

                // Carico l'elenco dei corsi di studio
                ArrayList<String> courses_names = new ArrayList<>();
                for (int i=0; i<user.getFaculties().size(); i++)
                    courses_names.add(user.getFaculties().valueAt(i));

                // Imposto nello Spinner i corsi di studio disponibili
                final ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                        R.layout.simple_spinner_item, courses_names);
                mBinding.coursesSpinner.setAdapter(adapter);

                Log.d("LOGIN_CHOOSE_FACULTY", "Faculties shown to user. Waiting for user input.");

                // Quando l'utente seleziona il corso di studio e da conferma
                mBinding.dialogButtonOK.setOnClickListener(v -> {
                    /* Save the chosen faculty */
                    selectedFaculty = user.getFaculties().keyAt(selected);
                    Log.d("LOGIN_CHOOSE_FACULTY", "Faculty chosen: " + selected);

                    // Now that the user has selected the faculty, do the login again
                    Log.d("LOGIN_CHOOSE_FACULTY", "Trying to tell the server.");
                    getSupportLoaderManager()
                            .initLoader(LOADER_CONFIRM_FACULTY_ID, null, this)
                            .forceLoad();

                    showProgress(true);
                });
                break;
            // Se il login ha avuto successo
            case S3Helper.OK_LOGGED_IN:
            case S3Helper.OK_UPDATED:
                // Finalize login process
                finishLogin(user);
                break;

            // Se S3 non è disponibile
            case S3Helper.ERROR_S3_NOT_AVAILABLE:
                // Avviso l'utente
                Snackbar.make(mBinding.loginView, R.string.error_S3_not_available, Snackbar.LENGTH_LONG)
                        .show();
                break;

            // Se ho sbagliato password
            case S3Helper.ERROR_WRONG_PASSWORD:
                // Avviso l'utente
                Snackbar.make(mBinding.loginView, R.string.error_password_incorrect, Snackbar.LENGTH_LONG)
                        .show();

                mBinding.passwordTextInputLayout.setError(getString(R.string.error_password_incorrect));
                mBinding.passwordTextInputLayout.requestFocus();
                break;

            // Se la connessione sta impiegando troppo tempo
            case S3Helper.ERROR_CONNECTION_TIMEOUT:
                // Avviso l'utente
                Snackbar.make(mBinding.loginView, R.string.error_connection_timeout, Snackbar.LENGTH_LONG)
                        .show();
                break;

            // For any other response
            default:
                Snackbar.make(mBinding.loginView, R.string.error_generic_exception, Snackbar.LENGTH_LONG)
                        .show();
                break;
        }
    }

    private void finishLogin(User user) {
        final Account account = new Account(user.getUsername(), AccountUtils.ACCOUNT_TYPE);

        Bundle bundle = new Bundle();
        bundle.putString(User.PREF_USERNAME, user.getUsername());
        bundle.putString(User.PREF_MATRICOLA, user.getMatricola());
        mAccountManager.addAccountExplicitly(account, user.getPassword(), bundle);

        Bundle data = new Bundle();
        data.putString(AccountManager.KEY_ACCOUNT_NAME, user.getUsername());
        data.putString(User.PREF_USERNAME, user.getUsername());
        data.putString(User.PREF_PASSWORD, user.getPassword());
        data.putString(User.PREF_MATRICOLA, user.getMatricola());
        setAccountAuthenticatorResult(data);

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @SuppressWarnings("unchecked")
    private void attemptLogin() {
        if (mBinding.fakeLoginCheck.isChecked()) {
            getSupportLoaderManager()
                    .initLoader(LOADER_FAKE_LOGIN_ID, null, this)
                    .forceLoad();

            return;
        }

        if (mBinding.termsCheck.isChecked()) {
            if (NetworkUtils.isDeviceOnline(this)) {
                Utils.hideKeyboard(this);

                // Reset errors.
                mBinding.usernameTextInputLayout.setError(null);
                mBinding.passwordTextInputLayout.setError(null);

                boolean cancel = false;
                View focusView = null;

                // Check for a valid password, if the user entered one.
                if (!isPasswordValid(mBinding.passwordTextInputLayout.getEditText())) {
                    focusView = mBinding.passwordTextInputLayout;
                    cancel = true;
                }

                // Check for a valid username.
                username = validateUsername(mBinding.usernameTextInputLayout.getEditText());
                if (username == null) {
                    focusView = mBinding.usernameTextInputLayout;
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

                    getSupportLoaderManager()
                            .initLoader(LOADER_LOGIN_ID, null, this)
                            .forceLoad();
                }
            }
            else
                Snackbar.make(
                        mBinding.loginView,
                        R.string.error_no_internet,
                        Snackbar.LENGTH_LONG).show();
        } else {
            mBinding.termsCheck.setError(getString(R.string.terms_not_checked));
            mBinding.termsCheck.requestFocus();
        }
    }

    private String validateUsername(TextView view) {
        String user = view.getText().toString().trim();

        if (TextUtils.isEmpty(user)){
            user = null;
            view.setError(getString(R.string.error_user_empty));
        } else if (user.contains(" ")) {
            user = null;
            view.setError(getString(R.string.error_user_with_blank_spaces));
        } else if (user.contains("@")) {
            user = user.substring(0, user.indexOf("@"));
        }

        return user;
    }

    private boolean isPasswordValid(TextView view) {
        String password = view.getText().toString().trim();
        boolean valid = true;

        if (TextUtils.isEmpty(password)){
            valid = false;
            view.setError(getString(R.string.error_password_empty));
        } else if (password.length() < 10){
            valid = false;
            view.setError(getString(R.string.error_password_less_then_10_chars));
        } else if (password.length() > 20){
            valid = false;
            view.setError(getString(R.string.error_password_more_then_20_chars));
        } else if (!password.matches("(.)*[0-9](.)*[0-9](.)*")){
            valid = false;
            view.setError(getString(R.string.error_password_less_then_2_digits));
        }

        return valid;
    }



    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_LOGIN_ID:
                /*
                 * Get the username and password inserted by the user, create an
                 * authentic user, then start the login process.
                 */
                username = mBinding.usernameTextInputLayout.getEditText().getText().toString();
                String password = mBinding.passwordTextInputLayout.getEditText().getText().toString();
                User temp_user = new User(username, password);
                temp_user.setFake(false);

                return new S3Helper.LoginLoader(this, temp_user);

            case LOADER_FAKE_LOGIN_ID:
                /* Create a fake user and start the fake login process. */
                temp_user = new User("fake", "fake");
                temp_user.setFake(true);

                return new S3Helper.LoginLoader(this, temp_user);

            case LOADER_CONFIRM_FACULTY_ID:
                /* Get the user, save the chosen faculty, then complete the login process. */
                Utils.user = UserUtils.getUser(this);
                Utils.user.setSelectedFaculty(selectedFaculty);

                return new S3Helper.UserDataLoader(this, Utils.user);

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        int loader_id = loader.getId();

        switch (loader_id) {
            case LOADER_LOGIN_ID:
            case LOADER_FAKE_LOGIN_ID:
            case LOADER_CONFIRM_FACULTY_ID:
                handleLoginResults(this, (User) data);
                break;

            default:
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader loader) {}



    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void getAccountPermission() {
        if (EasyPermissions.hasPermissions(this, android.Manifest.permission.GET_ACCOUNTS)) {
            Snackbar.make(
                    mBinding.loginView,
                    R.string.permissions_granted,
                    Snackbar.LENGTH_LONG).show();
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    getString(R.string.error_permission_accounts_necessary),
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        // Do nothing
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        // Do nothing
    }
}

