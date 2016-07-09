/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amazhurin
 */
public class DataSourceCSV2 extends DataSource {

    private String file1;
    private String file2;
    
    /**
     *
     * @param file1
     * @param file2
     */
    public DataSourceCSV2(
            Date dateStart, boolean monitoring,
            String file1, String file2){
        super(dateStart, monitoring);
        this.file1 = file1;
        this.file2 = file2;
    }
    
    @Override
    public void run() {
        BufferedReader reader1 = null;
        BufferedReader reader2 = null;
        String line1;
        String line2;
        DataSample sample1;
        DataSample sample2;
        double[] data1 = new double[3];
        double[] data2 = new double[3];

        try {
            reader1 = new BufferedReader(new FileReader(file1));
            if(file2.length() > 0)
                reader2 = new BufferedReader(new FileReader(file2));
            
            // read the historic data from the files
            while ((line1 = reader1.readLine()) != null) {
                if(Thread.interrupted()) break;
                sample1 = read_data(line1, false);
                if(dateStart != null && sample1.date.before(dateStart))
                    continue;
                
                if(file2.length() > 0){
                    line2 = reader2.readLine();
                    if(line2 == null) 
                        break;
                    sample2 = read_data(line2,false);

                    if(!sample2.date.equals(sample1.date)){
                        // sync
                        while ((line2 = reader2.readLine()) != null) {
                            if(Thread.interrupted())break;
                            sample2 = read_data(line2, false);
                            if(sample2.date.equals(sample1.date))
                                break;
                        }
                    } 
                    sample1.data[2] = sample1.data[2]/sample2.data[2];
                    OnData(sample1, this);
                } else {
                    OnData(sample1, this);
                }
                
            }            
            
            if(file2.length() > 0){
                reader2.close();
            }
            reader1.close();
            if(!monitoring)
                return;
            
            long file_size_1 = (new File(file1)).length();
            long file_size_2 = 0;
            
            if(file2.length() > 0){
                file_size_2 = (new File(file2)).length();
            }
            
            while(true){
                if(Thread.interrupted())
                    break;
                Thread.sleep(600);
                
                long new_file_size_1 = (new File(file1)).length();
                long new_file_size_2 = (new File(file2)).length();
                
                if(new_file_size_1 != file_size_1 
                        //&& new_file_size_connections != file_size_connections
                        ){

                    int num_to_check = 10;
                    ArrayList<String> new_1 = tail2(new File(file1), num_to_check);
                    int index_start = -1;
                    for(int i = 0; i < new_1.size(); i++){
                        DataSample new_Sample1;
                        if(line1 == null || line1.length() == 0)
                            continue;
                        
                        new_Sample1 = read_data(line1, false);
                        if(new_Sample1 == null || new_Sample1.date.before(dateStart))
                            continue;
                        index_start = i;
                    }                        
                    
                    ArrayList<String> new_2 = tail2(new File(file2), num_to_check);

                    if(file2.length() > 0){
                        for(int i = index_start; i < new_1.size(); i++){
                            DataSample new_Sample1 = read_data(new_1.get(i), false);
                            DataSample new_Sample2 = read_data(new_2.get(i), false);
                            new_Sample1.data[2] = new_Sample1.data[2]/
                                    new_Sample2.data[2];
                            OnData(new_Sample1, this);
                        }
                    } else {
                        for(int i = index_start; i >= 0 && i < new_1.size(); i++){
                            line1 = new_1.get(i);
                            if(line1 == null || line1.length() == 0)
                                continue;
                            
                            DataSample new_Sample1 = read_data(line1, false);
                            OnData(new_Sample1, this);
                        }
                    }
                }
                
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DataSourceCSV2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DataSourceCSV2.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
        }
    }
    
}
