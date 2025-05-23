package com.example.cacheapi.controller;

import com.example.cacheapi.dto.CacheRequest;
import com.example.cacheapi.dto.CacheResponse;
import com.example.cacheapi.dto.CacheConfig;
import com.example.cacheapi.model.SetAssociativeCache;
import com.example.cacheapi.model.AssociativeCache;
import com.example.cacheapi.model.DirectMappedCache;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin
public class CacheController {

    private SetAssociativeCache sa_cache;
    private DirectMappedCache dm_cache;
    private AssociativeCache a_cache;

    private String selectedType = "set-associative"; // default

    @PostMapping("/configure")
    public String configure(@RequestBody CacheConfig config) {
        selectedType = config.getCacheType();

        switch (selectedType.toLowerCase()) {
            case "set-associative":
                sa_cache = new SetAssociativeCache(config.getCacheSize(), config.getBlockSize(), config.getWays(), config.getWritePolicy());
                break;
            case "direct":
                dm_cache = new DirectMappedCache(config.getCacheSize(), config.getBlockSize(), config.getWritePolicy());
                break;
            case "associative":
                a_cache = new AssociativeCache(config.getCacheSize(), config.getBlockSize(), config.getWritePolicy());
                break;
            default:
                return "Invalid cache type selected!";
        }

        return "Configuration successful for " + selectedType + " cache.";
    }

    @PostMapping("/request")
    public CacheResponse simulateCache(@RequestBody CacheRequest request) {
        long[] response;

        switch (selectedType.toLowerCase()) {
            case "set-associative":
                response = sa_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 2);
                break;
            case "direct":
                response = dm_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 1);
                break;
            case "associative":
                response = a_cache.handleManualRequests(request.getAddress(), request.getAction(), request.getData(), 0);
                break;
            default:
                response = new long[]{0, 0, 0, 0};
                break;
        }

        return new CacheResponse(response);
    }

    @GetMapping("/ping")
    public String ping() {
        return "Cache API is working!";
    }
}
