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
import com.greymemory.evolution.Individual;
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
        this.max_error = range;
        this.max_storage_size_in_mb = 2000;
        
        this.average_anomaly = 5;
        this.averate_input = 5;
        
        genome.genes.add(new Gene("average_input", 1, 40, 41));
        genome.genes.add(new Gene("average_anomaly", 1, 20, 1));
        
        genome.genes.add(new Gene("resolution", range / 300, range / 20, range / 128)); // 1.8
        genome.genes.add(new Gene("activation", 15, 100, 51));
        
        genome.genes.add(new Gene("activation_day_of_week", 6f, 7f, 6f));
        genome.genes.add(new Gene("activation_hour", 18f, 24f, 27f));
        
        genome.genes.add(new Gene("num_hard_locations", 27f, 90f, 27f));
        genome.genes.add(new Gene("window", 1f, 2f, 2.52f));
        genome.genes.add(new Gene("forgetting_rate", 10, 2500, 10));
        
        genome.genes.add(new Gene("anomaly_window", 50, 700 / 3, 283));
        
        genome.genes.add(new Gene("training_period", 100f, 500f, 387));
        
        
    }
    
    @Override
    public Individual create() {
        IndividualAnomalyNAB individual = null;
        try {
            individual = new IndividualAnomalyNAB(input_file, log_file,
                    null, 0, 0, 0);
            individual.set_anomalies(anomalies);
        } catch (IOException ex) {
            Logger.getLogger(IndividualAnomalyNAB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return individual;
    }

    public Individual create_clone() {
        IndividualAnomalyNAB individual;
        try {
            individual = new IndividualAnomalyNAB(input_file, log_file,
                    null, 0, 0, 0);
            individual.genome = genome.create_clone();
            individual.set_anomalies(anomalies);
            return individual;
        } catch (IOException ex) {
            Logger.getLogger(IndividualAnomalyNAB.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    
    protected DataSource create_data_source(){
        DataSource data_source = new DataSourceCSV(
                start_from, 
                false, // monitoring
                input_file);
        
        data_source.set_NAB_format(true);
        
        if(log_file != null){
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
        }
                
        return data_source;
    }
    

    private int true_positive;
    private int true_negative;
    private int false_positive;
    private int false_negative;
    
    @Override
    public void calculate_cost() {
        
        DataSource data_source = null;

        true_positive = 0;
        true_negative = 0;
        false_positive = 0;
        false_negative = 0;
        try {
            create_xdm();
            set_cost(Double.MAX_VALUE);
            
            clear_log();

            data_source = create_data_source();
            data_source.addListener(this);
            data_source.start();
            data_source.join();
        } catch (InterruptedException ex) {
            //Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
            if(data_source != null){
                try {
                    data_source.interrupt();
                    data_source.join();
                } catch (InterruptedException ex1) {
                    //Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
        } finally{
        }
        
        float precision = true_positive * 1.0f / (true_positive + false_positive);
        float recall = true_positive * 1.0f/ (true_positive + false_negative);
        
        float f1_score = 2.0f * (precision*recall)/(precision+recall);
        set_cost(1.0f - f1_score);
        
        clear_xdm();
        
        System.out.printf("*");
    }

    protected void log_results(DataSample sample) {
        try {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            float label = 0.0f;
            for(NAB_Anomaly a : anomalies){
                if(sample.date.after(a.start) && sample.date.before(a.stop)) {
                    label = 1.0f;
                    break;
                }                
            }
            
            boolean anomaly = anomaly_rate >= threshold;
            boolean ground_truth = label > 0.9f;
            
            if(anomaly && ground_truth)
                true_positive++;
            else if(anomaly && !ground_truth)
                false_positive++;
            else if(!anomaly && ground_truth)
                false_negative++;
            else 
                true_negative++;
            
            if(log_file == null || log_file.length() == 0)
                return;
        
            BufferedWriter writer_log;
            writer_log = new BufferedWriter(new FileWriter(new File(log_file), true));
            PrintWriter o = new PrintWriter(writer_log);
            o.printf("%s,%f,%f,%f,%f,%f\n", 
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
