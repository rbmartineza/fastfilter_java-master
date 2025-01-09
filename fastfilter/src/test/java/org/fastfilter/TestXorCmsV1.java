package org.fastfilter;

//import static org.junit.Assert.assertEquals;

// import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;


//import org.fastfilter.Filter;
//import org.fastfilter.TestFilterType;
import org.fastfilter.utils.Hash;
import org.fastfilter.utils.RandomGenerator;
//import org.fastfilter.xor.XorCmsSerialV1;
import org.fastfilter.xor.XorCmsV1;
import org.junit.Test;

/*

## Should I Use an Approximate Member Filter?

In some cases, if the false positive rate of a filter is low enough, using _just_ a filter is good enough,
an one does not need the original data altogether.
For example, a simple spell checker might just use a filter that contains
the known words. It might be OK if a mistyped word is not detected, if this is rare enough.
Another example is using a filter to reject known passwords: the complete list of all known passwords
is very large, so using a filter makes sense. The application (or user) can deal
with the possibility of false positives: the filter will simplify mark a password
as "known" even if it's not in the list.

But in most cases the original data is needed, and filters are only used to avoid unnecessary lookups.
Whether or not using a filter makes sense, and which filter to use, depends on multiple factors:

* Is it worth the additional complexity?
* How much time is saved? One has to consider the time saved by true positives,
   minus the time needed to do lookups in the filter.
   Typically, avoiding I/O make sense,
   but avoiding memory lookups usually doesn't save time.
* The memory needed by the filter often also plays a role,
   as it means less memory is available for a cache,
   and a smaller cache can slow things down.

Specially the last point makes it harder to estimate how much time can be saved by which filter type and configuration,
as many factors come into play.

To compare accurately, it might be best to write a benchmark application that is close to the real-world,
and then run this benchmark with different filters.

(Best would be to have a benchmark that simulates such an application, but it takes some time.
Or change e.g. RocksDB to use different filters.
Would it be worth it? For caching, typically "trace files" are used to compare algorithms,
but for filters this is probably harder.)

## Which Features Do I Need?

... do you need a mutable filter, do you want to store satellite data, ...

## What are the Risks?

... (I think some filters have risks, for example the cuckoo filter and other fingerprint based ones
may not be able to store an entry in rare cases, if used in the mutable way)



---------------

## Which Filter Should I Use?

For a certain false positive rate, some filter types are faster but need more memory,
others use less memory but are slower.



To decide which type to use, the average time can be estimated as follows:

* filterFpp: false positive rate of the filter (0.01 for 1%)
* applicationFpp: false positive rate of the application (how often does the application perform a lookup if the entry doesn't exist)
* filterLookupTime: average time needed by the filter to perform a lookup
* falsePositiveTime: average time needed in case of a false positive, in nanoseconds

time = (1 - applicationFpp) * filterLookupTime +
           applicationFpp * (filterLookupTime + filterFpp * falsePositiveTime)

This could be, for a LSM tree:

* applicationFpp: 0.9
* falsePositiveTime: 40000 nanoseconds (0.04 milliseconds access time for a random read in an SSD)

...

 */

public class TestXorCmsV1 {
	private static double lookup_0;
	private static double lookup_100;
	private static double fpp_final;
    private static double construction;
    private static double meanMemAccessNeg;
    private static double meanMemAccessPos;
    private static double relative_error;
    private static double absolute_error;
    private static String name_dist;
    private static int testFreq;
    private static int flows;
    private static int size_neg;

        public static void main(String... args) {
            Hash.setSeed(1);
            
                for (int size2 = 300_000; size2 <= 350_000; size2 += 100_000) {
                    int size1 =65_000_000; // Keys for the Xor filter
                    //int size2 = 500_000; // Keys for the Bloom filter
                    int size = size1 + size2;  // Total keys to insert
                    System.out.println("size " + size);
                    int test = 0;
                    test(size1,size2, test, true);
                    //test2(TestFilterType.BLOOM, size2, test, true);
                    System.out.println();
                }
        }
    
        @Test
        public void test() {
            testAll(1000, false);
        }
    
