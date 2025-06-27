package com.example.cacheapi.dto;

public class CacheConfig {
    private String cacheType;      // "direct", "set-associative", "associative"
    private int cacheSize;         // in bytes, 32768
    private int blockSize;         // in bytes,  64
    private int ways;              // only for set-associative
    private String writePolicyOnHit;    //  "write-through", "write-back"
    private String writePolicyOnMiss;    //  "write-allocate", "write-no-allocate"
    private String replacementPolicy;
    private int mainMemorySize;
    private int wordSize;

    // Getters
    public int getWays() { return ways; }
    public int getCacheSize() { return cacheSize; }
    public int getBlockSize() { return blockSize; }
    public String getCacheType() { return cacheType; }
    public String getWritePolicyOnHit() { return writePolicyOnHit; }
    public String getWritePolicyOnMiss() { return writePolicyOnMiss; }
    public String getReplacementPolicy() { return replacementPolicy; } 
    public int getMainMemorySize() { return mainMemorySize; }
    public int getWordSize() { return wordSize; }

    // Setters
    public void setWays(int ways) { this.ways = ways; }
    public void setCacheSize(int cacheSize) { this.cacheSize = cacheSize; }
    public void setBlockSize(int blockSize) { this.blockSize = blockSize; }
    public void setCacheType(String cacheType) { this.cacheType = cacheType; }
    public void setWritePolicyOnHit(String writePolicyOnHit) { this.writePolicyOnHit = writePolicyOnHit; }
    public void setWritePolicyOnMiss(String writePolicyOnMiss) { this.writePolicyOnMiss = writePolicyOnMiss; }
    public void setReplacementPolicy(String replacementPolicy) { this.replacementPolicy = replacementPolicy; }
    public void setWordSize(int wordSize) {this.wordSize = wordSize; }
}