package com.example.cacheapi.dto;

import com.example.cacheapi.model.Cache.State;


public class CacheResponse {
    private Boolean hit;
    private Boolean miss;
    private long data;
    private State blockState;

    public CacheResponse(long[] response) {
        this.hit = response[0] == 1 ? true : false;
        this.miss = response[1] == 1 ? true : false;
        this.data = response[2];
        this.blockState = State.values()[(int)response[3]];
    }

    public Boolean getHit() { return hit; }
    public Boolean getMiss() { return miss; }
    public long getData() { return data; }
    public State getBlockState() { return blockState; }
}
