

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


public class Register extends Activity implements View.OnClickListener {

    static Button bRegister ;
    static EditText etAddedPassword, etConfirmedPassword, etAddedEmail, etAddedName, etRecoveryEmail;
    static TextView tvPasswordConditions;
    static LocalStorage customerLocalStore = new LocalStorage();
    static final String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "=", "-", "?", "|", ">", "<", "€", "{", "}", "[", "]", "?"};
    static final String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    static Context context;
    private User addedUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = Register.this;
        setContentView(R.layout.activity_register);
        bRegister = (Button) findViewById(R.id.bRegisterOrUpdate);
        etAddedPassword = (EditText) findViewById(R.id.etAddedPassword);
        etAddedEmail = (EditText) findViewById(R.id.etAddedEmail);
        etAddedName = (EditText) findViewById(R.id.etAddedName);
        etConfirmedPassword = (EditText) findViewById(R.id.etConfirmedPassword);
        etRecoveryEmail = (EditText) findViewById(R.id.etRecoveryEmail);
        tvPasswordConditions = (TextView)findViewById(R.id.tvPasswordConditions);
        bRegister.setOnClickListener(this);
        tvPasswordConditions.setOnClickListener(this);

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
                    if ((etConfirmedPassword.getText().length() != 0 || bRegister.getText().equals("Update Data") ||  bRegister.getText().equals("Change Password")) &&
                            etAddedPassword.getText().length() != 0 &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
                if (!isValidEmail(etAddedEmail.getText().toString())) {
                    etAddedEmail.setError("wrong Email format");
                    bRegister.setEnabled(false);
                } else {
                    etAddedEmail.setError(null);
                    if (etConfirmedPassword.getText().length() != 0 &&
                            etAddedPassword.getText().length() != 0 &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });

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
                    if ((etConfirmedPassword.getText().length() != 0 || bRegister.getText().equals("Update Data")) &&
                            etAddedPassword.getText().length() != 0 &&
                            etAddedEmail.getText().length() != 0 &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
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
                    if ((etConfirmedPassword.getText().length() != 0 || bRegister.getText().equals("Update Data")) &&
                            etAddedPassword.getText().length() != 0 &&
                            etAddedEmail.getText().length() != 0 &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });


        etAddedPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etAddedPassword.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etAddedPassword.setError(null);
                    if (etAddedEmail.getText().length() != 0 &&
                            (etConfirmedPassword.getText().length() != 0 || bRegister.getText().equals("Update Data") ||  bRegister.getText().equals("Change Password")) &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }

                if (!Login.isLegalPassword(etAddedPassword.getText().toString())) {
                    etAddedPassword.setError("invalid Password");
                    bRegister.setEnabled(false);
                } else {
                    etAddedEmail.setError(null);
                    if (etAddedEmail.getText().length() != 0 &&
                            etConfirmedPassword.getText().length() != 0 &&
                            etRecoveryEmail.getText().length() != 0 &&
                            etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });
        etConfirmedPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    etConfirmedPassword.setError("Required field");
                    bRegister.setEnabled(false);
                } else {
                    etConfirmedPassword.setError(null);
                    if (etAddedEmail.getText().length() != 0 && etAddedPassword.getText().length() != 0 && etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }

                if (!etAddedPassword.getText().toString().equals(etConfirmedPassword.getText().toString())) {
                    etConfirmedPassword.setError("Passwords are not identical");
                    bRegister.setEnabled(false);
                } else {
                    etConfirmedPassword.setError(null);
                    if (etAddedEmail.getText().length() != 0 && etAddedPassword.getText().length() != 0 && etAddedName.getText().length() != 0)
                        bRegister.setEnabled(true);
                }
            }
        });
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
                    if (etAddedEmail.getText().length() != 0 && etAddedPassword.getText().length() != 0 &&
                            (etConfirmedPassword.getText().length() != 0 || bRegister.getText().equals("Update Data")))
                        bRegister.setEnabled(true);
                }
            }
        });
    }

    //-----------------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.bRegisterOrUpdate:
                final String addedName = etAddedName.getText().toString();
                final String addedEmail = etAddedEmail.getText().toString();
                final String addedPassword = etAddedPassword.getText().toString();
                final String confirmedPassword = etConfirmedPassword.getText().toString();
                final String recoveryEmail = etRecoveryEmail.getText().toString();

                if (Login.isLegalPassword(addedPassword) && addedPassword.equals(confirmedPassword)
                       && isValidEmail(addedEmail) && isValidEmail(recoveryEmail)) {
                    addedUser = new User(addedEmail,addedPassword);
                    addedUser.setName(addedName);
                    addedUser.setRecoveryEmail(recoveryEmail);
                    if(bRegister.getText().toString().equals("Update Data"))
                        UpdateUser(addedUser);
                    if(bRegister.getText().toString().equals("Register"))
                        registerUser(addedUser);
                }
                break;
            case R.id.tvPasswordConditions:
                final AlertDialog alertDialog = new AlertDialog.Builder(Register.this).create();
                alertDialog.setTitle("Password Conditions");
                alertDialog.setMessage("Password should contain at least:\n" +
                        ">> one number [0-9]\n" +
                        ">> one special character like:\n     [   !@#$%^&*(){}[]<>   ]\n\n" +
                        "and should be at least 8 characters long");

                alertDialog.setButton(RESULT_OK, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.hide();
                    }
                });
                alertDialog.show();
                break;
        }
    }

    //-----------------------------------------------------------------------------------------

    public static void showErrorMsg(String title, String msg) {
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(context);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("OK", null);
        dialogueBuilder.show();
    }

    //------------------------------------------------------------------------------------------
    private void registerUser(final User user) {
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(user.getEmail(), new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedCustomer) {
                if (returnedCustomer != null) {
                    showErrorMsg("Error", "Registration failed .. This email is already registered");
                } else if(serverProcesses.connectionStatusMap.get("FetchUserByEmail Response").equals("200")){
                    serverProcesses.storeUserDataInBackground("Register", user, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
                } else {
                    serverProcesses.storeUserDataInBackground("Register", null, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
                }
            }
        });
    }


    private void UpdateUser(final User user) {
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(user.getEmail(), new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedUser) {
                if ((returnedUser != null) &&
                        (!returnedUser.getID().equals(customerLocalStore.userLocalDB.getString("ID", "")))) {
                    //because trying to insert new user in the table that already contains the email will fail and return null
                    showErrorMsg("Error", "Update failed .. The new email is already registered");
                } else {
                    addedUser.setID(new LocalStorage().getLoggedInUser().getID());
                    ServerProcesses.ID = addedUser.getID();
                    serverProcesses.storeUserDataInBackground("Update", user, new AfterUserAsyncTaskDone() {
                        @Override
                        public void done(User NoCustomer) {
                        }
                    });
                }
            }
        });
    }
                    //-----------------------------------------------------------------------------------------------

    public static void reset() {

            etAddedName.setText("");
            etAddedEmail.setText("");
            etAddedPassword.setText("");
            etConfirmedPassword.setText("");
            etRecoveryEmail.setText("");

    }
                //-----------------------------------------------------------------------------------------------

    public static boolean isValidEmail(CharSequence email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
                //-----------------------------------------------------------------------------------------------
    public static boolean isLegalPassword(String password) {
        boolean hasNumber = false;

        if (password.length() < 8) {
            return false;
        }
        for (int i = 0; i < specialChars.length - 1; i++) {
            if (password.contains(specialChars[i])) {
                for (int j = 0; j < numbers.length - 1; j++) {
                    if (password.contains(numbers[j])) {
                        hasNumber = true;
                        break;
                    }
                }
                break;
            }
        }
        return (hasNumber);
    }
}

