/*
 * Copyright(c) 2015 Mindmick Corp. to present 
 * Anton Mazhurin & Nawwaf Kharma  * 
 */
package com.greymemory.core;

/**
 *
 * @author amazhurin
 */
public class Counters {
    
    public enum Type {
        data,
        prediction,
        meta
    }
    
    public short [] data;
    public short [] prediction;
    public short [] meta;

    public void clear(){
        data = null;
        prediction= null;
        meta = null;
    }
    
    public short[] get_by_type(Counters.Type v){
        switch (v){
            case data : return data;
            case prediction: return prediction;
            case meta : return meta;
        }
        return data;
    }
   
}
