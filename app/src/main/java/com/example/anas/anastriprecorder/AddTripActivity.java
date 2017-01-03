package com.example.anas.anastriprecorder;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;



public class AddTripActivity extends FragmentActivity implements TimePickerDialog.OnTimeSetListener {
    private static final String BROWSER_KEY = "myBrowserAPIKey";
    // layout variables
    Button bNext, bPrevious, bAddTrip;
    LinearLayout startPartLayout, stopPartLayout;
    HorizontalScrollView hsv;
    EditText etStrtLat, etStopLat, etStrtLon, etStopLon;
    TextView tvStrtDate, tvStopDate, tvStrtTime, tvStopTime;
    AutoCompleteTextView autoCompleteTvStrt, autoCompleteTvStop;
    ImageView transparentStrtImageView, transparentStopImageView;
    RadioButton rbStringLocationStrt, rbLatLonLocationStrt, rbMapTapLocationStrt;
    RadioButton rbStringLocationStop, rbLatLonLocationStop, rbMapTapLocationStop;
    MapFragment strtMapFragment, stopMapFragment;
    static final int DARK_COLOR = 17170447;

    //Activity general variables
    static ProgressDialog pd;
    //google auto-complete-related variables
    PlacesTask placesTask;
    placeParserTask placeParserTask;

    //Time Data-related variables
    static final String START_TIME_PICKER = "start_time_picker";
    static final String STOP_TIME_PICKER  = "stop_time_picker";
    static int mHourStrt, mMinuteStrt, mHourStop, mMinuteStop;

    // Maps and locations-related variables
    static GoogleMap mGoogleMapStrt, mGoogleMapStop;
    static Address tmpMapStrtAddress, tmpMapStopAddress,
                   tmpLatLonStrtAddress, tmpLatLonStopAddress,
                   tmpAutoCompStrtAddress, tmpAutoCompStopAddress;
    static final String START_LOCATION = "Start Location";
    static final String STOP_LOCATION  = "Stop Location";
    static final int DEFAULT_ZOOM = 15;

    // Date data -related variables
    static DateSettings dateSettings;
    static final String START_DATE_PICKER = "start_date_picker";
    static final String STOP_DATE_PICKER = "stop_date_picker";
    static final List<String> months = new ArrayList<>();

    static {
        months.add("Jan"); months.add("Feb");months.add("Mar");months.add("Apr");
        months.add("May"); months.add("Jun");months.add("Jul");months.add("Aug");
        months.add("Sep"); months.add("Oct");months.add("Nov");months.add("Dec");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addtrip);
        dateSettings = new DateSettings();
        //context = getBaseContext();

