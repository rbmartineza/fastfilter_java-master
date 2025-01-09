package org.fastfilter;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Arrays;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


//import org.fastfilter.Filter;
//import org.fastfilter.TestFilterType;
import org.fastfilter.utils.Hash;
import org.fastfilter.utils.RandomGenerator;
import org.fastfilter.xor.Xor8Bloom;
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

public class TestXorBloomUrl {
	private static double lookup_0;
	private static double lookup_100;
	private static double fpp_final;

    public static void main(String... args) {
        Hash.setSeed(1);
        /*
        for (int size = 1_000_000; size <= 10_000_000; size *= 10) {
            System.out.println("size " + size);
            for (int test = 0; test < 10; test++) {
                test(TestFilterType.BLOOM, size, test, true);
                test(TestFilterType.BLOCKED_BLOOM, size, test, true);
                test(TestFilterType.COUNTING_BLOOM, size, test, true);
                test(TestFilterType.SUCCINCT_COUNTING_BLOOM, size, test, true);
                test(TestFilterType.SUCCINCT_COUNTING_BLOOM_RANKED, size, test, true);
            }
        }
        */
        /*
        for (int size = 1; size <= 100; size++) {
            System.out.println("size " + size);
            test(TestFilterType.XOR_BINARY_FUSE_8, size, 0, true);
        }
        for (int size = 100; size <= 100000; size *= 1.1) {
            System.out.println("size " + size);
            test(TestFilterType.XOR_BINARY_FUSE_8, size, 0, true);
        }
        */
        //for (int test = 0; test <= 9; test += 1){
           // System.out.println("run:" + test);
           //for (int size2 = 100_000; size2 <= 800_000; size2 += 100_000) {
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
        //}
        //System.out.println();
        /*for (int size = 10_000_000; size <= 80_000_000; size *= 2) {
            System.out.println("size " + size);
            testAll(size, true);
            System.out.println();
        }
        testAll(100_000_000, true);
        */
    }

    @Test
    public void test() {
        testAll(100000, false);
    }

    private static void testAll(int len, boolean log) {
       // for (TestFilterType type : TestFilterType.values()) {
            //test(type, len, 0, log);
            //test(len,len, 0, log);
	int run = 1000;
	int size_test = 6;
	double[] l0_array = new double [size_test];
	double[] l100_array = new double [size_test];
	double[] fpp_array = new double [size_test];
	for (int run_i = 1; run_i <= run; run_i += 1){
		//System.out.println("Run: " + run_i);
		int i = 0;
        int lenHash = 99000;
        int lenHashAdd = 5438;
        int test = (int)(Math.random()*100000+1);
            for (int size2 = 0; size2 <= lenHashAdd; size2 += 1_000) {
            //for (int size2 = 0; size2 <= 10_000_000; size2 += 1_000_000){  
                test(lenHash,size2, test, log);
                l0_array[i] = l0_array[i] + lookup_0;
                l100_array[i] = l100_array[i] + lookup_100;
                fpp_array[i] = fpp_array[i] + fpp_final;
                //test2(TestFilterType.BLOOM, size2, test, true);
                //System.out.println();
		        i += 1;
            }
        }

	for (int print = 0; print <= size_test-1; print +=1){
		l0_array[print] = l0_array[print]/run;
		l100_array[print] = l100_array[print]/run;
		fpp_array[print] = fpp_array[print]/run;
	
	}
	System.out.println(" lookup 0% ns/key: " + Arrays.toString(l0_array));
	System.out.println(" lookup 100% ns/key: " + Arrays.toString(l100_array));
	System.out.println(" fpp: " + Arrays.toString(fpp_array));
    }

