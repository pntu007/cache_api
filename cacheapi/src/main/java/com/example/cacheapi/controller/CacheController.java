package com.example.cacheapi.controller;

import com.example.cacheapi.dto.CacheRequest;
import com.example.cacheapi.dto.CacheResponse;
import com.example.cacheapi.model.SetAssociativeCache;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cache")
@CrossOrigin
public class CacheController {

    private final SetAssociativeCache cache = new SetAssociativeCache();

    @PostMapping("/simulate")
    public CacheResponse simulateCache(@RequestBody CacheRequest request) {
        double[] result = cache.handleManualRequests(request.getAddresses(), 1);
        return new CacheResponse(result[0], result[1]);
    }
}
