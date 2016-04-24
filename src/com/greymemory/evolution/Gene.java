/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.evolution;

import java.util.Random;

/**
 *
 * @author amazhurin
 */
public class Gene {
    
    public Gene(String name, float min, float range){
        this.name = name;
        this.min = min;
        this.range = range;
    }
    
    public Gene(String name, float min, float range, float value){
        this.name = name;
        this.min = min;
        this.range = range;
        this.value = value;
    }

    public String name;
    
    public float value;
    
    public float min;
    
    public float range;
    
    public float mutation;
    
    public void randomize(Random rnd){
        value = min + rnd.nextFloat()*range;
    }
    
    public void mutate(Random rnd){
        value = value + rnd.nextFloat()*range/3 - rnd.nextFloat()*range/6;
        if(value < min)
            value = min;
        if(value > min + range)
            value = min + range;
    }    
    
    public void print(){
        System.out.printf("%s = %f\n", name, value);
    }
    
    public Gene create_clone(){
        Gene gene = new Gene(name, min, range);
        gene.mutation = mutation;
        gene.value = value;
        return gene;
    }
}
