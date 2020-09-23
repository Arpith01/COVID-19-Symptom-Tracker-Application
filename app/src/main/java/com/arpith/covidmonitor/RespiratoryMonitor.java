package com.arpith.covidmonitor;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class RespiratoryMonitor extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor sensor;

    GraphView graphView;
    LineGraphSeries<DataPoint> series;
    TextView timerText;
    public static final String logTagName = RespiratoryMonitor.class.getName();
    List<Double> data;
    MaterialButton startButton;
    int duration = 45;
    boolean started;
    int counter;
    MaterialTextView infoText;
    double movingAverage;
    double windowSum;
    int windowLength;
    Deque<Double> window;
    double prevMovingAverage;
    boolean increasing;
    RecordingTimer recordingTimer;
    int breathCount;
    SignalProcessor signalProcessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_respiratory_monitor);

        Intent receivedIntent = getIntent();
        duration = receivedIntent.getIntExtra(Constants.TIMER_DURATION, 45);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        graphView = findViewById(R.id.pulse_graph_view);
        infoText = findViewById(R.id.breath_info_textView);
        timerText = findViewById(R.id.breath_timer_text);
        startButton = findViewById(R.id.button_breath_start);
        recordingTimer = new RecordingTimer(duration, 1);
        startButton.setOnClickListener(view -> {
            if (started) {
                startButton.setText(R.string.button_start_text);
                timerText.setVisibility(View.INVISIBLE);
                infoText.setVisibility(View.VISIBLE);
                graphView.removeAllSeries();
                graphView.setVisibility(View.INVISIBLE);
                recordingTimer.cancel();
                resetSessionParameters();
            } else {
                startButton.setText(R.string.cancel_breath_text);
                timerText.setVisibility(View.VISIBLE);
                infoText.setVisibility(View.INVISIBLE);
                graphView.clearAnimation();
                setupGraphView();
                signalProcessor = new SignalProcessor(30);
                graphView.setVisibility(View.VISIBLE);
                recordingTimer.start();
                started = true;
            }
        });
    }

    private void resetSessionParameters() {
        data = null;
        breathCount = 0;
        increasing = false;
        window = null;
        windowLength = 0;
        windowSum = 0;
        counter = 0;
        movingAverage = 0;
        prevMovingAverage = 0;
        started = false;
        signalProcessor = null;
        recordingTimer.cancel();
    }

    public class RecordingTimer extends CountDownTimer {
        public RecordingTimer(long seconds, long countDownIntervalInSeconds) {
            super(seconds * 1000, countDownIntervalInSeconds * 1000);
        }

        @Override
        public void onTick(long l) {
            int seconds = (int) l / 1000;
            timerText.setText(Integer.toString(seconds));
        }

        @Override
        public void onFinish() {
            Log.d(logTagName, "OnFinish timer called");
            timerText.setText(Integer.toString(0));
            breathCount = signalProcessor.getNumberOfPeaks();
            Intent breathRateReturnIntent = new Intent();
            breathRateReturnIntent.putExtra(Constants.BREATH_RATE, breathCount);
            setResult(Activity.RESULT_OK, breathRateReturnIntent);

            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(2000, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(2000);
            }
            finish();
        }
    }


    private void setupGraphView() {
        series = new LineGraphSeries<>();
        series.setColor(Color.rgb(250, 174, 75));
        graphView.addSeries(series);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(50);
        graphView.setBackgroundColor(Color.BLACK);
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        gridLabelRenderer.setVerticalLabelsColor(Color.TRANSPARENT);
        gridLabelRenderer.setHorizontalLabelsColor(Color.TRANSPARENT);
        graphView.setTitle("Respiratory Rate");
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY) {
            if (started)
                getAccelerometerData(sensorEvent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    private void getAccelerometerData(SensorEvent sensorEvent) {
        float[] values = sensorEvent.values;

        double z = values[2];

        addDataToSeries(z);
        signalProcessor.addData(z);
    }

    private void addDataToSeries(double z) {
        counter++;
        DataPoint point = new DataPoint(counter, z);
        this.series.appendData(point, true, 50);

        if (data == null)
            data = new ArrayList<>();

        data.add(z);
    }


    @Override
    protected void onResume() {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        sensorManager.unregisterListener(this);
        recordingTimer.cancel();
    }
}