/*
 * Copyright(c) 2015 Mindmick Corp. to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.core;

/**
 *
 * @author amazhurin
 */
public class SliderWrite {
    private final XDM xdm;
    private final WindowBuffer slider;
    private Sample sample;
    
    public SliderWrite(XDM xdm){
        this.xdm = xdm;
        sample = new Sample();
        slider = new WindowBuffer(xdm.param.num_channels, 
                xdm.param.window.length);
    }
    
    public void train(double[] data, int meta_data) throws Exception{
        if(data.length != xdm.param.num_channels)
            throw new Exception("Wrong data size");

        if(slider.is_full()){
            sample.data = slider.data;
            sample.future = data;
            sample.meta_data = meta_data;
            xdm.write(sample);
        }
        slider.add(data);
    }
}
