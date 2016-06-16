/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.greymemory.nab;

import com.greymemory.anomaly.DataSample;
import com.greymemory.anomaly.DataSource;
import com.greymemory.anomaly.DataSourceCSV;
import com.greymemory.anomaly.IndividualAnomaly;
import com.greymemory.evolution.Gene;
import com.greymemory.nab.Labels.NAB_Anomaly;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anton
 */
public class IndividualAnomalyNAB extends IndividualAnomaly {
    public IndividualAnomalyNAB(String input_file, String log_file, Date start_from, double max_error, double median, int max_samples) throws FileNotFoundException, IOException {
        super(input_file, log_file, start_from, max_error, median, max_samples);
        
       
        // pre process file
        BufferedReader reader = new BufferedReader(new FileReader(input_file));
        int num_samples = 0;
        String line;
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        
        // read the historic data from the files
        while ((line = reader.readLine()) != null) {
            if(num_samples == 0) {
                num_samples++;
                continue;
            }
            String[] parts = line.split(",");
            Float v = Float.parseFloat(parts[1]);
            if(v > max)
                max = v;
            if(v < min)
                min = v;
            num_samples++;
        }            
        reader.close();
        float range = max - min;
        
        genome.genes.add(new Gene("resolution", range / 10000, range / 100, range / 1000));
        genome.genes.add(new Gene("activation", range / 10000, range / 100, 40*range / 1000));
        
        genome.genes.add(new Gene("activation_day_of_week", 0f, 7f, 2f));
        genome.genes.add(new Gene("activation_hour", 1f, 24f, 6f));
        
        genome.genes.add(new Gene("num_hard_locations", 7f, 90f, 76f));
        genome.genes.add(new Gene("window", 1f, 9f, 2f));
        genome.genes.add(new Gene("forgetting_rate", 1000f, 5000f, num_samples / 10));
        
        genome.genes.add(new Gene("anomaly_window", 1000f, 5000f, 50));
        
        genome.genes.add(new Gene("training_period", 1000f, 1000f, 100));
        
    }
    
    protected DataSource create_data_source(){
        DataSource data_source = new DataSourceCSV(
                start_from, 
                false, // monitoring
                input_file);
        
        data_source.set_NAB_format(true);
        
        // create an empty log file
        BufferedWriter writer_log;
        try {
            writer_log = new BufferedWriter(new FileWriter(new File(log_file), true));
            PrintWriter o = new PrintWriter(writer_log);
            o.printf("timestamp,value,anomaly_score,label,prediction,error\n");
            writer_log.close();
        } catch (IOException ex) {
            Logger.getLogger(IndividualAnomalyNAB.class.getName()).log(Level.SEVERE, null, ex);
        }
                
        return data_source;
    }
    
    protected void log_results(DataSample sample) {
        if(log_file == null || log_file.length() == 0)
            return;
        
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            
            float label = 0.0f;
            for(NAB_Anomaly a : anomalies){
                if(sample.date.after(a.start) && sample.date.before(a.stop)) {
                    label = 1.0f;
                    break;
                }                
            }
            
            BufferedWriter writer_log;
            writer_log = new BufferedWriter(new FileWriter(new File(log_file), true));
            PrintWriter o = new PrintWriter(writer_log);
            o.printf("%s, %f, %f, %f, %f, %f\n", 
                    df.format(sample.date),
                    input_average.get_last_value(),
                    anomaly_rate,
                    label,
                    predicted_value,
                    error);
            writer_log.close();
        } catch (IOException ex) {
            Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private ArrayList<Labels.NAB_Anomaly> anomalies = null;

    /**
     * @param labels the labels to set
     */
    public void set_anomalies(ArrayList<Labels.NAB_Anomaly> anomalies) {
        this.anomalies = anomalies;
    }
    
    
}
