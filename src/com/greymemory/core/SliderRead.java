/*
 * Copyright(c) 2015 Mindmick Corp. to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.core;

/**
 *
 * @author amazhurin
 */
public class SliderRead
{
    public class FutureSample{
        public double [] data;
        public int meta_data;
        public Sample.Error error;
    }
    
    private final XDM xdm;
    private final WindowBuffer slider;
    private Sample sample;
    
    public SliderRead(XDM xdm){
        this.xdm = xdm;
        sample = new Sample();
        
        slider = new WindowBuffer(xdm.param.num_channels, 
                xdm.param.window.length);
    }
    
    public void process(double[] data) throws Exception{
        if(data.length != xdm.param.num_channels)
            throw new Exception("Wrong data size");
        
        slider.add(data);
    }
    
    public FutureSample predict() throws Exception{
        if(!slider.is_full())
            return null;
        sample.data = slider.data;
        Sample sample_read = xdm.read(sample);
        FutureSample future_sample = new FutureSample();
        future_sample.error = sample_read.error;
        future_sample.data = sample_read.future;
        future_sample.meta_data = sample_read.meta_data;
        return future_sample;
    }
}
