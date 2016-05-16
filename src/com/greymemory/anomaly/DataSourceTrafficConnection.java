/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author amazhurin
 */
public class DataSourceTrafficConnection 
    extends DataSource 
    implements DataConsumer {
    
    enum SourceType{
        Traffic,
        Connection
    };
    
    class DataSourceContext{
        public DataSource source;
        public ArrayList<DataSample> samples;
        public SourceType type;
        
        public DataSourceContext(DataSource source, SourceType type){
            this.source = source;
            this.type = type;
        }
    }
    
    private String folder;
    private ArrayList<DataSourceContext> sources;
    
    protected void create_data_sources(){
        File dir = new File(folder);
        File[] directoryListing = dir.listFiles();        
        
        for(File file : directoryListing){
            if(file.getName().contains("CONNECTIONS")){
                sources.add(new DataSourceContext(
                    new DataSourceCSV(dateStart, monitoring,
                        file.getAbsolutePath()), SourceType.Connection));
            }
        }
    }
    
    public DataSourceTrafficConnection(
            Date dateStart, boolean monitoring,
            String folder){
        super(dateStart, monitoring);
        this.folder = folder;
        
        create_data_sources();
        
    }

    @Override
    public void OnData(DataSource source, DataSample sample) {
        
        if(sources.isEmpty())
            return;
        
        // update the samples of the datasource
        for(DataSourceContext context : sources){
            if(context.source == source){
                context.samples.add(sample);
            }
        }
        
        // check if we have aggregated data from all the sources
        Date time;
        long timestamp = -1;
        int num_sources_ready = 0;
        for(DataSourceContext context : sources){
            if(timestamp < 0){
                if(context.samples.isEmpty())
                    break;
                timestamp = context.samples.get(0).timestamp;
                num_sources_ready++;
                continue;
            }
            
            
            
        }
    }
    
}
