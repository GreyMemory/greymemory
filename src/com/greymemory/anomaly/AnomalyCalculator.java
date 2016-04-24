/*
 * Copyright(c) 2015 Anton Mazhurin to present
 * Anton Mazhurin & Nawwaf Kharma
 */
package com.greymemory.anomaly;

//import org.apache.commons.math3.distribution.NormalDistribution;

import com.greymemory.anomaly.MovingAverage;
import com.greymemory.anomaly.Statistics;
 
/**
 *
 * @author amazhurin
 */
public class AnomalyCalculator {
    double[] values;
    double[] next_values;
    private double anomaly_rate;
    private int num_values;
    private int num_next_values;
    private double mean;
    private double deviation;

    public AnomalyCalculator(int window){
        values = new double[window];
        next_values = new double[window/10];
        InitQFunction();
    }
    
    private void calculate_distribution(){
        // calculate histogram
        //Histogram histogram = new Histogram(100);
        //histogram.calculate(values);
        
        Statistics stat = new Statistics(values);
        mean = stat.getMean();
        deviation = stat.getStdDev();
        
        //distribution = new NormalDistribution(mean, deviation);
        
        //StandardDeviation stddev = new StandardDeviation();
        //deviation = stddev.evaluate(values);
        //Mean mn = new Mean();
        //mean = mn.evaluate(values);
    }
    
    public void process(double value){
        // the initial period
        if(num_values < values.length){
            values[num_values++] = value;
            return;
        }
        
        // recalculate the distribution
        if(num_next_values == next_values.length){
            // update the values
            System.arraycopy(values, next_values.length, 
                    values, 0, 
                    values.length - next_values.length);
            System.arraycopy(next_values, 0,
                    values, values.length - next_values.length, 
                    next_values.length); 
            num_next_values = 0;
        } 
        
        if(num_next_values == 0)
            calculate_distribution();
        
        next_values[num_next_values++] = value;
        
        //float probability = 1f - (float) distribution.cumulativeProbability(
        //    average_test_distribution.get_average());
        
        //anomaly_rate = calculate_anomaly_rate(value);
        anomaly_rate = calculate_probability(value);
    }

// return phi(x) = standard Gaussian pdf
    public static double phi(double x) {
        return Math.exp(-x*x / 2) / Math.sqrt(2 * Math.PI);
    }

    // return phi(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
    public static double phi(double x, double mu, double sigma) {
        return phi((x - mu) / sigma) / sigma;
    }
    protected double calculate_anomaly_rate(double x){
        
        double err = x - mean;
        double exp = Math.exp(-(err*err) / (2*deviation*deviation));
        double den = Math.sqrt(2*Math.PI) * deviation;
        double p = exp / den;
        return 1.0 - p;
        
        //NormalDistribution d = new NormalDistribution(mean, deviation);
        //double p = d.probability(x);
        //double p = phi(x, mean, deviation);
        //return 1.0 - p;
    }
    
    /***
        Given the normal distribution specified in distributionParams, return
        the probability of getting samples > x
        This is essentially the Q-function
     * @param x
     * @return 
     */
    private double calculate_probability(double x){
        
        //Distribution is symmetrical around mean        
        if(x < mean){
            double xp = 2*mean - x;
            return calculate_probability(xp);
            //return 0.5;
        }
        
        if(deviation == 0)
            return 0.0f;
        
        // How many standard deviations above the mean are we, scaled by 10X for table
        double xs = 10*(x - mean) / deviation;

        int ixs = (int) xs;
        if(ixs > 70)
           return 1.0f;
        else
           return 1f - (double) QFunction[ixs];
    }
    
    public double get_anomaly(){
        return anomaly_rate;
    }
    
    private double [] QFunction = new double[71]; 

    private void InitQFunction(){
        //# Table lookup for Q function, from wikipedia
        //# http://en.wikipedia.org/wiki/Q-function
        QFunction[0] = 0.500000000;
        QFunction[1] = 0.460172163;
        QFunction[2] = 0.420740291;
        QFunction[3] = 0.382088578;
        QFunction[4] = 0.344578258;
        QFunction[5] = 0.308537539;
        QFunction[6] = 0.274253118;
        QFunction[7] = 0.241963652;
        QFunction[8] = 0.211855399;
        QFunction[9] = 0.184060125;
        QFunction[10] = 0.158655254;
        QFunction[11] = 0.135666061;
        QFunction[12] = 0.115069670;
        QFunction[13] = 0.096800485;
        QFunction[14] = 0.080756659;
        QFunction[15] = 0.066807201;
        QFunction[16] = 0.054799292;
        QFunction[17] = 0.044565463;
        QFunction[18] = 0.035930319;
        QFunction[19] = 0.028716560;
        QFunction[20] = 0.022750132;
        QFunction[21] = 0.017864421;
        QFunction[22] = 0.013903448;
        QFunction[23] = 0.010724110;
        QFunction[24] = 0.008197536;
        QFunction[25] = 0.006209665;
        QFunction[26] = 0.004661188;
        QFunction[27] = 0.003466974;
        QFunction[28] = 0.002555130;
        QFunction[29] = 0.001865813;
        QFunction[30] = 0.001349898;
        QFunction[31] = 0.000967603;
        QFunction[32] = 0.000687138;
        QFunction[33] = 0.000483424;
        QFunction[34] = 0.000336929;
        QFunction[35] = 0.000232629;
        QFunction[36] = 0.000159109;
        QFunction[37] = 0.000107800;
        QFunction[38] = 0.000072348;
        QFunction[39] = 0.000048096;
        QFunction[40] = 0.000031671;
        //# From here on use the approximation in http://cnx.org/content/m11537/latest/
        QFunction[41] = 0.000021771135897;
        QFunction[42] = 0.000014034063752;
        QFunction[43] = 0.000008961673661;
        QFunction[44] = 0.000005668743475;
        QFunction[45] = 0.000003551942468;
        QFunction[46] = 0.000002204533058;
        QFunction[47] = 0.000001355281953;
        QFunction[48] = 0.000000825270644;
        QFunction[49] = 0.000000497747091;
        QFunction[50] = 0.000000297343903;
        QFunction[51] = 0.000000175930101;
        QFunction[52] = 0.000000103096834;
        QFunction[53] = 0.000000059836778;
        QFunction[54] = 0.000000034395590;
        QFunction[55] = 0.000000019581382;
        QFunction[56] = 0.000000011040394;
        QFunction[57] = 0.000000006164833;
        QFunction[58] = 0.000000003409172;
        QFunction[59] = 0.000000001867079;
        QFunction[60] = 0.000000001012647;
        QFunction[61] = 0.000000000543915;
        QFunction[62] = 0.000000000289320;
        QFunction[63] = 0.000000000152404;
        QFunction[64] = 0.000000000079502;
        QFunction[65] = 0.000000000041070;
        QFunction[66] = 0.000000000021010;
        QFunction[67] = 0.000000000010644;
        QFunction[68] = 0.000000000005340;
        QFunction[69] = 0.000000000002653;
        QFunction[70] = 0.000000000001305;
    }
}
