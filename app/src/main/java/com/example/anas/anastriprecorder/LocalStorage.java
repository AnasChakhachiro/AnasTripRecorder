
package com.example.anas.anastriprecorder;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

import static android.content.SharedPreferences.Editor;

/**
* This class manages storing and getting user data in current session
* */
class LocalStorage {
    private static final String SP_Name = "userDetails";
    SharedPreferences userLocalDB = new SharedPreferences() {
        @Override
        public Map<String, ?> getAll() {
            return null;
        }
        @Override
        public String getString(String s, String s1) {
            return null;
        }
        @Override
        public Set<String> getStringSet(String s, Set<String> set) {
            return null;
        }
        @Override
        public int getInt(String s, int i) {
            return 0;
        }
        @Override
        public long getLong(String s, long l) {
            return 0;
        }
        @Override
        public float getFloat(String s, float v) {
            return 0;
        }
        @Override
        public boolean getBoolean(String s, boolean b) {
            return false;
        }
        @Override
        public boolean contains(String s) {
            return false;
        }
        @Override
        public Editor edit() {
            return null;
        }
        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {}
        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {}
    };


    LocalStorage(Context context){
        try {
            userLocalDB = context.getSharedPreferences(SP_Name,0);
        }catch (NullPointerException N){
            N.printStackTrace();
        }
    }


    // stores user data in shared preference
    void storeUserData(User customer) {
        try {
            Editor spEditor = userLocalDB.edit();
            spEditor.putString("ID"            , customer.getID()           );
            spEditor.putString("Name"          , customer.getName()         );
            spEditor.putString("Email"         , customer.getEmail()        );
            spEditor.putString("Password"      , customer.getPassword()     );
            spEditor.putString("RecoveryEmail" , customer.getRecoveryEmail());
            spEditor.apply();
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //returns the logged in user
    User getLoggedInUser(){
        String ID            = userLocalDB.getString("ID"           ,"");
        String name          = userLocalDB.getString("Name"         ,"");
        String email         = userLocalDB.getString("Email"        ,"");
        String password      = userLocalDB.getString("Password"     ,"");
        String recoveryEmail = userLocalDB.getString("RecoveryEmail","");
        return new User(ID,name,email,recoveryEmail,password);
    }



    // this method will be called with isLogged = true when the user is logged in
    // and with false otherwise. it defines the current status of a user whether logged in or not.
    void markAsLoggedIn(boolean isLoggedIn){
        Editor spEditor = userLocalDB.edit();
        spEditor.putBoolean("isLoggedIn",isLoggedIn);
        spEditor.apply();
    }

    //returns if a user is logged in or not .. not logged in is the default status
    boolean isLoggedIn() {
        try {
            return (userLocalDB.getBoolean("isLoggedIn", false));
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }


    // clears user data when he logs out
    void clearUserData(){
        Editor editor = userLocalDB.edit();
        editor.clear();
        editor.apply();
    }
}