package com.example.anas.anastriprecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.util.ArrayList;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    GoogleApiClient googleApiClient;
    LocalStorage userLocalStore;
    GoogleMap mGoogleMap;
    Spinner mMapStyleSpinner;
    ArrayAdapter<CharSequence> mapStyleSpinnerArrayAdapter;
    ArrayList<Location> recordedLocationsList;
    ArrayList<ArrayList<Location>> wholeTripLocationsList;
    Button bRecordOrPause, bStopRecording;
    RecordingTrip recordingTripClassObject;
    final int START = 1, PAUSE = 2, STOP = 3;
    LocationManager locationManager;
    LocationListener locationListener;

    protected void onStart() {
        super.onStart();
        userLocalStore = new LocalStorage(this);
        if (!userLocalStore.isLoggedIn())
            startActivity(new Intent(MainActivity.this, Login.class));
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
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    if (location != null) {
                        Log.e("location ", "Latitude = " + location.getLatitude() + " | Longitude=" + location.getLongitude());
                        addMarkerAtCurrentLocation(location, 17);
                        storeCurrentLocationInList(location);
                    }
                }
            };
            setClickListenerForButtons();
        }

    }

    private void storeCurrentLocationInList(Location location) {
        recordedLocationsList.add(location);
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
                startActivity(new Intent(getBaseContext(), AddTrip.class));
            }
        });

        bRecordOrPause.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    switch (bRecordOrPause.getText().toString()) {
                        case ("►"):
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                showGpsOffAlertMessage(MainActivity.this);
                            } else {
                                bRecordOrPause.setBackgroundResource(R.drawable.pause_button_square);
                                bRecordOrPause.setText(" ▌▌");
                                bRecordOrPause.setTextSize(14);
                                bStopRecording.setEnabled(true);
                            }
                            break;
                        case (" ▌▌"):
                            bRecordOrPause.setBackgroundResource(R.drawable.play_button_square);
                            bRecordOrPause.setText("►");
                            bRecordOrPause.setTextSize(24);
                            bStopRecording.setEnabled(true);
                    }
                    recordingTripClassObject = new RecordingTrip(bRecordOrPause);
                    recordingTripClassObject.configureStartPauseStopButtons();
                }
                return false;
            }
        });

        bStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordingTripClassObject = new RecordingTrip(bStopRecording);
                recordingTripClassObject.configureStartPauseStopButtons();
            }
        });

    }

    LocationRequest locationRequest;

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
                            googleApiClient.connect();
                            //locationManager.requestLocationUpdates("gps", 10000, 0, locationListener);
                        }
                        break;
                    case (STOP):
                        showErrorMsg("Stop Recording", "Do you sure want to:" +
                                "\nstop and ignore recorded trip," +
                                "\nstop and save recorded trip," +
                                "\nor keep recording the trip?");
                        // add function for final save

                        break;
                    case (PAUSE):
                        //locationManager.removeUpdates(locationListener);
                        googleApiClient.disconnect();
                        /** we save the recorded location so far in list for tripPart
                         then we add this tripPart_locations_list into (list of lists)
                         which represents the whole trip divided into parts
                         */
                        addPartLocationsListTowholeTripPartsList();
                        // add function for temporary save
                        break;
                }
            }
        };

        private void addPartLocationsListTowholeTripPartsList() {
            if (recordedLocationsList.size() > 0) {
                ArrayList<Location> tripSegment = recordedLocationsList;
                wholeTripLocationsList.add(tripSegment);
                recordedLocationsList.clear();
            }
        }


        void configureStartPauseStopButtons() {
            Message message = new Message();
            switch (this.mButton.getId()) {
                case (R.id.bRecordOrPause):
                    if (bRecordOrPause.getText().toString().equals(" ▌▌")) { // the button is yellow now due to onTouchListener
                        message.arg1 = START;
                        message.setTarget(handler);
                        message.sendToTarget();
                    } else if (bRecordOrPause.getText().toString().equals("►")) {
                        message.arg1 = PAUSE;
                        message.setTarget(handler);
                        message.sendToTarget();
                    }
                    break;
                case (R.id.bStop):
                    message.arg1 = STOP;
                    message.setTarget(handler);
                    message.sendToTarget();
                    break;
            }
        }
    }


    public void showErrorMsg(String title, String msg) {
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
                //locationManager.removeUpdates(locationListener);
                googleApiClient.disconnect();
                makeRecordPauseButtonGreen();
                Log.e("ddd", recordedLocationsList.size()+"");
                if (recordedLocationsList.size() > 0) {
                    // Takes effect only when stop pressed while playing .. The size will be 0 if stop pressed while pausing
                    wholeTripLocationsList.add(recordedLocationsList);
                    recordedLocationsList.clear();
                    Log.e("265 Main","cleared");
                    // function asynctask to add trip in records
                }
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
                makeRecordPauseButtonGreen();
                wholeTripLocationsList.clear();
                recordedLocationsList.clear();
                if (MapsOperations.markerMain!=null)
                    MapsOperations.markerMain.remove();
            }
        });
        dialogueBuilder.setNegativeButton("Keep recording", null);

        dialogueBuilder.show();
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
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        final Intent intent = new Intent(MainActivity.this, Login.class);
        userLocalStore.clearUserData();
        userLocalStore.markAsLoggedIn(false);
        startActivity(intent);
    }

    // prepares update activity layout
    public void openUpdateScreen(){
        startActivity(new Intent(this, Register.class));
        Log.e("debugging",userLocalStore.getLoggedInUser().getEmail());
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Register.bRegister      .setActivated(true);
                Register.bRegister      .setText("Update Data");
                Register.etAddedName    .setText(userLocalStore.getLoggedInUser().getName()         );
                Register.etAddedEmail   .setText(userLocalStore.getLoggedInUser().getEmail()        );
                Register.etRecoveryEmail.setText(userLocalStore.getLoggedInUser().getRecoveryEmail());
                Register.etAddedPassword.setText(userLocalStore.getLoggedInUser().getPassword()     );
                Register.etConfirmedPassword.setText("");
                Register.etConfirmedPassword.setVisibility(View.VISIBLE);
            }
        }, 100);
    }

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
