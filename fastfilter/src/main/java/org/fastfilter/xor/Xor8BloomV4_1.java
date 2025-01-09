package org.fastfilter.xor;

import java.io.*;

import org.fastfilter.Filter;
import org.fastfilter.utils.Hash;

/**
 * The xor filter, a new algorithm that can replace a Bloom filter.
 *
 * It needs 1.23 log(1/fpp) bits per key. It is related to the BDZ algorithm [1]
 * (a minimal perfect hash function algorithm).
 *
 * [1] paper: Simple and Space-Efficient Minimal Perfect Hash Functions -
 * http://cmph.sourceforge.net/papers/wads07.pdf
 */
public class Xor8BloomV4_1 implements Filter {

    private static final int BITS_PER_FINGERPRINT = 8; // For the main filter 
    private static final int BITS_PER_FINGERPRINT_AUX = 12; // For auxiliary filter
    private static final int HASHES = 3;
    private static final int FACTOR_TIMES_100 = 120;    // it can be changed
    private static final int FACTOR_TIMES_100_Aux = 123; // don't move, fix value for this version
    private final int size;
    private final int sizeAux;
    private final int arrayLength;
    private final int arrayLengthAux;
    private final int blockLength;
    private final int blockLengthAux;
    // private final int threshold;
    private long seed;
    private long seedAux;
    private byte[] fingerprints;
    private short[] fingerprintsAux;
    private final int bitCount;
    private final int bitCountAux;

    public long getBitCount() {
        return bitCount;
    }

    private static int getArrayLength(int size, int alpha) {
        return (int) (HASHES + (long) alpha * size / 100);
    }

    private static int getArrayLengthAux(int size) {
        return (int) (HASHES + (long) FACTOR_TIMES_100_Aux * size / 100);
    }

    public static Xor8BloomV4_1 construct(long[] keys, long[] keysBloom, int alpha) {
        return new Xor8BloomV4_1(keys, keysBloom, alpha);
    }

