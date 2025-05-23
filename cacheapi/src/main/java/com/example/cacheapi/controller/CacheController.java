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
        double[] result;

        switch (selectedType.toLowerCase()) {
            case "set-associative":
                result = sa_cache.handleManualRequests(request.getAddresses(), 1);
                break;
            case "direct":
                result = dm_cache.handleManualRequests(request.getAddresses(), 1);
                break;
            case "associative":
                result = a_cache.handleManualRequests(request.getAddresses(), 0);
                break;
            default:
                result = new double[]{0, 0};
                break;
        }

        return new CacheResponse(result[0], result[1]);
    }

    @GetMapping("/ping")
    public String ping() {
        return "Cache API is working!";
    }
}
