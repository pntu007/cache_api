package com.example.cacheapi.dto;

public class CacheResponse {
    private double hitRate;
    private double missRate;

    public CacheResponse(double hitRate, double missRate) {
        this.hitRate = hitRate;
        this.missRate = missRate;
    }

    public double getHitRate() {
        return hitRate;
    }

    public double getMissRate() {
        return missRate;
    }
}