        startPartLayout = (LinearLayout) findViewById(R.id.L1);
        stopPartLayout = (LinearLayout) findViewById(R.id.L2);
        etStrtLat = (EditText) findViewById(R.id.etLatitudeStart);
        etStrtLon = (EditText) findViewById(R.id.etLongitudeStart);
        etStopLat = (EditText) findViewById(R.id.etLatitudeStop);
        etStopLon = (EditText) findViewById(R.id.etLongitudeStop);
        tvStrtDate = (TextView) findViewById(R.id.tvStartDate);
        tvStopDate = (TextView) findViewById(R.id.tvStopDate);
        tvStrtTime = (TextView) findViewById(R.id.tvStartTime);
        tvStopTime = (TextView) findViewById(R.id.tvStopTime);
        bNext = (Button) findViewById(R.id.bGoToStopView);
        bPrevious = (Button) findViewById(R.id.bBackToStartLocation);
        bAddTrip = (Button) findViewById(R.id.bAdd);
        transparentStrtImageView = (ImageView) findViewById(R.id.transparent_strtImage);
        transparentStopImageView = (ImageView) findViewById(R.id.transparent_stopImage);
        autoCompleteTvStrt = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewStrt);
        autoCompleteTvStop = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextViewStop);
        strtMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragmentStrt);
        stopMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mapFragmentStop);
        hsv = (HorizontalScrollView) findViewById(R.id.addTripHorizontalScrollView);

        setLayoutDimensions(); //Make the layout for start and stop parts fit the screen each
        setLatLongEditTextsInputTypeToSignedFloat();
        setupMap(strtMapFragment); // Make start part map active

        bNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollHorizontallyToFarRight(hsv); //moving to stop part
                setupMap(stopMapFragment);     //Making stop part map active
            }
        });


        /**check trip data sanity and show message of them for confirmation*/
        bAddTrip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (allDatesAndTimesChosen())
                    if (!dateTimeIsInFuture(tvStrtDate, tvStrtTime))
                        if (!dateTimeIsInFuture(tvStopDate, tvStopTime))
                            if (stopDateTimeAfterStartOne())
                                if (MapsOperations.markerStrt != null)
                                    if (MapsOperations.markerStop != null)
                                        showAddingTripMessage("Trip Details", "are you sure you want to add the following trip?" +
                                                "\n\nStart date: " + tvStrtDate.getText().toString() +
                                                "\n" + tvStrtTime.getText().toString() +
                                                "\nStart location: " + MapsOperations.markerStrt.getSnippet() +
                                                "\nLat: " + MapsOperations.markerStrt.getPosition().latitude +
                                                "\nLon: " + MapsOperations.markerStrt.getPosition().longitude +
                                                "\n\nStop date: " + tvStopDate.getText().toString() +
                                                "\n" + tvStopTime.getText().toString() +
                                                "\nStop location: " + MapsOperations.markerStop.getSnippet() +
                                                "\nLat: " + MapsOperations.markerStop.getPosition().latitude +
                                                "\nLon: " + MapsOperations.markerStop.getPosition().longitude, 2);
                                    else showAddingTripMessage("Missing Data", "Stop location is not set", 1);
                                else showAddingTripMessage("Missing Data", "Start location is not set", 1);
                            else showAddingTripMessage("Invalid Data", "Start time is after stop time", 1);
                        else showAddingTripMessage("Invalid Data", "Stop date/time is in future", 1);
                    else showAddingTripMessage("Invalid Data", "Start date/time is in future", 1);
                else showAddingTripMessage("Missing Data", "Start or stop date/time is missing", 1);
            }
        });

        bPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrollHorizontallyToFarLeft(hsv); // move to start part
                setupMap(strtMapFragment);    // make start map active
            }
        });


        tvStrtDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageDatePicker(START_DATE_PICKER);
            }
        });

        tvStopDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manageDatePicker(STOP_DATE_PICKER);
            }
        });


        tvStrtTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Time_PickerFragment time_PickerDialog = new Time_PickerFragment();
                time_PickerDialog.show(getFragmentManager(), START_TIME_PICKER);
            }
        });

        tvStopTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Time_PickerFragment time_PickerDialog = new Time_PickerFragment();
                time_PickerDialog.show(getFragmentManager(), STOP_TIME_PICKER);
            }
        });


        /** Grab addresses corresponding to the content of the autocomplete textViews from google
         * whenever the text changes in them.
         */
        manageAutoCompleteTextView(autoCompleteTvStrt, START_LOCATION);
        manageAutoCompleteTextView(autoCompleteTvStop, STOP_LOCATION);


         /** for latitude/longitude address entry option, when the text changes, check if the internet
          * is available and focus instantly on the new coordinates A listener is assigned to each
          * edit text of start/stop location latitude/longitude (4 listeners in total)
          * P.S. internet is needed here to get the corresponding address
          */
        etStrtLat.addTextChangedListener(new TextWatcher() {
            MapsOperations mapsOperations = new MapsOperations();

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (networkIsAvailable(getBaseContext()))
                    updateLocationOnMapWhenTextChangesFor(etStrtLat, etStrtLon, mapsOperations,
                            mGoogleMapStrt, MapsOperations.markerStrt, START_LOCATION);
                else
                    showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);
            }
        });


        etStrtLon.addTextChangedListener(new TextWatcher() {
            MapsOperations mapsOperations = new MapsOperations();

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (networkIsAvailable(getBaseContext()))
                    updateLocationOnMapWhenTextChangesFor(etStrtLat, etStrtLon, mapsOperations,
                            mGoogleMapStrt, MapsOperations.markerStrt, START_LOCATION);
                else
                    showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);

            }
        });

        etStopLat.addTextChangedListener(new TextWatcher() {
            MapsOperations mapsOperations = new MapsOperations();

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (networkIsAvailable(getBaseContext()))
                    updateLocationOnMapWhenTextChangesFor(etStopLat, etStopLon, mapsOperations,
                            mGoogleMapStop, MapsOperations.markerStop, STOP_LOCATION);
                else
                    showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);
            }
        });

        etStopLon.addTextChangedListener(new TextWatcher() {
            MapsOperations mapsOperations = new MapsOperations();

            @Override
            public void afterTextChanged(Editable editable) {
            }

            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (networkIsAvailable(getBaseContext()))
                    updateLocationOnMapWhenTextChangesFor(etStopLat, etStopLon, mapsOperations,
                            mGoogleMapStop, MapsOperations.markerStop, STOP_LOCATION);
                else
                    showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);
            }
        });

        rbStringLocationStrt = (RadioButton) findViewById(R.id.rbStrtString);
        rbLatLonLocationStrt = (RadioButton) findViewById(R.id.rbLatLngStrt);
        rbMapTapLocationStrt = (RadioButton) findViewById(R.id.rbMapTapStrt);
        rbStringLocationStop = (RadioButton) findViewById(R.id.rbStopString);
        rbLatLonLocationStop = (RadioButton) findViewById(R.id.rbLatLngStop);
        rbMapTapLocationStop = (RadioButton) findViewById(R.id.rbMapTapStop);

        disableAllEditTexts();

        /** For all radio buttons, when their checking status changes:
        *  remove the previous marker on start/stop location map
        *  and activate only the considered option for start/stop address entry
        */
        rbStringLocationStrt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStrt != null) MapsOperations.markerStrt.remove();
                    MapsOperations.markerStrt = null;
                    activate(autoCompleteTvStrt);
                    deactivate(etStrtLat);
                    deactivate(etStrtLon);
                    deactivateMap(mGoogleMapStrt);
                    goToLocationInAutoCompLeteTextView(autoCompleteTvStrt, START_LOCATION);
                }
            }
        });


        rbStringLocationStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStop != null) MapsOperations.markerStop.remove();
                    MapsOperations.markerStop = null;
                    activate(autoCompleteTvStop);
                    deactivate(etStopLat);
                    deactivate(etStopLon);
                    deactivateMap(mGoogleMapStop);
                    goToLocationInAutoCompLeteTextView(autoCompleteTvStop, STOP_LOCATION);
                }
            }
        });

        rbLatLonLocationStrt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStrt != null) MapsOperations.markerStrt.remove();
                    MapsOperations.markerStrt = null;
                    activate(etStrtLat);
                    activate(etStrtLon);
                    deactivate(autoCompleteTvStrt);
                    deactivateMap(mGoogleMapStrt);
                    goToLocationInLatLngEditTexts(etStrtLat, etStrtLon, START_LOCATION);
                }
            }
        });

        rbLatLonLocationStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStop != null) MapsOperations.markerStop.remove();
                    MapsOperations.markerStop = null;
                    activate(etStopLat);
                    activate(etStopLon);
                    deactivate(autoCompleteTvStop);
                    deactivateMap(mGoogleMapStop);
                    goToLocationInLatLngEditTexts(etStopLat, etStopLon, STOP_LOCATION);
                }
            }
        });

        rbMapTapLocationStrt.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStrt != null) MapsOperations.markerStrt.remove();
                    MapsOperations.markerStrt = null;

                    deactivate(etStrtLat);
                    deactivate(etStrtLon);
                    deactivate(autoCompleteTvStrt);
                    transparentStrtImageView.setEnabled(true);
                    tryToRetrieveLastMapClickedLocation(START_LOCATION);
                    assignAddressWhenMapIsLongClicked(START_LOCATION);
                    assignAddressWhenMyLocationButtonClicked(START_LOCATION);
                }
            }
        });

        rbMapTapLocationStop.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    if (MapsOperations.markerStop != null) MapsOperations.markerStop.remove();
                    MapsOperations.markerStop = null;
                    deactivate(etStopLat);
                    deactivate(etStopLon);
                    transparentStopImageView.setEnabled(true);
                    tryToRetrieveLastMapClickedLocation(STOP_LOCATION);
                    assignAddressWhenMapIsLongClicked(STOP_LOCATION);
                    assignAddressWhenMyLocationButtonClicked(STOP_LOCATION);
                }
            }
        });

        disableManualScrollingOfHorizontalScrollView(hsv);
        enableScrollingOnImageView(transparentStrtImageView);
        enableScrollingOnImageView(transparentStopImageView);
    }


    void askToTurnGpsOnIfItIsOff() {
        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsOffAlertMessage(this);
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
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

// AutoCompTxtViews Functions ==================================================================================

    /** grab google addresses in a suggestions list to autocomplete the textView on text change*/
    private void manageAutoCompleteTextView(final AutoCompleteTextView autoCompleteTextView, final String tag) {
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (networkIsAvailable(getBaseContext())) {
                    if (tag.equals(START_LOCATION) && MapsOperations.markerStrt != null)
                        MapsOperations.markerStrt.remove();
                    else if (tag.equals(STOP_LOCATION) && MapsOperations.markerStop != null)
                        MapsOperations.markerStop.remove();
                    placesTask = new PlacesTask();
                    placesTask.execute(s.toString());
                } else {
                    showAddingTripMessage("No internet connection!", "This application needs " +
                            "internet connection to get locations details", 1);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            MapsOperations mapsOperations = new MapsOperations();

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                List<LatLng> coordinates = mapsOperations.locationString2Coordinates(getApplicationContext(),
                        autoCompleteTextView.getText().toString(), 10);
                if (tag.equals(START_LOCATION)) {
                    mapsOperations.goToLocation(mGoogleMapStrt, coordinates.get(0), DEFAULT_ZOOM, MapsOperations.markerStrt, tag,
                            autoCompleteTextView.getText().toString());
                    tmpAutoCompStrtAddress = mapsOperations.coordinates2locationString(getBaseContext(),
                            coordinates.get(0).latitude, coordinates.get(0).longitude, 1).get(0);
                }
                if (tag.equals(STOP_LOCATION)) {
                    mapsOperations.goToLocation(mGoogleMapStop, coordinates.get(0), DEFAULT_ZOOM, MapsOperations.markerStop, tag,
                            autoCompleteTextView.getText().toString());
                    tmpAutoCompStopAddress = mapsOperations.coordinates2locationString(getBaseContext(),
                            coordinates.get(0).latitude, coordinates.get(0).longitude, 1).get(0);
                }
            }
        });
    }


    /** gets the address where the coordinates are pointing and adds marker there and focuses
        the camera on the specified location*/
    private void goToLocationInLatLngEditTexts(EditText etLat, EditText etLon, String locationTag) {
        String latText = etLat.getText().toString();
        String lonText = etLon.getText().toString();
        if (latText.length() > 0 && lonText.length() > 0) {
            MapsOperations mapsOperations = new MapsOperations();
            LatLng latLng = new LatLng(Double.valueOf(latText), Double.valueOf(lonText));
            List<Address> list = mapsOperations.coordinates2locationString(getBaseContext(), latLng.latitude, latLng.longitude, 1);
            try {
                String snippet = list.get(0).getAddressLine(0) + ", " +
                        (list.get(0).getLocality() == null ? "" : (list.get(0).getLocality() + ", ")) +
                        list.get(0).getCountryName();
                if (locationTag.equals(START_LOCATION))
                    mapsOperations.goToLocation(mGoogleMapStrt, latLng, DEFAULT_ZOOM, MapsOperations.markerStrt, locationTag, snippet);
                if (locationTag.equals(STOP_LOCATION))
                    mapsOperations.goToLocation(mGoogleMapStop, latLng, DEFAULT_ZOOM, MapsOperations.markerStop, locationTag, snippet);
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), "No valid address found", Toast.LENGTH_LONG).show();
            }
        }
    }

    /** the same as above function but the address corresponds the autocomplete textView content*/
    private void goToLocationInAutoCompLeteTextView(AutoCompleteTextView autoCompleteTv, String locationTag) {
        String textLocation = autoCompleteTv.getText().toString();
        if (textLocation.length() > 0) {
            MapsOperations mapsOperations = new MapsOperations();
            List<LatLng> coordinates = mapsOperations.locationString2Coordinates(getApplicationContext(), textLocation, 10);
            if (locationTag.equals(START_LOCATION))
                mapsOperations.goToLocation(mGoogleMapStrt, coordinates.get(0), DEFAULT_ZOOM, MapsOperations.markerStrt, locationTag, textLocation);
            if (locationTag.equals(STOP_LOCATION))
                mapsOperations.goToLocation(mGoogleMapStop, coordinates.get(0), DEFAULT_ZOOM, MapsOperations.markerStop, locationTag, textLocation);
        }
    }


