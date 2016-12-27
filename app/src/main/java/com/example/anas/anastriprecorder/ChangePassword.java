

package com.example.anas.anastriprecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class ChangePassword extends Activity implements View.OnClickListener {
    static long timeWhenChangeButtonClicked;
    static long timeWhenCodeEmailSent;
    static final int timeOutInMinutes = 3;
    final int codeTimeOut =  (int)(long)(timeOutInMinutes * 60 *1000); //milli seconds
    private Button bChangeOldPassword;
    static EditText etNewPassword, etNewConfirmedPassword, etCode, etAddedName1,etAddedEmail1 ,etRecoveryEmail1 ;
    static final String[] specialChars = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "_", "+", "=", "-", "?", "|", ">", "<", "€", "{", "}", "[", "]", "?"};
    static final String[] numbers = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    static Context context;
    private User addedUser;
    static Cryptography mCrypt;
    private String   lastCode  ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        context = ChangePassword.this;
        setContentView(R.layout.activity_password);
        sendPW_RenewingCode();
        bChangeOldPassword = (Button) findViewById(R.id.bChangeOldPassword);
        Button bResendCode = (Button) findViewById(R.id.bResendCode);
        etNewPassword = (EditText) findViewById(R.id.etNewPassword);
        etCode = (EditText) findViewById(R.id.etCode);


        etAddedEmail1 = (EditText) findViewById(R.id.etAddedEmail1);
        etAddedName1 = (EditText) findViewById(R.id.etAddedName1);
        etRecoveryEmail1 = (EditText) findViewById(R.id.etRecoveryEmail1);

        etNewConfirmedPassword = (EditText) findViewById(R.id.etNewConfirmPassword);
        TextView tvPasswordConditions1 = (TextView) findViewById(R.id.tvPasswordConditions1);

        bChangeOldPassword.setOnClickListener(this);
        bResendCode.setOnClickListener(this);
        tvPasswordConditions1.setOnClickListener(this);

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() == 0) {
                    etNewPassword.setError("Required field");
                    bChangeOldPassword.setEnabled(false);
                } else {
                    etNewPassword.setError(null);
                    if (etNewConfirmedPassword.getText().length() != 0)
                        bChangeOldPassword.setEnabled(true);
                }

                if (!isLegalPassword(etNewPassword.getText().toString())) {
                    etNewPassword.setError("invalid Password");
                    bChangeOldPassword.setEnabled(false);
                } else {

                    if (
                            etNewConfirmedPassword.getText().length() != 0)
                        bChangeOldPassword.setEnabled(true);
                }
            }
        });

        etNewConfirmedPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {

                if (s.length() == 0) {
                    etNewConfirmedPassword.setError("Required field");
                    bChangeOldPassword.setEnabled(false);
                } else {
                    etNewConfirmedPassword.setError(null);
                    if (etNewPassword.getText().length() != 0)
                        bChangeOldPassword.setEnabled(true);
                }

                if (!etNewPassword.getText().toString().equals(etNewConfirmedPassword.getText().toString())) {
                    etNewConfirmedPassword.setError("Passwords are not identical");
                    bChangeOldPassword.setEnabled(false);
                } else {
                    etNewConfirmedPassword.setError(null);
                    if (etNewPassword.getText().length() != 0)
                        bChangeOldPassword.setEnabled(true);
                }
            }
        });
    }

    //-----------------------------------------------------------------------------------------------
    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.bChangeOldPassword:
                final String addedName = etAddedName1.getText().toString();
                final String addedEmail = etAddedEmail1.getText().toString();
                final String recoveryEmail = etRecoveryEmail1.getText().toString();
                final String code = etCode.getText().toString();
                final String newPassword = etNewPassword.getText().toString();
                final String newConfirmedPassword = etNewConfirmedPassword.getText().toString();

                timeWhenChangeButtonClicked = System.currentTimeMillis();
                Log.e("Time Change", timeWhenChangeButtonClicked + "");

                if (isLegalPassword(newPassword) &&
                        newPassword.equals(newConfirmedPassword) &&
                        code.length()>32 &&
                        (int)(long)(timeWhenChangeButtonClicked-timeWhenCodeEmailSent)<codeTimeOut &&
                        etCode.getText().toString().substring(0,56).equals(lastCode.substring(0, 56))) {
                     addedUser = new User(addedName, addedEmail, newPassword);
                     addedUser.setRecoveryEmail(recoveryEmail);
                     updateCustomer(addedUser, code);
                }else{

                    Log.e("c1", isLegalPassword(newPassword) + "");
                    Log.e("c2", newPassword.equals(newConfirmedPassword) + "");
                    Log.e("c3", (code.length() > 32) + "");
                    Log.e("c4", ((int) (long) (timeWhenChangeButtonClicked - timeWhenCodeEmailSent) < codeTimeOut) + "");
                    Log.e("c5", etCode.getText().toString().equals(lastCode) + "");

                    showErrorMsg("Error", "Please, make sure that all data are correct and the code hasn't" +
                            " timed out and you used the last code sent to your recovery email");

                }
                break;
            case R.id.bResendCode:
                sendPW_RenewingCode();
                timeWhenCodeEmailSent = System.currentTimeMillis();
                break;
            case R.id.tvPasswordConditions1:
                final AlertDialog alertDialog = new AlertDialog.Builder(ChangePassword.this).create();
                alertDialog.setTitle("Password Conditions");
                alertDialog.setMessage("Password should contain at least:\n" +
                        ">> one number [0-9]\n" +
                        ">> one special character like:\n[   !@#$%^&*(){}[]<>€   ]\n\n" +
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    //-------------------------------------------------------------------------------------------

    /*When a user forgets a password and wants to change it, he will receive a code in his recovery
    * email. He has to copy this code into the specified edit text in password changing activity and
    * define new password. This is done for protection.
    * The code has timeout. If it is exceeded, a new code will be needed to be sent .
    */
    final  ServerProcesses serverProcesses = new ServerProcesses(getBaseContext());
    public  void sendPW_RenewingCode(){
        serverProcesses.fetchUserDataByEmailOnlyInBackground(Login.email, new AfterUserAsyncTaskDone() {

            @Override
            public void done(User unknownCustomer) {
                try {
                    Cryptography mCrypt = new Cryptography();
                    String iv = mCrypt.byteArrayToHexString(mCrypt.generateIV());
                    Email mail = new Email("triprecordermailservice@gmail.com", "myAplication2016");
                    String returnedName = unknownCustomer.getName();
                    String returnedPassword = unknownCustomer.getPassword();
                    String encryptedReturnedPassword = mCrypt.encrypt(returnedPassword);
                    String returnedRecoveryEmail = unknownCustomer.getRecoveryEmail();
                    mail.setTo(new String[]{returnedRecoveryEmail});
                    mail.setSubject("triprecorder Account Password");
                    mail.setFrom("triprecorder");
                    lastCode =  iv + encryptedReturnedPassword;
                    mail.setBody("Dear " + returnedName + ",\n" +
                            "Your code is " + lastCode + "\n" +
                            "Use this code in password recovery in your Android application to set new password.\n" +
                            "If you have any other questions, you can reach us at:\n " +
                            "Tel: +492222555588\tEmail: triprecordermailservice@gmail.com\n" +
                            "Thank you for using Trip Recorder\n\nService team\nTrip Recorder");
                    mail.sendEmailInBackground();

                    ChangePassword.timeWhenCodeEmailSent = System.currentTimeMillis();

                    try {
                        showErrorMsg("Password Recovery", "A security code is sent to your password recovery email. The code is useable only for "
                                + ChangePassword.timeOutInMinutes + " minutes .. then you will have to resend the code");

                    } catch (Exception N) {
                        N.printStackTrace();//in case no value is assigned to Register.context though
                    }

                } catch (Exception e) {
                    showErrorMsg("Email not sent! .. please call us on +49222222222 to retrieve the password", e.getMessage() );
                    e.printStackTrace();
                }
            }
        });
    }

    //-----------------------------------------------------------------------------------------

    public void showErrorMsg(String title, String msg) {
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(this);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("OK", null);
        dialogueBuilder.show();
    }

    //------------------------------------------------------------------------------------------
    private void updateCustomer(final User customer, final String code1) {
        final ServerProcesses serverProcesses = new ServerProcesses(this);
        serverProcesses.fetchUserDataByEmailOnlyInBackground(customer.getEmail(), new AfterUserAsyncTaskDone() {
            @Override
            public void done(User returnedCustomer) {
                try {
                    mCrypt = new Cryptography();
                    String ivHex = code1.substring(0, 32);
                    String encryptedOldPW = code1.substring(32, code1.length());
                    mCrypt.setIV(ivHex);
                    if (returnedCustomer.getPassword().equals(mCrypt.decrypt(encryptedOldPW))) {
                        addedUser.setID(returnedCustomer.getID());
                        ServerProcesses.ID = addedUser.getID();
                        serverProcesses.storeUserDataInBackground("Update", customer, new AfterUserAsyncTaskDone() {
                            @Override
                            public void done(User NoCustomer) {
                            }
                        });
                    } else {
                        showErrorMsg("Incorrect code", "The security code you entered in incorrect. Please correct it or get new code by clicking Resend Code button");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorMsg("Error", "Please, make sure that all data are correct");
                }
            }
        });
    }

    //-----------------------------------------------------------------------------------------------

    public static void reset() {
        etCode.setText("");
        etNewPassword.setText("");
        etNewConfirmedPassword.setText("");
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


    public class MyContextWrapper extends ContextWrapper {

        public MyContextWrapper(Context base) {
            super(base);
        }
    }
}


