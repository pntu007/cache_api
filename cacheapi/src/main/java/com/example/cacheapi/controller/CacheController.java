package com.example.cacheapi.controller;

import com.example.cacheapi.dto.CacheRequest;
import com.example.cacheapi.dto.CacheResponse;
import com.example.cacheapi.dto.CacheConfig;
import com.example.cacheapi.dto.CacheConfigResponse;
import com.example.cacheapi.model.SetAssociativeCache;
import com.example.cacheapi.model.AssociativeCache;
import com.example.cacheapi.model.Cache.ReplacementPolicy;
import com.example.cacheapi.model.DirectMappedCache;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "http://localhost:3000")
public class CacheController {

    private SetAssociativeCache sa_cache;
    private DirectMappedCache dm_cache;
    private AssociativeCache a_cache;

    private String selectedType = "SET-ASSOCIATIVE"; // default

    @PostMapping("/configure")
    public CompletableFuture<CacheConfigResponse> configure(@RequestBody CacheConfig config) {
        selectedType = config.getCacheType();
        ReplacementPolicy policy = ReplacementPolicy.valueOf(config.getReplacementPolicy().toUpperCase());
        long[] memorySnapshot;

        switch (selectedType.toUpperCase()) {
            case "SET-ASSOCIATIVE":
                sa_cache = new SetAssociativeCache(config.getCacheSize(), config.getBlockSize(), config.getWays(), config.getWritePolicyOnHit(), config.getWritePolicyOnMiss());
                sa_cache.setReplacementPolicy(policy);
                memorySnapshot = sa_cache.getMainMemory();
                break;
            case "DIRECT":
                dm_cache = new DirectMappedCache(config.getCacheSize(), config.getBlockSize(), config.getWritePolicyOnHit(), config.getWritePolicyOnMiss());
                dm_cache.setReplacementPolicy(policy);
                memorySnapshot = dm_cache.getMainMemory();
                break;
            case "ASSOCIATIVE":
                a_cache = new AssociativeCache(config.getCacheSize(), config.getBlockSize(), config.getWritePolicyOnHit(), config.getWritePolicyOnMiss());
                a_cache.setReplacementPolicy(policy);
                memorySnapshot = a_cache.getMainMemory();
                break;
            default:
                return CompletableFuture.completedFuture(
                    new CacheConfigResponse("Invalid cache type selected!", new long[0])
                );
        }

        return CompletableFuture.completedFuture(new CacheConfigResponse("Configuration successful for " + selectedType + " cache.", memorySnapshot));
    }

    @PostMapping("/request")
    public CompletableFuture<CacheResponse> simulateCache(@RequestBody CacheRequest request) {
        System.out.println(selectedType);

        switch (selectedType.toLowerCase()) {
            case "set-associative":
                return sa_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 2);
            case "direct":
                System.out.println("vuwviwivw");
                return dm_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 1);
            case "associative":
                return a_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 0);
            default:
                CacheResponse dummyResponse = new CacheResponse(
                    new ArrayList<>(),  // cacheFinal
                    -1,                 // memoryIndex
                    new ArrayList<>(),  // memoryData
                    "INVALID",          // type
                    -1,                 // index
                    -1,                 // tag
                    -1,                 // offset
                    -1,                 // block
                    -1,                 // data
                    false,              // hit
                    "INVALID",          // oldState
                    "INVALID"           // newState
                );
                return CompletableFuture.completedFuture(dummyResponse);
        }
    }


    @GetMapping("/ping")
    public String ping() {
        return "Cache API is working!";
    }
}