// Map Related Functions ==================================================================================================================

    /** adds a marker and focuses on the address where last long-clicked point of the map (if there is one)*/
    private void tryToRetrieveLastMapClickedLocation(String tag) {
        if (tag.equals(START_LOCATION))
            try {
                MapsOperations mapsOperations = new MapsOperations();
                String snippet = tmpMapStrtAddress.getAddressLine(0) + ", "
                        + (tmpMapStrtAddress.getLocality() == null ? "" : (tmpMapStrtAddress.getLocality() + ", "))
                        + tmpMapStrtAddress.getCountryName();
                mapsOperations.goToLocation(mGoogleMapStrt, new LatLng(tmpMapStrtAddress.getLatitude(), tmpMapStrtAddress.getLongitude())
                        , DEFAULT_ZOOM, MapsOperations.markerStrt, START_LOCATION, snippet);
            } catch (Exception e) {
                e.printStackTrace();
            }

        if (tag.equals(STOP_LOCATION))
            try {
                MapsOperations mapsOperations = new MapsOperations();
                String snippet = tmpMapStopAddress.getAddressLine(0) + ", "
                        + (tmpMapStopAddress.getLocality() == null ? "" : (tmpMapStopAddress.getLocality() + ", "))
                        + tmpMapStopAddress.getCountryName();
                mapsOperations.goToLocation(mGoogleMapStop, new LatLng(tmpMapStopAddress.getLatitude(), tmpMapStopAddress.getLongitude())
                        , DEFAULT_ZOOM, MapsOperations.markerStop, STOP_LOCATION, snippet);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }


    private void assignAddressWhenMyLocationButtonClicked(final String tag) {
        if (tag.equals(START_LOCATION)) {
            mGoogleMapStrt.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    return setCurrentLocationAs(START_LOCATION, new MapsOperations());
                }
            });
        } else if (tag.equals(STOP_LOCATION)) {
            mGoogleMapStop.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    return setCurrentLocationAs(STOP_LOCATION, new MapsOperations());
                }
            });
        }
    }

    /** On myCurrentLocation button clicked on the map:
     * 1- If the location service is off ask the user to turn it on.
     * 2- if the user recently turned the GPS service on and the location is not detected yet show a message
     * 3- set the current location as start/stop location and focus on it if the location is detected
     */
    private boolean setCurrentLocationAs(String tag, MapsOperations mapsOperations) {
        askToTurnGpsOnIfItIsOff();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location;
        Criteria criteria = new Criteria();
        try {
            location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
            if (location == null) {
                Toast.makeText(getBaseContext(),
                        "Hasn't detected your location yet." +
                                "\nMake sure that your GPS is on." +
                                "\nTry again after some seconds.", Toast.LENGTH_SHORT).show();
                return true;
            }
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            if (tag.equals(STOP_LOCATION)) {
                tmpMapStopAddress = mapsOperations.coordinates2locationString(getBaseContext(), latitude, longitude, 1).get(0);
                String snippet = tmpMapStopAddress.getAddressLine(0) + " ,"
                        + ((tmpMapStopAddress.getLocality() == null) ? "" : (tmpMapStopAddress.getLocality() + " ,"))
                        + tmpMapStopAddress.getCountryName();
                mapsOperations.goToLocation(mGoogleMapStop, new LatLng(latitude, longitude), DEFAULT_ZOOM, MapsOperations.markerStop, tag, snippet);
            } else if (tag.equals(START_LOCATION)) {
                tmpMapStrtAddress = mapsOperations.coordinates2locationString(getBaseContext(), latitude, longitude, 1).get(0);
                String snippet = tmpMapStrtAddress.getAddressLine(0) + " ,"
                        + ((tmpMapStrtAddress.getLocality() == null) ? "" : (tmpMapStrtAddress.getLocality() + " ,"))
                        + tmpMapStrtAddress.getCountryName();
                mapsOperations.goToLocation(mGoogleMapStrt, new LatLng(latitude, longitude), DEFAULT_ZOOM, MapsOperations.markerStrt, tag, snippet);
            }
        } catch (SecurityException s) {
            Log.e("No Permissions Granted", "couldn't get location because permissions are not yet given");
        }
        return false;
    }


    /** set the location where the map is long-clicked as start/stop location and focus on it if the location is valid
        If the location is not valid (like in the middle of the sea) , show a message*/
    private void assignAddressWhenMapIsLongClicked(final String tag) {
        final MapsOperations mapsOperations = new MapsOperations();
        if (tag.equals(START_LOCATION)) {
            mGoogleMapStrt.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng position) {
                    try {
                        if (networkIsAvailable(getBaseContext())) {
                            tmpMapStrtAddress = mapsOperations.coordinates2locationString(getBaseContext(),
                                    position.latitude, position.longitude, 1).get(0);
                            String snippet = tmpMapStrtAddress.getAddressLine(0) + " ,"
                                    + ((tmpMapStrtAddress.getLocality() == null) ? "" : (tmpMapStrtAddress.getLocality() + " ,"))
                                    + tmpMapStrtAddress.getCountryName();
                            mapsOperations.goToLocation(mGoogleMapStrt, position, DEFAULT_ZOOM, MapsOperations.markerStrt, tag, snippet);
                            Toast.makeText(getBaseContext(), position.latitude + " : " + position.longitude, Toast.LENGTH_SHORT).show();
                        }else{
                            showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);
                        }
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), "cannot find valid address here", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });
        } else if (tag.equals(STOP_LOCATION)) {
            mGoogleMapStop.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng position) {
                    try {
                        if (networkIsAvailable(getBaseContext())) {
                            tmpMapStopAddress = mapsOperations.coordinates2locationString(getBaseContext(),
                                    position.latitude, position.longitude, 1).get(0);
                            String snippet = tmpMapStopAddress.getAddressLine(0) + " ,"
                                    + ((tmpMapStopAddress.getLocality() == null) ? "" : (tmpMapStopAddress.getLocality() + " ,"))
                                    + tmpMapStopAddress.getCountryName();
                            mapsOperations.goToLocation(mGoogleMapStop, position, DEFAULT_ZOOM, MapsOperations.markerStop, tag, snippet);
                            Toast.makeText(getBaseContext(), position.latitude + " : " + position.longitude, Toast.LENGTH_SHORT).show();
                        }else{
                            showAddingTripMessage("No internet connection!", "This application needs internet connection to get locations details", 1);
                        }
                    }catch (Exception e) {
                        Toast.makeText(getBaseContext(), "cannot find valid address here", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    /** same as above function but the location is defined by latitude+longitude entries*/
    private void updateLocationOnMapWhenTextChangesFor(EditText latET, EditText LonET, MapsOperations mapsOperations,
                                                       GoogleMap googleMap, Marker marker, String markerTitle) {
        String latString = latET.getText().toString();
        String lonString = LonET.getText().toString();
        if (lonString.length() > 0 && latString.length() > 0)
            try {
                NumberFormat format = NumberFormat.getInstance(Locale.US);
                double dlat = format.parse(latString).doubleValue();
                double dlon = format.parse(lonString).doubleValue();
                Log.e("pos", dlat + "  " + dlon);
                LatLng latLng = new LatLng(dlat, dlon);
                Address address = mapsOperations.coordinates2locationString(getBaseContext(),
                        latLng.latitude, latLng.longitude, 1).get(0);
                if (markerTitle.equals(START_LOCATION))
                    tmpLatLonStrtAddress = address;
                else if (markerTitle.equals(STOP_LOCATION))
                    tmpLatLonStopAddress = address;
                String snippet = address.getAddressLine(0)
                        + ", " + (address.getLocality() == null ? "" : (address.getLocality() + ", "))
                        + address.getCountryName();
                mapsOperations.goToLocation(googleMap, latLng, DEFAULT_ZOOM, marker, markerTitle, snippet);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getBaseContext(), "No valid address found", Toast.LENGTH_LONG).show();
                if (marker != null) marker.remove();
            }
        else //at least one coordinate is missing
            if (marker != null) marker.remove();
    }

    private void setupMap(final MapFragment mapFragment) {
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                try {
                    whenMapReady(googleMap, mapFragment);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /** When google Map is ready, make start/stop local Map refer to it (i.e. either the start or stop map is active at a time)
        show myLocation button on it*/
    private void whenMapReady(GoogleMap googleMap, MapFragment mapFragment) throws Exception {
        if (mapFragment == strtMapFragment) {
            mGoogleMapStrt = googleMap;
            mGoogleMapStrt.setMyLocationEnabled(true);
        } else if (mapFragment == stopMapFragment) {
            mGoogleMapStop = googleMap;
            mGoogleMapStop.setMyLocationEnabled(true);
        } else {
            throw (new Exception("Unknown fragment"));
        }

        if (ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            Log.e("permissions missing", "Make sure all location access permissions are given");
    }

//Layout Management Related Functions ===============================================================================================

    /** Display a view in the AddTripActivity layout*/
    private void activate(View view) {
        view.setEnabled(true);
        view.setBackgroundColor(Color.WHITE);
        if (view instanceof EditText)
            ((EditText) view).setHintTextColor(Color.LTGRAY);
    }

    /** hide a view in the AddTripActivity layout*/
    private void deactivate(View view) {
        view.setEnabled(false);
        view.setBackgroundColor(DARK_COLOR);
        if (view instanceof EditText)
            ((EditText) view).setHintTextColor(DARK_COLOR);
    }

    /** disable map in the AddTripActivity layout for long-clicking and hide myLocation button*/
    private void deactivateMap(GoogleMap mGoogleMap) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGoogleMap.setMyLocationEnabled(false);
        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {}
        });
    }


    private void disableAllEditTexts() {
        View[] views = {etStrtLat,etStrtLon,etStopLat,etStopLon,autoCompleteTvStop,autoCompleteTvStrt};
        for(View view : views)
            deactivate(view);
    }

    /** Make the scrolling on the screen unresponsive but on the maps. Thus, the user is forced
     to click next/previous buttons to switch between start and stop views*/
    private void disableManualScrollingOfHorizontalScrollView(HorizontalScrollView hsv) {
        hsv.setOnTouchListener( new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {return true;}
        });
    }

    /** we added transparent image over the map. The image has onClickListener. If the click is
        on the image <=> map,scrolling in scrollView is not active while it is active on the map.*/
    private void enableScrollingOnImageView(ImageView imageView) {
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        hsv.requestDisallowInterceptTouchEvent(true);// Disallow ScrollView to intercept touch events.
                        return false;// Disable touch on transparent view
                    case MotionEvent.ACTION_UP:
                        // Allow ScrollView to intercept touch events.
                        hsv.requestDisallowInterceptTouchEvent(false);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        hsv.requestDisallowInterceptTouchEvent(true);
                        return false;
                    default:
                        return true;
                }
            }
        });
    }

    private void setLatLongEditTextsInputTypeToSignedFloat() {
        etStrtLat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etStrtLon.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etStopLat.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
        etStopLon.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
    }

    /** Make each of start and stop locations parts of AddTripActivity Layout fill and fit in the device screen.*/
    private void setLayoutDimensions() {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int screenWidth  = displaymetrics.widthPixels ;
        int screenHeight = displaymetrics.heightPixels;
        changeLayoutDimensions(startPartLayout,screenHeight-getStatusBarHeight(), screenWidth);
        changeLayoutDimensions(stopPartLayout ,screenHeight-getStatusBarHeight(), screenWidth);
    }

    public  void changeLayoutDimensions(LinearLayout layout, int newHeight, int newWidth){
        ViewGroup.LayoutParams params = layout.getLayoutParams();
        params.height = newHeight;
        params.width  = newWidth ;
        layout.setLayoutParams(params);
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // called on "next" clicked to move from start to stop part of the layout
    public void scrollHorizontallyToFarRight(HorizontalScrollView hsv) {
        hsv.fullScroll(HorizontalScrollView.FOCUS_RIGHT);
                //hsv.smoothScrollBy(500, 0);
    }

    // called on "previous" clicked to move from stop to start part of the layout
    public void scrollHorizontallyToFarLeft(HorizontalScrollView hsv) {
        hsv.fullScroll(HorizontalScrollView.FOCUS_LEFT);
    }

//Time Date related functions ===============================================================================================

    private boolean stopDateTimeAfterStartOne() {
        return  generateChosenCalendar(tvStopDate,tvStopTime).after(
                generateChosenCalendar(tvStrtDate,tvStrtTime)     );
    }

    private boolean dateTimeIsInFuture(TextView dateTextView, TextView timeTextView) {
        Calendar currentCalendar = Calendar.getInstance();
        // there is a bug in Android 4.4.4: it skips December! hacked it in AddTripAsyncTask
        //currentCalendar.after will not work fine for chosen date in December
        int dayCurrent    = currentCalendar.get(Calendar.DAY_OF_MONTH);
        int monthCurrent  = currentCalendar.get(Calendar.MONTH) + 1;
        int yearCurrent   = currentCalendar.get(Calendar.YEAR);
        int hourCurrent   = currentCalendar.get(Calendar.HOUR_OF_DAY);
        int minuteCurrent = currentCalendar.get(Calendar.MINUTE);

        Calendar chosenCalendar = generateChosenCalendar(dateTextView, timeTextView);
        int dayChosen    = chosenCalendar.get(Calendar.DAY_OF_MONTH);
        int monthChosen  = chosenCalendar.get(Calendar.MONTH);
        int yearChosen   = chosenCalendar.get(Calendar.YEAR);
        int hourChosen   = chosenCalendar.get(Calendar.HOUR_OF_DAY);
        int minuteChosen = chosenCalendar.get(Calendar.MINUTE);
        yearChosen  = (monthChosen < 1 ? yearChosen - 1 : yearChosen);
        monthChosen = (monthChosen < 1 ? 12 : monthChosen);

        Log.e("current calendar", monthCurrent + " / " + yearCurrent);
        Log.e("chosen calendar", monthChosen + " / " + yearChosen);

        // return true if chosen date/time is fully after current date/time (returns false if equal)
        return yearChosen > yearCurrent ||
                yearChosen >= yearCurrent && (monthChosen > monthCurrent
                    || monthChosen >= monthCurrent && (dayChosen > dayCurrent
                       || dayChosen >= dayCurrent && (hourChosen > hourCurrent
                          || hourChosen >= hourCurrent && (minuteChosen > minuteCurrent
                             || minuteChosen >= minuteCurrent))));
    }

    /** returns calendar of the chosen data*/
    private Calendar generateChosenCalendar(TextView dateTextView,TextView timeTextView) {
        // there is a bug in Android 4.4.4: it skips December! hacked it in AddTripAsyncTask
        String date = dateTextView . getText( ) . toString( );
        String time = timeTextView . getText( ) . toString( );
        int day     = Integer.valueOf ( date.substring(6,8) );
        int month   = months.indexOf(date.substring(9,12))+ 1;
        int year    = Integer.valueOf(date.substring(13,17) );
        int hour    = Integer.valueOf(time.substring(12,14) );
        int minute  = Integer.valueOf(time.substring(17,19) );
        Calendar chosenCalendar  =  Calendar . getInstance( );
        chosenCalendar . set (  year,month,day,hour,minute  );
        return chosenCalendar;
    }

    private boolean allDatesAndTimesChosen() {
        return !(tvStrtTime.getText().toString().equals("Choose time") || tvStopTime.getText().toString().equals("Choose time") ||
                 tvStrtDate.getText().toString().equals("Choose date") || tvStopDate.getText().toString().equals("Choose date") );
    }


    private void manageDatePicker(String tag) {
        PickerDialogForDate.dateTag = tag;
        PickerDialogForDate pickerDialogForDate = new PickerDialogForDate();
        pickerDialogForDate.show(getSupportFragmentManager(),tag);
    }

    public static class PickerDialogForDate extends android.support.v4.app.DialogFragment {
        public static String dateTag;
        @NonNull @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Calendar calendar = Calendar.getInstance();
            int year  = calendar.get(Calendar.YEAR        );
            int month = calendar.get(Calendar.MONTH       );
            int day   = calendar.get(Calendar.DAY_OF_MONTH);
            return new DatePickerDialog(getActivity(),dateSettings,year,month,day);
        }
    }

    /** Fills date textView with the date chosen in datePicker */
    class DateSettings implements DatePickerDialog.OnDateSetListener{
        final String[] weekDays = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        final String[] months ={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int day) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year,month,day);
            int weekDayIndex = calendar.get(Calendar.DAY_OF_WEEK)-1;
            DecimalFormat myFormatter = new DecimalFormat("00");
            if(PickerDialogForDate.dateTag.equals(START_DATE_PICKER))
                tvStrtDate.setText(weekDays[weekDayIndex] + " : " + myFormatter.format(day) + " " + months[month]+ " " + year);
            else if (PickerDialogForDate.dateTag.equals(STOP_DATE_PICKER))
                tvStopDate.setText(weekDays[weekDayIndex] + " : " + myFormatter.format(day) + " " + months[month]+ " " + year);
        }
    }


    /** Shows Time Picker and stores the hour and minute values for start/stop in local variables by onTimeSet function*/
    public static class Time_PickerFragment extends DialogFragment {
        public static String timeTag;
        @Override public Dialog onCreateDialog(Bundle savedInstanceState) {
            timeTag = getTag();
            Calendar calendar = Calendar.getInstance();

            if(timeTag.equals(START_TIME_PICKER)) {
                mHourStrt   = calendar.get(Calendar.HOUR_OF_DAY);
                mMinuteStrt = calendar.get(Calendar.MINUTE     );
            }else if(timeTag.equals(STOP_TIME_PICKER)) {
                mHourStop   = calendar.get(Calendar.HOUR_OF_DAY);
                mMinuteStop = calendar.get(Calendar.MINUTE     );
            }

            if (!(getActivity() instanceof TimePickerDialog.OnTimeSetListener))
                throw new IllegalStateException("Activity should implement OnTimeSetListener!");
            TimePickerDialog.OnTimeSetListener timeSetListener = (TimePickerDialog.OnTimeSetListener) getActivity();

            switch (timeTag) {
                case START_TIME_PICKER:
                    return new TimePickerDialog(getActivity(), timeSetListener, mHourStrt, mMinuteStrt, true);
                case STOP_TIME_PICKER:
                    return new TimePickerDialog(getActivity(), timeSetListener, mHourStop, mMinuteStop, true);
                default:
                    return null;
            }
        }
    }

    /** assign chosen time values (hour-minute) to local variables*/
    @Override public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        DecimalFormat myFormatter = new DecimalFormat("00");
        String twoDigitsMinute,twoDigitsHoure;
        if(Time_PickerFragment.timeTag.equals(START_TIME_PICKER)) {
            mMinuteStrt =  minute;
            mHourStrt = hourOfDay;
            twoDigitsMinute = myFormatter.format(mMinuteStrt);
            twoDigitsHoure  = myFormatter.format(mHourStrt  );
            tvStrtTime.setText("Start time: " +twoDigitsHoure + " : " + twoDigitsMinute);
        } else if(Time_PickerFragment.timeTag.equals(STOP_TIME_PICKER)) {
            mMinuteStop =  minute;
            mHourStop = hourOfDay;
            twoDigitsMinute = myFormatter.format(mMinuteStop);
            twoDigitsHoure  = myFormatter.format(mHourStop  );
            tvStopTime.setText(" Stop time: " +twoDigitsHoure + " : " + twoDigitsMinute);
        }
    }

