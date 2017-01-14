package com.example.anas.anastriprecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.model.LatLng;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import static com.example.anas.anastriprecorder.AddTripActivity.pd;

/**This class manages all the processes between the application and the server*/
class ServerProcesses {
    private static Cryptography cryptography;
    private static final String SERVER_ADDRESS = "http://triprecorder.000webhostapp.com/";
    private static final int CONN_TIME_OUT = 15000;
    static String ID;
    private Context context;
    static LocalStorage localStorage ;
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded; charset=UTF-8";
    private AsyncTask addTripAsyncTask;

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



    /** ConnectionStatus map defines whether the php files and the server are reachable and internet is available*/
    private  void setConnectionStatusMapToDefaultValues(){
        connectionStatusMap.put("Network Availability","NO");
        connectionStatusMap.put("ServerURLResponse", "0");
        for (ServerFiles.PhpFile phpFile : ServerFiles.PhpFile.values()) {
            String phpFileName = ServerFiles.getFile(phpFile);
            connectionStatusMap.put(phpFileName.substring(0,phpFileName.length()-4) + " Response", "0");
        }
    }



    private String showServerRelatedError(boolean isDataExchangeWithServerSuccessful) {
        String message = "Failed to add trip";
        if (connectionStatusMap.get("Network Availability").equals("NO")) {
            showMessage("Error", "No network available", context);
        } else if (!connectionStatusMap.get("ServerURLResponse").equals("200")) {
            showMessage("Error", "Server URL" + SERVER_ADDRESS + "is unreachable", context);
        } else if (!connectionStatusMap.get("AddTrip Response").equals("200")) {
            showMessage("Error", "URL of file AddTrip.php is unreachable", context);
        } else if (!connectionStatusMap.get("MCrypt Response").equals("200")) {
            showMessage("Error", "URL of file MCrypt.php is unreachable", context);
        } else if (!isDataExchangeWithServerSuccessful) {
            showMessage("Error", "Error while exchanging data with the server", context);
        }else if (!connectionStatusMap.get("AddRecordedTrip Response").equals("200")) {
            showMessage("Error", "URL of file AddRecordedTrip.php is unreachable", context);
        }else if (!connectionStatusMap.get("FetchTripIDs Response").equals("200")) {
            showMessage("Error", "URL of file FetchTripIDs.php is unreachable", context);
        }else{
            message = "Trip is added";
        }
        return message;
    }

    private int getNextTripIdValue(int[] tripIDs) {
        int maxID = 0;
        for (int tripID : tripIDs){
            maxID = tripID>maxID? tripID : maxID;
        }
        return maxID+1;
    }

    boolean isNetworkAvailable (NetworkInfo netInfo) {
        return netInfo != null && netInfo.isConnected();
    }

    /** Establishes http connection to URL and returns an object of that connection*/
    private static HttpURLConnection getHttpUrlConnection(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
        urlc.setConnectTimeout(CONN_TIME_OUT);
        urlc.connect();
        return urlc;
    }

    /**Checks if the url of the server is accessible*/
    private  void checkServerUrlReachability()throws IOException {
        HttpURLConnection urlc = getHttpUrlConnection(SERVER_ADDRESS);
        connectionStatusMap.remove("ServerURLResponse");
        connectionStatusMap.put("ServerURLResponse", String.valueOf(urlc.getResponseCode()));
    }

    /**Checks if the php files on the server side are reachable*/
    private  void checkPhpFilesUrlsReachability()throws IOException {
        for (ServerFiles.PhpFile phpFile : ServerFiles.PhpFile.values()) {
            String phpFileName = ServerFiles.getFile(phpFile);
            HttpURLConnection urlc = getHttpUrlConnection(SERVER_ADDRESS + phpFileName);
            connectionStatusMap.remove(phpFileName.substring(0,phpFileName.length()-4) + " Response");
            connectionStatusMap.put(phpFileName.substring(0,phpFileName.length()-4) + " Response",
                    String.valueOf(urlc.getResponseCode()));
        }
    }

