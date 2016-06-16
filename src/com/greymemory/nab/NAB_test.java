/*
 * Copyright (c) 2016 Anton Mazhurin to present
 */
package com.greymemory.nab;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AMazhurin
 */
public class NAB_test {
     public static void main(String[] args)  {
        System.out.println("Greymemory anomaly detector. 2016. \nNumenta Anomaly Benchmark test. ");
        
        Labels labels = new Labels();
        labels.Read("../NAB/labels/combined_windows.json");
        
        for(int i = 0; i < labels.files.size(); i++){
            String result_file_name = "../NAB/results/greymemory/";
            String [] split = labels.files.get(i).file_name.split("/");
            result_file_name +=  split[0];
            result_file_name += "/greymemory_";
            result_file_name += split[1];
            
            IndividualAnomalyNAB individual;
            try {
                individual = new IndividualAnomalyNAB(
                        "../NAB/data/" + labels.files.get(i).file_name,
                        result_file_name,
                        null, 0, 0, 0);
                individual.set_anomalies(labels.files.get(i).anomalies);
                individual.calculate_cost();
            } catch (IOException ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
        
        
     }
}
