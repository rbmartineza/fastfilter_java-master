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
public class Xor5BloomV4 implements Filter {

    private static final int BITS_PER_FINGERPRINT = 5;
    private static final int HASHES = 3;
    private static final int FACTOR_TIMES_100 = 100;
    private final int size;
    private final int arrayLength;
    private final int blockLength;
    private long seed;
    private byte[] fingerprints;
    // private byte[] bloom;
    private final int bitCount;

    public long getBitCount() {
        return bitCount;
    }

    private static int getArrayLength(int size) {
        return (int) (HASHES + (long) FACTOR_TIMES_100 * size / 100);
    }

    public static Xor5BloomV4 construct(long[] keys, long[] keysBloom) {
        return new Xor5BloomV4(keys, keysBloom);
    }

    public Xor5BloomV4(long[] keys, long[] keysBloom) {
        
        this.size = keys.length;
        arrayLength = getArrayLength(size);
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
        int err = 0;    
        do {
            seed = Hash.randomSeed();
            
            byte[] t2count = new byte[m];
            long[] t2 = new long[m];
            int[] bl_count = new int[m];
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
                    int a1 = -1; int a2 = -1;
                    int e1 = 0; int e2 = 0;int e3 = 0;
                    if (reverseOrderPos != size2){
                        for (int l=0; l<size; l++){
                        if (s==0){
                            break;
                        }
                         if (copykeys[l] != 0 ){
                            int c1 = 0;
                            int e = 0;
                            for (int hi = 0; hi < HASHES; hi++) {                                
                                int h = getHash(copykeys[l], seed, hi);                                
                                if (t2count[h] == 2) {
                                    //add(copykeys[l]);
                                    c1++;
                                }
                                if (bl_count[h] == 1){
                                    e++;
                                }            
                                                      
                            }
                            if (c1 == 3){
                                a = 1;
                                if (e >= 1){
                                    e1 = 1; 
                                    k1 = copykeys[l];
                                    l1 = l; 
                                    break;                                  
                                }
                                if (e1 != 1){
                                    k1 = copykeys[l];
                                    l1 = l;
                                    a1 = 1; // if a1 is equal to 1 it is not necessary continue with c==2 and c==1
                                }
                            }else if (c1 == 2  && a1 != 1){
                                a = 1;
                                if (e >= 1){
                                    e2 = 1; 
                                    k1 = copykeys[l];
                                    l1 = l;                                  
                                }
                                if (e2 != 1){
                                    k1 = copykeys[l];
                                    l1 = l;
                                    a2 = 1; // if a2 is equal to 1 it is not necessary continue with c==1
                                }
                            }else if (c1 == 1 && a1 != 1 && a2 != 1){
                                a = 1;
                                if (e >= 1){
                                    e3 = 1; 
                                    k1 = copykeys[l];
                                    l1 = l;                                  
                                }
                                if (e3 != 1){
                                    k1 = copykeys[l];
                                    l1 = l;
                                }
                            }
                         } 
                    }
                    if (a == 1){
                        for (int hi = 0; hi < HASHES; hi++) {
                            int hh = getHash(k1, seed, hi);
                            t2[hh] ^= k1; 
                            bl_count[hh] = 1;                       
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
            if (err > 10) {
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
        //bloom = new byte[m];
        for(int i = 0; i < bloomPos; i++) {
            add(bl[i]);
        }
        for(long x : keysBloom) {
            add(x);
        }
        System.out.println("Number of elements in Bloom filter: " + bloomPos);      
        //
    }

    //check if both Xor and Bloom filter have the keys

    @Override
    public boolean mayContain(long key) {
        long hash = Hash.hash64(key, seed);
        int f = fingerprint(hash);
        int b;
        int r0 = (int) hash;
        int r1 = (int) Long.rotateLeft(hash, 21);
        int r2 = (int) Long.rotateLeft(hash, 42);
        int h0 = Hash.reduce(r0, blockLength);
        int h1 = Hash.reduce(r1, blockLength) + blockLength;
        int h2 = Hash.reduce(r2, blockLength) + 2 * blockLength;
        int h0f =fingerprints[h0];
        int h1f =fingerprints[h1];
        int h2f =fingerprints[h2];
        // Xor filter verification making cero the content of the Bloom filter
        f ^= (h0f^ h1f^ h2f);

        // Bloom filter verifiation shifting 7 bits to the right
        //b = ( (h0f & 0xff) >>> 7)& ( (h1f & 0xff) >>> 7 )& ((h2f & 0xff) >>> 7); 
        //return ( (  (f & 0xff) ) == 0   || b == 1 );
        //b = (h0f & h1f & h2f) >>> 7; 
        
        // if((f & 0x7f) == 0){
        //     return (f & 0x7f) == 0;
        // }
        // int h3f =fingerprints[h0];
        // int h4f =fingerprints[h1];
        // int h5f =fingerprints[h2];
        // b = (h3f & h4f & h5f);

        b = (h0f & h1f & h2f); 
        //b = b & 0x80; 
        return ( (  (f & 0x1f) ) == 0   || (b & 0x80) == 0x80 );
        //b = b & 0x1;
        //return ( (  (f & 0xff) ) == 0   || b == 1 );
        // return (f & 0xff) == 0;
        //return ( b == 1 );
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

    @Override
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
    

    public Xor5BloomV4(InputStream in) {
        try {
            DataInputStream din = new DataInputStream(in);
            size = din.readInt();
            arrayLength = getArrayLength(size);
            bitCount = arrayLength * (BITS_PER_FINGERPRINT +1); // Adding 1, that is the column of the Bloom filter
            blockLength = arrayLength / HASHES;
            seed = din.readLong();
            fingerprints = new byte[arrayLength];
            din.readFully(fingerprints);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
