package com.example.anas.anastriprecorder;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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


public class MainActivity extends AppCompatActivity  implements OnMapReadyCallback {
    private static Context contextOfApplication;
    LocalStorage userLocalStore;
    GoogleMap mGoogleMap;
    Spinner mMapStyleSpinner;
    ArrayAdapter<CharSequence> mapStyleSpinnerArrayAdapter;


    protected void onStart() {
        super.onStart();
        userLocalStore = new LocalStorage();
        if (!userLocalStore.isLoggedIn())
            startActivity(new Intent(MainActivity.this, Login.class));
    }

    public static Context getContext(){
        return contextOfApplication;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        contextOfApplication = getApplicationContext();
        if (isGooglePlayAvailable()) {
            Log.e("GooglePlayServices", "available");
            setContentView(R.layout.activity_main);
            initMap();
            setupMapStyleSpinner(R.id.mapStyleSpinner);
            setClickListenerForButtons();
        }
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
