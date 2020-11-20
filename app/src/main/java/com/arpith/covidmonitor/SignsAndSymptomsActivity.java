package com.arpith.covidmonitor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;

public class SignsAndSymptomsActivity extends AppCompatActivity implements LocationListener {

    TabLayout signsAndSymptomsLayout;
    ViewPager viewPager;
    SharedPreferences sharedPreferences;
    public static final String logTagName = SignsAndSymptomsActivity.class.getSimpleName();
    SignsFragment signsFragment;
    SymptomsFragment symptomsFragment;
    MaterialButton submitButton;
    FusedLocationProviderClient fusedLocationProviderClient;
    Location userLocation;
    public static final int LOCATION_PERMISSION_CODE = 99;
    DataBaseHelper dataBaseHelper;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signs_and_symptoms);

        submitButton = findViewById(R.id.button_submit_all);
        signsAndSymptomsLayout = findViewById(R.id.signsAndSymptomsLayout);
        viewPager = findViewById(R.id.viewPager);

        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), signsAndSymptomsLayout.getTabCount());
        signsFragment = new SignsFragment();
        symptomsFragment = new SymptomsFragment();
        adapter.addFragment(signsFragment, "Signs");
        adapter.addFragment(symptomsFragment, "Symptoms");
        viewPager.setAdapter(adapter);
        signsAndSymptomsLayout.setupWithViewPager(viewPager);

        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        String userName = sharedPreferences.getString(Constants.USER_NAME, "username");
        final long timestamp = sharedPreferences.getLong(Constants.TIMESTAMP, System.currentTimeMillis());
        dataBaseHelper = new DataBaseHelper(this, userName + ".db");

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setNumUpdates(10);
        locationRequest.setInterval(1000);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult == null){
                    return;
                }
                for (Location location:locationResult.getLocations()){
                    if(location!=null){
                        userLocation = location;

                        if(fusedLocationProviderClient!=null){
                            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
                        }
                    }
                }
            }
        };

        updateLocation();

        submitButton.setOnClickListener(view -> {
            onSubmit(timestamp);
        });
    }

    public void updateLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        }
        else {
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    userLocation = location;
                    Log.d(logTagName, userLocation.toString());
                }
                else{
                    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                }
            });
        }
    }

    public void onSubmit(long timestamp) {
        int heartRate = signsFragment.getHeartRate();
        int breathRate = signsFragment.getBreathRate();
        boolean symptomsSet = symptomsFragment.isSymptomsSet();

        if (!symptomsSet && heartRate == 0 && breathRate == 0) {
            Toast.makeText(SignsAndSymptomsActivity.this, "Nothing to save!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userLocation == null) {
            updateLocation();
            updateLocation();
            updateLocation();
        }

        HashMap<String, Integer> ratingsMap = symptomsFragment.getRatingsMap();

        if(userLocation == null){
            Toast.makeText(getApplicationContext(), "Unable to access location. Please check GPS settings or try later", Toast.LENGTH_LONG).show();
        }
        else{
            dataBaseHelper.insertOrUpdateData(timestamp, heartRate, breathRate, ratingsMap, userLocation);
            Log.d(logTagName, "User Location:" + userLocation.toString());
//        Log.d(logTagName, dataBaseHelper.getData());
            Toast.makeText(SignsAndSymptomsActivity.this, "Data saved.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(SignsAndSymptomsActivity.this, FinalActivity.class);
            startActivity(intent);
            finish();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @SuppressLint("MissingPermission")
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                userLocation = location;
                                Log.d(logTagName, userLocation.toString());
                            }
                            else{
                                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
                            }
                        }
                    });
                }
            } else {
                Toast.makeText(getApplicationContext(), "Location permission denied. Please provide necessary permissions.", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);


        alertDialog.setTitle("GPS is not Enabled!");

        alertDialog.setMessage("Do you want to turn on GPS?");


        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });


        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });


        alertDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocation();
    }

    @Override
    public void onLocationChanged(Location location) {
        if(location!=null){
            this.userLocation = location;
        }
    }
}