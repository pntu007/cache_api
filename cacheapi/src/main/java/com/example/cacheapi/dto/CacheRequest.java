package com.example.cacheapi.dto;

public class CacheRequest {
    private long address;
    private String action;
    private long data;

    public long getData() { return data; }
    public long getAddress() { return address; }
    public String getAction() { return action.toUpperCase(); }
    
    public void setData(long data) { this.data = data; }
    public void setAction(String action) { this.action = action; }
    public void setAddresses(long address) { this.address = address; }
}