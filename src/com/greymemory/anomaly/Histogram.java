/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.anomaly;

import java.util.Arrays;

/**
 *
 * @author amazhurin
 */
public class Histogram {
    
    public double bin_size;
    public double min;
    public double [] bins;
    
    public Histogram(int num_bins){
        bins = new double[num_bins];
    }
    
    public void calculate(float[] values){
        min = Float.MAX_VALUE;
        float max = 0;
        for(float v : values){
            if(min > v) min = v;
            if(max < v) max = v;
        }
        
        bin_size = (float) ((max-min) / bins.length);
        
        Arrays.fill(bins, 0f);
        
        int y = 0;
        for(float v : values){
            y++;
            int index = (int) (v / bin_size);
            if(index >= bins.length)
                index = bins.length-1;
            
            bins[index]++;
        }
    }
}
