package com.carrotsearch.hppc;

import static it.unimi.dsi.fastutil.HashCommon.arraySize;
import gnu.trove.map.hash.TIntIntHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.hash.MurmurHash3.IntMurmurHash;

public class BigramCountingBase
{
    /* Prepare some test data */
    public static char [] DATA;

    /* Prevent dead code removal. */
    public volatile int guard;

    @BeforeClass
    public static void prepareData() throws IOException
    {
        byte [] dta = IOUtils.toByteArray(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("books-polish.txt"));
        DATA = new String(dta, "UTF-8").toCharArray();
    }

    @Test
    public void hppc()
    {
        // [[[start:bigram-counting]]]
        // Some character data
        final char [] CHARS = DATA;
        
        // We'll use a int -> int map for counting. A bigram can be encoded
        // as an int by shifting one of the bigram's characters by 16 bits
        // and then ORing the other character to form a 32-bit int.
    
        /* 
         * The input data is specific; it has low variance on lower bits and
         * high variance overall. We need to use a better hashing function
         * than simple identity. MurmurHash is good enough.
         */ 
        final IntIntOpenHashMap map = new IntIntOpenHashMap(
            IntIntOpenHashMap.DEFAULT_CAPACITY, 
            IntIntOpenHashMap.DEFAULT_LOAD_FACTOR, 
            new IntMurmurHash());
    
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.putOrAdd(bigram, 1, 1);
        }
        // [[[end:bigram-counting]]]

        guard = map.size();
    }

    @Test
    public void trove()
    {
        final char [] CHARS = DATA;
        final TIntIntHashMap map = new TIntIntHashMap();
    
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.adjustOrPutValue(bigram, 1, 1);
        }
    
        guard = map.size();
    }

    @Test
    public void mahoutCollections()
    {
        final char [] CHARS = DATA;
        final org.apache.mahout.math.map.OpenIntIntHashMap map = 
            new org.apache.mahout.math.map.OpenIntIntHashMap();
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.adjustOrPutValue(bigram, 1, 1);
        }
    
        guard = map.size();
    }

    @SuppressWarnings("serial")
    private static class Int2IntOpenHashMapExt extends Int2IntOpenHashMap
    {
        public void putOrAdd(final int k, final int v, final int adjust)
        {
            final int key[] = this.key;
            final boolean used[] = this.used;
            final int mask = this.mask;
            // The starting point.
            int pos = (it.unimi.dsi.fastutil.HashCommon.murmurHash3((k))) & mask;
            // There's always an unused entry.
            while (used[pos] && !((k) == (key[pos])))
                pos = (pos + 1) & mask;
            if (used[pos])
            {
                value[pos] += adjust;
                return;
            }
            used[pos] = true;
            key[pos] = k;
            value[pos] = v;
            if (++size >= maxFill) rehash(arraySize(size, f));
        }
    }

    @Test
    public void fastutilOpenHashMap()
    {
        final char [] CHARS = DATA;
        final Int2IntOpenHashMapExt map = new Int2IntOpenHashMapExt(); 
        map.defaultReturnValue(0);
    
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.putOrAdd(bigram, 1, 1);
        }
    
        guard = map.size();
    }

    @Test
    public void fastutilLinkedOpenHashMap()
    {
        final char [] CHARS = DATA;
        final Int2IntLinkedOpenHashMap map = new Int2IntLinkedOpenHashMap(); 
        map.defaultReturnValue(0);
    
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }
    
        guard = map.size();
    }
}