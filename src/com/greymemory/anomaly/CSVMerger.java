/*
 * Copyright (c) 2015 Anton Mazhurin to present
 *   * 
 */
package com.greymemory.anomaly;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amazhurin
 */
public class CSVMerger implements DataConsumer {

    BufferedWriter writer = null;
    
    public void File1DividedFile2(String file1, String file2,
            String file_output){
        
        DataSourceCSV2 data_source = new DataSourceCSV2(
            null, false,
            file1, file2);
        
        data_source.addListener(this);
        
        try {
            writer = new BufferedWriter(new FileWriter(new File(file_output)));
            data_source.start();
            data_source.join();
            writer.close();
        } catch (InterruptedException ex) {
            Logger.getLogger(IndividualAnomaly.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CSVMerger.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    @Override
    public void OnData(DataSource source, DataSample sample) {
        try {
            writer.write(
                    Long.toString(sample.timestamp) + "," +
                            Double.toString(sample.data[2]) + "\n" );
        } catch (IOException ex) {
            Logger.getLogger(CSVMerger.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

}
