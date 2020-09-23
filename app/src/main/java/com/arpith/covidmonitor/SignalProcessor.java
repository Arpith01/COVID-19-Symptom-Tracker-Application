package com.arpith.covidmonitor;

import java.util.ArrayDeque;
import java.util.Deque;

public class SignalProcessor {
    private double currentAverage;
    private double previousAverage;
    int windowLength;
    int lag;
    private double windowSum;
    private Deque<Double> window;
    private int peakCount;
    private boolean increasing;

    public SignalProcessor(int lag) {
        this.lag = lag;
    }

    public static final String logTagName = SignalProcessor.class.getSimpleName();

    public void addData(double dataPoint) {

        if (window == null){
            window = new ArrayDeque<>();
        }
        window.add(dataPoint);
        processAddedData(dataPoint);
    }

    private void processAddedData(double dataPoint) {
        windowLength += 1;
        windowSum += dataPoint;
        if(windowLength>lag) {
            windowSum-=window.remove();
            windowLength-=1;
            previousAverage = currentAverage;
            currentAverage = windowSum / windowLength;

            if(currentAverage > previousAverage){
                if(!increasing){
                    peakCount++;
                }
                increasing = true;
            } else if (currentAverage < previousAverage){
                if(increasing){
                    peakCount++;
                }
                increasing = false;
            }
        }
    }

    public int getNumberOfPeaks(){
        return peakCount/2;
    }
}
