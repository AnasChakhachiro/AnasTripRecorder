
package com.example.anas.anastriprecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class LoginActivity extends Activity implements View.OnClickListener{

    final static String Register="Register";
    final static String Update="Update";
    Context context;
    TextView     tvRegister  , tvForgotPassword   ;
    Button       bLogin      , bExit              ;
    EditText     etPassword  , etEmail            ;
    LocalStorage localStorage;
    static       String   email = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        context = this;
        localStorage = new LocalStorage(this);
        etEmail           = (EditText) findViewById (R.id.etEmail          );
        etPassword        = (EditText) findViewById (R.id.etPassword       );
        tvForgotPassword  = (TextView) findViewById (R.id.tvForgotPassword );
        tvRegister        = (TextView) findViewById (R.id.tvRegister       );
        bLogin            = (Button)   findViewById (R.id.bLogin           );
        bExit             = (Button)   findViewById (R.id.bExit            );


        bExit           .setOnClickListener(this);
        bLogin          .setOnClickListener(this);
        tvRegister      .setOnClickListener(this);
        tvForgotPassword.setOnClickListener(this);

        etPassword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (etPassword.getText().toString().length() == 0) {
                        etPassword.setError("Required field");
                        bLogin.setEnabled(false);
                    } else {
                        etPassword.setError(null);
                        if (etEmail.getText().length() != 0)
                            bLogin.setEnabled(true);
                    }
                    if (!ChangePasswordActivity.isLegalPassword(etPassword.getText().toString())) {
                        etPassword.setError("invalid Password");
                        bLogin.setEnabled(false);
                    } else {
                        etEmail.setError(null);
                        if (etEmail.getText().length() != 0)
                            bLogin.setEnabled(true);
                    }
                }
            }
        });


        etEmail.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    if (etPassword.getText().toString().length() == 0) {
                        etEmail.setError("Required field");
                        bLogin.setEnabled(false);
                    } else {
                        etEmail.setError(null);
                        if (etPassword.getText().length() != 0)
                            bLogin.setEnabled(true);
                    }

                    if (!Email.isValidEmail(etEmail.getText().toString())) {
                        etEmail.setError("wrong Email format");
                        bLogin.setEnabled(false);
                    } else {
                        etEmail.setError(null);
                        if (etPassword.getText().length() != 0)
                            bLogin.setEnabled(true);
                    }
                }
            }
        });



        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etEmail.setError("Required field");
                    bLogin.setEnabled(false);
                } else {
                    etEmail.setError(null);
                    if (etPassword.getText().length() != 0)
                        bLogin.setEnabled(true);
                }

                if (!Email.isValidEmail(etEmail.getText().toString())) {
                    etEmail.setError("wrong Email format");
                    bLogin.setEnabled(false);
                } else {
                    etEmail.setError(null);
                    if (etPassword.getText().length() != 0)
                        bLogin.setEnabled(true);
                }
            }
        });


        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etPassword.setError("Required field");
                    bLogin.setEnabled(false);
                } else {
                    etPassword.setError(null);
                    if (etEmail.getText().length() != 0)
                        bLogin.setEnabled(true);
                }

                if (!ChangePasswordActivity.isLegalPassword(etPassword.getText().toString())) {
                    etPassword.setError("invalid Password");
                    bLogin.setEnabled(false);
                } else {
                    etEmail.setError(null);
                    if (etEmail.getText().length() != 0)
                        bLogin.setEnabled(true);
                }
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.bExit:
                moveTaskToBack(true);
                etPassword.setText("");
                etEmail.setText("");
                localStorage.clearUserData();
                localStorage.markAsLoggedIn(false);
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;


            case R.id.bLogin:
                String email            = etEmail   .getText().toString();
                String password         = etPassword.getText().toString();
                User loggingOnUser  = new User(email,password);
                authenticate(loggingOnUser);
                break;

            case R.id.tvForgotPassword:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                final EditText editText= new EditText(LoginActivity.this);
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                alert.setMessage("Please, enter the email of your account");
                alert.setTitle  ("Forgot password?");
                alert.setView   (editText);
                alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String emailOfAccount = editText.getText().toString();
                        fetchUserPassword(emailOfAccount);
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
                alert.show();
                break;

            case R.id.tvRegister:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    public void fetchUserPassword(String userEmail) {
        email = userEmail;
        ServerProcesses serverProcesses = new ServerProcesses(this);
        //returns name, password and recovery email
        serverProcesses.fetchUserDataByEmailOnlyInBackground(email, new AfterUserAsyncTaskDone() {

            @Override
            public void done(User user) {
                if (user == null) {
                    showErrorMsg("Error", "Please, make sure that the email entered is correct\n" +
                            "and check your internet connection");
                } else {
                    openChangePasswordScreen();
                }
            }
        });
    }


    public void authenticate(final User user){
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataInBackground(user, new AfterUserAsyncTaskDone() {

            @Override
            public void done(User returnedUser) {
                if (returnedUser != null)
                    LoginActivity.this.logUserIn(returnedUser);
                else if(serverProcesses.connectionStatusMap.get("Network Availability"  ).equals("YES")
                     && serverProcesses.connectionStatusMap.get("ServerURLResponse"     ).equals("200")
                     && serverProcesses.connectionStatusMap.get("FetchUserData Response").equals("200")){
                    showErrorMsg("LoginActivity Error", "User name or/and password are incorrect");
                } else
                    serverProcesses.fetchUserDataInBackground(null, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
            }
        });
    }

    @Override
    protected void onStart() {
        etEmail.clearFocus();
        etPassword.clearFocus();
        showMsgFromPreviousActivity();
        super.onStart();
    }

    private void showMsgFromPreviousActivity(){
        try { // when LoginActivity is called by Register class
            if (getIntent().getStringExtra("Purpose").equals("Register")) {
                showErrorMsg("User Registered", "An email of your registration " +
                        "data is sent to your recovery account");
            } else if (getIntent().getStringExtra("Purpose").equals("Update")) {
                showErrorMsg("Your session ended", "An email of your updated data is sent to your " +
                        "recovery email. Please log in again using the new credentials");
            }
            //reset the purpose field not get this message every time the activity starts
            getIntent().putExtra("Purpose","");
        }catch (Exception e){
            // when LoginActivity is not called by Register class or on the application start
            Log.e("expected", " Just ignore this exception");
        }
    }

    @Override
    public void onBackPressed(){}


    private void logUserIn(User user){
        try {
            localStorage.storeUserData(user);
            localStorage.markAsLoggedIn(true);
            ServerProcesses.localStorage.storeUserData(user);
            startActivity(new Intent(this, MainActivity.class));
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public void openChangePasswordScreen(){
        ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(LoginActivity.email, new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedUser) {
                if (returnedUser != null) {
                    Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
                    intent.putExtra("Name", returnedUser.getName());
                    intent.putExtra("Email", returnedUser.getEmail());
                    intent.putExtra("RecoveryEmail", returnedUser.getRecoveryEmail());
                    startActivity(intent);

                } else {
                    Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
                    intent.putExtra("Name", "");
                    intent.putExtra("Email", "");
                    intent.putExtra("RecoveryEmail", "");
                    startActivity(intent);
                }
            }
        });

    }


    public void showErrorMsg(String title,String msg){
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(this);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("OK",null);
        dialogueBuilder.show();
    }

}
