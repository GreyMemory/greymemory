package com.greymemory.anomaly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author amazhurin
 */
public class DataSourceCSV extends DataSource {

    private String file;
    
    /**
     *
     * @param file
     */
    public DataSourceCSV(
            Date dateStart, boolean monitoring,
            String file){
        super(dateStart, monitoring);
        this.file = file;
    }
    
    @Override
    public void run() {
        BufferedReader reader = null;
        String line;
        DataSample sample;
        double[] data = new double[3];

        try {
            while(!isInterrupted()){
                try {
                    reader = new BufferedReader(new FileReader(file));

                    int num_processed = 0;
                    boolean header = true;
                    // read the historic data from the files
                    while ((line = reader.readLine()) != null) {
                        if(Thread.interrupted()) break;
                        sample = read_data(line, header);
                        header = false;
                        if(sample == null)
                            continue;
                        if(dateStart != null && sample.date.before(dateStart))
                            continue;

                        OnData(sample, this);
                        num_processed++;
                    }            

                    reader.close();
                    if(!monitoring)
                        return;

                    long file_size = (new File(file)).length();

                    while(true){
                        if(Thread.interrupted())
                            break;
                        Thread.sleep(600);

                        long new_file_size = (new File(file)).length();

                        if(new_file_size != file_size){
                            file_size = new_file_size;
                            int num_to_check = 10;
                            ArrayList<String> new_data = tail2(new File(file), num_to_check);
                            int index_start = -1;
                            for(int i = 0; i < new_data.size(); i++){
                                DataSample new_Sample;
                                line = new_data.get(i);
                                new_Sample = read_data(line, false);
                                if(new_Sample == null) continue;
                                if(last_processed_date != null && 
                                        (new_Sample.date.before(last_processed_date) 
                                        || new_Sample.date.equals(last_processed_date)))
                                    continue;
                                index_start = i;
                                break;
                            }                        

                            for(int i = index_start; i >= 0 && i < new_data.size(); i++){
                                line = new_data.get(i);
                                DataSample new_Sample = read_data(line, false);
                                OnData(new_Sample, this);
                            }
                        }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(DataSourceCSV2.class.getName()).log(Level.SEVERE, null, ex);
                    Thread.sleep(600);
                } catch (IOException ex) {
                    Logger.getLogger(DataSourceCSV2.class.getName()).log(Level.SEVERE, null, ex);
                    Thread.sleep(600);
                } 
            }
        }catch (InterruptedException ex) {
        }
    }
}
