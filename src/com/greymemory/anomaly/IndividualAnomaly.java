/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;
import com.greymemory.anomaly.Anomaly;
import com.greymemory.anomaly.AnomalyCalculator;
import com.greymemory.anomaly.MovingAverage;
import com.greymemory.core.Sample;
import com.greymemory.core.SliderRead;
import com.greymemory.core.SliderWrite;
import com.greymemory.core.XDM;
import com.greymemory.core.XDMParameters;
import com.greymemory.evolution.Gene;
import com.greymemory.evolution.Individual;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author alm
 */
public class IndividualAnomaly extends Individual implements DataConsumer  {
    private List<AnomalyConsumer> listeners = new ArrayList<AnomalyConsumer>();
    public void addListener(AnomalyConsumer v) {
        listeners.add(v);
    }

    public String input_file;
    protected String log_file;

    private XDM xdm;
    private SliderWrite trainer;
    private SliderRead predictor;
    private AnomalyCalculator anomaly_calculator;
    private SliderRead.FutureSample prediction = null;
    private long num_samples;
    protected double anomaly_rate;
    protected double predicted_value = 0f;
    protected double error = 0f;
    private MovingAverage anomaly_average;
    protected MovingAverage input_average;
    private double total_error;
    int num_total_error;
    
    double max_error = 1;
    double median = 1.0;
    
    public double prediction_rate;
    
    public double threshold = 0.95;
    public int averate_anomaly = 9;
    public int averate_input = 10;
    public String channel;
    
    public int max_samples = 01;
    protected Date start_from;
    
    public IndividualAnomaly(String input_file, String log_file,
            Date start_from, double max_error, double median, int max_samples){
        this.input_file = input_file;
        this.log_file = log_file;
        this.max_error = max_error;
        this.median = median;
        this.max_samples = max_samples;
        this.start_from = start_from;
    }

    @Override
    public Individual create() {
        Individual individual = new IndividualAnomaly(input_file, log_file,
            start_from, max_error, median, max_samples);
        return individual;
    }

