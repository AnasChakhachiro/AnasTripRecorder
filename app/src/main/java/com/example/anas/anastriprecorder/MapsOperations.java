package com.example.anas.anastriprecorder;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;
import android.widget.Toast;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class MapsOperations  {

    static Marker markerStart, markerStop , markerMain;


    List<LatLng> locationString2Coordinates(Context context, String locationName, int resultsNumber){
        Geocoder geoCoder = new Geocoder(context);
        List<LatLng>  coordinatesList = new ArrayList<>();
        List<Address> addressesList;
        try {
            addressesList =  geoCoder.getFromLocationName(locationName , resultsNumber);
            for(Address address : addressesList){
                Log.e("address" , address.getLocality() + " - " +address.getLongitude());
                coordinatesList.add(new LatLng(address.getLatitude(),address.getLongitude()));
                Log.e("error" , address.getLatitude() + " , " + address.getLongitude() + " is added");
            }
        }catch(Exception e) {
            try{
                Toast.makeText(context, "Only " + coordinatesList.size() + " locations were found", Toast.LENGTH_LONG).show();
            }catch(Exception N){
                Toast.makeText(context, "No locations were found", Toast.LENGTH_LONG).show();
            }
        }
        return coordinatesList;
    }



    void  goToLocation(GoogleMap gm, LatLng latLng, int zoom, Marker marker, String markerTitle, String markerSnippet) {

        if(marker == MapsOperations.markerStart) {
            if (MapsOperations.markerStart != null)
                MapsOperations.markerStart.remove();
            MapsOperations.markerStart = gm.addMarker(new MarkerOptions()
                                          .position(latLng)
                                          .title(markerTitle)
                                          .snippet(markerSnippet));
        }else if(marker == MapsOperations.markerStop) {
            if (MapsOperations.markerStop!= null)
                MapsOperations.markerStop.remove();
            MapsOperations.markerStop = gm.addMarker(new MarkerOptions()
                                          .position(latLng)
                                          .title(markerTitle)
                                          .snippet(markerSnippet));
        }else if(marker == MapsOperations.markerMain) {
            if (MapsOperations.markerMain != null)
                MapsOperations.markerMain.remove();
            MapsOperations.markerMain = gm.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(markerTitle)
                    .snippet(markerSnippet));
        }
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
        gm.animateCamera(cameraUpdate);
    }


    /** This requires internet connection to get the address of the given LatLng coordinates*/
    List<Address> latLng2AddressesList(Context context, double latitude, double longitude, int resultsNumber){
        Geocoder geoCoder = new Geocoder(context);
        List<Address> addressesList = null;
        try {
            addressesList =  geoCoder.getFromLocation(latitude,longitude,resultsNumber);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                Toast.makeText(context, "Only " + addressesList.size() +
                        " registered addresses were found", Toast.LENGTH_LONG).show();
            }catch (NullPointerException N){
                Toast.makeText(context, "No registered addresses were found",
                        Toast.LENGTH_LONG).show();
            }
        }
        return addressesList;
    }

}