    /**Returns a map whose values are the server response codes when trying to access server and php urls.*/
    private  Map<String,String> connectionStatus(Context context) {
        NetworkInfo networkInfo = getNetworkInfo(context);
        if (isNetworkAvailable(networkInfo)) {
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

    NetworkInfo getNetworkInfo(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo();
    }


    /**Gets the status map elements which indicate which urls can ba reached then tries to establish
       http connection to the specified URL and returns http object of that connection */

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

    // Posts data to the server and gets its response if available
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

    private void showMessage(String title, String msg, Context context) {
        final AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(context);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setCancelable(true);
        dialogueBuilder.setPositiveButton("OK",null);
        dialogueBuilder.show();
    }
   //==================================================================================================================================

    /** Used for Login. sends email and pw and checks if there's user in DB with those credentials.*/
    void fetchUserDataInBackground(User user, AfterUserAsyncTaskDone callback){
        //    progressDialog.show();
        new FetchUserDataAsyncTask(user, callback).execute();
    }

    private class FetchUserDataAsyncTask extends AsyncTask<Void,Void,User> {
        boolean isDataExchangeWithServerSuccessful = false;
        User               user    ;
        AfterUserAsyncTaskDone callback;
        FetchUserDataAsyncTask(User user, AfterUserAsyncTaskDone callback) {
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
                isDataExchangeWithServerSuccessful = true;
            } catch (Exception e) {
                isDataExchangeWithServerSuccessful = false;
                e.printStackTrace();
                return null;
            }
            return returnedUser;
        }

        @Override
        protected void onPostExecute(User returnedUser) {
            callback.done(returnedUser);
            for(String connectedEntity : connectionStatusMap.keySet())
                Log.e(connectedEntity + " = ", connectionStatusMap.get(connectedEntity));
            showServerRelatedError(isDataExchangeWithServerSuccessful);
            super.onPostExecute(returnedUser);
        }
    }

    //==================================================================================================================================

    /** stores the user private data in the data base*/
    void storeUserDataInBackground(String purpose, User user, AfterUserAsyncTaskDone callback){
        new StoreUserDataAsynTask(purpose, user, callback).execute();
    }

    private class StoreUserDataAsynTask extends AsyncTask <Void,Void,Void> {
        boolean isDataExchangeWithServerSuccessful = false;
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
                isDataExchangeWithServerSuccessful = true;
                Email.sendEmailAfterRegistrationOrUpdate(user);
            }catch(Exception e){
                isDataExchangeWithServerSuccessful = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            callback.done(null);
            String message = showServerRelatedError(isDataExchangeWithServerSuccessful);
            if( message.equals("Trip is added") && purpose .equals("Register")){
                Intent intentRegisterLogin = new Intent(context, LoginActivity.class);
                intentRegisterLogin.putExtra("Purpose","Register");
                context.startActivity(intentRegisterLogin);

            }else if(message.equals("Trip is added") && purpose .equals("Update")){
                try {
                    RegisterActivity.localStorage.clearUserData();
                    RegisterActivity.localStorage.markAsLoggedIn(false);
                    Intent intentRegisterLogin = new Intent(context, LoginActivity.class);
                    intentRegisterLogin.putExtra("Purpose","Update");
                    context.startActivity(intentRegisterLogin);
                }catch(NullPointerException N){
                    N.printStackTrace(); // we are in change password activity
                    context.startActivity(new Intent(context, LoginActivity.class));
                }
            }

            super.onPostExecute(aVoid);
        }
    }

//=================================================================================================================================

    /**Used in log in to check if the credentials are correct*/
    void fetchUserDataByEmailOnlyInBackground(String email, AfterUserAsyncTaskDone callback){
        new FetchUserDataByEmailOnlyAsyncTask(email, callback).execute();
    }

