package com.example.cacheapi.dto;
import java.util.List;

public class CacheRequest {
    private long address;
    private String action;
    private List<Long> data;

    public List<Long> getData() { return data; }
    public long getAddress() { return address; }
    public String getAction() { return action.toUpperCase(); }
    
    public void setData(List<Long> data) { this.data = data; }
    public void setAction(String action) { this.action = action; }
    public void setAddresses(long address) { this.address = address; }
}