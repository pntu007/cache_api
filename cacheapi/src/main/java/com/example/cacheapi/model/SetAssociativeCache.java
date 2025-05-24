package com.example.cacheapi.model;
// import java.util.*;

public class SetAssociativeCache extends Cache {
    static int sets = 64;

    public SetAssociativeCache(int cacheSize, int blockSize,int ways, String writePolicyOnHit, String writePolicyOnMiss) {
        // cacheSize, blockSize, sets, ways, writePolicy
        super(cacheSize, blockSize, (cacheSize / (blockSize * ways)), ways, writePolicyOnHit, writePolicyOnMiss);
    }


    // public static void main(String[] args) {
    //     SetAssociativeCache cache = new SetAssociativeCache();
    //     // Block[][] cacheBlocks = cache.getCache();
    //     List<Long> manualAddresses = Arrays.asList(
    //         64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
    //     );
    //     cache.handleManualRequests(manualAddresses,1);
    // }
}
