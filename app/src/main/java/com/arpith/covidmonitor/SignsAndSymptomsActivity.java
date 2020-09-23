package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.HashMap;

public class SignsAndSymptomsActivity extends AppCompatActivity {

    TabLayout signsAndSymptomsLayout;
    ViewPager viewPager;
    SharedPreferences sharedPreferences;
    public static final String logTagName = SignsAndSymptomsActivity.class.getSimpleName();
    SignsFragment signsFragment;
    SymptomsFragment symptomsFragment;
    MaterialButton submitButton;
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
        final DataBaseHelper dataBaseHelper = new DataBaseHelper(this, userName + ".db");

        submitButton.setOnClickListener(view -> {
            int heartRate = signsFragment.getHeartRate();
            int breathRate = signsFragment.getBreathRate();
            boolean symptomsSet = symptomsFragment.isSymptomsSet();

            if(!symptomsSet && heartRate ==0 && breathRate == 0){
                Toast.makeText(SignsAndSymptomsActivity.this, "Nothing to save!", Toast.LENGTH_LONG).show();
                return;
            }

            HashMap<String, Integer> ratingsMap = symptomsFragment.getRatingsMap();
            dataBaseHelper.insertOrUpdateData(timestamp, heartRate, breathRate,ratingsMap);
            Log.d(logTagName, dataBaseHelper.getData());
            Toast.makeText(SignsAndSymptomsActivity.this, "Data saved.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(SignsAndSymptomsActivity.this, FinalActivity.class);
            startActivity(intent);
            finish();
        });
    }
}