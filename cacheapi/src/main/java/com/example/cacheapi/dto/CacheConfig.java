package com.example.cacheapi.dto;

public class CacheConfig {
    private String cacheType;      // "direct", "set-associative", "associative"
    private int cacheSize;         // in bytes, 32768
    private int blockSize;         // in bytes,  64
    private int ways;              // only for set-associative
    private String writePolicy;    //  "write-through", "write-back"

    // Getters
    public String getCacheType() {
        return cacheType;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public int getWays() {
        return ways;
    }

    public String getWritePolicy() {
        return writePolicy;
    }

    // Setters
    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public void setWays(int ways) {
        this.ways = ways;
    }

    public void setWritePolicy(String writePolicy) {
        this.writePolicy = writePolicy;
    }
}
