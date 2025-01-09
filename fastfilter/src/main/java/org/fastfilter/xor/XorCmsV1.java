package org.fastfilter.xor;

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
public class XorCmsV1 implements Filter {

    private static final int BITS_PER_FINGERPRINT = 8;
    private static final int HASHES = 3;
    private static final int FACTOR_TIMES_100 = 123;
    private final int blockLength;
    private long seed;
    private int[] fingerprints;
    private int[] fingerprintsCopy;
    private final int bitCount;
    private int memAccess;

    public long getBitCount() {
        return bitCount;
    }

    private static int getArrayLength(int size) {
        return (int) (HASHES + (long) FACTOR_TIMES_100 * size / 100);
    }

    public static XorCmsV1 construct(long[] keys) {
        return new XorCmsV1(keys);
    }

    public XorCmsV1(long[] keys) {
        int size = keys.length;
        int arrayLength = getArrayLength(size);
        bitCount = arrayLength * BITS_PER_FINGERPRINT+1;
        blockLength = arrayLength / HASHES;
        long[] reverseOrder = new long[size];
        byte[] reverseH = new byte[size];
        int reverseOrderPos;
        long seed;
        do {
            seed = Hash.randomSeed();
            byte[] t2count = new byte[arrayLength];
            long[] t2 = new long[arrayLength];
            for (long k : keys) {
                for (int hi = 0; hi < HASHES; hi++) {
                    int h = getHash(k, seed, hi);
                    t2[h] ^= k;
                    if (t2count[h] > 120) {
                        throw new IllegalArgumentException();
                    }
                    t2count[h]++;
                }
            }
            int[] alone = new int[arrayLength];
            int alonePos = 0;
            reverseOrderPos = 0;
            for (int nextAloneCheck = 0; nextAloneCheck < arrayLength; ) {
                while (nextAloneCheck < arrayLength) {
                    if (t2count[nextAloneCheck] == 1) {
                        alone[alonePos++] = nextAloneCheck;
                        // break;
                    }
                    nextAloneCheck++;
                }
                while (alonePos > 0) {
                    int i = alone[--alonePos];
                    if (t2count[i] == 0) {
                        continue;
                    }
                    long k = t2[i];
                    byte found = -1;
                    for (int hi = 0; hi < HASHES; hi++) {
                        int h = getHash(k, seed, hi);
                        int newCount = --t2count[h];
                        if (newCount == 0) {
                            found = (byte) hi;
                        } else {
                            if (newCount == 1) {
                                alone[alonePos++] = h;
                            }
                            t2[h] ^= k;
                        }
                    }
                    reverseOrder[reverseOrderPos] = k;
                    reverseH[reverseOrderPos] = found;
                    reverseOrderPos++;
                }
            }
        } while (reverseOrderPos != size);
        this.seed = seed;
        int[] fp = new int[arrayLength];
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
            fp[change] = (int) xor;
        }
        fingerprints = new int[arrayLength];
        System.arraycopy(fp, 0, fingerprints, 0, fp.length);
        for(long x : keys) {
           for (int hi = 0; hi < HASHES; hi++) {
                int h = getHash(x, seed, hi);
                fingerprints[h] += 1 << 8; 
           }
        }
        fingerprintsCopy = new int[arrayLength];
        System.arraycopy(fingerprints, 0, fingerprintsCopy, 0, fingerprints.length);
    }

    @Override
    public boolean mayContain(long key) {
        long hash = Hash.hash64(key, seed);
        int f = fingerprint(hash);
        int r0 = (int) hash;
        int r1 = (int) Long.rotateLeft(hash, 21);
        int r2 = (int) Long.rotateLeft(hash, 42);
        int h0 = Hash.reduce(r0, blockLength);
        int h1 = Hash.reduce(r1, blockLength) + blockLength;
        int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
        int h0f =fingerprints[h0];
        int h1f =fingerprints[h1];
        int h2f =fingerprints[h2];
        memAccess += 3;
        f ^= h0f ^ h1f ^ h2f;
        if ((f & 0x000000ff) == 0){        
            if ( ( (h0f >> 8) * (h1f >> 8) * (h2f >> 8) ) == 0){
            //if ( ( ((h0f >> 8) | (h1f >> 8) | (h2f >> 8)) & 0xff00) != 0){ 
                return (false);
            }
            else{
                int c0 = (h0f + (1  << 8));
                int c1 = (h1f + (1  << 8));
                int c2 = (h2f + (1  << 8));
                if ( ( (h0f >> 8) & 0x000FFFFF ) >= 1_000_000){
                 System.out.println("Exceed the counter! counters c0= " + ( (h0f >> 8) & 0x0FFFFF )  + " c1: "+ ( (h1f >> 8) & 0x0FFFFF )  +" c2: "+ ( (h2f >> 8) & 0x0FFFFF ) );
                    throw new AssertionError();
                }
                else if ( ( (h1f >> 8)  & 0x00000FFFFF) >= 1_000_000){
                 System.out.println("Exceed the counter! counters c0= " + ( (h0f >> 8) & 0x0FFFFF )  + " c1: "+ ( (h1f >> 8) & 0x0FFFFF )  +" c2: "+ ( (h2f >> 8) & 0x0FFFFF ) );
                    throw new AssertionError();
                }
                else if ( ( (h2f >> 8)  & 0x00000FFFFF) >= 1_000_000){
                 System.out.println("Exceed the counter! counters c0= " + ( (h0f >> 8) & 0x0FFFFF )  + " c1: "+ ( (h0f >> 8) & 0x0FFFFF )  +" c2: "+ ( (h2f >> 8) & 0x0FFFFF ) );
                    throw new AssertionError();
                }
                fingerprints[h0] = c0;
                fingerprints[h1] = c1;
                fingerprints[h2] = c2;
                memAccess += 3;  
                
                
            }               
        }
        return (f & 0x000000ff) == 0;
    }

    private int getHash(long key, long seed, int index) {
        long r = Long.rotateLeft(Hash.hash64(key, seed), 21 * index);
        r = Hash.reduce((int) r, blockLength);
        r = r + index * blockLength;
        return (int) r;
    }

    private int fingerprint(long hash) {
        return (int) (hash & ((1 << BITS_PER_FINGERPRINT) - 1));
    }

    public int getMemAccess() {
        return (int) memAccess;
    }

    public void resetMemAccess() {
        memAccess = 0 ;
    }

    public void resetCms(long[] keys) {
        System.arraycopy(fingerprintsCopy, 0, fingerprints, 0, fingerprintsCopy.length);
    }

    // Frequency compute the minimum number of repetition of a key with CMS
