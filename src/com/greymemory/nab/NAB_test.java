/*
 * Copyright (c) 2016 Anton Mazhurin to present
 */
package com.greymemory.nab;

import com.greymemory.evolution.Evolver;
import com.greymemory.evolution.Individual;
import com.greymemory.evolution.Population;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author AMazhurin
 */
public class NAB_test {
    class Task implements Callable{
        private Individual individual;
        public Task(Individual individual){
            this.individual = individual;
        }
        
        @Override
        public String call() throws Exception {
            individual.calculate_cost();
            return "";
        }
        
    }
    
    public void runScore()  {
        Labels labels = new Labels();
        labels.Read("../NAB/labels/combined_windows.json");
        for(int i = 0; i < labels.files.size(); i++){
            System.out.print("_");
        }
        System.out.println("100%");
        
        ExecutorService pool= Executors.newFixedThreadPool(8);
        Set<Callable<String>> callables;
        callables = new HashSet<>();

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
                callables.add(new NAB_test.Task(individual));
            } catch (IOException ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            } catch (java.lang.OutOfMemoryError ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        
        try {
            pool.invokeAll(callables);
        } catch (InterruptedException ex) {
            Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        System.out.println("\nDone.");
        pool.shutdown(); // Disable new tasks from being submitted
    }
        
    public void run1()  {
        Labels labels = new Labels();
        labels.Read("../NAB/labels/combined_windows.json");
        for(int i = 0; i < labels.files.size(); i++){
            System.out.print("_");
        }
        System.out.println("100%");

        
        for(int i = 0; i < labels.files.size(); i++){
            String result_file_name = "../NAB/results/greymemory/";
            String [] split = labels.files.get(i).file_name.split("/");
            result_file_name +=  split[0];
            result_file_name += "/greymemory_";
            result_file_name += split[1];
            
            //if(!split[1].equals("art_daily_flatmiddle.csv")) continue;
            if(!split[1].equals("TravelTime_387.csv")) continue;
            //if(!split[1].equals("Twitter_volume_UPS.csv")) continue;
            
            /*
            IndividualAnomalyNAB individual;
            try {
                individual = new IndividualAnomalyNAB(
                        "../NAB/data/" + labels.files.get(i).file_name,
                        null,
                        null, 0, 0, 0);
                individual.set_anomalies(labels.files.get(i).anomalies);
                
                Evolver evolver = new Evolver();
                Individual best = evolver.Evolve(individual);
                
            } catch (IOException ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            } catch (java.lang.OutOfMemoryError ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            
             
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
            } catch (java.lang.OutOfMemoryError ex) {
                Logger.getLogger(NAB_test.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
        System.out.println("\nDone.");
        
        

    }

    public static void main(String[] args)  {
        System.out.println("Greymemory anomaly detector. 2016. \nNumenta Anomaly Benchmark test. ");
        NAB_test test = new NAB_test();
        
        test.runScore();
        //test.run1();
        
     }
}
