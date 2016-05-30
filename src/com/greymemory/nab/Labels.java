/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.greymemory.nab;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 *
 * @author AMazhurin
 */
public class Labels {
    
    class NAB_Anomaly{
        public Date start;
        public Date stop;
    }
    
    class NAB_File{
        public String file_name;
        public ArrayList<NAB_Anomaly> anomalies = new ArrayList<>();
    }
    
    public ArrayList<NAB_File> files = new ArrayList<>();
    public void Read(String file_name)
    {
        JSONParser parser = new JSONParser();

        try {
            JSONObject root = (JSONObject) parser.parse(new FileReader(file_name));
            SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss"); 
            
            files.clear();
            for ( Object key : root.keySet() ) { 
                NAB_File f = new NAB_File();
                f.file_name = (String)key;
                
		JSONArray file = (JSONArray) root.get(key);
		Iterator<JSONArray> iterator_file = file.iterator();
		while (iterator_file.hasNext()) {
                    
                    JSONArray timestamps = iterator_file.next();
                    NAB_Anomaly a = new NAB_Anomaly();
                    a.start = format1.parse((String) timestamps.get(0));
                    a.stop = format1.parse((String) timestamps.get(1));
                    f.anomalies.add(a);
		}
                
                files.add(f);
            }
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (java.text.ParseException e) {
            e.printStackTrace();
        }

    }
}
