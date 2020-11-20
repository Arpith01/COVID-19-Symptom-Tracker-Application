package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeActivity extends AppCompatActivity {

    private MaterialButton recordSymptomsButton;
    SharedPreferences sharedPreferences;
    TextView helloTextView;
    DataBaseHelper dataBaseHelper;
    MaterialButton syncButton;
    MaterialButton traceContactButton;
    public static final String LogTagName = HomeActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        recordSymptomsButton = findViewById(R.id.record_symptoms);
        syncButton = findViewById(R.id.sync_button);
        traceContactButton = findViewById(R.id.trace_contact);
        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);
        String userName = sharedPreferences.getString(Constants.USER_NAME, "User");
        String _userName = userName.replace("_", " ");

        helloTextView = findViewById(R.id.hello_textView);
        helloTextView.setText("Hello "+ _userName +"! Welcome to COVID-19 Symptom Tracker");
        dataBaseHelper = new DataBaseHelper(this, userName + ".db");
        recordSymptomsButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, SignsAndSymptomsActivity.class);
            startActivity(intent);
        });

        syncButton.setOnClickListener(view -> {
            uploadToServer();
        });

        traceContactButton.setOnClickListener(view -> {
            Intent intent = new Intent(this, ContactTracingActivity.class);
            startActivity(intent);
        });

    }

    private void uploadToServer(){
        File data = Environment.getDataDirectory();
        String databaseName = DataBaseHelper.DatabaseName;
        String dbPath = "/data/" + "com.arpith.covidmonitor" + "/databases/" + databaseName;
//        String url = "http://192.168.0.226/ContactTracing/php/upload.php";
        String url = "http://192.168.0.226:5000/upload";

        File currentDB = new File(data, dbPath);
        long fileSize = currentDB.length();
        Log.d(LogTagName, String.valueOf(fileSize));

        final MediaType MEDIA_TYPE_OCTET_STREAM = MediaType.parse("application/octet-stream");
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).
                addFormDataPart("database", databaseName, RequestBody.create(MEDIA_TYPE_OCTET_STREAM, currentDB)).
                addFormDataPart("lastname", "Singareddy").build();

        Request request = new Request.Builder().url(url).post(requestBody).build();


        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Unexpected Code: " + response);
                }
                else {
                    runOnUiThread(() -> Toast.makeText(HomeActivity.this, "Upload Successful", Toast.LENGTH_SHORT).show());
                    Log.d(LogTagName, "Upload Successful");
                }
                response.close();
            }
        });

    }

}