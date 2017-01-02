package com.example.anas.anastriprecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**Used for new user signing up*/
public class RegisterActivity extends Activity implements View.OnClickListener {

    Button bRegister ;
    EditText etAddedPW, etConfirmedPW, etAddedEmail, etAddedName, etRecoveryEmail;
    TextView tvPWConditions;
    static LocalStorage localStorage;
    Context context;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = RegisterActivity.this;
        setContentView(R.layout.activity_register);
        localStorage = new LocalStorage(this);
        bRegister       = (Button)  findViewById(R.id.bRegisterOrUpdate);
        etAddedPW       = (EditText)findViewById(R.id.etAddedPassword);
        etAddedEmail    = (EditText)findViewById(R.id.etAddedEmail);
        etAddedName     = (EditText)findViewById(R.id.etAddedName);
        etConfirmedPW   = (EditText)findViewById(R.id.etConfirmedPassword);
        etRecoveryEmail = (EditText)findViewById(R.id.etRecoveryEmail);
        tvPWConditions  = (TextView)findViewById(R.id.tvPasswordConditions);
        bRegister.setOnClickListener(this);
        tvPWConditions.setOnClickListener(this);

        /* If the updating is done, the login activity will be started without erasing texts in
        *  update(Register) activity. Instead, the content will be erased when Register is created
        */
        reset();

        /* We are using the same RegisterActivity.class for registration and account info update.
        *  Registration is called from the LoginActivity while update is called from MainActivity.
        *  If the registration activity is called by the main one => updating : change the layout.
        */
        String purpose = getIntent().getStringExtra("Purpose");
        try {
            if (purpose.equals("Update")){
                changeRegisterLayoutIntoUpdateLayout();
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        setTextWatchersForEditTexts();
    }
    //------------------------------------------------------------------------------------------------------------------------
    private void setTextWatchersForEditTexts() {
        etAddedEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etAddedEmail.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etAddedEmail.setError(null);
                    if ((etConfirmedPW.getText().length()!= 0 || bRegister.getText().equals("Update Data") ||  bRegister.getText().equals("Change Password"))
                            && etAddedPW.getText().length() != 0 && etRecoveryEmail.getText().length() != 0   &&  etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
                if (!isValidEmail(etAddedEmail.getText().toString())) {
                    etAddedEmail.setError("wrong Email format");
                    bRegister.setEnabled(false);
                } else {
                    etAddedEmail.setError(null);
                    if (etConfirmedPW.getText().length() != 0 && etAddedPW.getText().length() != 0
                            && etRecoveryEmail.getText().length() != 0 && etAddedName    .getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });
        //****************************************************************************
        etRecoveryEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                etRecoveryEmail.setError("Required field");
                bRegister.setEnabled(false);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etRecoveryEmail.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etRecoveryEmail.setError(null);
                    if ((etConfirmedPW.getText().length() != 0 || bRegister.getText().equals("Update Data")) &&
                            etAddedPW.getText().length() != 0 &&
                            etAddedEmail    .getText().length() != 0 &&
                            etRecoveryEmail .getText().length() != 0 &&
                            etAddedName     .getText().length() != 0)
                        bRegister.setEnabled(true);
                }
                if (!isValidEmail(etRecoveryEmail.getText().toString())) {
                    etRecoveryEmail.setError("wrong Email format");
                    bRegister.setEnabled(false);
                } else if(etRecoveryEmail.getText().toString().equals(etAddedEmail.getText().toString())){
                    etRecoveryEmail.setError("Account and recovery emails connot be identical");
                    bRegister.setEnabled(false);
                }else{
                    etRecoveryEmail.setError(null);
                    if ((etConfirmedPW.getText().length() != 0 || bRegister.getText().equals("Update Data")) &&
                            etAddedPW.getText().length() != 0 &&
                            etAddedEmail    .getText().length() != 0 &&
                            etRecoveryEmail .getText().length() != 0 &&
                            etAddedName     .getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });
        //****************************************************************************

        etConfirmedPW.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    etConfirmedPW.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etConfirmedPW.setError(null);
                    if (etAddedEmail.getText().length() != 0 && etAddedPW.getText().length() != 0 && etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }

                if (!etAddedPW.getText().toString().equals(etConfirmedPW.getText().toString())) {
                    etConfirmedPW.setError("Passwords are not identical");
                    bRegister.setEnabled(false);
                } else {
                    etConfirmedPW.setError(null);
                    if (etAddedEmail.getText().length() != 0 && etAddedPW.getText().length() != 0 && etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });

        //****************************************************************************
        etAddedName.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    etAddedName.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etAddedName.setError(null);
                    if (etAddedEmail.getText().length() != 0 && etAddedPW.getText().length() != 0 &&
                            (etConfirmedPW.getText().length() != 0 || bRegister.getText().equals("Update Data")))
                        bRegister.setEnabled(true);
                }
            }
        });
    }
    //------------------------------------------------------------------------------------------------------------------------

    private void changeRegisterLayoutIntoUpdateLayout() {
        bRegister.setActivated(true);
        bRegister.setText(R.string.Register);
        etAddedName.setText(localStorage.getLoggedInUser().getName());
        etAddedEmail.setText(localStorage.getLoggedInUser().getEmail());
        etRecoveryEmail.setText(localStorage.getLoggedInUser().getRecoveryEmail());
        etAddedPW.setText(localStorage.getLoggedInUser().getPassword());
        etAddedPW.setEnabled(false);
        etConfirmedPW.setText("");
        etConfirmedPW.setVisibility(View.VISIBLE);
    }
