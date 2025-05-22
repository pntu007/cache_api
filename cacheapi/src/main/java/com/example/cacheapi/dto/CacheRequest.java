package com.example.cacheapi.dto;

import java.util.List;

public class CacheRequest {
    private List<Long> addresses;

    public List<Long> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Long> addresses) {
        this.addresses = addresses;
    }
}