        private static void testAll(int len, boolean log) {
           // for (TestFilterType type : TestFilterType.values()) {
                //test(type, len, 0, log);
                //test(len,len, 0, log);
                int run = 1;
                int size_test = 1;
                double[] l0_array = new double [size_test];
                double[] l100_array = new double [size_test];
                double[] fpp_array = new double [size_test];
                double[] mem_pos_array = new double [size_test];
                double[] mem_neg_array = new double [size_test];
                double[] mean_relative_error_array = new double [size_test];
                double[] mean_absolute_error_array = new double [size_test];
               
                    for (int run_i = 1; run_i <= run; run_i += 1){
                        //System.out.println(" Run: " + run_i);
                        int i = 0;
                        size_neg =1000000; // Number of keys not included in the Xor filter
                            for (int size2 = 0; size2 <= 0; size2 += 100_000) {
                                int seed_CMS = 50;
                                test(size_neg,size2, seed_CMS, log);
                                l0_array[i] = l0_array[i] + lookup_0;
                                l100_array[i] = l100_array[i] + lookup_100;
                                fpp_array[i] = fpp_array[i] + fpp_final;
                                mem_pos_array[i] = mem_pos_array[i] + meanMemAccessPos;
                                mem_neg_array[i] = mem_neg_array[i] + meanMemAccessNeg;
                                mean_relative_error_array[i] = mean_relative_error_array [i] + relative_error;
                                mean_absolute_error_array[i] = mean_absolute_error_array [i] + absolute_error;
                                i += 1;
                            }                      
                        }
                
                    for (int print = 0; print <= size_test-1; print +=1){
                        l0_array[print] = l0_array[print]/run;
                        l100_array[print] = l100_array[print]/run;
                        fpp_array[print] = fpp_array[print]/run;
                        mem_pos_array[print] = mem_pos_array[print]/run;
                        mem_neg_array [print] =  mem_neg_array [print]/run;
                        mean_relative_error_array [print] = mean_relative_error_array [print]/run;
                        mean_absolute_error_array [print] = mean_absolute_error_array [print]/run;
                    
                    
                    }
                    System.out.println("");
                    System.out.println("Number of runs: " + run); 
                    System.out.println("Number of flows: " + flows);
                    System.out.println(" Total keys: " + testFreq); 
                    System.out.println(" Total no keys: " + size_neg); 
                    System.out.println(" FPP: " + Arrays.toString(fpp_array));
                    System.out.println(" Lookup 0% ns/key: " + Arrays.toString(l0_array));
                    System.out.println(" Lookup 100% ns/key: " + Arrays.toString(l100_array));
                    System.out.println(" construction time ns/key: " + (construction/run));
                    System.out.println(" Memory access/key negatives: " + Arrays.toString(mem_neg_array));
                    System.out.println(" Memory access/key positives: " + Arrays.toString(mem_pos_array));
                    System.out.println("");
                    System.out.println("### STATISTICS OF ERRORS ### "); 
                    System.out.println(name_dist);
                    System.out.println("Mean absolute error: " + Arrays.toString(mean_absolute_error_array));
                    System.out.println("Mean relative error: " + Arrays.toString(mean_relative_error_array));                   
                    System.out.println("");
                    fpp_array = new double [size_test]; 
                    l0_array = new double [size_test];
                    l100_array = new double [size_test];
                    mem_pos_array = new double [size_test];
                    mem_neg_array = new double [size_test];
                    mean_relative_error_array = new double [size_test];
                    mean_absolute_error_array = new double [size_test];
              
                    }

                
    
        private static void test(int len_neg, int len2, int seedCMS, boolean log) {

            // Generate keys with pareto, gaussian and poisson distributions and calculate the frequency

            Random random = new Random();
            Map<Integer, Integer> frequencyMap = new HashMap<>();       
            testFreq = 100; // Number of keys
            int seed_CMS = seedCMS;
            int len = len_neg;
            int dist = 0; // Distributions: for Pareto, dist = 0. For Gaussian, dist = 1.  For Poisson, dist = 2 
            long[] allKeys = new long[testFreq+len];
            switch(dist) {
                case 0:
                    double alpha = 3; // Parmeter
                    double scale = 445; // Scale (lower value)
                    
                    //System.out.println("Pareto distribution, alpha: " + alpha + " scale: " + scale);
                    name_dist = "Pareto distribution, alpha: " + alpha + " scale: " + scale;
                    for (int i = 0; i < testFreq; i++) {
                        int randomNumber = getParetoRandom(alpha, scale, random);
                        allKeys[i] = Hash.hash64(randomNumber, seed_CMS);
                        frequencyMap.put(randomNumber, frequencyMap.getOrDefault(randomNumber, 0) + 1);
                    }
                    break;
                case 1:
                    double mean = 200.0;
                    double std_d = 600.0;
                    //System.out.println("Gaussian distribution, mean: " + mean + " standard deviation: " + std_d);
                    name_dist = "Gaussian distribution, mean: " + mean + " standard deviation: " + std_d;
                    for (int i = 0; i < testFreq; i++) {
                        int randomNumber = (int) (random.nextGaussian() * std_d + mean);
                        allKeys[i] = Hash.hash64(randomNumber, seed_CMS);
                        frequencyMap.put(randomNumber, frequencyMap.getOrDefault(randomNumber, 0) + 1);
                    }
                    break;
                case 2:
                    double lambda = 10000.0;
                    //System.out.println("Poisson distribution, lambda: " + lambda);
                    name_dist = "Poisson distribution, lambda: " + lambda;
                    for (int i = 0; i < testFreq; i++) {
                        int randomNumber = getPoissonRandom(lambda, random);
                        allKeys[i] = Hash.hash64(randomNumber, seed_CMS);
                        frequencyMap.put(randomNumber, frequencyMap.getOrDefault(randomNumber, 0) + 1);
                    }
                    break;
                default:
                    System.out.println("Error: Distribution not defined.");
                    throw new AssertionError();
            }
        
            int keyC = 0;
            int seed_nonKeys = 856000;
            int frequencies = 0;
            long hashKey;
            long[] uniqueKeys = new long[frequencyMap.size()];
            long[] list = new long[len];
            RandomGenerator.createRandomUniqueListFast(list, seed_nonKeys );
            for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
                hashKey = Hash.hash64(entry.getKey(), seed_CMS);
                uniqueKeys[keyC] = hashKey;
                keyC +=1;
            }
            for (int i = 0; i < len; i++) {
                allKeys[i+testFreq] = list[i];
            }     
            //System.out.println("Total keys : " + allKeys.length );
            // System.out.println("Repeated keys : " + uniqueKeys.length );
            long time = System.nanoTime();
            Filter fCMS = XorCmsV1.construct(uniqueKeys);
            time = System.nanoTime() - time;
            double nanosPerAdd = time / keyC;
            
