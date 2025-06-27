package com.example.cacheapi.dto;

public class CacheConfigResponse {
    private String message;
    private long[] mainMemory;

    public CacheConfigResponse(String message, long[] mainMemory) {
        this.message = message;
        this.mainMemory = mainMemory;
    }

    public String getMessage() { return message; }
    public long[] getMainMemory() { return mainMemory; }
    
    public void setMessage(String message) { this.message = message; }
    public void setMainMemory(long[] mainMemory) { this.mainMemory = mainMemory; }
}
