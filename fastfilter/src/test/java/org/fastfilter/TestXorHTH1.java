package org.fastfilter;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

import org.fastfilter.Filter;
import org.fastfilter.TestFilterType;
import org.fastfilter.utils.Hash;
import org.fastfilter.utils.RandomGenerator;
import org.fastfilter.xor.Xor8;
import org.junit.Test;



public class TestXorHTH1 {

 private static double lookup_0;
 private static double lookup_100;
 private static double fpp_final;

    public static void main(String... args) {
        Hash.setSeed(1);
    
             for (int size2 = 300_000; size2 <= 350_000; size2 += 100_000) {
                int size1 =65_000_000; // Keys for the Xor filter
                int size = size1 + size2;  // Total keys to insert
                System.out.println("size " + size);
                int test = 0;
                test(size1,size2, test, true);
                System.out.println();
            }
    }

    @Test
    public void test() {
        testAll(100000, false);
    }

    private static void testAll(int len, boolean log) {
       // for (TestFilterType type : TestFilterType.values()) {
            //test(type, len, 0, log);
            //test(len,len, 0, log);
	int run = 100000;
	int size_test = 1;
	double[] l0_array = new double [size_test];
	double[] l100_array = new double [size_test];
	double[] fpp_array = new double [size_test];
	for (int run_i = 1; run_i <= run; run_i += 1){
		//System.out.println("Run: " + run_i);
		int i = 0;
        int lenHash = 99000;
        int lenHashAdd = 5438;
        int test = (int)(Math.random()*100000+1);
            for (int size2 = 0; size2 <= 0; size2 += 1) {
            //for (int size2 = 0; size2 <= 10_000_000; size2 += 1_000_000){  
                test(lenHash,size2, test, log);
                l0_array[i] = l0_array[i] + lookup_0;
                l100_array[i] = l100_array[i] + lookup_100;
                fpp_array[i] = fpp_array[i] + fpp_final;
		        i += 1;
            }
        }

	for (int print = 0; print <= size_test-1; print +=1){
		l0_array[print] = l0_array[print]/run;
		l100_array[print] = l100_array[print]/run;
		fpp_array[print] = fpp_array[print]/run;
	
	}
    System.out.println("Number of runs: " + run);
	System.out.println(" lookup 0% ns/key: " + Arrays.toString(l0_array));
	System.out.println(" lookup 100% ns/key: " + Arrays.toString(l100_array));
	System.out.println(" fpp: " + Arrays.toString(fpp_array));
    }

    private static void test(int lenHash, int lenHashAdd, int seed, boolean log) {
        //int len = lenHash;
        long[] keys = KeysBenign(lenHash, 100_000_000 + seed);
        //int len = String.valueOf(keys).length();
        int len = 11117;
        long[] list = new long[len];
        RandomGenerator.createRandomUniqueListFast(list, 100_000 + seed);
        long[] nonKeys = new long[len];
        for (int i = 0; i < len; i++) {
            nonKeys[i] = list[i];
        }
        long time = System.nanoTime();
        Filter f = Xor8.construct(keys);
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

    public static long[] KeysBenign(int lenHash, int seedh) {
        String txtFile = "/home/boris/Projects/fastfilter_java-master/fastfilter/src/test/java/org/fastfilter/notrepeated.txt";
        //int lenLines = 11934;
        int lenLines = 11117;
        long tmp;
        long[] keys = new long[lenLines];
        try (BufferedReader br = new BufferedReader(new FileReader(txtFile))) {
            String line;
            int i = 0;
            while ((line = br.readLine()) != null) {
                    //BigInteger bi = new BigInteger(line, 16);
                    //tmp = bi.longValue();
                    //tmp = hex2decimal(line);
                    //keys[i]= Hash.hash64(tmp, seedh);
                    tmp = hex2decimal(line);
                    keys[i]= Hash.hash64(tmp, seedh);;
                    i += 1;
            }
        } catch (IOException e) {
            e.printStackTrace();
            }
        return (keys);
    }

    public static long hex2decimal(String s) {
        String digits = "0123456789ABCDEF";
        s = s.toUpperCase();
        long val = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            long d = digits.indexOf(c);
            val = 16*val + d;
        }
        return val;
    }
}