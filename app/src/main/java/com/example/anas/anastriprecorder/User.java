
package com.example.anas.anastriprecorder;

public class User {
    private String id            = "";
    private String name          = "";
    private String password      = "";
    private String email         = "";
    private String recoveryEmail = "";

    public User( String mEmail, String mPassword){
        this.email    =    mEmail;
        this.password = mPassword;
    }

    public User(String mName, String mEmail,String mPassword){
        this.email    = mEmail;
        this.password = mPassword;
        this.name     = mName;
    }

    public User(String mID, String mName, String mEmail, String mRecoveryEmail, String mPassword){
        this.id             = mID           ;
        this.name           = mName         ;
        this.email          = mEmail        ;
        this.recoveryEmail  = mRecoveryEmail;
        this.password       = mPassword     ;
    }

    public String getID()           {return this.id           ;}
    public String getName()         {return this.name         ;}
    public String getEmail()        {return this.email        ;}
    public String getPassword()     {return this.password     ;}
    public String getRecoveryEmail(){return this.recoveryEmail;}

    public void setID           (String mID)           {this.id       = mID                ;}
    public void setName         (String mName)         {this.name     = mName              ;}
    public void setEmail        (String mEmail)        {this.email    = mEmail             ;}
    public void setPassword     (String mPassword)     {this.password = mPassword          ;}
    public void setRecoveryEmail(String mRecoveryEmail){this.recoveryEmail = mRecoveryEmail;}
}