// Getting Google places Classes and Tasks ===============================================================================================

    /** Fetches all places from GooglePlaces AutoComplete Web Service*/
    private class PlacesTask extends AsyncTask<String, Void, String>{
        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            // Obtain browser key from https://code.google.com/apis/console
            String key  = "key="+BROWSER_KEY;
            String input="";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                ServerProcesses sp = new ServerProcesses(getApplicationContext());
                data = sp.downloadUrl(url);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            //Log.e("data is: " , data);
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Creating placeParserTask
            placeParserTask = new placeParserTask();

            // Starting Parsing the JSON string returned by Web Service
            placeParserTask.execute(result);
        }
    }



    /** A class to parsePlace the Google Places in JSON format */
    private class placeParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>> {
        JSONObject jObject;
        @Override protected List<HashMap<String, String>> doInBackground(String... jsonData) {
            List<HashMap<String, String>> places = null;
            PlaceJSONParser placeJsonParser = new PlaceJSONParser();
            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parsePlace(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {
            String[] from = new String[] {"description"};
            int[] to = new int[] { android.R.id.text1 };

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);

            // Setting the adapter
            autoCompleteTvStrt.setAdapter(adapter);
            autoCompleteTvStop.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

//===============================================================================================

    /**used to show messages on trip adding (error messages or entered data displaying for confirmation)*/
    public void showAddingTripMessage(String title, String msg, final int buttonsNumber){
        final AlertDialog.Builder dialogueBuilder = new AlertDialog.Builder(this);
        dialogueBuilder.setTitle(title);
        dialogueBuilder.setMessage(msg);
        dialogueBuilder.setCancelable(true);
        pd = new ProgressDialog(this,R.style.MyTheme);
        pd.setCancelable(false);
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Large);

        dialogueBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if(buttonsNumber == 2) {
                    pd.show();
                    addTripToUserRecords();
                }
            }
        });
        if(buttonsNumber==2)
            dialogueBuilder.setNegativeButton("No", null);
        dialogueBuilder.show();
    }

