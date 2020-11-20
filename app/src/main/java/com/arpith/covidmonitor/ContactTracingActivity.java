package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.arpith.covidmonitor.utilities.DateTimeUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ContactTracingActivity extends AppCompatActivity {
    public static final String LogTagName = ContactTracingActivity.class.getSimpleName();
    MaterialDatePicker.Builder datePickerBuilder;
    MaterialDatePicker datePicker;
    MaterialButton selectDateButton;
    MaterialButton findContactsButton;
    TextInputEditText subjectNameTextInput;
    Date queryDate;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_tracing);
        datePickerBuilder = MaterialDatePicker.Builder.datePicker();
        datePicker = datePickerBuilder.build();
        subjectNameTextInput = findViewById(R.id.subject_name_edit_text);

        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd,yyyy", Locale.US);

        findContactsButton = findViewById(R.id.find_contacts_button);

        queryDate = Calendar.getInstance().getTime();
        selectDateButton = findViewById(R.id.select_date_button);
        selectDateButton.setText(formatter.format(queryDate));


        selectDateButton.setOnClickListener(view -> {
            datePicker.show(getSupportFragmentManager(), datePicker.toString());
        });

        findContactsButton.setOnClickListener(view -> findContacts(subjectNameTextInput.getText().toString(), queryDate));


        datePicker.addOnPositiveButtonClickListener(selection -> {

            String datePickerText = datePicker.getHeaderText();
            try {
                queryDate = formatter.parse(datePickerText);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Log.d(LogTagName, datePickerText);
            selectDateButton.setText(datePickerText);
        });
    }


    public void findContacts(String subjectName, Date queryDate){
        if(subjectName.isEmpty()){
            Toast.makeText(getApplicationContext(), "Enter a subject name", Toast.LENGTH_LONG).show();
            return;
        }

        String queryEpoch = DateTimeUtils.getEpochFromDate(queryDate);
        getContactsFromServer(subjectName, queryEpoch);
    }

    public void getContactsFromServer(String subjectName, String queryDateEpoch){
//        String url = "http://192.168.0.226/ContactTracing/php/process.php"; //PHP
        String url = "http://192.168.0.226:5000/contacts"; //Flask
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        urlBuilder.addQueryParameter("subjectname", subjectName);
        urlBuilder.addQueryParameter("querydate", queryDateEpoch);
        String queryUrl = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(queryUrl).get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() -> {
                        ConstraintLayout constraintLayout = (ConstraintLayout) findViewById(R.id.contact_trace_layout);
                        Snackbar.make(constraintLayout, "Error: Subject not Found. Please check the input.", BaseTransientBottomBar.LENGTH_LONG).show();
//                        Toast.makeText(getApplicationContext(), "Error: Could not generate contact matrix", Toast.LENGTH_LONG).show();
                    });
//                    throw new IOException("Failed to download file: " + response);
                }
                else {
                    FileOutputStream fos = openFileOutput("contact_matrix.txt", MODE_PRIVATE);
//                FileOutputStream fos = new FileOutputStream("/data/data/com.arpith.covidmonitor/misc/result.txt");
                    fos.write(response.body().bytes());
                    fos.close();
                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), "Contact matrix file downloaded into Files", Toast.LENGTH_LONG).show();
                    });
                }

                response.close();
            }
        });
    }
}
