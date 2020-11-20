package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FinalActivity extends AppCompatActivity {

//    MaterialButton uploadToServerButton;
    MaterialButton newRecordButton;
    MaterialTextView goodbyeText;
    SharedPreferences sharedPreferences;
    String userName;
    String logTagName = FinalActivity.class.getSimpleName();
    DataBaseHelper dataBaseHelper;
    MaterialButton homeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_final);
//        uploadToServerButton = findViewById(R.id.upload_to_server_btn);
        newRecordButton = findViewById(R.id.new_record_button);
        homeButton = findViewById(R.id.home_button);
        goodbyeText = findViewById(R.id.goodbye_textView);
        sharedPreferences = getSharedPreferences(Constants.PREFERENCES, MODE_PRIVATE);
        userName = sharedPreferences.getString(Constants.USER_NAME, "User");
        String _userName = userName.replace("_", " ");
        dataBaseHelper = new DataBaseHelper(this, userName + ".db");
        goodbyeText.setText("Your Symptoms have been Recorded.\nGoodBye "+ _userName +"! Stay Safe!");

        newRecordButton.setOnClickListener(view -> {
            Intent intent = new Intent(this,SignsAndSymptomsActivity.class);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.TIMESTAMP,System.currentTimeMillis());
            editor.apply();
            startActivity(intent);
            finish();
        });

        homeButton.setOnClickListener(view -> {
            Intent intent = new Intent(this,HomeActivity.class);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(Constants.TIMESTAMP,System.currentTimeMillis());
            editor.apply();
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        dataBaseHelper.closeDB();
    }

    private boolean uploadToServer() throws IOException {
        File data = Environment.getDataDirectory();
//        Log.d(logTagName, data.getAbsolutePath());
        String databaseName = dataBaseHelper.DatabaseName;
        String dbPath = "/data/" + "com.arpith.covidmonitor" + "/databases/" + databaseName;
        String url = "http://192.168.0.226/ContactTracing/php/upload.php";

        File currentDB = new File(data, dbPath);
        long fileSize = currentDB.length();
        Log.d(logTagName, String.valueOf(fileSize));
//
//        RequestQueue queue = Volley.newRequestQueue(this);
//
//        SimpleMultiPartRequest request = new SimpleMultiPartRequest(Request.Method.POST, url,
//                new Response.Listener<String>() {
//                    @Override
//                    public void onResponse(String response) {
//                        Log.d("Response", response);
//                    }
//                }, error -> Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT));
//
//        request.addFile("database", dbPath);
//        request.addStringParam("lastname", "Singareddy");
//
//        RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
//        mRequestQueue.add(request);

        final MediaType MEDIA_TYPE_OCTET_STREAM = MediaType.parse("application/octet-stream");
        OkHttpClient client = new OkHttpClient();

        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM).
                addFormDataPart("database", dbPath, RequestBody.create(MEDIA_TYPE_OCTET_STREAM, currentDB)).
                addFormDataPart("lastname", "Singareddy").build();

        Request request = new Request.Builder().url(url).post(requestBody).build();

//        Response response = client.newCall(request).execute();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
//                Toast.makeText(getApplicationContext(), "Upload Failed!", Toast.LENGTH_SHORT);
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if(!response.isSuccessful()){
                    throw new IOException("Unexpected Code: " + response);
                }
                else {
//                    Toast.makeText(getApplicationContext(), "Upload Successfull", Toast.LENGTH_LONG);
                    Log.d(logTagName, "Upload Successful");
                }

            }
        });

        return true;
    }
}