@Override
    public int frequency(long key) {
        
            long hash = Hash.hash64(key, seed);
            int f = fingerprint(hash);
            int r0 = (int) hash;
            int r1 = (int) Long.rotateLeft(hash, 21);
            int r2 = (int) Long.rotateLeft(hash, 42);
            int h0 = Hash.reduce(r0, blockLength);
            int h1 = Hash.reduce(r1, blockLength) + blockLength;
            int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
            int h0f =fingerprints[h0];
            int h1f =fingerprints[h1];
            int h2f =fingerprints[h2];
            f ^= h0f ^ h1f ^ h2f;
            long lower;
            if ((f & 0x000000ff) == 0 || ( ( (h0f >> 8) * (h1f >> 8) * (h2f >> 8) ) != 0)){
                long c0 = (h0f  >> 8) & 0x000fffffL;
                long c1 = (h1f  >> 8) & 0x000fffffL;
                long c2 = (h2f  >> 8) & 0x000fffffL;               
                if (c1 < c0 && c1 < c2) {
                     lower = c1;                    
                    }
                else if (c2 < c0 && c2 < c1){
                    lower = c2; 
                } else{
                    lower = c0; 
                }
                lower = (lower -1);
                return (int) (lower);
            }
            else{
                return (0); 
            }
    }

    // Only if the filter accept additions, otherwise don't use.
    @Override
    public void add(long key) {
        
            long hash = Hash.hash64(key, seed);
            int r0 = (int) hash;
            int r1 = (int) Long.rotateLeft(hash, 21);
            int r2 = (int) Long.rotateLeft(hash, 42);
            int h0 = Hash.reduce(r0, blockLength);
            int h1 = Hash.reduce(r1, blockLength) + blockLength;
            int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
            fingerprints[h0] |= 0x8000; //Making OR with 1 at the hash position 
            fingerprints[h1] |= 0x8000;
            fingerprints[h2] |= 0x8000; 
     
    }

}
