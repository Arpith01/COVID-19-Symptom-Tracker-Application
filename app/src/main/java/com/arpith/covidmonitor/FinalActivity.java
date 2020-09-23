package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

public class FinalActivity extends AppCompatActivity {

    MaterialButton exitAppButton;
    MaterialButton newRecordButton;
    MaterialTextView goodbyeText;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);
        exitAppButton = findViewById(R.id.exit_app_button);
        newRecordButton = findViewById(R.id.new_record_button);
        goodbyeText = findViewById(R.id.goodbye_textView);
        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);
        String userName = sharedPreferences.getString(Constants.USER_NAME, "User");
        userName = userName.replace("_", " ");
        goodbyeText.setText("Your Symptoms have been Recorded.\nGoodBye "+ userName +"! Stay Safe!");

        exitAppButton.setOnClickListener(view -> finish());
        newRecordButton.setOnClickListener(view -> {
            Intent intent = new Intent(this,SignsAndSymptomsActivity.class);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.TIMESTAMP,System.currentTimeMillis());
            editor.apply();
            startActivity(intent);
            finish();
        });
    }
}