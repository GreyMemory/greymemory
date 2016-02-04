/*
 * VICI. Copyright(c) 2015. 
 * Anton Mazhurin & Nawwaf Kharma  * 
 */
package com.greymemory.core;

/**
 *
 * @author alm
 */
public class Sample {
    public enum Error{
        OK,
        No_Hard_Locations
    }
    
    public double[] data;
    public double[] future;
    public int meta_data;
    public int class_value; 
    
    // the winning counter value (only for read sample)
    public double counter_value;   
    public Error error;
    
    boolean isOK(){
        return error == Error.OK;
    }
}
