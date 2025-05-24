package com.example.cacheapi.model;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressWarnings({"unused", "unchecked"})
public class Cache {

    // cache information
    private int sets, ways;
    private int cacheSize = 32768; // 32KB
    private int blockSize = 64; // 64 bytes
    private String writePolicyOnHit = "";
    private String writePolicyOnMiss = "";

    // cache states
    public static enum State { INVALID, VALID, MISS_PENDING, MODIFIED };

    // cache running state
    private volatile boolean running = false;
    
    private final Random rand = new Random();

    // Block structure
    protected static class Block {
        State state = State.INVALID;
        long[] data = new long[16];

        Block() {
            this.state = State.INVALID;
        }

        Block(State state) {
            this.state = State.MISS_PENDING;
        }
    }

    // Address structure
    private static class AddressLocation {
        long tag;
        int index;
        int offset;
    }

    // Request structure
    private static class CacheRequest {
        long address;
        String action;
        List<Long> data;
        int cacheType;
        CompletableFuture<long[]> future;

        CacheRequest(long address, String action, List<Long> data, int cacheType) {
            this.address = address;
            this.action = action;
            this.data = data;
            this.cacheType = cacheType;
            this.future = new CompletableFuture<>();
        }
    }

    // Memory Response structure
    private static class MemoryResponse extends CacheRequest {
        State state;

        MemoryResponse(long address, String action, List<Long> data, State state) {
            super(address, action, data, -1);
            this.state = state;
        }
    }

    // Miss State Holding Registers
    private static class MissStateHoldingRegisters {
        State state;
        long address;
        long data;
        String action;

        MissStateHoldingRegisters(State state, long address, String action, long data) {
            this.state = state;
            this.address = address;
            this.action = action;
            this.data = data;
        }
    }

    // cache queues
    private final BlockingQueue<CacheRequest> cacheRequestQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<MemoryResponse> memoryResponseQueue = new LinkedBlockingQueue<>();
    
    // cache getters
    protected int getBlockSize() { return this.blockSize; }
    protected int getCacheSize() { return this.cacheSize; }
    
    // cache setters
    protected void setBlockSize(int newBlockSize) { this.blockSize = newBlockSize; }
    protected void setCacheSize(int newCacheSize) { this.cacheSize = newCacheSize; }

    // Cache, Main Memory and Registers implementation
    private Map<Long, Block>[] cache;
    private long[] mainMemory = new long[1 << 10];
    private List<MissStateHoldingRegisters> MSHR = new ArrayList<>();
    private HashMap<Long, Long> writeBackBuffer = new HashMap<>();

    // cache controller to set up and start running cache thread
    public Cache(int cacheSize, int blockSize, int sets, int ways, String writePolicyOnHit, String writePolicyOnMiss) {
        this.cacheSize = cacheSize;
        this.blockSize = blockSize;
        this.sets = sets;
        this.ways = ways;
        this.writePolicyOnHit = writePolicyOnHit;
        this.writePolicyOnMiss = writePolicyOnMiss;

        cache = new Map[this.sets];
        for(int i = 0; i < sets; i++) {
            cache[i] = new HashMap<>();
        }

        this.running = true;
        startCacheThread();
        startMemoryThread();
    }

    public void stop() {
        running = false;
    }

