package com.example.cacheapi.model;
// import java.util.*;

public class AssociativeCache extends Cache {
    static int ways = 32;

    public AssociativeCache(int cacheSize, int blockSize, String writePolicyOnHit, String writePolicyOnMiss, int wordSize) {
        // cacheSize, blockSize, sets, ways, writePolicy
        super(cacheSize, blockSize, 1, (cacheSize / blockSize), writePolicyOnHit, writePolicyOnMiss, wordSize);
    }

    // public static void main(String[] args) {
    //     AssociativeCache cache = new AssociativeCache();
    //     // Block[][] cacheBlocks = cache.getCache();
    //     List<Long> manualAddresses = Arrays.asList(
    //         64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
    //     );
    //     cache.handleManualRequests(manualAddresses, 0);
    // }
}