    public void clear_log(){
        if(log_file != null && log_file.length() > 0){
            try {
                Files.delete(Paths.get(log_file));
            } catch (IOException ex) {
                //Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public void clear_xdm(){
        if(xdm != null){
            xdm.clear();
            xdm = null;
            trainer = null;
            predictor = null;
            System.gc();
        }
    }
    
    public void create_xdm() throws Exception{
        XDMParameters param;
        param = new XDMParameters();
        
        param.window = new int[(int)genome.get_gene("window").value];
        param.predict = true;
        param.num_channels = 3;
        param.num_channels_prediction = 1;
        param.max_storage_size_in_mb = 2000;

        param.activation_radius = new double[param.num_channels];
        param.resolution = new double[param.num_channels];
        param.medians = new double[param.num_channels];
        param.prediction_radius = new double[param.num_channels_prediction];
        
        // value
        param.resolution[0] = genome.get_gene("resolution").value;
        param.activation_radius[0] = param.resolution[0] * 
                    genome.get_gene("activation").value;
        param.prediction_radius[0] = param.activation_radius[0] * 20;
        param.medians[0] = median; 
        
        // hour
        param.resolution[1] = 1;
        param.activation_radius[1] = 
                genome.get_gene("activation_hour").value;
        param.medians[1] = 12; 

        // day of week
        param.resolution[2] = 1;
        param.activation_radius[2] = 
                genome.get_gene("activation_day_of_week").value;
        param.medians[2] = 3; 
        
        param.min_num_hard_location = 
                (int)genome.get_gene("num_hard_locations").value;
        
        param.forgetting_rate = (int)genome.get_gene("forgetting_rate").value;
        
        xdm = new XDM(param);
        
        trainer = new SliderWrite(xdm);
        predictor = new SliderRead(xdm);        
        anomaly_calculator = new AnomalyCalculator(
                (int)genome.get_gene("anomaly_window").value);
        num_samples = 0;
        
        anomaly_average = new MovingAverage(averate_anomaly);
        input_average = new MovingAverage(averate_input);
        total_error = 0;
        num_total_error = 0;
        
        prediction_rate = 0;
        num_predicted = 0;
    }
    
    private double prev_sample = 0.0;
    private long num_predicted;
    
    protected void log_results(DataSample sample) {
        //exception handling left as an exercise for the reader
        
        if(log_file == null || log_file.length() == 0)
            return;
        
        try {
            BufferedWriter writer_log;
            writer_log = new BufferedWriter(new FileWriter(new File(log_file), true));
            PrintWriter o = new PrintWriter(writer_log);
            o.printf("%s, %d, %f, %f, %f, %f, %f\n", 
                    sample.get_date_UTC(),
                    sample.timestamp, 
                    //sample.date,
                    //sample.data[2], 
                    input_average.get_average(),
                    
                    //prediction != null ? prediction.data[0] : 0,
                    predicted_value,
                    
                    error,
                    anomaly_rate,
                    anomaly_rate > threshold ? 1f : 0f);
            if(log_file.length() > 0)
                writer_log.close();
        } catch (IOException ex) {
            Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
    @Override
    public void OnData(DataSource source, DataSample sample) {
        if(sample.data.length < 3)
            return;
        num_samples++;
        
        if(num_samples % (288*5) == 0 && log_file != null && log_file.length() > 0)
            System.out.printf("%s\n", sample.date.toString());
                    
        if(max_samples > 0 && num_samples > max_samples)
            return;
        
        double[] data = new double[3];
        data[0] = sample.data[2];
        data[1] = sample.data[1];
        data[2] = sample.data[0];
        
        //data[0] = num_samples*100 % 3000;
        if(Double.isNaN(data[0]))
            return;

        input_average.add(data[0]);
        data[0] = input_average.get_average();
               
        try {
            if(num_samples > genome.get_gene("training_period").value){
                error = max_error;
                predicted_value = 0f;
                
                if(prediction != null && prediction.error == Sample.Error.OK){
                    predicted_value = prediction.data[0];
                    error = data[0] - predicted_value;
                    num_predicted++;
                } 
                /*
                else {
                    predicted_value = prev_sample;
                    error = data[0] - predicted_value;
                } */
                
                prev_sample = data[0];

                total_error += error*error;
                num_total_error++;
                        
                anomaly_calculator.process(error);
                
                double new_rate = anomaly_calculator.get_anomaly();
                anomaly_average.add(new_rate);
                anomaly_rate = anomaly_average.get_average();
            }

            // train
            trainer.train(data, 0);
            
            log_results(sample);
            
            process(sample);

            // predict
            predictor.process(data);
            
            prediction = predictor.predict();
        
           
        } catch (Exception ex) {
            Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    protected DataSource create_data_source(){
        DataSource data_source = new DataSourceCSV(
                start_from, 
                true, // monitoring
                input_file);
        return data_source;
    }
            

    @Override
    public void calculate_cost() {
        
        DataSource data_source = null;
        
        try {
            create_xdm();
            set_cost(Double.MAX_VALUE);
            
            data_source = create_data_source();

            data_source.addListener(this);

            clear_log();

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
        if(num_total_error > 0)
            set_cost(Math.sqrt(total_error / num_total_error));
        
        prediction_rate = num_predicted * 1.0 / num_total_error;
                
        clear_xdm();
        
        System.out.printf("*");
 
    }

    /**
     * @return the log_file
     */
    public String getLog_file() {
        return log_file;
    }

    /**
     * @param log_file the log_file to set
     */
    public void setLog_file(String log_file) {
        this.log_file = log_file;
    }
    
    private void broadcast(Anomaly anomaly){
        for (AnomalyConsumer hl : listeners){
            hl.OnAnomaly(anomaly);
        }
    }
    
    private boolean current_anomaly = false;
    
    private void process(DataSample sample){
        if(anomaly_rate > threshold){
            if(!current_anomaly){
                current_anomaly = true;
                
                Anomaly anomaly = new Anomaly();
                anomaly.sample = sample;
                anomaly.anomaly_rate = anomaly_rate;
                anomaly.start = true;
                broadcast(anomaly);
            }
        } else {
            if(current_anomaly){
                current_anomaly = false;
                
                Anomaly anomaly = new Anomaly();
                anomaly.sample = sample;
                anomaly.anomaly_rate = anomaly_rate;
                anomaly.start = false;
                broadcast(anomaly);
            }
        }
    }

    
}
