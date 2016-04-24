/*
 * Copyright(c) 2015 Anton Mazhurin to present 
 * Anton Mazhurin & Nawwaf Kharma  * 
 */
package com.greymemory.core;

import java.util.Arrays;
/**
 *
 * @author amazhurin
 */
public class HardLocation {
    public double [] address;
    public Counters [] counters;
    public int timestamp;
    
    public HardLocation(){
    }
    
    public void set_zero(){
        for (Counters c : counters) {
            if(c.data != null)
                Arrays.fill(c.data, (short)0);
            if(c.prediction != null)
                Arrays.fill(c.prediction, (short)0);
            if(c.meta != null)
                Arrays.fill(c.meta, (short)0);
        }
    }
    
}
