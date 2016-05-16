/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * @author amazhurin
 */

// An interface to be implemented by everyone interested in data events
interface DataConsumer {
    void OnData(DataSource source, DataSample sample);
}

abstract public class DataSource extends Thread {
    
    private List<DataConsumer> listeners = new ArrayList<DataConsumer>();
    
    protected Date last_processed_date;

    public void addListener(DataConsumer v) {
        listeners.add(v);
    }

    protected void OnData(DataSample sample) {
        if(sample == null)
            return;
        last_processed_date = sample.date;
        for (DataConsumer hl : listeners)
            hl.OnData(this, sample);
    }
    
    protected Date dateStart;
    protected boolean monitoring;
    
    public String name;
    
    @Override
    public void run() {
    }
    
    protected DataSample read_data(String line){
        if(line == null || line.length() < 3)
            return null;
        try{
            String[] parts = line.split(",");
            String stime = parts[0];

            DataSample result = new DataSample();

            result.timestamp = Long.parseLong(stime);

            result.date = new java.util.Date((long)result.timestamp*1000);
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            c.setTime(result.date);
            result.data = new double[parts.length-1+2];

            result.data[0] = c.get(Calendar.DAY_OF_WEEK);        

            // hour
            result.data[1] = c.get(Calendar.HOUR);

            for(int i = 2; i < result.data.length; i++){
                if(i-1 >= parts.length)
                    break;
                double value = Double.parseDouble(parts[i-1]);
                result.data[i] = value;
            }
            return result;
        } catch(Exception ex){
            return null;
        }
    }
    
    protected ArrayList<String> tail2( File file, int lines) {
        ArrayList<String> result = new ArrayList<>();
        java.io.RandomAccessFile fileHandler = null;
        try {
            fileHandler = 
                new java.io.RandomAccessFile( file, "r" );
            long fileLength = fileHandler.length() - 1;
            StringBuilder sb = new StringBuilder();
            int line = 0;
            long filePointer = fileLength;
            
            for(; filePointer != -1; filePointer--){
                fileHandler.seek( filePointer );
                int readByte = fileHandler.readByte();

                 if( readByte == 0xA ) {
                    if (filePointer < fileLength) {
                        line = line + 1;
                        result.add(0, sb.reverse().toString());
                        sb = new StringBuilder();
                    }
                } else if( readByte == 0xD ) {
                    if (filePointer < fileLength-1) {
                        String s = sb.reverse().toString();
                        if(s.compareTo("\n") != 0){
                            line = line + 1;
                            result.add(0, s);
                        }
                        sb = new StringBuilder();
                    }
                }
                if (line >= lines) {
                    break;
                }
                sb.append( ( char ) readByte );
            }
            if(filePointer < 0 && sb.length() > 0)
                result.add(0, sb.reverse().toString());
            return result;
        } catch( java.io.FileNotFoundException e ) {
            //e.printStackTrace();
            return result;
        } catch( java.io.IOException e ) {
            //e.printStackTrace();
            return result;
        }
        finally {
            if (fileHandler != null )
                try {
                    fileHandler.close();
                } catch (IOException e) {
                }
        }
    }    

    
    
    /**
     * Start streaming new data entries to the listeners
     * 
     * @param dateStart the date to start 
     * @param monitoring start_monitoring if true, continue monitoring the file(s) and 
     * call OnData() when new data added to the file(s)
     */
    public DataSource(Date dateStart, boolean monitoring){
        this.dateStart = dateStart;
        this.monitoring = monitoring;
    }
    
}