//------------------------------------------------------------------------------------------------------------------------
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bRegisterOrUpdate:
                final String addedName     = etAddedName    .getText().toString();
                final String addedEmail    = etAddedEmail   .getText().toString();
                final String addedPW       = etAddedPW      .getText().toString();
                final String confirmedPW   = etConfirmedPW  .getText().toString();
                final String recoveryEmail = etRecoveryEmail.getText().toString();

                if (ChangePasswordActivity.isLegalPassword(addedPW) && addedPW.equals(confirmedPW)
                                && isValidEmail(addedEmail) && isValidEmail(recoveryEmail)) {
                    user = new User(addedEmail,addedPW);
                    user.setName(addedName);
                    user.setRecoveryEmail(recoveryEmail);
                    if(bRegister.getText().toString().equals("Update Data"))
                        UpdateUser(user);
                    if(bRegister.getText().toString().equals("Register"))
                        registerUser(user);
                }
                break;
            case R.id.tvPasswordConditions:
                showPWConditionsDialog();
                break;
        }
    }

    //------------------------------------------------------------------------------------------------------------------------
    private void showPWConditionsDialog() {
        final AlertDialog alertDialog = new AlertDialog.Builder(RegisterActivity.this).create();
        alertDialog.setTitle("Password Conditions");
        alertDialog.setMessage("Password should have at least:\n" +
                "► one number [0-9]\n" +
                "► special character like\n" +
                "    [ !@#$%^&*(){}[]<> ]\n" +
                "► 8 characters");

        alertDialog.setButton(RESULT_OK, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.hide();
            }
        });
        alertDialog.show();
    }
    //------------------------------------------------------------------------------------------------------------------------

    public void showErrorMsg(String title, String msg) {
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(context);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("OK", null);
        dialogueBuilder.show();
    }
    //------------------------------------------------------------------------------------------------------------------------
    private void registerUser(final User user) {
        //try to see if this user is stored in the DB
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(user.getEmail(), new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedCustomer) {
                //if user already exists in DB, show error message
                if (returnedCustomer != null) {
                    showErrorMsg("Error", "Registration failed .. This email is already registered");
                //if not, register the user if the php file URL for storing users data is reached
                } else if(serverProcesses.connectionStatusMap.get("FetchUserByEmail Response").equals("200")){
                    serverProcesses.storeUserDataInBackground("Register", user, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
                // if the php file is not reached, register null user
                // (we need to run registration any way to trigger the following actions)
                } else {
                    serverProcesses.storeUserDataInBackground("Register", null, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
                }
            }
        });
    }
    //------------------------------------------------------------------------------------------------------------------------

    private void UpdateUser(final User user) {
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(user.getEmail(), new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedUser) {
        //if the user updates his email to an email that's already registered for another user with different ID, prevent it.
                if ((returnedUser != null) && (!returnedUser.getID().equals(localStorage.userLocalDB.getString("ID","")))) {
                    showErrorMsg("Error", "Update failed .. The new email is already registered");
                } else {
                    RegisterActivity.this.user.setID(new LocalStorage(getBaseContext()).getLoggedInUser().getID());
                    ServerProcesses.ID = RegisterActivity.this.user.getID();
                    serverProcesses.storeUserDataInBackground("Update", user, new AfterUserAsyncTaskDone() {
                        @Override
                        public void done(User NoCustomer) {
                        }
                    });
                }
            }
        });
    }
    //------------------------------------------------------------------------------------------------------------------------

    public void reset() {
        EditText[] editTexts = {etAddedName, etAddedEmail, etAddedPW, etConfirmedPW, etRecoveryEmail};
        for (EditText editText : editTexts)
            editText.setText("");
    }
    //------------------------------------------------------------------------------------------------------------------------

    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}