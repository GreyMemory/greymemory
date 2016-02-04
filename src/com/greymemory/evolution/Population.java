/*
 * Copyright(c) 2015 Mindmick Corp. to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.evolution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author amazhurin
 */
public class Population extends Thread{
    
    private Random rnd = new Random(33);
    
    private double current_cost;
    private int tournament_size = 3;
    private int thread_pool_size = 4;

    public synchronized double get_current_cost(){
        return current_cost;
    }
    private synchronized void set_current_cost(double v){
        current_cost = v;
    }
    
    private Individual best_individual;
    public synchronized Individual get_best_individual(){
        return best_individual;
    }
    
    public synchronized void set_best_individual(Individual ind){
        best_individual = ind.create_clone();
    }

    private Individual find_best_individual(ArrayList<Individual> individuals){
        double cost = -1.0f;
        int iMax = 0;
        for(int i = 0; i < individuals.size(); i++){
            double cur_cost;
            cur_cost = individuals.get(i).get_cost();
            if(cur_cost < cost){
                cost = cur_cost;
                iMax = i;
            }
        }
        return individuals.get(iMax);
    }
    
    private Individual root;
    private int size;
    public Population(Individual root, int size, int thread_pool_size){
        this.root = root;
        this.size = size;
        this.thread_pool_size = thread_pool_size;
    }
    
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
    
    ExecutorService pool;
    
    @Override
    public void interrupt(){
        if(pool != null)
            pool.shutdown();
        super.interrupt();
    }
    
    private void calculate_cost(ArrayList<Individual> individuals) throws InterruptedException{
        Set<Callable<String>> callables;
        callables = new HashSet<>();

        
        for (Individual individual : individuals) {
            System.out.printf("_");
            callables.add(new Task(individual));
        }
        System.out.printf("\n");
        pool.invokeAll(callables);
        System.out.printf("\n");
    }
    
    private void shutdown_pool(){
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
        }
    }
    
    private void sort_individuals(ArrayList<Individual> individuals){
        individuals.sort((Individual i1, Individual i2) -> {
            double diff = i1.get_cost() - i2.get_cost();
            if(diff > 0)
                return 1;
            else if (diff < 0)
                return -1;
            else
                return 0;
        });
    }
    
    private void downsize_individuals(ArrayList<Individual> individuals){
        while(individuals.size() > size){
            individuals.remove(individuals.size()-1);
        }
    }
            
    private Individual tournament(ArrayList<Individual> individuals) {
        ArrayList<Individual> runners = new ArrayList<>();
        
        for(int i = 0; i < tournament_size; i++){
            runners.add(individuals.get(rnd.nextInt(individuals.size())));
        }
        
        Individual result = runners.get(0);
        
        for(Individual individual : runners){
            if(result.get_cost() < individual.get_cost())
                result = individual;
        }

        return result;
    }
    
    public void run(){
        pool = Executors.newFixedThreadPool(thread_pool_size);
        int generation = 1;
        
        ArrayList<Individual> individuals;
        individuals = new ArrayList<>();
        
        System.out.printf("Calculating starting cost...\n"); 
        root.calculate_cost();
        double previous_cost = root.get_cost();
        System.out.printf("Starting cost : (%f)\n", previous_cost);

            // create a random population
        individuals.add(root);
        for(int i = 0; i < size-1; i++){
            Individual ind;
            ind = root.create_clone();
            ind.randomize(rnd);
            individuals.add(ind);
        }
        
        set_best_individual(root);
        
        try {
            System.out.printf("Generation %d...\n", generation);
            calculate_cost(individuals);
            
            // sort along with the parents
            sort_individuals(individuals);
            
            // find the best in the population
            Individual best = individuals.get(0);
            set_best_individual(best);
            System.out.printf("Best in generation %d: %f : (%f)\n", generation,
                    best.get_cost(), best.get_cost() - previous_cost);
            best.genome.print();
            previous_cost = best.get_cost();
            
            while(!Thread.interrupted()){
                generation++;
                System.out.printf("Generation %d...\n", generation);
                
                ArrayList<Individual> children = new ArrayList<>();
                
                // the best couple
                Individual partner1 = best;
                Individual partner2 = individuals.get(1);
                children.addAll(partner1.mate(partner2, rnd));
                
                // population children
                while(children.size() < individuals.size()){
                    partner1 = tournament(individuals);
                    partner2 = tournament(individuals);
                    children.addAll(partner1.mate(partner2, rnd));
                }
    
                // calculate children cost
                calculate_cost(children);
                
                // add all the children
                individuals.addAll(children);
                
                // sort along with the parents
                sort_individuals(individuals);
                
                // back to original size of population
                downsize_individuals(individuals);
                
                // find the best in the population
                best = individuals.get(0);
                set_best_individual(best);
                System.out.printf("Best in generation %d: %f : (%f)\n", generation,
                        best.get_cost(), best.get_cost() - previous_cost);
                best.genome.print();
                previous_cost = best.get_cost();
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        }
        
        shutdown_pool();
        
    }
}