    private static void test(int lenHash, int lenHashAdd, int seed, boolean log) {
        int len = lenHash + lenHashAdd;
        // Total malicious = 104438
        long[] keysMalTotal = KeysMalicious(lenHash + lenHashAdd, 100_000 + seed);
        long[] keysBen = KeysBenign(lenHash + lenHashAdd, 100_000 + seed);
        long[] keysMal = new long[lenHash];
        long[] keysMalAdd = new long[lenHashAdd];

        for (int i = 0; i < lenHashAdd; i++){
            keysMalAdd[i] = keysMalTotal[i+ lenHash];
        }
        for (int i = 0; i < lenHash; i++){
            keysMal[i] = keysMalTotal[i];
        }

        long time = System.nanoTime();
        Filter f = Xor8Bloom.construct(keysMal, keysMalAdd);
        time = System.nanoTime() - time;
        double nanosPerAdd = time / len;
        time = System.nanoTime();
        // each key in the set needs to be found
        
        int falseNegatives = 0;
        for (int i = 0; i < lenHash; i++) {
            if (!f.mayContain(keysMalTotal[i])) {
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
        time = System.nanoTime();
        // non keys _may_ be found - this is used to calculate false
        // positives
        int falsePositives = 0;
        for (int i = 0; i < lenHash; i++) {
            if (f.mayContain(keysBen[i])) {
                falsePositives++;
            }
        }
        time = System.nanoTime() - time;
        double nanosPerLookupNoneInSet = time / 2 / len;
        double fpp = (double) falsePositives / len;
	long bitCount = f.getBitCount();
	double bitsPerKey = (double) bitCount / len;
	double nanosPerRemove= -1;
	
	lookup_0 = nanosPerLookupNoneInSet;
	lookup_100 = nanosPerLookupAllInSet;
	fpp_final = fpp;
    	//System.out.println(" false positive: " + fpp_final);

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



    public static long[] KeysMalicious(int lenHash, int seedh) {
        
        String csvFile = "/home/boris/Projects/fastfilter_java-master/fastfilter/src/test/java/org/fastfilter/urldata.csv";

        // Lista para almacenar las líneas con cuarto campo igual a 1
        //List<long> linesWithFieldEqualsOne = new ArrayList<>();
        long[] linesWithFieldEqualsOne = new long[lenHash];

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int i = 0;
            long tmp = 0;
            long lineHash = 0;
            //while ((line = br.readLine()) != null) {
            while ((line = br.readLine()) != null) {
                // Dividir la línea por comas para obtener los campos
                String[] fields = line.split(",");

                // Verificar si el cuarto campo es igual a 1
                if (fields.length >= 4 && "1".equals(fields[3].trim())) {
                    tmp = Long.parseLong(fields[0].trim());
                    lineHash = Hash.hash64(tmp, seedh);
                    linesWithFieldEqualsOne[i] = lineHash;
                    i += 1;
                }
                if (i >= lenHash)
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (linesWithFieldEqualsOne);

        // Imprimir las líneas con cuarto campo igual a 1
        /*System.out.println("Líneas con cuarto campo igual a 1:");
        for (String line : linesWithFieldEqualsOne) {
            System.out.println(line);
        }*/
    }

    public static long[] KeysBenign(int lenHash, int seedh) {
        
        String csvFile = "/home/boris/Projects/fastfilter_java-master/fastfilter/src/test/java/org/fastfilter/urldata.csv";

        // Lista para almacenar las líneas con cuarto campo igual a 1
        //List<long> linesWithFieldEqualsOne = new ArrayList<>();
        long[] linesWithFieldEqualsZero = new long[lenHash];

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int i = 0;
            long tmp = 0;
            long lineHash = 0;
            while ((line = br.readLine()) != null) {
                // Dividir la línea por comas para obtener los campos
                String[] fields = line.split(",");

                // Verificar si el cuarto campo es igual a 1
                if (fields.length >= 4 && "0".equals(fields[3].trim())) {
                    tmp = Long.parseLong(fields[0].trim());
                    lineHash = Hash.hash64(tmp, seedh);
                    linesWithFieldEqualsZero[i] = lineHash;
                    i += 1;
                }
                if (i >= lenHash)
                break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (linesWithFieldEqualsZero);

        // Imprimir las líneas con cuarto campo igual a 1
        /*System.out.println("Líneas con cuarto campo igual a 1:");
        for (String line : linesWithFieldEqualsOne) {
            System.out.println(line);
        }*/
    }



}
