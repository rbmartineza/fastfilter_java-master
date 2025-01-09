package org.fastfilter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

//import org.fastfilter.Filter;
//import org.fastfilter.TestFilterType;
import org.fastfilter.utils.Hash;
import org.fastfilter.utils.RandomGenerator;
import org.fastfilter.Newxor.Xor8Selector;
import org.fastfilter.xor.Xor15Bloom;
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

public class TestXorSelector {
	private static double lookup_0;
	private static double lookup_100;
	private static double fpp_final;
    private static double meanMemAccess;
    private static double meanMemAccessPos;


        public static void main(String... args) {
            Hash.setSeed(1);
            
                for (int size2 = 300_000; size2 <= 350_000; size2 += 100_000) {
                    int size1 =65_000_000; // Keys for the Xor filter
                    //int size2 = 500_000; // Keys for the Bloom filter
                    int size = size1 + size2;  // Total keys to insert
                    System.out.println("size " + size);
                    int test = 0;
                    test(size1,size2, test, true, 123);
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
                int run = 50;
                int size_test = 1;
                int alpha = 0;
                int[] Alpha = {100, 105, 110, 115, 120, 123};
                //int[] Alpha = {123, 120, 115, 110, 105, 100};
                double[] l0_array = new double [size_test];
                double[] l100_array = new double [size_test];
                double[] fpp_array = new double [size_test];
                double[] mem_pos_array = new double [size_test];
                double[] mem_neg_array = new double [size_test];
                for (int alpha_i = 0; alpha_i <= 5; alpha_i += 1){
                    System.out.println("Alpha: " + Alpha[alpha_i]);
                    alpha = Alpha[alpha_i];
                    for (int run_i = 1; run_i <= run; run_i += 1){
                        //System.out.println(" Run: " + run_i);
                        int i = 0;
                        int size1 =100000; // Keys for the Xor filter
                            for (int size2 = 0; size2 <= 0; size2 += 100_000) {
                            //for (int size2 = 0; size2 <= 10_000_000; size2 += 1_000_000){
                                
                                // int size2 = 500_000; // Keys for the Bloom filter
                                // int size = size1 + size2;  // Total keys to insert
                                // System.out.println("size " + size);
                                int test = 0;
                                test(size1,size2, test, log, alpha);
                        l0_array[i] = l0_array[i] + lookup_0;
                        l100_array[i] = l100_array[i] + lookup_100;
                        fpp_array[i] = fpp_array[i] + fpp_final;
                        mem_pos_array[i] = mem_pos_array[i] + meanMemAccessPos;
                        mem_neg_array[i] = mem_neg_array[i] + meanMemAccess;
                        
                                //test2(TestFilterType.BLOOM, size2, test, true);
                                //System.out.println();
                        i += 1;
                            } 
                        }
                
                    for (int print = 0; print <= size_test-1; print +=1){
                        l0_array[print] = l0_array[print]/run;
                        l100_array[print] = l100_array[print]/run;
                        fpp_array[print] = fpp_array[print]/run;
                        mem_pos_array[print] = mem_pos_array[print]/run;
                        mem_neg_array [print] =  mem_neg_array [print]/run;
                    
                    }
                    System.out.println("run: " + run); 
                    System.out.println(" lookup 0% ns/key: " + Arrays.toString(l0_array));
                    System.out.println(" lookup 100% ns/key: " + Arrays.toString(l100_array));
                    System.out.println(" fpp: " + Arrays.toString(fpp_array));
                    System.out.println(" Memory access/key negatives: " + Arrays.toString(mem_neg_array));
                    System.out.println(" Memory access/key positives: " + Arrays.toString(mem_pos_array));
                    System.out.println(" ");
                    fpp_array = new double [size_test]; 
                    l0_array = new double [size_test];
                    l100_array = new double [size_test];
                    mem_pos_array = new double [size_test];
                    mem_neg_array = new double [size_test];
                    
                    }
                     
        
                }
    
        private static void test(int len1, int len2, int seed, boolean log, int alpha) {
            int len = len1;
            long[] list = new long[len * 2];
            RandomGenerator.createRandomUniqueListFast(list, 100_000 + seed);
            long[] keys = new long[len]; // keys for XOR filter
            //long[] keysBloom = new long[len2]; // keys for Bloom filter
            //long[] keysAdded = new long[len]; // keys for Bloom filter
            long[] nonKeys = new long[len];
            // first half is keys, second half is non-keys
            for (int i = 0; i < len; i++) {
                keys[i] = list[i];
                nonKeys[i] = list[i + len];
            }
            long time = System.nanoTime();
            Filter f = Xor8Selector.construct(keys, alpha);
            time = System.nanoTime() - time;
            double nanosPerAdd = time / len;
            time = System.nanoTime();
            // each key in the set needs to be found
            
            int falseNegatives = 0;
            for (int i = 0; i < len; i++) {
                if (!f.mayContain(keys[i])) {
                    falseNegatives++;
                    // f.mayContain(keys[i]);
                    // throw new AssertionError();
                }
            }
            if (falseNegatives > 0) {
                throw new AssertionError("false negatives: " + falseNegatives);
            }
            
            time = System.nanoTime() - time;
            double nanosPerLookupAllInSet = time / 2 / len;
            meanMemAccessPos= (double) f.getMemAccess() / len;
            f.resetMemAccess();
            time = System.nanoTime();
            // non keys _may_ be found - this is used to calculate false
            // positives
            int falsePositives = 0;
            for (int i = 0; i < len; i++) {
                if (f.mayContain(nonKeys[i])) {
                    falsePositives++;
                }
            }
            time = System.nanoTime() - time;
            double nanosPerLookupNoneInSet = time / 2 / len;
            double fpp = (double) falsePositives / len;
        long bitCount = f.getBitCount();
        double bitsPerKey = (double) bitCount / len;
        double nanosPerRemove= -1;
        if (f.supportsRemove()) {
            time = System.nanoTime();
            for (int i =0; i< len ; i++) {
                f.remove(keys[i]);
            }	
                time = System.nanoTime() - time;
            nanosPerRemove = time /len;
            if (f.cardinality() != 0) {
                System.out.println(f.cardinality());
            }
        
            assertEquals(f.toString(), 0, f.cardinality());
        }
        meanMemAccess = (double) f.getMemAccess() / len; 
        lookup_0 = nanosPerLookupNoneInSet;
        lookup_100 = nanosPerLookupAllInSet;
        fpp_final = fpp;
        //System.out.println("FPP: " + fpp );

        if (log) {
            System.out.println("Xor8Bloom" + " fpp: " + fpp +
            //
            //
            //
            " lookup_0%_ns/key: " + nanosPerLookupNoneInSet +
            " lookup_100%_ns/key: " + nanosPerLookupAllInSet +
            (nanosPerRemove < 0 ? "" : (" remove ns/key: " + nanosPerRemove)));
        }
        }
    }
    
    
