/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
    
/**
 *
 * @author amazhurin
 */
public class DataSample {
    public long timestamp;
    public Date date;
    public double[] data;
    
    public String get_date_UTC(){
        
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar c = Calendar.getInstance(timeZone);
        c.setTimeInMillis((long)timestamp*1000);
        SimpleDateFormat sdf = 
               new SimpleDateFormat("EE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        sdf.setTimeZone(timeZone);
        return sdf.format(c.getTime());  
    }
}
