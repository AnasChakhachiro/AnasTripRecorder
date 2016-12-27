
package com.example.anas.anastriprecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class Login extends Activity implements View.OnClickListener{

    TextView     tvRegister  , tvForgotPassword   ;
    Button       bLogin      , bExit              ;
    EditText     etPassword  , etEmail            ;
    LocalStorage localStorage = new LocalStorage();
    static final String[] specialChars = { "!", "@", "#", "$", "%", "^", "&", "*", "(", ")","_","+"
                                          ,"=", "-", "?", "|", ">", "<", "{", "}", "[", "]", "?" };
    static final String[] numbers      = {"1", "2" , "3" , "4" , "5" , "6" , "7" , "8" , "9" ,"0"};
    static       String   email = "";
    static       Context  context   ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
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
                    if (!isLegalPassword(etPassword.getText().toString())) {
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

                if (!isLegalPassword(etPassword.getText().toString())) {
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


    public static boolean isLegalPassword(String password) {
        // PW has 8 or more characters and one char at least is a number and one at least is special char

        if(password.length()<8)  return false;

        for (int i = 0; i < specialChars.length - 1; i++) {
            if (password.contains(specialChars[i]))
                for (int j = 0; j < numbers.length - 1; j++)
                    if (password.contains(numbers[j]))  return true;
        }
        return false;
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
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
                break;


            case R.id.bLogin:
                String email            = etEmail   .getText().toString();
                String password         = etPassword.getText().toString();
                User loggingOnCustomer  = new User(email,password);
                authenticate(loggingOnCustomer);
                break;

            case R.id.tvForgotPassword:
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                final EditText editText= new EditText(Login.this);
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
                startActivity(new Intent(this, Register.class));
                break;
        }
    }

    public void fetchUserPassword(String userEmail) {
        email = userEmail;
        ServerProcesses serverProcesses = new ServerProcesses(this);
        //returns name, password and recovery email
        serverProcesses.fetchUserDataByEmailOnlyInBackground(email, new AfterUserAsyncTaskDone() {

            @Override
            public void done(User unknownCustomer) {
                if (unknownCustomer == null) {
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
                    Login.this.logUserIn(returnedUser);
                else if(serverProcesses.connectionStatusMap.get("Network Availability"  ).equals("YES")
                     && serverProcesses.connectionStatusMap.get("ServerURLResponse"     ).equals("200")
                     && serverProcesses.connectionStatusMap.get("FetchUserData Response").equals("200")){
                    showErrorMsg("Login Error", "User name or/and password are incorrect");
                } else
                    serverProcesses.fetchUserDataInBackground(null, new AfterUserAsyncTaskDone() {
                        @Override public void done(User returnedUser){}
                    });
            }
        });
    }

    @Override
    public void onBackPressed(){}




    private void logUserIn(User customer){
        try {
            localStorage.storeUserData(customer);
            localStorage.markAsLoggedIn(true);
            ServerProcesses.localStorage.storeUserData(customer);
            startActivity(new Intent(this, MainActivity.class));
        }catch(Exception e){
            e.printStackTrace();
        }
    }


    public void openChangePasswordScreen(){
        startActivity(new Intent(Login.this, ChangePassword.class));
        ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(Login.email, new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedCustomer) {
                if (returnedCustomer != null) {
                    ChangePassword.etAddedName1.setText(returnedCustomer.getName());
                    ChangePassword.etAddedEmail1.setText(returnedCustomer.getEmail());
                    ChangePassword.etRecoveryEmail1.setText(returnedCustomer.getRecoveryEmail());
                    ChangePassword.etAddedName1.setActivated(false);
                    ChangePassword.etAddedEmail1.setActivated(false);
                    ChangePassword.etRecoveryEmail1.setActivated(false);

                    ChangePassword.etNewPassword.setText("");
                    ChangePassword.etNewConfirmedPassword.setText("");

                } else {
                    try {
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                showErrorMsg("Connection Error", "Couldn't get data from BE. Please, check your internet connection");
                            }
                        }, 50);//we need some delay to allow Register.java to assign value to ChangePassword.context or we'll get crash
                    } catch (Exception N) {
                        N.printStackTrace();//in case no value is assigned to Register.context though
                    }
                }
            }
        });

    }



    public static void showErrorMsg(String title,String msg){
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(context);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("OK",null);
        dialogueBuilder.show();
    }

}
