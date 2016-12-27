package com.example.anas.anastriprecorder;

import android.location.Address;
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
    private Calendar mStartCalendar,mStopCalendar;
    private String distance = "0 m", duration = "0 sec";

    Trip(Address startAddress, Address stopAddress, Calendar startCalendar, Calendar stopCalendar){
        this.mStartCalendar=startCalendar;
        this.mStopCalendar = stopCalendar;
        this.mStartAddress = startAddress;
        this.mStopAddress = stopAddress;
        Map<TimeUnit, Long> timeDifference = computeDuration(startCalendar.getTime(), stopCalendar.getTime());
        this.duration = timeDifference.get(TimeUnit.DAYS) + " DAYS\n" +
                timeDifference.get(TimeUnit.HOURS) + " HOURS\n" +
                timeDifference.get(TimeUnit.MINUTES) + " MINUTES";
        this.distance = getDistanceFromGoogle(MapsOperations.markerStrt.getPosition().latitude ,
                                              MapsOperations.markerStrt.getPosition().longitude,
                                              MapsOperations.markerStop.getPosition().latitude ,
                                              MapsOperations.markerStop.getPosition().longitude);
    }

    private static Map<TimeUnit,Long> computeDuration(Date date1, Date date2) {
        long diffInMillies = date2.getTime() - date1.getTime();
        List<TimeUnit> units = new ArrayList<>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);
        Map<TimeUnit,Long> result = new LinkedHashMap<>();
        long millisRest = diffInMillies;
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
        /*isDistanceTaskFinished = false;
        DistanceTask distanceTask = new DistanceTask();
        distanceTask.execute(MapsOperations.markerStrt.getPosition(), MapsOperations.markerStop.getPosition());
        */
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
        return distance;
    }

    String getDuration(){
        return this.duration;
    }

    Address getStartAddress(){
        return mStartAddress;
    }

    Address getStopAddress(){
        return mStopAddress;
    }

    Calendar getStartCalendar(){
        return mStartCalendar;
    }

    Calendar getStopCalendar(){
        return mStopCalendar;
    }
}

