package org.fastfilter;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.fastfilter.Filter;
import org.fastfilter.TestFilterType;
import org.fastfilter.utils.Hash;
import org.fastfilter.utils.RandomGenerator;
import org.junit.Test;
import org.fastfilter.xor.XorBinaryFuse8BloomSerial;


public class TestBinaryFuseBloomSerial {

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
        
        for (int size = 1; size <= 100; size++) {
            System.out.println("size " + size);
            test(TestFilterType.XOR_BINARY_FUSE_8, size, size, 0, true);
        }
        /*
        for (int size = 100; size <= 100000; size *= 1.1) {
            System.out.println("size " + size);
            test(TestFilterType.XOR_BINARY_FUSE_8, size, 0, true);
        }
        */
        /*
        for (int size = 1_000_000; size <= 8_000_000; size *= 2) {
            System.out.println("size " + size);
            testAll(size, true);
            System.out.println();
        }
        System.out.println();
        for (int size = 10_000_000; size <= 80_000_000; size *= 2) {
            System.out.println("size " + size);
            testAll(size, true);
            System.out.println();
        }
        */
        //int size = 10_200_000;
        //for (int test = 0; test <= 40; test += 4){  
          //  System.out.println("run:" + test);
          // for (int size = 10_100_000; size <= 10_800_000; size += 100_000) {
            for (int size = 70_000_000; size <= 70_100_000; size += 150_000) {
           // for (int size = 11_000_000; size <= 12_000_000; size += 200_000) {
                //int size2 = 60_000;
                System.out.println("size " + size);
                //testAll(size, true);
                int test = 0;
                test(TestFilterType.XOR_16, size, size, test, true);
                //test2(TestFilterType.BLOOM, size2, test, true);
                System.out.println();
            }
        //}
        //testAll(100_000_000, true);
    }

    @Test
    public void test() {
	int run = 4;
	int size_test = 11;
	double [] l0_array = new double[size_test];
	double [] l100_array = new double[size_test];
	double [] fpp_array = new double[size_test];
	for (int run_i = 1; run_i <= run; run_i +=1){
		System.out.println("Run: " + run_i);
		int i = 0;
        int size1 =100_000_000; // Keys for the Xor filter
 		for (int size2 = 0; size2 <= 10_000_000; size2 += 1_000_000) {
		//for(int size_i = 100_000_000; size_i <= 110_000_000; size_i += 1_000_000){
            int size = size1 + size2;  // Total keys to insert
       		testAll(size1, size2, false);
		l0_array[i] = l0_array[i] + lookup_0;
		l100_array[i] = l100_array[i] + lookup_100;
		fpp_array[i] = fpp_array[i] + fpp_final;
		i +=1;
		}
        }
	for (int print = 0; print <= size_test-1; print += 1) {
	l0_array[print] = l0_array[print]/run;
	l100_array[print] = l100_array[print]/run;
	fpp_array[print] = fpp_array[print]/run;
	}
	System.out.println("lookup 0% ns/ key: " + Arrays.toString(l0_array));
	System.out.println("lookup 100% ns/ key: " + Arrays.toString(l100_array));	
	System.out.println("fpp " + Arrays.toString(fpp_array));
    }

    private static void testAll(int len1, int len2, boolean log) {
       TestFilterType type = TestFilterType.XOR_BINARY_FUSE_8_BLOOM_SERIAL;
       //TestFilterType type = TestFilterType.XOR_16;
       //System.out.println("size " + len);
            test(type, len1, len2, 0, log);
            //test(TestFilterType.XOR_8, len, 0, log);
        

            
       
    }

    private static void test(TestFilterType type, int len1, int len2, int seed, boolean log) {
        int len = len1 + len2;
        long[] list = new long[len * 2];
        RandomGenerator.createRandomUniqueListFast(list, 100_000 + seed);
        long[] keys = new long[len1]; // keys for XOR filter
        long[] keysBloom = new long[len2]; // keys for Bloom filter
        long[] keysAdded = new long[len]; // keys for Bloom filter
        long[] nonKeys = new long[len];
        // first half is keys, second half is non-keys
        for (int i = 0; i < len; i++) {
            //keys[i] = list[i];
            nonKeys[i] = list[i + len];
        }
        for (int i2 = 0; i2 < len1; i2++) {
            keys[i2] = list[i2];
        }
        for (int i3 = len1; i3 < len; i3++) {
            keysBloom[i3 - len1] = list[i3];
        }
        for (int i4 = 0; i4 < len; i4++) {
            keysAdded[i4] = list[i4];
        }
        long time = System.nanoTime();
        Filter f = type.construct(keys, 10);
        for(long x : keysBloom) {
            f.add(x);
        }
        time = System.nanoTime() - time;
        double nanosPerAdd = time / len;
        time = System.nanoTime();
        // each key in the set needs to be found
        int falseNegatives = 0;
        for (int i = 0; i < len; i++) {
            if (!f.mayContain(keysAdded[i])) {
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
        double nanosPerRemove = -1;
        if (f.supportsRemove()) {
            time = System.nanoTime();
            for (int i = 0; i < len; i++) {
                f.remove(keys[i]);
            }
            time = System.nanoTime() - time;
            nanosPerRemove = time / len;
if (f.cardinality() != 0) {
    System.out.println(f.cardinality());
}
            assertEquals(f.toString(), 0, f.cardinality());
        }
	lookup_0 = nanosPerLookupNoneInSet;
	lookup_100 = nanosPerLookupAllInSet;
	fpp_final = fpp;
        if (log) {
            System.out.println(" fpp: " + fpp +
                  // " size: " + len +
                  // " bits/key: " + bitsPerKey +
                  // " add ns/key: " + nanosPerAdd +
                   " lookup 0% ns/key: " + nanosPerLookupNoneInSet +
                   " lookup 100% ns/key: " + nanosPerLookupAllInSet +
                   (nanosPerRemove < 0 ? "" : (" remove ns/key: " + nanosPerRemove)));
        }
    }

}