    public Xor8BloomV4_1(long[] keys, long[] keysBloom, int alpha) {
        
        this.size = keys.length;
        arrayLength = getArrayLength(size, alpha);
        bitCount = arrayLength * (BITS_PER_FINGERPRINT+1);
        blockLength = arrayLength / HASHES;
        int m = arrayLength;
        long[] copykeys = new long[size];
        for (int i=0; i< size; i++){
            copykeys[i] = keys[i];  
        } 
        long[] reverseOrder = new long[size];
        byte[] reverseH = new byte[size];
        int reverseOrderPos;
        long[] bl = new long[size];
        int bloomPos;
        long seed;
        int size2;
        // int th = -1; 
        int err = 0;  
        do {
            seed = Hash.randomSeed();
            byte[] t2count = new byte[m];
            long[] t2 = new long[m];
            //int aa = 0;
            
            reverseOrderPos = 0;
            size2 = keys.length;        
            for (long k : keys) {
                for (int hi = 0; hi < HASHES; hi++) {
                    int h = getHash(k, seed, hi);
                    t2[h] ^= k;
                    if (t2count[h] > 120) {
                        // probably something wrong with the hash function
                        throw new IllegalArgumentException();
                    }
                    t2count[h]++;
                }
            }
            long k1 = 0;
            int l1 = 0;
            bloomPos = 0;
            int[][] alone = new int[HASHES][blockLength];
            int[] alonePos = new int[HASHES];
            for (int nextAlone = 0; nextAlone < HASHES; nextAlone++) {
                for (int i = 0; i < blockLength; i++) {
                    if (t2count[nextAlone * blockLength + i] == 1) {
                        alone[nextAlone][alonePos[nextAlone]++] = nextAlone * blockLength + i;
                    }
                }
            }
            int found = -1;
            while (true) {
                int i = -1;
                for (int hi = 0; hi < HASHES; hi++) {
                    if (alonePos[hi] > 0) {
                        i = alone[hi][--alonePos[hi]];
                        found = hi;
                        break;
                    }
                }
                if (i == -1) {
                    // no entry found
                    int s = -1; int a = -1;
                    // int selector;
                    // int maxSelector = size;
                    
                    if (reverseOrderPos != size2){
                        for (int l=0; l<size; l++){
                        if (s==0){
                            break;
                        }
                         if (copykeys[l] != 0 ){
                            int c1 = 0;
                            int a1 = -1; int a2 = -1;
                            // selector = getSelector(copykeys[l] , seed); 

                            for (int hi = 0; hi < HASHES; hi++) {                                
                                int h = getHash(copykeys[l], seed, hi);                                
                                if (t2count[h] == 2) {
                                    c1++;
                                }                                  
                            }
                            if (c1 == 3){
                                a = 1; a1 = 1; l1 = l;
                                    
                            }else if (c1 == 2 && a1 != 1){
                                a = 1; a2 = 1; l1 = l;

                            }else if (c1 == 1 && a1 != 1  && a2 != 1) {
                                a = 1; l1 = l;
                                                                     
                            }                            
                         } 
                    }  

                    if (a == 1){
                        k1 = copykeys[l1];
                        for (int hi = 0; hi < HASHES; hi++) {
                            int hh = getHash(k1, seed, hi);
                            t2[hh] ^= k1;                      
                            int newCount = --t2count[hh];
                            if (newCount == 1) {
                                alone[hi][alonePos[hi]++] = hh;
                            }           
                        }
                        bl[bloomPos] = k1;
                        bloomPos ++;
                        copykeys[l1] = 0;  
                        size2 --;
                        s = 0;
                        // if (th <= maxSelector){
                        //     th = maxSelector;
                        // }
                    }

                    
                    }
                    if (s == -1){                       
                        break;
                    }  
                    continue;                      
                }
                if (t2count[i] <= 0) {
                    continue;
                }
                long k = t2[i];
                if (t2count[i] != 1) {
                    throw new AssertionError();
                }
                --t2count[i];

                for (int l=0; l<size; l++) {
                    if (k==copykeys[l]){
                        copykeys[l]=0;
                        break;
                    }
                }
                for (int hi = 0; hi < HASHES; hi++) {
                    if (hi != found) {
                        int h = getHash(k, seed, hi);
                        int newCount = --t2count[h];
                        if (newCount == 1) {
                            alone[hi][alonePos[hi]++] = h;
                        }
                        t2[h] ^= k;
                    }
                }
                reverseOrder[reverseOrderPos] = k;
                reverseH[reverseOrderPos] = (byte) found;
                reverseOrderPos++;
            }
            err++;
            if (err > 100) {
                // Still changing the seed
                System.out.println("IT IS TAKING MANY TRIES TO CONVERGE");
                throw new IllegalArgumentException();
            }
        } while (reverseOrderPos != size2);
        this.seed = seed;
        byte[] fp = new byte[m];
        for (int i = reverseOrderPos - 1; i >= 0; i--) {
            long k = reverseOrder[i];
            int found = reverseH[i];
            int change = -1;
            long hash = Hash.hash64(k, seed);
            int xor = fingerprint(hash);
            for (int hi = 0; hi < HASHES; hi++) {
                int h = getHash(k, seed, hi);
                if (found == hi) {
                    change = h;
                } else {
                    xor ^= fp[h];
                }
            }
            fp[change] = (byte) xor;
        }
        fingerprints = new byte[m];
        System.arraycopy(fp, 0, fingerprints, 0, fp.length);
        // threshold = th;
        long [] auxKeys= new long[bloomPos];
        System.arraycopy(bl, 0, auxKeys, 0, bloomPos);

        sizeAux = auxKeys.length;
        arrayLengthAux = getArrayLengthAux(sizeAux);
        bitCountAux = arrayLengthAux * BITS_PER_FINGERPRINT;
        blockLengthAux = arrayLengthAux / HASHES;
        aux(auxKeys);
        
        /*for(long x : keysBloom) {
            add(x);
        }
        */
        // System.out.println("Number of elements in Auxiliary Xor filter: " + bloomPos);
        //  System.out.println("Threshold " + threshold);      

        //
    }

    //check if both Xor and Bloom filter have the keys

    @Override
    public boolean mayContain(long key) {
        long hash = Hash.hash64(key, seed);
        long hashAux = Hash.hash64(key, seedAux);
        int f = fingerprint(hash);
        int f_Aux = fingerprintAux(hashAux);
        // int selector = getSelector(key , seed); 
        int r0 = (int) hash;
        int r1 = (int) Long.rotateLeft(hash, 21);
        int r2 = (int) Long.rotateLeft(hash, 42);
        int h0 = Hash.reduce(r0, blockLength);
        int h1 = Hash.reduce(r1, blockLength) + blockLength;
        int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
        int h0f =fingerprints[h0];
        int h1f =fingerprints[h1];
        int h2f =fingerprints[h2];
        
        f ^= (h0f^ h1f^ h2f);
        
        //selector > threshold
        // else if (selector <= threshold){
        
            int r0_a = (int) hashAux;
            int r1_a = (int) Long.rotateLeft(hashAux, 21);
            int r2_a = (int) Long.rotateLeft(hashAux, 42);
            int h0_a = Hash.reduce(r0_a, blockLengthAux);
            int h1_a = Hash.reduce(r1_a, blockLengthAux) + blockLengthAux;
            int h2_a = Hash.reduce(r2_a, blockLengthAux) + 2 * blockLengthAux;
            int h0f_a =fingerprintsAux[h0_a];
            int h1f_a =fingerprintsAux[h1_a];
            int h2f_a =fingerprintsAux[h2_a];
            f_Aux ^= (h0f_a^ h1f_a^ h2f_a);
            return ( (  (f & 0xff) == 0 )   || ( (f_Aux & 0xfff) == 0) );
        
    }

