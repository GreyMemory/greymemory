/*
 * Copyright(c) 2015 Mindmick Corp. to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.evolution;

import java.util.ArrayList;
import java.util.Random;

/**
 *
 * @author amazhurin
 */
public class Genome {
    
    public ArrayList<Gene> genes = new ArrayList<>();
    
    public Genome create_clone(){
        Genome clone = new Genome();
        for(Gene gene : genes)
            clone.genes.add(gene.create_clone());
        return clone;
    }
    
    public void randomize(Random rnd){
        for(Gene gene : genes){
            gene.randomize(rnd);
        }
    }

    public void mutate(Random rnd) {
        for(Gene gene : genes){
            gene.mutate(rnd);
        }
    }
    
    public Gene get_gene(String name){
        for(Gene g : genes){
            if(g.name == name){
                return g;
            }
        }
        return null;
    }
    
    public void print(){
        for(Gene gene : genes){
            gene.print();
        }
    }
 }