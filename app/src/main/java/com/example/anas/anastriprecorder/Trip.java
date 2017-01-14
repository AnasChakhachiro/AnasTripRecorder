package com.example.anas.anastriprecorder;

import android.location.Address;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class Trip {
    private Address  mStartAddress , mStopAddress;
    private String mStartAddressString = "" ,mStopAddressString = "";
    private Calendar mStartCalendar,mStopCalendar;
    private String distance = "0 m", duration = "0 sec";
    private LatLng mStartLatLng,mStopLatLng;
    private boolean mIsManuallyAdded;
    private int startMonth = -1 , stopMonth = -1;
    private int tripID;

    Trip(Address startAddress, Address stopAddress, Calendar startCalendar, Calendar stopCalendar, boolean isManuallyAdded){
        this.mStartCalendar=startCalendar;
        this.mStopCalendar = stopCalendar;
        this.mStartAddress = startAddress;
        this.mStopAddress = stopAddress;
        this.mIsManuallyAdded = isManuallyAdded;
        Map<TimeUnit, Long> timeDifference = computeDuration(startCalendar.getTime(), stopCalendar.getTime());
        this.duration = timeDifference.get(TimeUnit.DAYS) + " DAYS\n" +
                timeDifference.get(TimeUnit.HOURS) + " HOURS\n" +
                timeDifference.get(TimeUnit.MINUTES) + " MINUTES";
        this.distance = getDistanceFromGoogle(startAddress.getLatitude(),
                                              startAddress.getLongitude(),
                                              stopAddress.getLatitude(),
                                              stopAddress.getLongitude());
        if(!isManuallyAdded) { // needed to unify the way of hacking December bug in addTrip and Main activities
            this.startMonth = mStartCalendar.get(Calendar.MONTH) + 1;
            this.stopMonth = mStopCalendar.get(Calendar.MONTH) + 1;
        }
    }

    //this constructor used when no internet service
    //we need the month here to unify the way of hacking the December bug in adding manually and recording
    Trip(String startAddress, String stopAddress, LatLng startLatLng, LatLng stopLatLng, Calendar startDate, Calendar stopDate, boolean isManuallyAdded) {
        this.mStartCalendar=startDate;
        this.mStopCalendar = stopDate;
        this.mStartAddressString = startAddress;
        this.mStopAddressString  = stopAddress ;
        this.mIsManuallyAdded = isManuallyAdded;
        this.mStartLatLng = startLatLng;
        this.mStopLatLng  = stopLatLng ;
        Map<TimeUnit, Long> timeDifference = computeDuration(startDate.getTime(), stopDate.getTime());
        this.duration = timeDifference.get(TimeUnit.DAYS)    + " DAYS\n" +
                        timeDifference.get(TimeUnit.HOURS)   + " HOURS\n"+
                        timeDifference.get(TimeUnit.MINUTES) + " MINUTES";
        this.distance = new MapsOperations().findDistanceBetween(startLatLng, stopLatLng);
        if(!isManuallyAdded) {// needed to unify the way of hacking December bug in addTrip and Main activities
            this.startMonth = mStartCalendar.get(Calendar.MONTH) + 1;
            this.stopMonth  = mStopCalendar.get(Calendar.MONTH)  + 1;
        }
    }

    private static Map<TimeUnit,Long> computeDuration(Date date1, Date date2) {
        long diffInMillis = date2.getTime() - date1.getTime();
        List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);
        Map<TimeUnit,Long> result = new LinkedHashMap<>();
        long millisRest = diffInMillis;
        for ( TimeUnit unit : units ) {
            long diff = unit.convert(millisRest,TimeUnit.MILLISECONDS);
            long diffInMillisForUnit = unit.toMillis(diff);
            millisRest = millisRest - diffInMillisForUnit;
            result.put(unit,diff);
        }
        return result;
    }

    private String parsedDistance;
    private String getDistanceFromGoogle(final double lat1, final double lon1, final double lat2, final double lon2) {

        Thread thread=new Thread(new Runnable() {
            @Override
                public void run() {
                    try {
                        String response;
                        URL url = new URL("http://maps.googleapis.com/maps/api/directions/json?origin=" + lat1 + "," + lon1
                                + "&destination=" + lat2 + "," + lon2 + "&sensor=false&units=metric&mode=driving");
                        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        response = org.apache.commons.io.IOUtils.toString(in, "UTF-8");

                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray array = jsonObject.getJSONArray("routes");
                        JSONObject routes = array.getJSONObject(0);
                        JSONArray legs = routes.getJSONArray("legs");
                        JSONObject steps = legs.getJSONObject(0);
                        JSONObject distance = steps.getJSONObject("distance");
                        parsedDistance=distance.getString("text");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        return parsedDistance;
    }

    String getDistance(){
        return this.distance;
    }
    String getDuration(){
        return this.duration;
    }

    Address getStartAddress(){
        return this.mStartAddress;
    }
    String getStartAddressString(){
        return this.mStartAddressString;
    }


    Address getStopAddress(){return this.mStopAddress;}
    String getStopAddressString(){return this.mStopAddressString;}

    LatLng getStartLatLng(){return this.mStartLatLng;}
    LatLng getStopLatLng() {return this.mStopLatLng;}

    int getStartMonth(){return  this.startMonth;}
    int getStopMonth(){return  this.stopMonth;}


    Calendar getStartCalendar(){
        return this.mStartCalendar;
    }

    Calendar getStopCalendar(){
        return this.mStopCalendar;
    }

    boolean isManuallyAdded() {return this.mIsManuallyAdded;}

    void setTripID(int tripID) {
        this.tripID = tripID;
    }

    int getTripID() {
        return tripID;
    }
}

