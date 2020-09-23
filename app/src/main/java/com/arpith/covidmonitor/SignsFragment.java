package com.arpith.covidmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class SignsFragment extends Fragment {

    public SignsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    SharedPreferences sharedPreferences;
    public static final int REQUEST_HEART_RATE = 1;
    public static final int REQUEST_BREATH_RATE = 2;
    protected int heartRate = 0;
    protected int breathRate = 0;
    MaterialTextView heartRateTextView;
    MaterialTextView respiratoryRateTextView;
    public static final String logTagName = SignsFragment.class.getSimpleName();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_signs, container, false);
        heartRateTextView = v.findViewById(R.id.heart_rate_bpm);
        respiratoryRateTextView = v.findViewById(R.id.respiratory_rate_bpm);

        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        MaterialButton measureHeartRateButton = v.findViewById(R.id.measure_heart_rate_button);
        measureHeartRateButton.setOnClickListener(view -> {
            Intent heartRateIntent = new Intent(getContext(), HeartRateMonitor.class);
            heartRateIntent.putExtra(Constants.TIMER_DURATION, 45);
            startActivityForResult(heartRateIntent, REQUEST_HEART_RATE);
        });


        MaterialButton measureRespiratoryRateButton = v.findViewById(R.id.measure_respiratory_rate_button);
        measureRespiratoryRateButton.setOnClickListener(view -> {
            Intent breathRateIntent = new Intent(getContext(), RespiratoryMonitor.class);
            breathRateIntent.putExtra(Constants.TIMER_DURATION, 45);
            startActivityForResult(breathRateIntent, REQUEST_BREATH_RATE);
        });

        return v;
    }

    protected int getHeartRate(){
        return heartRate;
    }

    protected int getBreathRate(){
        return breathRate;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_HEART_RATE) {
            if (resultCode == Activity.RESULT_OK) {
                heartRate = data.getIntExtra(Constants.HEART_RATE, 0);
                heartRate = Math.round(heartRate);
                heartRate = heartRate * 4 / 3;
                heartRateTextView.setText(heartRate + " BPM");
            }
        }
        if (requestCode == REQUEST_BREATH_RATE) {
            if (resultCode == Activity.RESULT_OK) {
                breathRate = data.getIntExtra(Constants.BREATH_RATE, 0);
                breathRate = Math.round(breathRate);
                breathRate = breathRate * 4 / 3;
                respiratoryRateTextView.setText(breathRate + " BPM");
            }
        }

    }
}