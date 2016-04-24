/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.core;

/**
 *
 * @author amazhurin
 */
public class WindowBuffer {
    
    private int num_added;
    private final int window_size;
    private final int size;
    public WindowBuffer(int size, int window_size){
        this.window_size = window_size;
        this.size = size;
        data = new double[size*window_size];
    }
    
    public void add(double[] data_chunk) throws Exception{
        if(data_chunk.length != size)
            throw new Exception("Wrong data size in slider.");
        
        if(num_added < window_size){
            System.arraycopy(data_chunk, 0, data, num_added*size, size);
            num_added++;
        } else{
            // 
            System.arraycopy(data, size, data, 0, (num_added-1)*size);
            System.arraycopy(data_chunk, 0, data, (num_added-1)*size, size);
        }
    }
    
    public boolean is_full(){
        return num_added == window_size;
    }
    
    public double [] data;
}
