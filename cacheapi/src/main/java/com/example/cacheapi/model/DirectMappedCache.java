package com.example.cacheapi.model;
// import java.util.*;

public class DirectMappedCache extends Cache {
    static int sets = 32;

    public DirectMappedCache() {
        super(sets, 1);
    }

    public DirectMappedCache(int cacheSize, int blockSize, String writePolicy) {
        // cacheSize, blockSize, sets, ways, writePolicy
        super(cacheSize, blockSize, (cacheSize / blockSize), 1, writePolicy);
    }

    // public static void main(String[] args) {
    //     DirectMappedCache cache = new DirectMappedCache();
    //     // Block[][] cacheBlocks = cache.getCache();
    //     List<Long> manualAddresses = Arrays.asList(
    //         64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
    //     );
    //     cache.handleManualRequests(manualAddresses, 1);
    // }
}