    private class FetchUserDataByEmailOnlyAsyncTask extends AsyncTask <Void,Void,User> {
        String  email;
        AfterUserAsyncTaskDone callback;
        FetchUserDataByEmailOnlyAsyncTask(String email, AfterUserAsyncTaskDone callback) {
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
        protected void onPostExecute(User user) {
            callback.done(user);
            super.onPostExecute(user);
        }
    }

//============================================================================================================
    /** A method to download json data from url used in placesTask in AddTripActivity */
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
     */
    void addTripInBackground(Trip trip, AfterTripAddingTaskDone callback){
        addTripAsyncTask = new AddTripAsyncTask(trip, callback).execute();

        //The timer is used in case the response of the server takes too long without returning 200 (OK) or
        //something else ... So, we will skip the trip addition in this case and prompt the user to add later
        Timer timer = new Timer(); // set timeout of 10 seconds to add the trip and skip trip adding if exceeded
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if(pd.isShowing()) {
                    pd.dismiss();
                    addTripAsyncTask.cancel(true);
                    addTripAsyncTask = null;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                showMessage("Timeout!", "The trip is not added. The server is not responsive at the moment", context);
                                            }
                                        },100);
                }
            }
        },ServerProcesses.CONN_TIME_OUT);
    }

    private class AddTripAsyncTask extends AsyncTask<Trip,Void,Void> {
        boolean isDataExchangeWithServerSuccessful = false;
        Trip trip;
        AfterTripAddingTaskDone callback;
        String feedbackMessage;
        AddTripAsyncTask(Trip trip, AfterTripAddingTaskDone callback) {
            this.callback = callback;
            this.trip = trip;
        }

        @Override
        protected Void doInBackground(Trip... trips) {
            try {
                URL url = new URL(SERVER_ADDRESS + "AddTrip.php");
                Map<String,String> dataToSend = new HashMap<>();
                setTripDataToBeSent(trip , dataToSend);
                dataToSend.put("TripID",String.valueOf(trip.getTripID()));
                HttpURLConnection  http = setHttpPostRequest(url, dataToSend);
                JSONObject jObj = postAndGetJasonResponse(http);

                if (jObj!=null) {
                    cryptography.setIV(jObj.getString("IV"));
                    feedbackMessage = cryptography.decrypt(jObj.getString("status"));
                }
                isDataExchangeWithServerSuccessful = true;
            } catch (Exception e) {
                isDataExchangeWithServerSuccessful = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            String message = showServerRelatedError(isDataExchangeWithServerSuccessful);
            pd.dismiss();
            Toast.makeText(context,message,Toast.LENGTH_LONG).show();
            super.onPostExecute(aVoid);
        }
    }


    private void setTripDataToBeSent(Trip trip, Map<String,String> dataToSend) {
        try {
            User user = localStorage.getLoggedInUser();
            dataToSend.clear();
            dataToSend.put("IV", cryptography.getIV());
            dataToSend.put("UserID", cryptography.encrypt(user.getID()));
            dataToSend.put("Duration", cryptography.encrypt(trip.getDuration()));


            // for distance, if fails to get it from google, calculate the length of the direct line between 2 locations
            Scanner scanner = new Scanner(trip.getDistance() != null ? trip.getDistance() :
                    new MapsOperations().findDistanceBetween(new LatLng(trip.getStartAddress().getLatitude(), trip.getStartAddress().getLongitude()),
                            new LatLng(trip.getStopAddress().getLatitude(), trip.getStopAddress().getLongitude())));
            dataToSend.put("Distance", cryptography.encrypt(String.valueOf(scanner.nextFloat())));
            dataToSend.put("DistanceUnit", cryptography.encrypt(scanner.next()));

            if(trip.getStartAddress()!= null) {
                dataToSend.put("StartAddress", cryptography.encrypt(trip.getStartAddress().getAddressLine(0) + ", " +
                        (trip.getStartAddress().getLocality() == null ? "" : (trip.getStartAddress().getLocality() + ", ")) +
                        trip.getStartAddress().getCountryName()));
            }else {
                dataToSend.put("StartAddress", cryptography.encrypt(trip.getStartAddressString()));
            }

            if(trip.getStopAddress()!= null) {
                dataToSend.put("StopAddress", cryptography.encrypt(trip.getStopAddress().getAddressLine(0) + ", " +
                        (trip.getStopAddress().getLocality() == null ? "" : (trip.getStopAddress().getLocality() + ", ")) +
                        trip.getStopAddress().getCountryName()));
            }else {
                dataToSend.put("StopAddress", cryptography.encrypt(trip.getStopAddressString()));
            }

            //if manually added we don't need Trip.start/stopMonth variables. If recorded, we use them
            int monthStart = trip.getStartMonth()<0 ? trip.getStartCalendar().get(Calendar.MONTH):trip.getStartMonth();
            int monthStop =  trip.getStopMonth ()<0 ? trip.getStopCalendar() .get(Calendar.MONTH):trip.getStopMonth ();
            int yearStart = trip.getStartCalendar().get(Calendar.YEAR);
            int yearStop = trip.getStopCalendar().get(Calendar.YEAR);

            DecimalFormat formatter = new DecimalFormat("00");
            dataToSend.put("StartDate", cryptography.encrypt(trip.getStartCalendar().get(Calendar.DAY_OF_MONTH) +
                    //hacking of month-year error as December is skipped
                    ", " + (monthStart < 1 ? 12 : formatter.format(monthStart))+
                    ", " + (monthStart < 1 ? yearStart - 1 : yearStart)));
            dataToSend.put("StopDate", cryptography.encrypt(trip.getStopCalendar().get(Calendar.DAY_OF_MONTH) +
                    ", " + (monthStop < 1 ? 12 : formatter.format(monthStop)) +
                    ", " + (monthStop < 1 ? yearStop - 1 : yearStop)));

            dataToSend.put("StartTime", cryptography.encrypt(formatter.format(trip.getStartCalendar().get(Calendar.HOUR_OF_DAY))+
                    " : " + formatter.format(trip.getStartCalendar().get(Calendar.MINUTE))));
            dataToSend.put("StopTime", cryptography.encrypt(formatter.format(trip.getStopCalendar().get(Calendar.HOUR_OF_DAY)) +
                    " : " + formatter.format(trip.getStopCalendar().get(Calendar.MINUTE))));

            if(trip.getStartAddress()!= null) {
                dataToSend.put("StartLatitude", cryptography.encrypt(String.valueOf(trip.getStartAddress().getLatitude())));
            }else{
                dataToSend.put("StartLatitude", cryptography.encrypt(String.valueOf(trip.getStartLatLng().latitude)));
            }

            if(trip.getStartAddress()!= null) {
                dataToSend.put("StartLongitude", cryptography.encrypt(String.valueOf(trip.getStartAddress().getLongitude())));
            }else{
                dataToSend.put("StartLongitude", cryptography.encrypt(String.valueOf(trip.getStartLatLng().longitude)));
            }

            if(trip.getStopAddress()!= null) {
                dataToSend.put("StopLatitude", cryptography.encrypt(String.valueOf(trip.getStopAddress().getLatitude())));
            }else{
                dataToSend.put("StopLatitude", cryptography.encrypt(String.valueOf(trip.getStopLatLng().latitude)));
            }

            if(trip.getStopAddress()!= null) {
                dataToSend.put("StopLongitude", cryptography.encrypt(String.valueOf(trip.getStopAddress().getLongitude())));
            }else{
                dataToSend.put("StopLongitude", cryptography.encrypt(String.valueOf(trip.getStopLatLng().longitude)));
            }

            dataToSend.put("ManuallyAdded", cryptography.encrypt(String.valueOf(trip.isManuallyAdded())));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //==================================================================================================================================
    /**
     * Puts manually-added Trip in TripSummary table of the data base
     */
    void addRecordedTripInBackground(ArrayList<Trip> tripParts_List, AfterTripAddingTaskDone callback){
        //noinspection unchecked
        new AddRecordedTripAsyncTask(tripParts_List, callback).execute();
    }

    private class AddRecordedTripAsyncTask extends AsyncTask<ArrayList<Trip>,Void,Void> {
        boolean isDataExchangeWithServerSuccessful = false;
        ArrayList<Trip> tripPartsList;
        AfterTripAddingTaskDone callback;
        String feedbackMessage;

        AddRecordedTripAsyncTask(ArrayList<Trip> tripPartsList, AfterTripAddingTaskDone callback) {
            this.callback = callback;
            this.tripPartsList = tripPartsList;
        }

        @SafeVarargs
        @Override
        protected final Void doInBackground(ArrayList<Trip>... tripPartsLists) {
            try {
                URL url = new URL(SERVER_ADDRESS + "AddRecordedTrip.php");
                Map<String, String> dataToSend = new HashMap<>();
                int tripPartID = 0;
                for (Trip tripPart : tripPartsList) {
                    setTripDataToBeSent(tripPart,dataToSend);
                    dataToSend.put("TripPartID",String.valueOf(++tripPartID));
                    // this is the whole trip ID (unified for all parts)
                    dataToSend.put("TripID",String.valueOf(tripPart.getTripID()));
                    final HttpURLConnection http = setHttpPostRequest(url, dataToSend);
                    Thread th = new Thread(){
                        @Override
                        public void run() {
                            JSONObject jObj = postAndGetJasonResponse(http);
                            if (jObj != null) {
                                try {
                                    cryptography.setIV(jObj.getString("IV"));
                                    feedbackMessage = cryptography.decrypt(jObj.getString("status"));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            isDataExchangeWithServerSuccessful = true;
                        }
                    };
                    th.start();
                    th.join();
                }
            }catch(Exception e){
                isDataExchangeWithServerSuccessful = false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            callback.done();
            String message = showServerRelatedError(isDataExchangeWithServerSuccessful);
           // pd.dismiss();
            Toast.makeText(context,message,Toast.LENGTH_LONG).show();
            super.onPostExecute(aVoid);
        }
    }

    //==================================================================================================================================

    /** Used for Login. sends email and pw and checks if there's user in DB with those credentials.*/
    void fetchTripIDsInBackground(User user, AfterTripIDsFetchingTaskDone callback){
        //    progressDialog.show();
        new FetchTripIDsAsyncTask(user, callback).execute();
    }

    private class FetchTripIDsAsyncTask extends AsyncTask<User,Void,int[]> {
        boolean isDataExchangeWithServerSuccessful = false;
        User               user    ;
        AfterTripIDsFetchingTaskDone callback;
        FetchTripIDsAsyncTask(User user, AfterTripIDsFetchingTaskDone callback) {
            this.callback = callback;
            this.user  = user    ;
        }

        @Override
        protected int[] doInBackground(User... users) {
            int[] returnedTripIDs = null;
            try {
                URL url = new URL(SERVER_ADDRESS + "FetchTripIDs.php");
                Map<String, String> dataToSend = new HashMap<>();
                dataToSend.put("UserID", cryptography.encrypt(user.getID()));
                dataToSend.put("IV" ,cryptography.getIV() );
                HttpURLConnection  http = setHttpPostRequest(url, dataToSend);
                JSONObject jObj = postAndGetJasonResponse(http);

                assert jObj != null;
                //2 TripIDs are returned for summary and details tables. + IV
                if (jObj.length() == 3) {
                    cryptography.setIV(jObj.getString("IV"));
                    int maxTripDetailsID = Integer.valueOf(jObj.getString("MaxTripDetailsID"));
                    Log.e("maxTripDetailsID",maxTripDetailsID+"" );
                    int maxTripSummaryID = Integer.valueOf(jObj.getString("MaxTripSummaryID"));
                    Log.e("maxTripSummaryID",maxTripSummaryID+"" );

                    returnedTripIDs = new int[] {maxTripDetailsID, maxTripSummaryID} ;
                    isDataExchangeWithServerSuccessful = true;
                }else{
                    isDataExchangeWithServerSuccessful = false;
                }
            } catch (Exception e) {
                isDataExchangeWithServerSuccessful = false;
                e.printStackTrace();
                return new int[]{-1,-1};
            }
            return returnedTripIDs;
        }

        @Override
        protected void onPostExecute(int[] returnedTripIDs) {
            callback.done(getNextTripIdValue(returnedTripIDs));
            for(String connectedEntity : connectionStatusMap.keySet())
                Log.e(connectedEntity + " = ", connectionStatusMap.get(connectedEntity));
            //showServerRelatedError(isDataExchangeWithServerSuccessful);
            super.onPostExecute(returnedTripIDs);
        }
    }

    //==================================================================================================================================

}