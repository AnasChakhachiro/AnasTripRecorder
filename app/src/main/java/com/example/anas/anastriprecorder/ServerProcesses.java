
package com.example.anas.anastriprecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

class ServerProcesses {
    private static Cryptography cryptography;
    private static final String SERVER_ADDRESS = "http://triprecorder.000webhostapp.com/";
    private static final int CONN_TIME_OUT = 15000;
    static String ID;
    private Context context;
    static LocalStorage localStorage ;
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";

     Map<String, String> connectionStatusMap;
     {
        connectionStatusMap = new HashMap<>();
        setConnectionStatusMapToDefaultValues();
     }


    ServerProcesses(Context mContext) {
        try {
            cryptography = new Cryptography();
            this.context = mContext;
            localStorage = new LocalStorage(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // connectionStatus map defines whether the php files and the server are reachable and internet is available
    private  void setConnectionStatusMapToDefaultValues(){
        connectionStatusMap.put("Network Availability", "NO");
        connectionStatusMap.put("ServerURLResponse", "0");
        for (ServerFiles.PhpFile phpFile : ServerFiles.PhpFile.values()) {
            String phpFileName = ServerFiles.getFile(phpFile);
            Log.e("test", phpFileName.substring(0,phpFileName.length()-4) + " Response");
            connectionStatusMap.put(phpFileName.substring(0,phpFileName.length()-4) + " Response", "0");
        }
    }



    private static boolean isNetworkAvailable (NetworkInfo netInfo) {
        return netInfo != null && netInfo.isConnected();
    }

    // establishes http connection to URL and returns an object of that connection
    private static HttpURLConnection getHttpUrlConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setConnectTimeout(CONN_TIME_OUT);
        urlc.connect();
        return urlc;
    }

    private  void checkServerUrlReachability()throws IOException {
        HttpURLConnection urlc = getHttpUrlConnection(SERVER_ADDRESS);
        connectionStatusMap.remove("ServerURLResponse");
        connectionStatusMap.put("ServerURLResponse", String.valueOf(urlc.getResponseCode()));
    }

    // checks if the php files on the server side are reachable
    private  void checkPhpFilesUrlsReachability()throws IOException {
        for (ServerFiles.PhpFile phpFile : ServerFiles.PhpFile.values()) {
            String phpFileName = ServerFiles.getFile(phpFile);
            HttpURLConnection urlc = getHttpUrlConnection(SERVER_ADDRESS + phpFileName);
            connectionStatusMap.remove(phpFileName.substring(0,phpFileName.length()-4) + " Response");
            connectionStatusMap.put(phpFileName.substring(0,phpFileName.length()-4) + " Response",
                    String.valueOf(urlc.getResponseCode()));
        }
    }

    // returns a map whose values are the server response codes when trying to access server and php urls.
    private  Map<String,String> connectionStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (isNetworkAvailable(netInfo)) {
            try {
                connectionStatusMap.put("Network Availability", "YES");
                checkServerUrlReachability();
                checkPhpFilesUrlsReachability();
            }catch(Exception e){
                e.printStackTrace();
                return connectionStatusMap;
            }
        }
        return connectionStatusMap;
    }



    private HttpURLConnection setHttpPostRequest(final URL url, Map<String, String> dataToSend){
        Thread connectionStatusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                connectionStatusMap.clear();
                setConnectionStatusMapToDefaultValues();
                connectionStatus(context);
            }
        });
        connectionStatusThread.start();

        try {
            connectionStatusThread.join();
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setConnectTimeout(CONN_TIME_OUT);
            http.setReadTimeout   (CONN_TIME_OUT);
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : dataToSend.entrySet()) {
                sb.append(URLEncoder.encode(entry.getKey()  ,"UTF-8") ).append("=")
                  .append(URLEncoder.encode(entry.getValue(), "UTF-8")).append("&");
            }
            byte[] out = sb.toString().getBytes(Charset.forName("UTF-8"));
            int length = out.length;
            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", CONTENT_TYPE );
            http.connect();
            OutputStream os = http.getOutputStream();
            try {
                os.write(out);
            }finally {
                try {
                    if (os != null) os.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            return http;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }


    private JSONObject postAndGetJasonResponse(HttpURLConnection http){
        try {
            InputStream inputStream = http.getInputStream();
            String responseString   = IOUtils.toString(inputStream, "UTF-8");

            JSONObject jObj = new JSONObject(responseString.substring(responseString.
                    indexOf("{"), responseString.lastIndexOf("}")+1));
            Iterator iterate = jObj.keys();
            while (iterate.hasNext()) {
                String key = iterate.next().toString( );
                String value = jObj.get(key).toString();
                Log.e("debugging", key + " = " + value);
            }
            return jObj;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
   //==================================================================================================================================

    void fetchUserDataInBackground(User user, AfterUserAsyncTaskDone callback){
        //    progressDialog.show();
        new FetchCustomerDataAsyncTask(user, callback).execute();
    }

    private class FetchCustomerDataAsyncTask extends AsyncTask<Void,Void,User> {
        boolean isSuc = false;
        User               user    ;
        AfterUserAsyncTaskDone callback;
        FetchCustomerDataAsyncTask(User user, AfterUserAsyncTaskDone callback) {
            this.callback = callback;
            this.user     = user    ;
        }

        @Override
        protected User doInBackground(Void... voids) {
            User returnedUser = null;
            try {
                URL url = new URL(SERVER_ADDRESS + "FetchUserData.php");
                Map<String, String> dataToSend = new HashMap<>();
                dataToSend.put("Email", cryptography.encrypt(user.getEmail()));
                dataToSend.put("IV" ,cryptography.getIV() );
                dataToSend.put("Password", cryptography.encrypt(user.getPassword()));

                HttpURLConnection  http = setHttpPostRequest(url, dataToSend);
                JSONObject jObj = postAndGetJasonResponse(http);

                assert jObj != null;
                if (jObj.length() >= 5) {
                    returnedUser = new User(user.getID(), user.getName(), user.getEmail()
                            ,user.getPassword(), user.getRecoveryEmail());
                    cryptography.setIV(jObj.getString("IV"));
                    returnedUser.setID           (cryptography.decrypt(jObj.getString("ID")));
                    returnedUser.setName         (cryptography.decrypt(jObj.getString("Name")));
                    returnedUser.setPassword     (cryptography.decrypt(jObj.getString("Password")));
                    returnedUser.setEmail        (cryptography.decrypt(jObj.getString("Email")));
                    returnedUser.setRecoveryEmail(cryptography.decrypt(jObj.getString("RecoveryEmail")));
                }
                isSuc = true;
            } catch (Exception e) {
                isSuc = false;
                e.printStackTrace();
                return null;
            }
            return returnedUser;
        }

        @Override
        protected void onPostExecute(User returnedUser) {
            callback.done(returnedUser);
            Log.e("code233 NA= ", connectionStatusMap.get("Network Availability"));
            Log.e("code SR= "   , connectionStatusMap.get("ServerURLResponse"));
            Log.e("code FUD= "  , connectionStatusMap.get("FetchUserData Response"));
            Log.e("code FUBE= " , connectionStatusMap.get("FetchUserByEmail Response"));
            Log.e("code ROU= "  , connectionStatusMap.get("RegisterOrUpdateUser Response"));
            Log.e("code MC= "   , connectionStatusMap.get("MCrypt Response"));
            Log.e("code AT= "   , connectionStatusMap.get("AddTrip Response"));


            if (connectionStatusMap.get("Network Availability").equals("NO"))
                showMessage("Error", "No network available",context);

            else if (!connectionStatusMap.get("ServerURLResponse").equals("200"))
                showMessage("Error","Server URL" + SERVER_ADDRESS + "is unreachable",context);

            else if (!connectionStatusMap.get("FetchUserData Response").equals("200"))
                showMessage("Error", "URL of file FetchUserData.php is unreachable",context);

            else if (!connectionStatusMap.get("MCrypt Response").equals("200"))
                showMessage("Error", "URL of file MCrypt.php is unreachable",context);

            else if (!isSuc)
                showMessage("Error","Error while exchanging data with the server",context);

            super.onPostExecute(returnedUser);
        }
    }

    //==================================================================================================================================

    // stores user data in userPrivateData table
    void storeUserDataInBackground(String purpose, User customer, AfterUserAsyncTaskDone callback){
        new StoreUserDataAsynTask(purpose, customer, callback).execute();
    }


    private class StoreUserDataAsynTask extends AsyncTask <Void,Void,Void> {
        boolean isSuc = false;
        String  purpose;
        User    user;
        AfterUserAsyncTaskDone callback;
        StoreUserDataAsynTask(String purpose, User user, AfterUserAsyncTaskDone callback){
            this.purpose  = purpose ;
            this.callback = callback;
            this.user     = user    ;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                cryptography.generateIV();
                URL url = new URL(SERVER_ADDRESS + "RegisterOrUpdateUser.php");
                final Map<String , String>  dataToSend   =   new HashMap<>()  ;
                dataToSend.put("IV", cryptography.getIV());
                if (purpose.equals("Update"))
                    dataToSend.put("ID", cryptography.encrypt(ServerProcesses.ID));
                dataToSend.put("Name"         , cryptography.encrypt(user.getName()         ));
                dataToSend.put("Email"        , cryptography.encrypt(user.getEmail()        ));
                dataToSend.put("Password"     , cryptography.encrypt(user.getPassword()     ));
                dataToSend.put("RecoveryEmail", cryptography.encrypt(user.getRecoveryEmail()));
                dataToSend.put("Purpose", cryptography.encrypt(purpose));
                HttpURLConnection http = setHttpPostRequest(url, dataToSend);
                postAndGetJasonResponse(http);
                isSuc = true;
                Email.sendEmailAfterRegistrationOrUpdate(user);
            }catch(Exception e){
                isSuc = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            callback.done(null);
            Log.e("code NA=   " , connectionStatusMap.get("Network Availability"         ));
            Log.e("code SR=   " , connectionStatusMap.get("ServerURLResponse"            ));
            Log.e("code FUD=  " , connectionStatusMap.get("FetchUserData Response"       ));
            Log.e("code FUBE= " , connectionStatusMap.get("FetchUserByEmail Response"    ));
            Log.e("code ROU=  " , connectionStatusMap.get("RegisterOrUpdateUser Response"));
            Log.e("code MC=   " , connectionStatusMap.get("MCrypt Response"              ));

            if (connectionStatusMap.get("Network Availability").equals("NO"))
                //Register.showErrorMsg("Error", "No network available");
                showMessage("Error", "No network available",context);
            else if (!connectionStatusMap.get("ServerURLResponse").equals("200"))
               // Register.showErrorMsg("Error","Server URL" + SERVER_ADDRESS + "is unreachable");
               showMessage("Error","Server URL" + SERVER_ADDRESS + "is unreachable",context);

            else if (!connectionStatusMap.get("FetchUserByEmail Response").equals("200"))
                showMessage("Error", "URL of file FetchUserByEmail.php is unreachable",context);

            else if (!connectionStatusMap.get("RegisterOrUpdateUser Response").equals("200"))
                showMessage("Error","URL of file RegisterOrUpdateUser.php is unreachable",context);

            else if (!connectionStatusMap.get("MCrypt Response").equals("200"))
                showMessage("Error", "URL of file MCrypt.php is unreachable",context);

            else if (!isSuc)
                    Register.showErrorMsg("Error","Error while exchanging data with the server");

            else if( purpose .equals("Register")){
                Login.prposeOfRegisterClass=Login.Register;
                Register.context.startActivity(new Intent(Register.context, Login.class));
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Login.showErrorMsg("User Registered"  , "An email of your registration "  +
                                           "data is sent to your recovery account");
                    }
                }, 500);

            }else if(purpose .equals("Update")){
                Login.prposeOfRegisterClass=Login.Update;
                try {
                    Register.reset();// We are in Register Activity
                    Register.localStorage.clearUserData();
                    Register.localStorage.markAsLoggedIn(false);
                    Register.context.startActivity(new Intent(Register.context, Login.class));
                }catch(NullPointerException N){
                    N.printStackTrace(); // we are in change password activity
                    ChangePassword.context.startActivity(new Intent(ChangePassword.context, Login.class));
                }

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {Login.showErrorMsg("Your session ended", "An email of your updated data is sent to your " +
                                       "recovery email. Please log in again using the new credentials");
                        }
                }, 500);
            }
            super.onPostExecute(aVoid);
        }
    }

