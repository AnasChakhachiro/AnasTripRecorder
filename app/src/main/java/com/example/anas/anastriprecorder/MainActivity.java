package com.example.anas.anastriprecorder;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    //private static Context contextOfApplication;
    LocalStorage userLocalStore;
    GoogleMap mGoogleMap;
    Spinner mMapStyleSpinner;
    ArrayAdapter<CharSequence> mapStyleSpinnerArrayAdapter;

    Button bRecordOrPause, bStopRecording;
    RecordingTrip recordingTripClassObject;
    final int START = 1 , PAUSE = 2 , STOP = 3;
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
        //contextOfApplication = getApplicationContext();
        if (isGooglePlayAvailable()) {
            Log.e("GooglePlayServices", "available");
            setContentView(R.layout.activity_main);
            initMap();
            setupMapStyleSpinner(R.id.mapStyleSpinner);
            bRecordOrPause = (Button) findViewById(R.id.bRecordOrPause);
            bStopRecording = (Button) findViewById(R.id.bStop);
            bStopRecording.setEnabled(false);
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.e("location ","Latitude = "+ location.getLatitude() + " | Longitude=" + location.getLongitude());
                    addMarkerAtCurrentLocation(location,17);

                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                }
            };
            setClickListenerForButtons();
        }

    }

    private void addMarkerAtCurrentLocation(Location location, int zoom) {
        MapsOperations mapsOperations= new MapsOperations();
        mapsOperations.goToLocation(mGoogleMap,new LatLng(location.getLatitude(),location.getLongitude()),
                zoom,MapsOperations.markerMain,"Your Location","");
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
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    switch (bRecordOrPause.getText().toString()){
                        case ("►"):
                            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                showGpsOffAlertMessage(MainActivity.this);
                            }else {
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



    private class RecordingTrip {
        Button mButton;
        RecordingTrip(Button button) {
            this.mButton = button;
        }

        Handler handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message message) {
                switch (message.arg1) {
                    case(START):
                        // TODO check the permissions
                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }

                        // if GPS off ask to turn it on
                        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                            showGpsOffAlertMessage(MainActivity.this);
                        }else {
                            locationManager.requestLocationUpdates("gps", 4000, 0, locationListener);
                        }
                        break;
                    case (STOP):
                        locationManager.removeUpdates(locationListener);
                        //TODO add function for final save
                        bRecordOrPause.setText("►");
                        bRecordOrPause.setTextSize(24);
                        bRecordOrPause.setBackgroundResource(R.drawable.play_button_square);
                        bStopRecording.setEnabled(false);
                        break;
                    case (PAUSE):
                        locationManager.removeUpdates(locationListener);
                        //TODO add function for temporary save
                        break;
                }
            }
        };

        void configureStartPauseStopButtons(){
            Message message = new Message();
            switch (this.mButton.getId()){
                case (R.id.bRecordOrPause):
                    if(bRecordOrPause.getText().toString().equals(" ▌▌")) { // the button is yellow now dut to onTouchListener
                        message.arg1 = START;
                        message.setTarget(handler);
                        message.sendToTarget();
                    }else if(bRecordOrPause.getText().toString().equals("►")) {
                        message.arg1 = PAUSE;
                        message.setTarget(handler);
                        message.sendToTarget();
                    }
                    break;
                case(R.id.bStop):
                    message.arg1=STOP;
                    message.setTarget(handler);
                    message.sendToTarget();
                    break;
            }
        }
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