    private int getHash(long key, long seed, int index) {
        long r = Long.rotateLeft(Hash.hash64(key, seed), 21 * index);
        r = Hash.reduce((int) r, blockLength);
        r = r + index * blockLength;
        return (int) r;
    }

    private int getHashAux(long key, long seed, int index) {
        long r = Long.rotateLeft(Hash.hash64(key, seed), 21 * index);
        r = Hash.reduce((int) r, blockLengthAux);
        r = r + index * blockLengthAux;
        return (int) r;
    }


    // private int getSelector(long key, long seed) {
    //     long r = Long.rotateLeft(Hash.hash64(key, seed), 3*21);
    //     r = Hash.reduce((int) r, size);
    //     return (int) r;
    // }

    private int fingerprint(long hash) {
        return (int) (hash & ((1 << BITS_PER_FINGERPRINT) - 1));
    }

    private int fingerprintAux(long hash) {
        return (int) (hash & ((1 << BITS_PER_FINGERPRINT_AUX) - 1));
    }

    public byte[] getData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream d = new DataOutputStream(out);
            d.writeInt(size);
            d.writeLong(seed);
            d.write(fingerprints);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

// Add keys on the Bloom Filter after complete the insertions on the Xor Filter

    /*@Override
    public void add(long key) {
        
            long hash = Hash.hash64(key, seed);
            int r0 = (int) hash;
            int r1 = (int) Long.rotateLeft(hash, 21);
            int r2 = (int) Long.rotateLeft(hash, 42);
            int h0 = Hash.reduce(r0, blockLength);
            int h1 = Hash.reduce(r1, blockLength) + blockLength;
            int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
            fingerprints[h0] |= 0x80; //Making OR with 1 at the hash position 
            fingerprints[h1] |= 0x80;
            fingerprints[h2] |= 0x80;
        
    }
    */
   
// Add keys in the auxiliary xor filter

    @Override
    public void aux(long [] keys) {
        
        int m = arrayLengthAux;
        long[] reverseOrder = new long[sizeAux];
        byte[] reverseH = new byte[sizeAux];
        int reverseOrderPos;
        long seedAux;
        do {
            seedAux = Hash.randomSeed();
            byte[] t2count = new byte[m];
            long[] t2 = new long[m];
            for (long k : keys) {
                for (int hi = 0; hi < HASHES; hi++) {
                    int h = getHashAux(k, seedAux, hi);
                    t2[h] ^= k;
                    if (t2count[h] > 120) {
                        // probably something wrong with the hash function
                        throw new IllegalArgumentException();
                    }
                    t2count[h]++;
                }
            }
            reverseOrderPos = 0;
            int[][] alone = new int[HASHES][blockLengthAux];
            int[] alonePos = new int[HASHES];
            for (int nextAlone = 0; nextAlone < HASHES; nextAlone++) {
                for (int i = 0; i < blockLengthAux; i++) {
                    if (t2count[nextAlone * blockLengthAux + i] == 1) {
                        alone[nextAlone][alonePos[nextAlone]++] = nextAlone * blockLengthAux + i;
                    }
                }
            }
            int found = -1;
            while (true) {
                int i = -1;
                for (int hi = 0; hi < HASHES; hi++) {
                    if (alonePos[hi] > 0) {
                        i = alone[hi][--alonePos[hi]];
                        found = hi;
                        break;
                    }
                }
                if (i == -1) {
                    // no entry found
                    break;
                }
                if (t2count[i] <= 0) {
                    continue;
                }
                long k = t2[i];
                if (t2count[i] != 1) {
                    throw new AssertionError();
                }
                --t2count[i];
                for (int hi = 0; hi < HASHES; hi++) {
                    if (hi != found) {
                        int h = getHashAux(k, seedAux, hi);
                        int newCount = --t2count[h];
                        if (newCount == 1) {
                            alone[hi][alonePos[hi]++] = h;
                        }
                        t2[h] ^= k;
                    }
                }
                reverseOrder[reverseOrderPos] = k;
                reverseH[reverseOrderPos] = (byte) found;
                reverseOrderPos++;
            }
        } while (reverseOrderPos != sizeAux);
        this.seedAux = seedAux;
        short[] fp = new short[m];
        for (int i = reverseOrderPos - 1; i >= 0; i--) {
            long k = reverseOrder[i];
            int found = reverseH[i];
            int change = -1;
            long hash = Hash.hash64(k, seedAux);
            int xor = fingerprintAux(hash);
            for (int hi = 0; hi < HASHES; hi++) {
                int h = getHashAux(k, seedAux, hi);
                if (found == hi) {
                    change = h;
                } else {
                    xor ^= fp[h];
                }
            }
            fp[change] = (short) xor;
        }
        fingerprintsAux = new short[m];
        System.arraycopy(fp, 0, this.fingerprintsAux, 0 , fp.length);
        
    }
    

}
