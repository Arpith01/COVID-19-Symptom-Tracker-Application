package com.arpith.covidmonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.HashMap;

public class SymptomsFragment extends Fragment {
    public SymptomsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    public static final String logTagName = SymptomsFragment.class.getSimpleName();
    public HashMap<String, Integer> ratingsMap;
    private ConstraintLayout cardsLayout;
    SharedPreferences sharedPreferences;
    boolean symptomsSet;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_symptoms, container, false);

        ratingsMap = new HashMap<>();
        cardsLayout = v.findViewById(R.id.cards_layout);

        for (int i = 3; i < cardsLayout.getChildCount(); i++) {
            MaterialCardView cardView = (MaterialCardView) cardsLayout.getChildAt(i);
            ConstraintLayout cardLayout = (ConstraintLayout) cardView.getChildAt(0);
            RatingBar ratingBar = (RatingBar) cardLayout.getChildAt(1);
            ratingsMap.put(getResources().getResourceEntryName(ratingBar.getId()), 0);
            ratingBar.setOnRatingBarChangeListener((ratingBar1, v1, b) -> {
                Log.d(logTagName, "Rating Changed " + ratingBar1.getRating() + " " + getResources().getResourceEntryName(ratingBar1.getId()));
                ratingsMap.put(getResources().getResourceEntryName(ratingBar1.getId()), (int) ratingBar1.getRating());
                symptomsSet = true;
            });
        }

        sharedPreferences = getActivity().getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        return v;
    }

    protected HashMap<String, Integer> getRatingsMap(){
        return ratingsMap;
    }

    protected boolean isSymptomsSet(){
        return symptomsSet;
    }
}