            for (int i = 0; i < testFreq + len; i++) {
                    fCMS.mayContain(allKeys[i]);      
                }   
            double error_a = 0;
            double error_r = 0;
            relative_error = 0;
            absolute_error = 0;
            flows = 0;
            for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
                hashKey = Hash.hash64(entry.getKey(), seed_CMS);
                frequencies = fCMS.frequency(hashKey);
                error_a = abs( entry.getValue() - frequencies );
                error_r =  (error_a)/ entry.getValue();
                absolute_error += error_a;
                relative_error += error_r;
                //System.out.println("Key " + entry.getKey() + ": " + entry.getValue() + " times " + "CMS + Xor: " + frequencies + " times");
                flows ++;
            }
            //System.out.println("Number of flows " + flows);
            absolute_error = absolute_error/keyC;
            relative_error = relative_error/keyC;
            fCMS.resetMemAccess();
            fCMS.resetCms(uniqueKeys);

            time = System.nanoTime();
            // each key in the set needs to be found          
            int falseNegatives = 0;
            for (int i = 0; i < keyC; i++) {
                if (!fCMS.mayContain(uniqueKeys[i])) {
                    falseNegatives++;
                    // f.mayContain(keys[i]);
                    // throw new AssertionError();
                }
            }   
            if (falseNegatives > 0) {
                throw new AssertionError("false negatives: " + falseNegatives);
            }
            time = System.nanoTime() - time;
            double nanosPerLookupAllInSet = time / 2 / keyC;
            meanMemAccessPos= (double) fCMS.getMemAccess() / (keyC);
            fCMS.resetMemAccess();
            fCMS.resetCms(uniqueKeys);

            //fCMS = XorCmsV1.construct(uniqueKeys);

            time = System.nanoTime();
            // non keys _may_ be found - this is used to calculate false
            // positives
            int falsePositives = 0;
            for (int i = 0; i < len; i++) {
                if (fCMS.mayContain(list[i])) {
                    falsePositives++;
                }
            }
            time = System.nanoTime() - time;
            double nanosPerLookupNoneInSet = time / 2 / len;
            fpp_final = (double) falsePositives / (len);
            meanMemAccessNeg = (double) fCMS.getMemAccess() / (len); 
            lookup_0 = nanosPerLookupNoneInSet;
            lookup_100 = nanosPerLookupAllInSet;
            construction = nanosPerAdd;
        }

        public static int abs (int number) {
            return number > 0 ? number : -number; 
            
        }

        // Generate numbers with Pareto distribution
        public static int getParetoRandom(double alpha, double scale, Random random) {
            double u = random.nextDouble();
            return (int) (scale / Math.pow(u, 1.0 / alpha));
        }

        // Generate numbers with Poisson distribution
        public static int getPoissonRandom(double lambda, Random random) {
            double L = Math.exp(-lambda);
            double p = 1.0;
            int k = 0;
            do {
                k++;
                p *= random.nextDouble();
            } while (p > L);
            return k - 1;
        }
    
    }
    
    