    // running cache in background
    private void startCacheThread() {
        Thread thread = new Thread(() -> {
            while(running) {
                try {
                    CacheRequest req = cacheRequestQueue.take(); // blocks until a request arrives
                    long[] cache_result = processRequest(req.address, req.action, req.data, req.cacheType, false);
                    req.future.complete(cache_result);

                    // Handle memory responses
                    MemoryResponse memResp = memoryResponseQueue.take();
                    if(memResp != null) {
                        long[] memory_result = processRequest(memResp.address, memResp.action, memResp.data, memResp.cacheType, true);
                        req.future.complete(memory_result);
                    }

                    Thread.sleep(1); // prevent tight loop
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public CompletableFuture<long[]> handleManualRequests(long address, String action, List<Long> data, int cacheType) {
        CacheRequest req = new CacheRequest(address, action, data, cacheType);
        cacheRequestQueue.offer(req);
        return req.future;
    }

    // running memory in background
    private void startMemoryThread() {
        Thread thread = new Thread(() -> {
            while(running) {
                try {
                    if(!MSHR.isEmpty()) {
                        MissStateHoldingRegisters req;
                        synchronized(MSHR) {
                            if(MSHR.isEmpty()) continue;
                            req = MSHR.remove(0);
                        }

                        // Simulate memory access delay
                        Thread.sleep(50); // e.g., 50ms

                        // Fake read from main memory
                        List<Long> memoryData = new ArrayList<>();

                        if(req.action.equals("READ")) {
                            memoryData = getDataFromMainMemory(req.address);
                        }
                        else if(req.action.equals("WRITE")) {
                            writeDataIntoMainMemory(req.address, req.data);
                            memoryData.clear();
                        }

                        // Create new block with fetched data
                        AddressLocation loc = getLocationInfo(req.address);
                        Block newBlock = new Block(State.VALID);
                        for(int i = 0; i < 16; i++) {
                            newBlock.data[i] = memoryData.get(i);
                        }

                        // Send it back to cache as a "MEMORY RESPONSE"
                        memoryResponseQueue.put(new MemoryResponse(req.address, req.action, memoryData, req.state));
                    }
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    // function to process requests
    public long[] processRequest(long requestAddress, String action, List<Long> data, int cacheType, Boolean memRsp) {
        Boolean hit = false, miss = false;
        long output = 0;
        AddressLocation req = getLocationInfo(requestAddress);

        if(cacheType == 0) req.index = 0;
        Map<Long, Block> setBlocks = cache[req.index];

        if(setBlocks.containsKey(req.tag)) {
            Block block = setBlocks.get(req.tag);
            if(block.state == State.VALID || block.state == State.MODIFIED) {
                hit = true;
                if(action.equals("READ")) output = block.data[req.offset / 4];
                if(action.equals("WRITE")) {
                    block.data[req.offset / 4] = data.get(0);
                    if(writePolicyOnHit.equals("WRITE-THROUGH")) {
                        MSHR.add(new MissStateHoldingRegisters(State.VALID, requestAddress, "WRITE", data.get(0)));
                    }
                    if(writePolicyOnHit.equals("WRITE-BACK")) {
                        writeBackBuffer.put(requestAddress, data.get(0));
                    }
                }

                updateReplacementInfo();
            }
            else if(block.state == State.MISS_PENDING) {
                if(memRsp == true) {
                    hit = true;
                    block.state = State.VALID;
                    output = data.get(0);
                }
                else {
                    miss = true;
                    if(action.equals("READ")) {
                        MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
                    }
                    if(action.equals("WRITE")) {
                        if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                            handleWriteAllocate(requestAddress, setBlocks, req.tag);
                        }
                        if(writePolicyOnMiss.equals("WRITE-NO-ALLOCATE")) {
                            MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "WRITE", data.get(0)));
                        }
                    }
                }
            }
            else if(block.state == State.INVALID) {
                miss = true;

                if(memRsp == false) {
                    runEvictionAlgorithm(setBlocks);

                    setBlocks.put(req.tag, new Block(State.MISS_PENDING));

                    if(action.equals("READ")) {
                        block.state = State.MISS_PENDING;
                        MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
                    }
                    if(action.equals("WRITE")) {
                        if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                            handleWriteAllocate(requestAddress, setBlocks, req.tag);
                        }
                        if(writePolicyOnMiss.equals("WRITE-NO-ALLOCATE")) {
                            MSHR.add(new MissStateHoldingRegisters(State.INVALID, requestAddress, "WRITE", data.get(0)));
                        }
                    }
                }
            }
        }
        else {
            miss = true;
            runEvictionAlgorithm(setBlocks);

            setBlocks.put(req.tag, new Block(State.MISS_PENDING));
            updateReplacementInfo();

            if(action.equals("READ")) {
                MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
            }
            else if(action.equals("WRITE")) {
                if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                    handleWriteAllocate(requestAddress, setBlocks, req.tag);
                }
                if(writePolicyOnMiss.equals("WRITE-NO-ALLOCATE")) {
                    MSHR.add(new MissStateHoldingRegisters(State.INVALID, requestAddress, "WRITE", data.get(0)));
                }
            }
        }

        long[] response = {hit ? 1 : 0, miss ? 1 : 0, output, State.VALID.ordinal()};
        return response;
    }

    private AddressLocation getLocationInfo(long address) {
        String binary = String.format("%40s", Long.toBinaryString(address)).replace(' ', '0');
        AddressLocation info = new AddressLocation();
        info.tag = Long.parseLong(binary.substring(0, 28), 2);
        info.index = Integer.parseInt(binary.substring(28, 34), 2);
        info.offset = Integer.parseInt(binary.substring(34), 2);
        return info;
    }

    private long getNewLocation(int set) {
        // to get the new location within the set
        return rand.nextLong(ways);
    }

    private List<Long> getDataFromMainMemory(long req) {
        req /= 4;
        long start = (req / 16) * 16;
        List<Long> data = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            data.add(start + i);
        }
        return data;
    }

    private void writeDataIntoMainMemory(long address, long data) {
        mainMemory[(int)address] = data;
    }

    private void handleWriteAllocate(long address, Map<Long, Block> blocks, long tag) {
        if(blocks.size() == ways) runEvictionAlgorithm(blocks);

        // LOAD BLOCK INTO CACHE
        long[] incomingDataFromMemory = loadBlocksIntoCache(address);
        
        // Write in cache
        // mark cache as modified
        Block block = new Block(State.MODIFIED);
        block.data = incomingDataFromMemory;
        blocks.put(tag, block);
    }

    private long[] loadBlocksIntoCache(long address) {
        long addr = address / 4;
        long start = (addr / 16) * 16;
        long[] incomingDataFromMemory = new long[16];
        for(int i = 0; i < 16; i++) {
            incomingDataFromMemory[i] = mainMemory[(int)(start + i)];
        }
        return incomingDataFromMemory;
    }

    private void runEvictionAlgorithm(Map<Long, Block> blocks) {
        // LATER TO BE CHANGED BASED ON DIFFERENT ALGORITHMS
        // IMPLEMENTING RANDOM EVICTION HERE

        List<Long> keys = new ArrayList<>(blocks.keySet());
        int randomIndex = new Random().nextInt(keys.size());
        Long keyToRemove = keys.get(randomIndex);

        blocks.remove(keyToRemove);

        if(!writeBackBuffer.isEmpty()) {
            for(Map.Entry<Long, Long> e : writeBackBuffer.entrySet()) {
                MSHR.add(new MissStateHoldingRegisters(State.VALID, e.getKey(), "WRITE", e.getValue()));
            }
        }
    }

    private void updateReplacementInfo() {
        
    }

    // to fetch cache details
    public Map<Long, Block>[] getCache() {
        return cache;
    }
}