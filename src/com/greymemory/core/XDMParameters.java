/*
 * Copyright(c) 2015 Anton Mazhurin to present 
 * Anton Mazhurin & Nawwaf Kharma  * 
 */
package com.greymemory.core;

/**
 *
 * @author amazhurin
 */
public class XDMParameters {
    /**
     * The size of the atomic data sample, 
     * or number of channels (or dimensions) 
     * in a single data point in a window.
     */
    public int num_channels; 
    
    /**
     *  The number of channels used for prediction
     */
    public int num_channels_prediction; 

    /**
     * if true, prediction is ON
     */
    public boolean predict; 
    
    /**
     * If true, do not read the address in prediction, 
     * read only the prediction. 
     * Note: Ignored if the number of classes more then one.
     */
    public boolean read_prediction_only = true; 
    
    /**
     * The size of meta data (only if predict is true)
     */
    public int meta_data_range; 

    /**
     * The approximate maximum size of the storage in mb
     */
    public int max_storage_size_in_mb = 2000; 

    /**
     * The minimum number of hard locations to be allocated 
     * within the activation radius
     */
    public int min_num_hard_location = 7; 

    /**
     * Activation radius for each dimension
     */
    public double[] activation_radius; 
    
    /**
     * Resolution or the minumum scale division for each dimension
     */
    public double resolution[]; 
    
    /**
     * The maximum distance for the prediction sample in each dimension
     */
    public double[] prediction_radius; 
    
    /**
     * The indexes of the window samples should be ordered in decrement order
     * for example: 
     * past---> [50, 40, 30, 20, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0] <-- now
     */
    public int window[] = {0}; 
    
    /**
     * This is number of write() function calls before one decrement of counters
     */ 
    public int forgetting_rate = 1000; 

    /**
     * The number of hard locations to check every time write() function called.
     * This number define the speed of the global forgetting process
     */
    public int num_hard_location_to_forget = 10;
    
    public int classes[] = {0};
    
    public double medians[] = {0};
    public int max_num_of_levels = 18;
    
    /**
     * The increment value during write
     */
    public int increment = 3; 
    
    /**
     * Increase activation radius for read
     */
    public float radius_multiplier_for_read = 1; 
    
    /*
    The default initialization
    */
    public XDMParameters(int num_channels, int num_channels_prediction,
            int window_size, double min, 
            double range, double resolution, double activation_radius,
            double prediction_radius, int num_classes){
        this.num_channels = num_channels;
        this.num_channels_prediction = num_channels_prediction;
        this.activation_radius = new double[num_channels];
        this.resolution = new double[num_channels];
        this.prediction_radius = new double[num_channels_prediction];
        
        this.classes = new int[num_classes];
        for(int i = 0; i < classes.length; i++)
            classes[i] = i;
        
        window = new int[window_size];
        this.medians = new double[num_channels];
        
        for(int i = 0; i < num_channels; i++){
            this.activation_radius[i] = activation_radius;
            this.resolution[i] = resolution;
            this.medians[i] = min + range/2;
        }
        
        for(int i = 0; i < num_channels_prediction; i++){
            this.prediction_radius[i] = prediction_radius;
        }
    }

    public XDMParameters() {
    }

}