//===============================================================================================

    /**returns true if network is available and the device is connected to it*/
    private static boolean networkIsAvailable (Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mNetInfo = cm.getActiveNetworkInfo();
        return mNetInfo != null && mNetInfo.isConnected();
    }

//===============================================================================================
    /**Adds the manually-entered trip to TripSummary table in the database*/
    private void addTripToUserRecords() {
        Trip trip = new Trip(getFinalStartAddress(),
                             getFinalStopAddress() ,
                             generateChosenCalendar(tvStrtDate,tvStrtTime),
                             generateChosenCalendar(tvStopDate,tvStopTime)
        );
        new ServerProcesses(AddTripActivity.this).addTripInBackground(trip, new AfterTripTaskDone() {
            @Override
            public void done(Trip trip) {
            }
        });
    }

//===============================================================================================
    /**returns the last available chosen address*/
    private Address getFinalStartAddress() {
        return rbStringLocationStrt.isChecked()? tmpAutoCompStrtAddress :
               rbLatLonLocationStrt.isChecked()? tmpLatLonStrtAddress   :
               rbMapTapLocationStrt.isChecked()? tmpMapStrtAddress: null;

    }

    private Address getFinalStopAddress() {
        return rbStringLocationStop.isChecked()? tmpAutoCompStopAddress :
               rbLatLonLocationStop.isChecked()? tmpLatLonStopAddress   :
               rbMapTapLocationStop.isChecked()? tmpMapStopAddress: null;
    }

}
