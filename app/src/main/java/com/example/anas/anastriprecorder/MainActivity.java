package com.example.anas.anastriprecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    GoogleApiClient googleApiClient;
    LocalStorage userLocalStore;
    GoogleMap mGoogleMap;
    Spinner mMapStyleSpinner;
    ArrayAdapter<CharSequence> mapStyleSpinnerArrayAdapter;
    ArrayList<Location> recordedLocationsList;
    ArrayList<LatLng> recordedLatLngList;
    ArrayList<Trip> wholeTripPartsList;
    ArrayList<Polyline> subPathsPlotsList;
    ArrayList<ArrayList<Location>> wholeTripLocationsList;
    Button bRecordOrPause, bStopRecording;
    RecordingTrip recordingTripClassObject;
    final int START = 1, PAUSE = 2, STOP = 3;
    LocationManager locationManager;
    LocationListener locationListener;
    Polyline subPathPlot;
    Calendar startDate,stopDate;

    protected void onStart() {
        super.onStart();
        userLocalStore = new LocalStorage(this);
        if (!userLocalStore.isLoggedIn())
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (isGooglePlayAvailable()) {
            Log.e("GooglePlayServices", "available");
            setContentView(R.layout.activity_main);
            initMap();
            setupMapStyleSpinner(R.id.mapStyleSpinner);
            bRecordOrPause = (Button) findViewById(R.id.bRecordOrPause);
            bStopRecording = (Button) findViewById(R.id.bStop);
            bStopRecording.setEnabled(false);
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            recordedLocationsList = new ArrayList<>();
            wholeTripLocationsList = new ArrayList<>();
            subPathsPlotsList = new ArrayList<>();
            wholeTripPartsList = new ArrayList<>();
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        Log.e("location ", "Latitude = " + location.getLatitude() + " | Longitude=" + location.getLongitude());
                        addMarkerAtCurrentLocation(location, 17);
                        storeCurrentLocationInList(location);
                        recordedLatLngList = getLatLngListOfLocationsList(recordedLocationsList);
                        if(recordedLatLngList.size()>1) {
                            subPathPlot = drawCurrentSubPath(mGoogleMap, recordedLatLngList,Color.YELLOW,5);
                        }
                    }
                }
            };
            setClickListenerForButtons();
        }

    }

    private Polyline drawCurrentSubPath(GoogleMap map, List<LatLng> latLngList , int colorCode, int width ){
            if (subPathPlot != null)
                subPathPlot.remove();
            return map.addPolyline(new PolylineOptions()
                    .addAll(latLngList)
                    .width(width)
                    .color(colorCode));
    }

    private void drawPreviousSubPathsIfAvailable(GoogleMap map , int colorCode, int width ){
        if(subPathsPlotsList != null && subPathsPlotsList.size()>0)
            for(Polyline subPathPlot : subPathsPlotsList)
                map.addPolyline(new PolylineOptions()
                        .addAll(subPathPlot.getPoints())
                        .width(width)
                        .color(colorCode));

    }


    private void storeCurrentLocationInList(Location location) {
        recordedLocationsList.add(location);
    }

    ArrayList<LatLng> getLatLngListOfLocationsList(List<Location> locationList){
        ArrayList<LatLng> latLngList = new ArrayList<>();
        for (Location location: locationList){
            latLngList.add(new LatLng(location.getLatitude(),location.getLongitude()));
        }
        return  latLngList;
    }

    private void addMarkerAtCurrentLocation(Location location, int zoom) {
        MapsOperations mapsOperations = new MapsOperations();
        mapsOperations.goToLocation(mGoogleMap, new LatLng(location.getLatitude(), location.getLongitude()),
                zoom, MapsOperations.markerMain, "Your Location", "");
    }

    private void setClickListenerForButtons() {
        final Button bChangeAccountData = (Button) findViewById(R.id.bUpdateUser);
        bChangeAccountData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.openUpdateScreen();
            }
        });


        final Button bAddTrip = (Button) findViewById(R.id.bAddTrip);
        bAddTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), AddTripActivity.class));
            }
        });

        bRecordOrPause.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    int messageCode = -1;
                    switch (bRecordOrPause.getText().toString()) {
                        case ("►"):
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                showGpsOffAlertMessage(MainActivity.this);
                            } else {
                                bRecordOrPause.setBackgroundResource(R.drawable.pause_button_square);
                                bRecordOrPause.setText(" ▌▌");
                                bRecordOrPause.setTextSize(14);
                                bStopRecording.setEnabled(true);
                                messageCode = START;
                            }
                            break;
                        case (" ▌▌"):
                            bRecordOrPause.setBackgroundResource(R.drawable.play_button_square);
                            bRecordOrPause.setText("►");
                            bRecordOrPause.setTextSize(24);
                            bStopRecording.setEnabled(true);
                            messageCode = PAUSE;
                    }
                    recordingTripClassObject =  new RecordingTrip(bRecordOrPause);
                    recordingTripClassObject.StartPauseStopRecording(messageCode);
                }
                return false;
            }
        });

        bStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordingTripClassObject = new RecordingTrip(bStopRecording);
                recordingTripClassObject.StartPauseStopRecording(STOP);
            }
        });

    }

    LocationRequest locationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startDate = Calendar.getInstance();
        drawPreviousSubPathsIfAvailable(mGoogleMap,Color.YELLOW,5);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {
        //ToDo
        if(MapsOperations.markerMain!= null)
            MapsOperations.markerMain.remove();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    private void addTripPartToWholeTripPartsList(){
        MapsOperations mapsOperations = new MapsOperations();
        Address startAddress,stopAddress;
        Trip tripPart;
        try{
            startAddress = mapsOperations.latLng2AddressesList(getBaseContext(),
                recordedLatLngList.get(0).latitude , recordedLatLngList.get(0).longitude, 1).get(0);
            Log.e("start Address", startAddress.getLatitude() + " " + startAddress.getLongitude());
            stopAddress = mapsOperations.latLng2AddressesList(getBaseContext(),
                    recordedLatLngList.get(recordedLocationsList.size()-1).latitude ,
                    recordedLatLngList.get(recordedLocationsList.size()-1).longitude, 1).get(0);
            Log.e("stop Address", stopAddress.getLatitude() + " " + stopAddress.getLongitude());
            tripPart = new Trip(startAddress,stopAddress,startDate,stopDate,false);

        }catch(Exception e){ // No internet service at the moment this function is called
            tripPart = new Trip("Unknown Address","Unknown Address", recordedLatLngList.get(0),
                                recordedLatLngList.get(recordedLocationsList.size()-1),
                                startDate,stopDate,false);
        }
        wholeTripPartsList.add(tripPart);
        startDate = null;
        stopDate = null;

    }


    private class RecordingTrip {
        Button mButton;

        RecordingTrip(Button button) {
            this.mButton = button;
        }

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.arg1) {
                    case (START):
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        // if GPS off ask to turn it on
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showGpsOffAlertMessage(MainActivity.this);
                        } else {
                            startDate = Calendar.getInstance();
                            googleApiClient.connect();
                        }
                        break;
                    case (STOP):
                        showMessage("Stop Recording", "Do you sure want to:" +
                                "\nstop and ignore recorded trip," +
                                "\nstop and save recorded trip," +
                                "\nor keep recording the trip?");
                        break;
                    case (PAUSE):
                        googleApiClient.disconnect();
                        mGoogleMap.clear();
                        stopDate =Calendar.getInstance();
                        /** we save the recorded location so far in list for tripPart
                         then we add this tripPart_locations_list into (list of lists)
                         which represents the whole trip divided into parts
                         */
                        addTripPartToWholeTripPartsList();

                        //call these together
                        addPartLocationsListToWholeTripPartsListAndClearLists();
                        subPathsPlotsList.add(subPathPlot);

                        drawPreviousSubPathsIfAvailable(mGoogleMap,Color.YELLOW,5);
                        Log.e("wholeTripPartsSize", wholeTripPartsList.size()+"");
                        Log.e("wholeTripLocationsPause", wholeTripLocationsList.size()+"");
                        break;
                }
            }
        };



        private void addPartLocationsListToWholeTripPartsListAndClearLists() {
            if (recordedLocationsList.size() > 0) {
                wholeTripLocationsList.add(recordedLocationsList);
                recordedLocationsList.clear();
                recordedLatLngList.clear();
            }
        }


        void StartPauseStopRecording(int messageCode) {
            Message message = new Message();
            if (messageCode == START || messageCode == PAUSE ||messageCode == STOP) {
                message.arg1 = messageCode;
                message.setTarget(handler);
                message.sendToTarget();
            } else{
                Log.e("ERROR","StartPauseStopRecording was fed by error message code");
            }
        }
    }


    public void showMessage(String title, String msg) {
        AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(this);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setPositiveButton("Stop/Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                googleApiClient.disconnect();
                makeRecordPauseButtonGreen();
                if (recordedLocationsList.size() > 0) {
                    // Takes effect only when stop pressed while playing .. The size will be 0 if stop pressed while pausing
                    stopDate = Calendar.getInstance();
                    wholeTripLocationsList.add(recordedLocationsList);
                    recordedLatLngList = getLatLngListOfLocationsList(recordedLocationsList);
                    addTripPartToWholeTripPartsList();
                }
                Log.e("last LocationsList size", recordedLocationsList.size()+"");
                Log.e("wholeTripPartsSize 344", wholeTripPartsList.size()+"");
                Log.e("wholeLocationListSize", wholeTripLocationsList.size()+"");

                ServerProcesses serverProcesses = new ServerProcesses(MainActivity.this);
                serverProcesses.addRecordedTripInBackground(wholeTripPartsList, new AfterTripTaskDone() {
                    @Override
                    public void done() {
                        //Todo  make sure it is correct to put them here
                        cleanAllLists();
                        mGoogleMap.clear();
                    }
                });

            }
        });
        dialogueBuilder.setNeutralButton("Stop/Discard", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    //  Consider calling
                    return;
                }
                //locationManager.removeUpdates(locationListener);
                googleApiClient.disconnect();
                stopDate = null;
                startDate = null;
                makeRecordPauseButtonGreen();
                if(recordedLocationsList != null) {
                    wholeTripLocationsList.clear();
                    wholeTripPartsList    .clear();
                    recordedLocationsList .clear();
                    recordedLatLngList    .clear();
                    if(subPathsPlotsList!=null)
                         subPathsPlotsList.clear();
                }
                mGoogleMap.clear();

            }
        });
        dialogueBuilder.setNegativeButton("Keep recording", null);

        dialogueBuilder.show();
    }

    private void cleanAllLists() {
        Log.e("onPost Clearing", "Applied");
        recordedLocationsList.clear();
        recordedLatLngList   .clear();
        if(subPathsPlotsList!=null)
            subPathsPlotsList.clear();
        wholeTripPartsList.clear();
        wholeTripPartsList.clear();
    }

    private void makeRecordPauseButtonGreen() {
        bRecordOrPause.setText("►");
        bRecordOrPause.setTextSize(24);
        bRecordOrPause.setBackgroundResource(R.drawable.play_button_square);
        bStopRecording.setEnabled(false);
    }

    /**used to show dialog asking user to notify user about missing GPS*/
    private void showGpsOffAlertMessage(Context context) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage("This service uses GPS. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog,
                                        @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        bRecordOrPause.setEnabled(true);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isGooglePlayAvailable() {
        Dialog dialog;
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int availabilityOutput = api.isGooglePlayServicesAvailable(this);
        if (availabilityOutput == ConnectionResult.SUCCESS) // google play is available
            return true;
        else if (api.isUserResolvableError(availabilityOutput)) { // googleplay not available but can be downloaded
            dialog = api.getErrorDialog(this, availabilityOutput, 0);
            dialog.show();
        } else
            Toast.makeText(this, "cannot connect to google play services", Toast.LENGTH_LONG).show();
        return false;
    }



    private void initMap() {
        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // the permissions are embedded in the manifest no need for check
            return;
        }

        googleApiClient = new GoogleApiClient.Builder(this)
                        .addApi(LocationServices.API).addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this).build();

        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng position) {
                Toast.makeText(getApplicationContext(),position.latitude+" : "+position.longitude,Toast.LENGTH_SHORT).show();
            }
        });
    }



    public void logout(View view){
        final Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        userLocalStore.clearUserData();
        userLocalStore.markAsLoggedIn(false);
        startActivity(intent);
    }

    // prepares update activity layout
    public void openUpdateScreen(){
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("Purpose", "Update");
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {}

    // manages the spinner from which we can choose the Main activity map style
    public void setupMapStyleSpinner(int mapStyleSpinnerID) {
        mMapStyleSpinner = (Spinner)findViewById(mapStyleSpinnerID);
        mapStyleSpinnerArrayAdapter = ArrayAdapter.createFromResource(this, R.array.mapStyleOptions,android.R.layout.simple_spinner_item);
        mapStyleSpinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMapStyleSpinner.setAdapter(mapStyleSpinnerArrayAdapter);
        mMapStyleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position , long id) {
                try {
                    mGoogleMap.setMapType(position);
                }catch(Exception e){
                    Log.e("error on item select","Map is not ready yet");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }
}
