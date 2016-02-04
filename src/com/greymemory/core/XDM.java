/*
 * Copyright(c) 2015 Mindmick Corp. to present 
 * Anton Mazhurin & Nawwaf Kharma  * 
 */
package com.greymemory.core;
/**
 *
 * @author amazhurin
 */

import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class XDM {
    private class Leaf extends ArrayList<HardLocation>{};
    private final Leaf[] leafs;

    public final XDMParameters param;
    
    private int current_forget_leaf;
    private int current_forget_hard_location;
    private int num_hard_locations;
    private int num_levels;
    
    private int num_input_samples;
    private int counter_data_size;
    private int counter_prediction_size;
    private int counter_meta_size;
    
    private int[] num_counters;
    private int[] num_counters_prediction;
    private int timestamp;
    private long size;
    private double [] address_medians;
    private double [] address_activation_radius;
    
    private Random random = new Random(22); 
    
    public void clear(){
        for(int i = 0; i < leafs.length; i++){
            leafs[i].clear();
        }
    }
    
    public XDM(XDMParameters aParam) throws Exception{
        param = aParam;

        if (param.predict && param.classes.length > 1)
            throw new Exception("More than one class for prediction.");
        
	if (param.num_channels <= 0)
            throw new Exception("Wrong numChannels.");
        
        if(param.num_channels != param.resolution.length)
            throw new Exception("Wrong resolution.length().");
        if(param.num_channels != param.activation_radius.length)
            throw new Exception("Wrong activationRadius.length().");
        if(param.num_channels_prediction != 0 && param.num_channels_prediction != param.prediction_radius.length)
            throw new Exception("Wrong predictionRadius.length().");
            
        for (int i = 0; i < param.num_channels; i++){
            if (param.resolution[i] == 0)
                param.resolution[i] = 1;
            if(param.activation_radius[i] == 0)
                param.activation_radius[i] = 100*param.resolution[i];
        }        

        for (int i = 0; i < param.num_channels_prediction; i++){
            if (param.prediction_radius[i] < param.activation_radius[i])
                param.prediction_radius[i] = 10*param.activation_radius[i];
        }        

        // init the indexes if they are not filled
        boolean zeroIndexes = true;
        for (int i = 0; i < param.window.length; i++){
            if (param.window[i] != 0){
                zeroIndexes = false;
                break;
            }
        }
        if (zeroIndexes){
            for ( int i = 0; i < param.window.length; i++)
                param.window[i] = param.window.length - 1 - i;
        }

        // check window indexes
        int index = 999999999;
        for (int i = 0; i < param.window.length; i++){
            if (param.window[i] > index)
                    throw new Exception("Wrong window index.");
            index = param.window[i];
        }

        
        // calculate the total number of samples in the data 
        // for read/write functions
        num_input_samples = param.window[0] + 1;

        // calculate the size of counters memory block 
        counter_data_size = 0;
        counter_prediction_size = 0;
        
        num_counters = new int[param.num_channels];
        
        // calculate the length of the array for the voting
        int max_num_counters = 0;
        for (int i = 0; i < param.num_channels; i++){
            num_counters[i] = (int) (1 + 2 * (Math.round(param.activation_radius[i] / 
                    param.resolution[i])));
            if(max_num_counters < num_counters[i])
                max_num_counters = num_counters[i];
            counter_data_size += num_counters[i];
        }
        
        counter_data_size *= param.window.length;
        if (param.predict){
            num_counters_prediction = new int[param.num_channels_prediction];
            max_num_counters = 0;
            for (int i = 0; i < param.num_channels_prediction; i++){
                num_counters_prediction[i] = (int) (1 + 2 * 
                        Math.round(param.prediction_radius[i] / param.resolution[i]));
                counter_prediction_size += num_counters_prediction[i];
                if(max_num_counters < num_counters_prediction[i])
                    max_num_counters = num_counters_prediction[i];
            }
        }

        if (param.meta_data_range > 0)
            counter_meta_size += param.meta_data_range;
        
        num_levels = param.medians.length*param.window.length;
        if(num_levels > param.max_num_of_levels)        
            num_levels = param.max_num_of_levels;
        int num_leafs = 1 << num_levels;
                
        leafs = new Leaf[num_leafs];
        for(int i = 0; i < num_leafs; i++){
            leafs[i] = new Leaf();
        }
        
        address_medians = new double[param.window.length*param.num_channels];
        address_activation_radius = new double[param.window.length*param.num_channels];
        int k = 0;
        for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                address_medians[k] = param.medians[i];
                address_activation_radius[k] = param.activation_radius[i];
                k++;
            }
        }        
        
        timestamp = 0;
        current_forget_leaf = 0;
        current_forget_hard_location = 0;
        num_hard_locations = 0;
    }
    
    private Leaf get_leaf_for_address(int index, int level, double[] address){
        if(level == num_levels)
            return leafs[index];
        
        if(address[level] > address_medians[level]){
            index += 1 << (num_levels - level - 1);
        }
        
        return get_leaf_for_address(index, level+1, address);
    }
    
    public void add_hard_location(HardLocation hard_location) throws Exception{
        Leaf leaf = get_leaf_for_address(0, 0, hard_location.address);
        leaf.add(hard_location);
        num_hard_locations++;
    }
    
    private void get_leafs_for_range(int index, int level, double[] address, 
            ArrayList<Leaf> result, int activation_radius_factor) {
        if(level == num_levels){
            result.add(leafs[index]);
            return;
        }
        
        if(address[level] + address_activation_radius[level] * 
                activation_radius_factor > address_medians[level]){
            int indexRight = index;
            indexRight += 1 << (num_levels - level - 1);
            get_leafs_for_range(indexRight, level+1, address, result, 
                    activation_radius_factor);
        }
        
        if(address[level] - 
                address_activation_radius[level]*activation_radius_factor
                < address_medians[level]){
            get_leafs_for_range(index, level+1, address, result,
                    activation_radius_factor);
        }
    }
    
    private int get_counter_size(){
        return  (counter_data_size + counter_prediction_size +
                counter_meta_size) * Short.BYTES;

    }
    private int get_class_index(int classValue)
    {
        if (param.classes.length == 1)
                return 0;
        for (int i = 0; i < param.classes.length; i++)
        {
            if (param.classes[i] == classValue)
                    return i;
        }
        return -1;
    }

    private void forget(){
        if(current_forget_leaf >= leafs.length)
            return;
        if(num_hard_locations < 10000)
            return;
        int i = 0;
        while(i < param.num_hard_location_to_forget){
            Leaf leaf;
            leaf = leafs[current_forget_leaf];
            for(; current_forget_hard_location < leaf.size(); 
                    current_forget_hard_location++){
                forget_hard_location(leaf, 
                        leaf.get(current_forget_hard_location));
                i++;
                if(i >= param.num_hard_location_to_forget)
                    break;
            }
            if(i >= param.num_hard_location_to_forget)
                break;
            current_forget_hard_location = 0;
            if(current_forget_leaf < leafs.length -1)
                current_forget_leaf++;
            else
                break;
        }
    }
    
    private double[] get_address(double[] data){
	double [] res = new double[param.num_channels * param.window.length];

	int startOfNowSample;
        startOfNowSample = (num_input_samples - 1)*param.num_channels;

	int index = 0;
	for (int w = 0; w < param.window.length; w++)
	{
            int startOfSample = startOfNowSample - 
                    param.window[w] * param.num_channels;
            for (int i = 0; i < param.num_channels; i++)
            {
                res[index++] = data[startOfSample + i];
            }
	}        
        return res;
    }
    
    private boolean is_in_activation_distance(
            double[] address1,
            double[] address2,
            int radius_factor
    ) throws Exception
    {
        int length = param.window.length*param.num_channels;
        if (address1.length != length || address1.length != length)
            throw new Exception("Wrong address length");

        int k = 0;
        for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                double diff = java.lang.Math.abs(address1[k] - address2[k]);
                if (diff > param.activation_radius[i]*radius_factor)
                    return false;
                k++;
            }
        }
        return true;
        /*
        int k = 0;
        double distance = 0.0;
        for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                double diff = java.lang.Math.abs(address1[k] - address2[k]);
                diff /= param.resolution[0];
                distance += diff ;
                k++;
            }
        }
        
        return distance < param.activation_radius[0] / param.resolution[0];
        */
        
    }
    
    /**
     * Returns true if all the values are zero
     * @param counters
     * @param decrement_value
     * @return 
    */
    public boolean decrement_counters(Counters counters,
            int decrement_value){
        boolean zero = true;
	int offset = 0;
	// window counters
	for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                for(int d = 0; d < num_counters[i]; d++){
                    if(counters.data[offset] > 0){
                        counters.data[offset]--;
                        zero = false;
                    }        
                    offset++;
                }
            }
	}

	if (param.predict)
	{
            offset = 0;
            // use the last sample as a reference for the prediction sample counters
            
            for (int i = 0; i < param.num_channels_prediction; i++){
                // calculate the maximum
                for(int d = 0; d < num_counters_prediction[i]; d++){
                    if(counters.prediction[offset] > 0){
                        counters.prediction[offset]--;
                        zero = false;
                    }        
                    offset++;
                }
            }
            
            // meta data
            if (param.meta_data_range > 0){
                // calculate the maximum
                for (int i = 0; i < param.meta_data_range; i++){
                    if(counters.meta[i] > 0){
                        counters.meta[i]--;
                        zero = false;
                    }        
                }
            }
	}

        return zero;
    }

    /**
     * Returns true if all the values are zero
     * @param hard_location
     * @param decrement_value
     * @return 
    */
    public boolean decrement_hard_location(HardLocation hard_location,
            int decrement_value){
        boolean all_zero = true;
        for (Counters counter : hard_location.counters) {
            if (counter == null) {
                continue;
            }
            if (!decrement_counters(counter, decrement_value)) {
                all_zero = false;
            }
        }
        return all_zero;
    }


    /**
     * Returns true if the hard location is too old and has been deleted
     * @param hard_location
     * @return 
     */
    private boolean forget_hard_location(Leaf leaf, HardLocation hard_location){
	int tsStorage = timestamp;
	int ts = hard_location.timestamp;
	int num_decrement_period;
	if (ts > tsStorage){
            // integer overflow and wrap around
            num_decrement_period = (tsStorage + (Integer.MAX_VALUE - ts)) 
                    / param.forgetting_rate;
	} else {
            num_decrement_period = (tsStorage - ts) / param.forgetting_rate;
	}

	if (num_decrement_period == 0) return false;

	// update the timestamp of the hard location
	hard_location.timestamp += num_decrement_period*param.forgetting_rate;

	int decrement;
        decrement = num_decrement_period;
	if (decrement_hard_location(hard_location, decrement))
	{
            // update the total memory size
            for (Counters counter : hard_location.counters) {
                if(counter.data.length > 0)
                    size -= get_counter_size();
            }

            // delete the hard location
            leaf.remove(hard_location);
            num_hard_locations--;
            return true;
	}

	return false;
    }
    
    private HardLocation new_hard_location(int class_index, 
            ArrayList<HardLocation> reservedList) throws Exception{
        
        HardLocation hard_location = null;
	if (size / 1000000 < param.max_storage_size_in_mb)
	{
            // allocate a new hard location with the counters for the class
            hard_location = new HardLocation();
            hard_location.counters = new Counters[param.classes.length];
            create_counters_for_class(hard_location, class_index);
            hard_location.address = new double[param.num_channels*
                    param.window.length];
	}
        
        // we hit the maximum size of the storage.
        int i = 0;
        if(hard_location == null){
            if(leafs.length == 0)
                return null;
            
            while(true){
                Leaf leaf;
                int random_leaf = random.nextInt(leafs.length);
                leaf = leafs[random_leaf];
                boolean flag_zero = false;
                while(leaf.size() == 0){
                    if(random_leaf+1 < leafs.length)
                        random_leaf++;
                    else{
                        if(flag_zero)
                            break;
                        flag_zero = true;
                        random_leaf = 0;
                    }
                    leaf = leafs[random_leaf];
                }
                
                if(leaf.size() == 0)
                    break;
                
                int random_index = random.nextInt(leaf.size());
                hard_location = leaf.get(random_index);

                // just a hack to avoid infiniti loop
                if(i++ > 1000) {
                    hard_location = null;
                    break;
                }

                if(!reservedList.contains(hard_location))
                    break;
                
                // remove extra classes from the stolen hard location
                for(int c = 0; c < hard_location.counters.length; c++){
                    if(c == class_index) 
                        continue;
                    size -= get_counter_size();
                    hard_location.counters[c].clear();
                }
                
                // zero the counters
                hard_location.set_zero();
                leaf.remove(hard_location);
            }
        }
        
        if(hard_location != null){
            hard_location.timestamp = timestamp;
        }

        return hard_location;
    }

    private ArrayList<HardLocation> get_hard_locations_for_read(double [] address,
            int radius_factor) throws Exception{
        
        // get the local leafs
        ArrayList<Leaf>  local_leafs = new ArrayList<>();
        get_leafs_for_range(0, 0, address, local_leafs, radius_factor);
        
        ArrayList<HardLocation>  result = new ArrayList<>();
        // eliminate hard locations which are not inside the hyper rectangle
        // eliminate old hard locations
        for(Leaf leaf : local_leafs){
            for(int i = leaf.size()-1; i >= 0; i--){
                HardLocation hard_location = leaf.get(i);
                if(forget_hard_location(leaf, hard_location))
                    continue;
                if(is_in_activation_distance(address, hard_location.address,
                        radius_factor)){
                    result.add(hard_location);
                    if(result.size() > 1000)
                        return result;
                }
            }
        }
        return result;
    }
    
    private ArrayList<HardLocation> get_hard_locations(double [] address,
            int class_index, boolean create) throws Exception{
        
        // get the local leafs
        ArrayList<Leaf>  local_leafs = new ArrayList<>();
        get_leafs_for_range(0, 0, address, local_leafs, 1);
        
        ArrayList<HardLocation>  result = new ArrayList<>();
        // eliminate hard locations which are not inside the hyper rectangle
        // eliminate old hard locations
        for(Leaf leaf : local_leafs){
            for(int i = leaf.size()-1; i >= 0; i--){
                HardLocation hard_location = leaf.get(i);
                if(forget_hard_location(leaf, hard_location))
                    continue;
                if(is_in_activation_distance(address, hard_location.address, 1))
                    result.add(hard_location);
            }
        }

        
        // return if we don't have to create hard locations
        if(!create)
            return result;
        
        // not enough hard location here.
        // first, we add the address itself
        HardLocation hard_location = new_hard_location(class_index, result);
        if(hard_location == null)
            return result;
        
        // first, add the addres itself as a new hard location
        hard_location.address = address.clone();
        //if (is_good_hard_location(hard_location, result)) 
        {
            add_hard_location(hard_location);
            result.add(hard_location);
            hard_location = null;
            if (result.size() >= param.min_num_hard_location)
                return result;
        }
        
        // we still need more hard locations
        int num_tries = 0;
        while (result.size() < param.min_num_hard_location)
        {
            // just a hack
            if (num_tries++ > 500)
                break;

            if (hard_location == null)
                hard_location = new_hard_location(class_index, result);
            
            if (hard_location == null)
                break;

            hard_location.address = address.clone();
            
            // generate a new addres inside the activation distance
            int k = 0;
            for ( int w = 0; w < param.window.length; w++)
            {
                for (int i = 0; i < param.num_channels; i++)
                {
                    int random_int = random.nextInt((int) 
                        (param.activation_radius[i] * 2 / param.resolution[i])+1);
                    double random_address = random_int* param.resolution[i];
                    
                    random_address -= param.activation_radius[i];
                    double new_address = hard_location.address[k] + random_address;
                    
                    hard_location.address[k] = new_address;
                    k++;
                }
            }
            
            //if (is_good_hard_location(hard_location, result))
            //if(is_in_activation_distance(address, hard_location.address))
            {
                add_hard_location(hard_location);
                result.add(hard_location);
                hard_location = null;
                if (result.size() >= param.min_num_hard_location)
                    break;
            }
        }
        
        return result;
    }

    /**
     * Is this hard location is a good candidate for this group
     * @param hardLocation
     * @param result
     * @return 
     */
    private boolean is_good_hard_location(HardLocation hard_location, 
            ArrayList<HardLocation> hard_locations) {
        return true;
    }
    
    private void create_counters_for_class(HardLocation hard_location,
            int class_index){
        hard_location.counters[class_index] = new Counters();
        hard_location.counters[class_index].data = 
                new short[counter_data_size];
        hard_location.counters[class_index].prediction = 
                new short[counter_prediction_size];
        hard_location.counters[class_index].meta = 
                new short[counter_meta_size];
        size += get_counter_size();
    }
    /*        
    private boolean is_valid_for_increment(HardLocation hard_location,
	double[] address, double[] future, int meta_data, int class_index) 
            throws Exception{

	Counters counters = hard_location.counters[class_index];
        if(counters == null){
            create_counters_for_class(hard_location, class_index);
            return true;
        }

	int offset = 0;
	int index = 0;
	// window counters
	for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                int diff = (int)((address[index] - hard_location.address[index]) 
                        / param.resolution[i]);
		int counter_index = diff + (num_counters[i] - 1) / 2;

		if (counter_index < 0 || counter_index >= num_counters[i])
                    throw new Exception("Bad number of address counters");

                if (counters.data[offset + counter_index] + 
                        param.increment*normalized_increment[i] > counter_max_value)
                    return false;
                
                offset += num_counters[i];
                index++;
            }
	}

	if (param.predict)
	{
            offset = 0;
            // use the last sample as a reference for the prediction sample counters
            index -= param.num_channels;
            int index_prediction = 0;
            
            for (int i = 0; i < param.num_channels_prediction; i++){
                int diff = (int)((hard_location.address[index]-future[index_prediction]) 
                        / param.resolution[i]);
		int counter_index = diff + (num_counters_prediction[i] - 1) / 2;

		if (counter_index < 0 || counter_index >= num_counters_prediction[i])
                    //"Not enough prediction counters");
                    return false;

                if (counters.prediction[offset + counter_index] + 
                        param.increment*normalized_increment_prediction[i] > counter_max_value)
                    return false;
                
                offset += num_counters_prediction[i];
                index_prediction++;
            }
            
	}

	return true;
    }
*/
    
    private void increment_counters(HardLocation hard_location,
	double[] address, double[] future, int meta_data, int class_index) 
            throws Exception{

	Counters counters = hard_location.counters[class_index];
        if(counters == null){
            create_counters_for_class(hard_location, class_index);
            counters = hard_location.counters[class_index];
        }

	int offset = 0;
	int index = 0;
	// window counters
	for (int w = 0; w < param.window.length; w++){
            for (int i = 0; i < param.num_channels; i++){
                int diff = (int)((address[index] - hard_location.address[index]) 
                        / param.resolution[i]);
		int counter_index = diff + (num_counters[i] - 1) / 2;

		if (counter_index < 0 || counter_index >= num_counters[i])
                    throw new Exception("Bad number of address counters");

                if(counters == null)
                    counters.data = null;
                if (counters.data[offset + counter_index] + 
                        param.increment <= Short.MAX_VALUE)
                    counters.data[offset + counter_index]++;
                    
                offset += num_counters[i];
                index++;
            }
	}

	if (param.predict)
	{
            offset = 0;
            // use the last sample as a reference for the prediction sample counters
            index -= param.num_channels;
            int index_prediction = 0;
            
            for (int i = 0; i < param.num_channels_prediction; i++){
                int diff = (int)((future[index_prediction]-hard_location.address[index]) 
                        / param.resolution[i]);
		int counter_index = diff + (num_counters_prediction[i] - 1) / 2;

		//if (counter_index < 0 || counter_index >= num_counters_prediction[i])
                //    throw new Exception("Not enough prediction counters");

                if (counter_index >= 0 && counter_index < num_counters_prediction[i]){
                    if(counters.prediction[offset + counter_index] + param.increment <= 
                            Short.MAX_VALUE){
                        counters.prediction[offset + counter_index] += param.increment;
                    }
                }
                
                offset += num_counters_prediction[i];
                index_prediction++;
                index++;
            }
            
            // meta data
            if (param.meta_data_range > 0){
                if (meta_data < param.meta_data_range)
                    if(counters.meta[meta_data] + param.increment <= Short.MAX_VALUE)
                        counters.meta[meta_data] += param.increment;
            }
	}
    }

    public void write(Sample sample) throws Exception {
        if(sample.data == null || 
                sample.data.length != num_input_samples * param.num_channels)
             throw new Exception("Wrong data size.");
        
        //if(param.predict && sample.future.length != param.num_channels_prediction)
        //     throw new Exception("Wrong future data size.");

        int class_index = get_class_index(sample.class_value);
        if(class_index < 0) throw new Exception("Wrong class index.");
      
        timestamp++;
        
        double[] address;
        address = get_address(sample.data);
        
        forget();
        
        ArrayList<HardLocation> local_hard_locations = get_hard_locations(address,
            class_index, true // create if needed
        );
        
        if(local_hard_locations.isEmpty()) {
            //No hard locations
            return;
        }
        
        // check for overloads and increment counters if we are fine
        for (HardLocation hard_location : local_hard_locations) {
            if (class_index >= hard_location.counters.length)
                throw new Exception("Wrong class value");

            /*
            // check for overload
            if(!is_valid_for_increment(hard_location, 
                    address, sample.future, sample.meta_data, class_index)){
                return; // nothing wrong(overloading or few counters), just return
            }
            */

            // incrementing the counters
            increment_counters(hard_location, 
                    address, sample.future, sample.meta_data, class_index);
        }        
    }

    class Winner_value{
        public double value;
        public double counter_value;
    }
    
    private Winner_value read_value(int class_index, 
        double resolution, 
        int address_index, 
        double address_reference, 
        ArrayList<HardLocation> local_hard_locations,
        Counters.Type counter_type, int counters_offset,
        int num_counters,double[] votes) throws Exception{

        // sum up the counters
        for (HardLocation hard_location : local_hard_locations) {
            Counters counters = hard_location.counters[class_index];
            if (counters == null)
                continue;

            double address_hard_location;
            address_hard_location = hard_location.address[address_index];
            int start_index_vote = (int) ((votes.length - 1) / 2 + 
                    (address_hard_location - address_reference) / resolution
                    - (num_counters -1) / 2);
            int start_index_hl = 0;

            if (start_index_vote < 0){
                start_index_hl += abs(start_index_vote);
                start_index_vote = 0;
            }

            if (start_index_vote < 0 || start_index_vote >= votes.length){
                throw new Exception("Bad number of votes");
            }

            int max_k;
            max_k = start_index_vote + num_counters - 1;
            if(max_k >= votes.length)
                max_k = votes.length-1;
                
            short [] values = counters.get_by_type(counter_type);
            for (int k = start_index_vote; k <= max_k; k++){
                votes[k] += values[counters_offset + start_index_hl++];
                if (start_index_hl >= num_counters)
                    break;
            }
        }
        
        Winner_value result;
        result = new Winner_value();

        // find the winner value 
        int iMax = 0;
        result.counter_value = 0;
        for (int v = 0; v < votes.length; v++){
            if (votes[v] > result.counter_value) {
                result.counter_value = votes[v];
                iMax = v;
            }
        }

        if (result.counter_value != 0){
            result.value = (double) (address_reference + 
                    resolution * (iMax*1.0 - (votes.length - 1.0) / 2.0));
        }

        return result;
    }

    private double get_distance(double[] a1, double [] a2) throws Exception{
	int length = param.window.length*param.num_channels;
	
        if (a1.length != a2.length)
            throw new Exception("Different address lengths.");
        if (a1.length != length)
            throw new Exception("Wrong address lengths.");

        double res = 0;
        int index = 0;
	for (int i = 0; i < param.window.length; i++)
            for (int k = 0; k < param.num_channels; k++){
                res += abs(a1[index] - a2[index]) / param.activation_radius[k];
                index++;
            }

	return res;
    }
            
    ArrayList<HardLocation> get_nearest_hard_locations(
        ArrayList<HardLocation> hard_locations, 
            double[] address,
            int max_number) throws Exception{
        
        ArrayList<HardLocation> result = 
                new ArrayList<>();
        
        class Hard_location_context {
            HardLocation hl;
            double distance;
        };
        
        ArrayList<Hard_location_context> contexts = 
                new ArrayList<>();
        for (HardLocation hard_location : hard_locations) {
            Hard_location_context context = new Hard_location_context();
            context.hl = hard_location;
            context.distance = get_distance(address, hard_location.address);
            contexts.add(context);
        }
        
        //Sorting
        Collections.sort(contexts, new Comparator<Hard_location_context>() {
            @Override
            public int compare(Hard_location_context o1, Hard_location_context o2) {
                return o1.distance > o2.distance ? 1 : 
                        (o1.distance < o2.distance ? -1 : 0);
            }
        });
            
        int num = max_number;
        if(num >= contexts.size())
            num = contexts.size();
        for(int i = 0; i < contexts.size(); i++){
            result.add(contexts.get(i).hl);
        }
        return result;
    }
            
    public Sample read(Sample sample) throws Exception {
        if(sample.data == null || 
            sample.data.length != num_input_samples * param.num_channels)
             throw new Exception("Wrong data size.");
        
        Sample result = new Sample();
        result.future = new double[param.num_channels_prediction];
        
        double[] address;
        address = get_address(sample.data);
        
        forget();
        
        int radius_factor = (int)param.radius_multiplier_for_read;
        
        ArrayList<HardLocation> local_hard_locations = 
            get_hard_locations_for_read(address,
                radius_factor
        );
        /*
        
        ArrayList<HardLocation> local_hard_locationsAll = 
            get_hard_locations_for_read(address,
                radius_factor
        );
        
        ArrayList<HardLocation> local_hard_locations;
         
        local_hard_locations = get_nearest_hard_locations(
                local_hard_locationsAll, address, 5);
        */
        
        if(local_hard_locations.isEmpty()) {
            //No hard locations
            result.error = Sample.Error.No_Hard_Locations;
            for(int i = 0; i < param.num_channels_prediction; i++){
                result.future[i] = 0;
            }
            return result;
        }
        
        int iClass = 0;
        Winner_value v; 

        // do not read the address for prediction
        if(!param.predict || 
           !param.read_prediction_only ||
           param.classes.length > 1){
            double[] class_distances; 
            class_distances = new double[param.classes.length];

            double[] address_read;
            address_read = new double[address.length];    

            // allocate voting arrays per channel
            ArrayList<double[]> votes;
            votes = new ArrayList<>();
            votes.ensureCapacity(param.num_channels);
            for (int i = 0; i < param.num_channels; i++){
                votes.add(new double[1 + 4 * (int)(param.activation_radius[i] 
                                * radius_factor
                                / param.resolution[i])]);
            }

            // buffer for winner counter values
            double[] counter_values;
            counter_values = new double[address.length];

            double[] class_counter_values;
            class_counter_values = new double[param.classes.length];

            for (int c = 0; c < param.classes.length; c++){
                int offset = 0;
                int index = 0;
                boolean zero_counters;
                zero_counters = false;
                for (int w = 0; w < param.window.length; w++){
                    for (int i = 0; i < param.num_channels; i++){
                        Arrays.fill(votes.get(i), (short)0);

                        v = read_value(c, param.resolution[i], 
                                index, 
                                address[index], 
                                local_hard_locations, Counters.Type.data, 
                                offset, num_counters[i], votes.get(i));

                        counter_values[index] = v.counter_value * 1.0/ 
                                num_hard_locations;
                        if (v.counter_value == 0){
                            zero_counters = true;
                            break;
                        }

                        address_read[index++] = v.value;
                        offset += num_counters[i];
                    }
                    if (zero_counters)
                        break;
                }

                if (zero_counters){
                    class_counter_values[c] = 0;
                    class_distances[c] = Double.MAX_VALUE;
                } else {
                    class_distances[c] = get_distance(address, address_read);

                    double min_counter = Double.MAX_VALUE;
                    for (int i = 0; i < address.length; i++) {
                        if (counter_values[i] > 0 && min_counter > counter_values[i])
                            min_counter = counter_values[i];
                    }

                    class_counter_values[c] = min_counter;
                }            
            }

            // find the nearest class
            double Min = class_distances[0];
            for (int c = 1; c < param.classes.length; c++){
                if (class_distances[c] < 0)
                    continue;
                if (class_distances[c] < Min){
                    Min = class_distances[c];
                    iClass = c;
                }
            }        

            result.class_value = param.classes[iClass];
            result.counter_value = class_counter_values[iClass] / 0xFFFF;
        }
        
        if(param.predict){
            // read prediction for the detected class
            int address_index;
            address_index = param.num_channels * (param.window.length - 1);
            int offset = 0;
            for(int i = 0; i < param.num_channels_prediction; i++){
                int num_votes = 1 + 4 * (int)(param.prediction_radius[i] / 
                        param.resolution[i]);
                
                double[] votes_prediction;
                votes_prediction = new double[num_votes];
                Arrays.fill(votes_prediction, 0);

                v = read_value(iClass, param.resolution[i], 
                        address_index, 
                        address[address_index], 
                        local_hard_locations, Counters.Type.prediction, 
                        offset, num_counters_prediction[i], votes_prediction);

                result.future[i] = v.value;
                address_index++;
                offset += num_counters_prediction[i];
                
                if(v.counter_value == 0){
                    result.error = Sample.Error.No_Hard_Locations;
                    return result;
                }
            }
            
            if(param.meta_data_range > 0){
                long[] votes_meta;
                votes_meta = new long[param.meta_data_range];
                Arrays.fill(votes_meta, 0L);

                // sum up the counters
                for (HardLocation hard_location : local_hard_locations) {
                    if (hard_location.counters[iClass] == null)
                        continue;
                    
                    short [] values = hard_location.counters[iClass].get_by_type(
                            Counters.Type.meta);
                    
                    if (values == null)
                        continue;
                    
                    for(int k = 0; k < param.meta_data_range; k++)
                        votes_meta[k] += values[k];
                }

                // find the winner value 
                int iMax = 0;
                long counter_value = 0;
                for (int i = 0; i < param.meta_data_range; i++){
                    if (votes_meta[i] > counter_value){
                        counter_value = votes_meta[i];
                        iMax = i;
                    }
                }
                
                result.meta_data = iMax;
            }            
        }        
               
        result.error = Sample.Error.OK;
        return result;
    }   
    
    public double get_normalized_error(double[] data, double[] prediction) throws Exception {
        if(prediction == null)
            return 1f;
        
        if(prediction.length != param.num_channels_prediction){
            throw new Exception("Wrong future array length");
        }
        
        double total_anomaly = 0;
        for(int i = 0; i < param.num_channels_prediction; i++){
            total_anomaly += (data[i] - prediction[i]) /
                    param.prediction_radius[i];
        }
        
        total_anomaly /= param.num_channels_prediction;
        return total_anomaly;
    }
    
}

/**
 * TODO:
 * - the maximum saturation value for the counters!!!
 * - range for the maximum counters value 
 */