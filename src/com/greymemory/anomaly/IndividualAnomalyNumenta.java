/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.greymemory.anomaly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author anton
 */
public class IndividualAnomalyNumenta extends IndividualAnomaly {
    
    public IndividualAnomalyNumenta(String input_file, String log_file, Date start_from, double max_error, double median, int max_samples) {
        super(input_file, log_file, start_from, max_error, median, max_samples);
    }
    
    protected DataSource create_data_source(){
        DataSource data_source = new DataSourceCSV(
                start_from, 
                false, // monitoring
                input_file);
        
        data_source.set_NAB_format(true);
                
        return data_source;
    }
    
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
    
    
}
