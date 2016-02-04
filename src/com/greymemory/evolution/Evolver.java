/*
 * Copyright (c) 2015 Mindmick Corp.
 *  * 
 */
package com.greymemory.evolution;

import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amazhurin
 */
public class Evolver {
    public Individual Evolve(Individual individual){
        Scanner in = new Scanner(System.in);
        Random rnd = new Random(33);
        
        System.out.print("Enter population size : ");
        String s = in.nextLine();
        int population_size = Integer.parseInt(s);

        System.out.print("Enter num threads : ");
        s = in.nextLine();
        int num_threads = Integer.parseInt(s);

        Individual best_individual = individual;
                
        if(population_size > 0){
            Population population;
            population = new Population(individual, population_size, num_threads);
            population.start();

            s = in.nextLine();
            System.out.println("Stopping evolution...");
            if(population.isAlive())
                population.interrupt();
            try {
                population.join();
            } catch (InterruptedException ex) {
                System.out.println(ex);
            }
            System.out.println("Stopped.");
            best_individual = population.get_best_individual();
        }
        
       // System.out.println("Running the best...");
        //individual.calculate_cost();
        System.out.println("Cost  = " + best_individual.get_cost());
        best_individual.genome.print();
        
        System.out.println("Done.");
        
        return best_individual;
    }
}
