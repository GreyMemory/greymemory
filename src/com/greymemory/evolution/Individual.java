/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.evolution;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author amazhurin
 */
public abstract class Individual {
    public Genome genome = new Genome();

    private double cost = 0.0f;
    protected synchronized void set_cost(double v){
        cost = v;
    }

    public synchronized double get_cost(){
        return cost;
    }
    
    public void mutate(Random rnd){
        genome.mutate(rnd);
    };
    
    void randomize(Random rnd) {
        genome.randomize(rnd);
        cost = -1.0f;
    }

    public abstract Individual create();

    public Individual create_clone() {
        Individual clone = create();
        clone.genome = genome.create_clone();
        return clone;
    }
    
    public ArrayList<Individual> mate(Individual partner, Random rnd) throws Exception{
        ArrayList<Individual> children = new ArrayList<>();

        if(genome.genes.size() != partner.genome.genes.size()){
            throw new Exception("Different genomes in crossover");
        }
        
        Individual child1 = create();
        Individual child2 = create();
        child1.genome = new Genome();
        child2.genome = new Genome();
        
        // crossover
        for(int i = 0; i < genome.genes.size()/2; i++){
            child1.genome.genes.add(genome.genes.get(i).create_clone());
            child2.genome.genes.add(partner.genome.genes.get(i).create_clone());
        }
        
        for(int i = genome.genes.size()/2; i < genome.genes.size(); i++){
            child1.genome.genes.add(partner.genome.genes.get(i).create_clone());
            child2.genome.genes.add(genome.genes.get(i).create_clone());
        }
        
        // mutate
        child1.mutate(rnd);
        child2.mutate(rnd);
        
        children.add(child1);
        children.add(child2);
        return children;
    }
    
    /*
    Calculate cost function of the individual.
    The range is 0.0 .. 1.0
    */
    public abstract void calculate_cost();
}
