/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.anomaly;

/**
 *
 * @author amazhurin
 */
public class MovingAverage {
    private int current_size = 0;
    private double total = 0f;
    private int index = 0;
    private final double samples[];

    public MovingAverage(int size) {
        samples = new double[size];
        for (int i = 0; i < size; i++) 
            samples[i] = 0f;
    }

    public void add(double x) {
        if(current_size < samples.length){
            samples[current_size] = x;
            total += x;
            current_size++;
            return;
        }
        
        total -= samples[index];
        samples[index] = x;
        total += x;
        index++;
        if (index == samples.length) 
            index = 0; // cheaper than modulus
    }

    public double get_average() {
        return (double)(total / current_size);
    }   
    
    public double get_last_value() {
        if(current_size >= samples.length)
            return 0.0;
        return samples[current_size-1];
    }   

    int get_size(){
        return samples.length;
    }
}
