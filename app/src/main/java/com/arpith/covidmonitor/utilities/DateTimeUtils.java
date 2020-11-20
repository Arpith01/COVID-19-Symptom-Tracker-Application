package com.arpith.covidmonitor.utilities;

import java.util.Date;

public class DateTimeUtils {
    public static final String getEpochFromDate(Date date){
        long millis = date.getTime();
        long epoch = millis/1000;
        return String.valueOf(epoch);
    }
}
