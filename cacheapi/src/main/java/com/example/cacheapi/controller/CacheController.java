package com.example.cacheapi.controller;

import com.example.cacheapi.dto.CacheRequest;
import com.example.cacheapi.dto.CacheResponse;
import com.example.cacheapi.dto.CacheConfig;
import com.example.cacheapi.dto.CacheConfigResponse;
import com.example.cacheapi.model.SetAssociativeCache;
import com.example.cacheapi.model.AssociativeCache;
import com.example.cacheapi.model.Cache.ReplacementPolicy;
import com.example.cacheapi.model.DirectMappedCache;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin
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
        CompletableFuture<long[]> future;

        switch (selectedType.toLowerCase()) {
            case "set-associative":
                future = sa_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 2);
                break;
            case "direct":
                System.out.println("vuwviwivw");
                future = dm_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 1);
                break;
            case "associative":
                future = a_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 0);
                break;
            default:
                // Return an already completed future for invalid types
                future = CompletableFuture.completedFuture(new long[]{0, 0, 0, 0});
                break;
        }

        // Transform the response to CacheResponse
        return future.thenApply(CacheResponse::new);
    }


    @GetMapping("/ping")
    public String ping() {
        return "Cache API is working!";
    }
}