//=================================================================================================================================

    //used in log in to check if the credentials are correct
    void fetchUserDataByEmailOnlyInBackground(String email, AfterUserAsyncTaskDone callback){
        new FetchCustomerDataByEmailOnlyAsyncTask(email, callback).execute();
    }

    private class FetchCustomerDataByEmailOnlyAsyncTask extends AsyncTask <Void,Void,User> {
        String  email;
        AfterUserAsyncTaskDone callback;
        FetchCustomerDataByEmailOnlyAsyncTask(String email, AfterUserAsyncTaskDone callback) {
            this.callback = callback;
            this.email = email;
        }

        @Override
        protected User doInBackground(Void... voids) {
            try {
                URL url = new URL(SERVER_ADDRESS+"FetchUserByEmail.php");
                final  Map<String, String>  dataToSend = new HashMap<>();
                cryptography.generateIV();
                dataToSend.put("IV"   , cryptography.getIV (    )  );
                dataToSend.put("Email", cryptography.encrypt(email));
                HttpURLConnection http  = setHttpPostRequest(url, dataToSend);
                JSONObject jObj = postAndGetJasonResponse(http);
                assert jObj != null;
                String IV = jObj.getString("IV");
                cryptography.setIV(IV);
                String ID             = cryptography.decrypt(jObj.getString("ID")           );
                String name           = cryptography.decrypt(jObj.getString("Name")         );
                String email          = cryptography.decrypt(jObj.getString("Email")        );
                String password       = cryptography.decrypt(jObj.getString("Password")     );
                String recoveryEmail  = cryptography.decrypt(jObj.getString("RecoveryEmail"));
                return  new User(ID,name,email,recoveryEmail,password);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(User unknownCustomer) {
            callback.done(unknownCustomer);
            super.onPostExecute(unknownCustomer);
        }
    }

//============================================================================================================
    /** A method to download json data from url used in placesTask in AddTrip */
    String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }catch(Exception e){
            Log.d("Error while getting url", e.toString());
        }finally{
            assert iStream != null;
            try {
                iStream.close();
            }catch (IOException e){
                e.printStackTrace();
            }
            urlConnection.disconnect();
        }
        return data;
    }

    //=============================================================================================

    /**
     * Puts manually-added Trip in TripSummary table of the data base
     * */
    void addTripInBackground(Trip trip, AfterTripTaskDone callback){
        //    progressDialog.show();
        new AddTripAsyncTask(trip, callback).execute();
    }

    private class AddTripAsyncTask extends AsyncTask<Trip,Void,Void> {
        boolean isSuc = false;
        Trip               trip     ;
        AfterTripTaskDone callback ;
        String      feedbackMessage ;
        AddTripAsyncTask(Trip trip  , AfterTripTaskDone callback) {
            this.callback = callback;
            this.trip     = trip    ;
        }

        @Override
        protected Void doInBackground(Trip... trips) {
            User user = localStorage.getLoggedInUser()  ;
            Log.e("user Id is", user.getID());

            try {
                URL url = new URL(SERVER_ADDRESS + "AddTrip.php");
                Map<String,String> dataToSend = new HashMap<>();
                dataToSend.put("IV"          , cryptography.getIV());
                dataToSend.put("UserID"      , cryptography.encrypt(user.getID())  );
                dataToSend.put("Duration"    , cryptography.encrypt(trip.getDuration()));
                Scanner scanner = new Scanner(trip.getDistance());
                dataToSend.put("Distance"    , cryptography.encrypt(String.valueOf(scanner.nextFloat())));
                dataToSend.put("DistanceUnit", cryptography.encrypt(scanner.next()));
                dataToSend.put("StartAddress", cryptography.encrypt(trip.getStartAddress().getAddressLine(0)));
                dataToSend.put("StopAddress" , cryptography.encrypt(trip.getStopAddress().getAddressLine(0)));
                int monthStrt = trip.getStartCalendar().get(Calendar.MONTH);
                int monthStop = trip.getStopCalendar(). get(Calendar.MONTH);
                int yearStrt  = trip.getStartCalendar().get(Calendar.YEAR);
                int yearStop  = trip.getStopCalendar(). get(Calendar.YEAR);
                DecimalFormat formatter = new DecimalFormat("00");

                dataToSend.put("StartDate",cryptography.encrypt(trip.getStartCalendar().get(Calendar.DAY_OF_MONTH)+
                        //hacking of month-year error as December is skipped
                        ", " +( monthStrt<1 ? 12:formatter.format(monthStrt))+
                        ", " +( monthStrt<1 ? yearStrt-1:yearStrt)));

                dataToSend.put("StopDate" ,cryptography.encrypt(trip.getStopCalendar() .get(Calendar.DAY_OF_MONTH)+
                        ", " +( monthStop<1 ? 12:formatter.format(monthStop))+
                        ", " +( monthStop<1 ? yearStop-1:yearStop)));

                dataToSend.put("StartTime",cryptography.encrypt(formatter.format(trip.getStartCalendar().get(Calendar.HOUR_OF_DAY ))+
                                       " : " +formatter.format(trip.getStartCalendar().get(Calendar.MINUTE))));
                dataToSend.put("StopTime" ,cryptography.encrypt(formatter.format(trip.getStartCalendar().get(Calendar.HOUR_OF_DAY ))+
                                       " : " +formatter.format(trip.getStartCalendar().get(Calendar.MINUTE))));

                dataToSend.put("StartLatitude" ,cryptography.encrypt(String.valueOf(trip.getStartAddress().getLatitude ())));
                dataToSend.put("StartLongitude",cryptography.encrypt(String.valueOf(trip.getStartAddress().getLongitude())));
                dataToSend.put("StopLatitude"  ,cryptography.encrypt(String.valueOf(trip.getStopAddress() .getLatitude ())));
                dataToSend.put("StopLongitude" ,cryptography.encrypt(String.valueOf(trip.getStopAddress() .getLongitude())));

                HttpURLConnection  http = setHttpPostRequest(url, dataToSend);
                JSONObject jObj = postAndGetJasonResponse(http);

                if (jObj!=null) {
                    cryptography.setIV(jObj.getString("IV"));
                    feedbackMessage = cryptography.decrypt(jObj.getString("status"));
                }
                isSuc = true;
            } catch (Exception e) {
                isSuc = false;
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void aVoid) {
            Log.e("code NA= "  , connectionStatusMap.get("Network Availability"));
            Log.e("code SR= "  , connectionStatusMap.get("ServerURLResponse"));
            Log.e("code FUD= " , connectionStatusMap.get("FetchUserData Response"));
            Log.e("code FUBE= ", connectionStatusMap.get("FetchUserByEmail Response"));
            Log.e("code ROU= " , connectionStatusMap.get("RegisterOrUpdateUser Response"));
            Log.e("code MC= "  , connectionStatusMap.get("MCrypt Response"));
            Log.e("code AT= "  , connectionStatusMap.get("AddTrip Response"));

            String message = "Trip is added";
            if (connectionStatusMap.get("Network Availability").equals("NO")) {
                showMessage("Error", "No network available", context);
                message = "Failed to add trip";
            } else if (!connectionStatusMap.get("ServerURLResponse").equals("200")) {
                showMessage("Error", "Server URL" + SERVER_ADDRESS + "is unreachable", context);
                message = "Failed to add trip";
            } else if (!connectionStatusMap.get("AddTrip Response").equals("200")) {
                showMessage("Error", "URL of file AddTrip.php is unreachable", context);
                message = "Failed to add trip";
            } else if (!connectionStatusMap.get("MCrypt Response").equals("200")) {
                showMessage("Error", "URL of file MCrypt.php is unreachable", context);
                message = "Failed to add trip";
            } else if (!isSuc) {
                showMessage("Error", "Error while exchanging data with the server", context);
                message = "Failed to add trip";
            }

            AddTrip.pd.dismiss();
            Toast.makeText(context,message,Toast.LENGTH_LONG).show();
            super.onPostExecute(aVoid);
        }

    }
    //==================================================================================================================================

     private void showMessage(String title, String msg, Context context) {
         final AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(context);
         dialogueBuilder.setTitle(title);
         dialogueBuilder.setMessage(msg);
         dialogueBuilder.setCancelable(true);
         dialogueBuilder.setPositiveButton("OK",null);
         dialogueBuilder.show();
    }
}
