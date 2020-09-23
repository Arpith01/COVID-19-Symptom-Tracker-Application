package com.arpith.covidmonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.VideoCapture;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class HeartRateMonitor extends AppCompatActivity {

    private int REQUEST_CODE_PERMISSIONS = 0;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    TextureView textureView;
    VideoCapture videoCapture;
    TextView timerText;
    RecordingTimer recordingTimer;
    GraphView graphView;
    LineGraphSeries<DataPoint> series;
    List<Double> data;
    static long startTime = 0;
    int duration = 45;
    int counter;
    MaterialButton startButton;
    boolean analysisStarted;
    SignalProcessor signalProcessor;
    MaterialTextView heartInfoTextView;
    ImageAnalysis imageAnalysis;
    Preview preview;
    double movingAverage;
    double windowSum;
    int windowLength;
    Deque<Double> window;
    double prevMovingAverage;
    boolean increasing;
    int heartBpm;

    public static final String logTagName = HeartRateMonitor.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_rate_monitor);

        textureView = findViewById(R.id.view_finder);
        timerText = findViewById(R.id.breath_timer_text);
        graphView = findViewById(R.id.pulse_graph_view);
        startButton = findViewById(R.id.button_heart_rate_start);
        heartInfoTextView = findViewById(R.id.heart_info_textView);

        Intent receivedIntent = getIntent();
        duration = receivedIntent.getIntExtra(Constants.TIMER_DURATION, 45);

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        recordingTimer = new RecordingTimer(duration, 1);
        startButton.setOnClickListener(view -> {
            if (analysisStarted) {
                startButton.setText(R.string.button_start_text);
                timerText.setVisibility(View.INVISIBLE);
                heartInfoTextView.setVisibility(View.VISIBLE);
                graphView.removeAllSeries();
                resetSessionParameters();
                graphView.setVisibility(View.INVISIBLE);
                imageAnalysis.removeAnalyzer();
            } else {
                startButton.setText(R.string.cancel_breath_text);
                timerText.setVisibility(View.VISIBLE);
                heartInfoTextView.setVisibility(View.INVISIBLE);
                graphView.clearAnimation();
                setupGraphView();
                graphView.setVisibility(View.VISIBLE);
                signalProcessor = new SignalProcessor(10);
                recordingTimer.start();
                analysisStarted = true;
                imageAnalysis = startAnalysis();
                CameraX.bindToLifecycle(this, imageAnalysis);
                recordingTimer.start();
            }
        });
    }

    private void resetSessionParameters(){
        data = null;
        heartBpm = 0;
        increasing = false;
        window = null;
        windowLength = 0;
        windowSum = 0;
        counter = 0;
        movingAverage = 0;
        prevMovingAverage = 0;
        analysisStarted = false;
        signalProcessor = null;
        recordingTimer.cancel();
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {

        CameraX.unbindAll();

        Rational aspectRatio = new Rational(textureView.getWidth(), textureView.getHeight());
        Size screen = new Size(textureView.getWidth(), textureView.getHeight()); //size of the screen


        PreviewConfig pConfig = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                (Preview.PreviewOutput output) -> {
                    ViewGroup parent = (ViewGroup) textureView.getParent();
                    parent.removeView(textureView);
                    parent.addView(textureView, 0);
                    textureView.setSurfaceTexture(output.getSurfaceTexture());
                });

        imageAnalysis = startAnalysis();
        CameraX.bindToLifecycle(this, preview);
        preview.enableTorch(true);
    }

    private ImageAnalysis startAnalysis() {
        HandlerThread handlerThread = new HandlerThread("RGB Thread");
        handlerThread.start();

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder().
                setTargetResolution(new Size(1280, 720)).
                setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE).
                setCallbackHandler(new Handler(handlerThread.getLooper())).setImageQueueDepth(1).build();
        ImageAnalysis imageAnalysis = new ImageAnalysis(imageAnalysisConfig);
        imageAnalysis.setAnalyzer((image, rotationDegrees) -> {
            final Bitmap bitmap = textureView.getBitmap();


            if (bitmap == null) {
                return;
            }


            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int length = width * height;

            int[] pixels = new int[width * height];
            bitmap.getPixels(pixels, 0, width, 0, 0,
                    width, height);

            int sumRed = 0;
            for (int color :
                    pixels) {
                sumRed += (color >> 16 & 0xFF);
            }

            double averageRed = (double) sumRed / length;
            addDataToSeries(averageRed);
            signalProcessor.addData(averageRed);
        });

        return imageAnalysis;
    }

    @Override
    protected void onPause() {
        super.onPause();
        recordingTimer.cancel();
    }

    private void setupGraphView() {
        series = new LineGraphSeries<>();
        series.setColor(Color.rgb(255, 0, 0));
        graphView.addSeries(series);
        graphView.getViewport().setScrollable(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(120);
        graphView.setBackgroundColor(Color.BLACK);
        GridLabelRenderer gridLabelRenderer = graphView.getGridLabelRenderer();
        gridLabelRenderer.setGridStyle(GridLabelRenderer.GridStyle.NONE);
        gridLabelRenderer.setVerticalLabelsColor(Color.TRANSPARENT);
        gridLabelRenderer.setHorizontalLabelsColor(Color.TRANSPARENT);
        graphView.setTitle("Heart Rate");
    }

    private void addDataToSeries(double redValue) {
        counter++;
        long currentTime = System.currentTimeMillis();
        double timeFromStart = ((currentTime - startTime) * 30.0) / 1000;
        DataPoint point = new DataPoint(timeFromStart, redValue);
        if (data == null)
            data = new ArrayList<>();
        data.add(redValue);
        this.series.appendData(point, true, 120);
    }

    @SuppressLint("RestrictedApi")
    protected void startRecording() {
        File videoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/temp_video.mp4");
        recordingTimer = new RecordingTimer(15, 1);
        Log.i(logTagName, "Video recording started");
        recordingTimer.start();

        videoCapture.startRecording(videoFile, new VideoCapture.OnVideoSavedListener() {
            @Override
            public void onVideoSaved(@NonNull File file) {
                String message = "Video saved at " + videoFile.getAbsolutePath();
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                Log.i(logTagName, message);
            }

            @Override
            public void onError(@NonNull VideoCapture.VideoCaptureError videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                String errorMessage = "Error capturing video. " + message;
                Log.e(logTagName, errorMessage);
            }
        });
    }

    public class RecordingTimer extends CountDownTimer {

        public RecordingTimer(long seconds, long countDownIntervalInSeconds) {
            super(seconds * 1000, countDownIntervalInSeconds * 1000);
        }

        @Override
        public void onTick(long l) {
            int seconds = (int) l / 1000;
            timerText.setText(Integer.toString(seconds));
            Log.d(logTagName, "Heart rate: " + heartBpm);
        }

        @Override
        public void onFinish() {
            Log.d(logTagName, "OnFinish timer called");
            timerText.setText(Integer.toString(0));
            heartBpm = signalProcessor.getNumberOfPeaks();
            Intent returnHeartRateIntent = new Intent();
            returnHeartRateIntent.putExtra(Constants.HEART_RATE, heartBpm);
            setResult(Activity.RESULT_OK, returnHeartRateIntent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(logTagName, "All required permissions granted.");
                startCamera();
            } else {
                Log.w(logTagName, "Required permissions not granted");
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
                finish();
            }
        }
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


}