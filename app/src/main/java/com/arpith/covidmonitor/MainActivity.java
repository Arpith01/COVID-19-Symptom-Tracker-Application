package com.arpith.covidmonitor;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {
    protected long timestamp;
    protected String userName;
    TextInputEditText nameEditText;
    SharedPreferences sharedPreferences;
    public static final String logTagName = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nameEditText = findViewById(R.id.name_edit_text);
        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, Context.MODE_PRIVATE);
        userName = sharedPreferences.getString(Constants.USER_NAME, "");
        userName = userName.replace('_', ' ');
        nameEditText.setText(userName);
    }

    public void handleNext(View view) {
        userName = nameEditText.getText().toString();
        if (userName.length() > 0) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            userName = userName.trim();
            userName = userName.replace(' ', '_');
            timestamp = System.currentTimeMillis();
            editor.putString(Constants.USER_NAME, userName);
            editor.putLong(Constants.TIMESTAMP, timestamp);
            editor.apply();
            Log.d(logTagName, "TIMESTAMP: "+ timestamp);
            final DataBaseHelper dataBaseHelper = new DataBaseHelper(this, userName + ".db");
            Log.d(logTagName, dataBaseHelper.getData());
            Intent intent = new Intent(this, SignsAndSymptomsActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Please enter your name to proceed", Toast.LENGTH_LONG).show();
        }
    }
}