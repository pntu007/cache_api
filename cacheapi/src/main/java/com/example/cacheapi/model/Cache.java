package com.example.cacheapi.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.expression.MapAccessor;

@SuppressWarnings({"unused", "unchecked", "uninitialized"})
public class Cache {

    // cache information
    private int sets, ways;
    private int cacheSize = 32768; // 32KB
    private int blockSize = 64; // 64 bytes
    private String writePolicyOnHit = "";
    private String writePolicyOnMiss = "";

    // cache states
    public static enum State { INVALID, VALID, MISS_PENDING, MODIFIED }

    // Replacement Policy
    public enum ReplacementPolicy { RANDOM, LRU, FIFO, LFU }
    protected ReplacementPolicy policy = ReplacementPolicy.RANDOM;
    protected TreeMap<Integer, LinkedList<Long>> evictionQueues = new TreeMap<>();
    protected Map<Integer, Map<Long, Integer>> frequencyCounters; // set → (tag → frequency)

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

            System.out.printf("address: %d\n", this.address);
            System.out.printf("action: %s\n", this.action);
            System.out.printf("cacheType: %d\n", this.cacheType);
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
    private long[] mainMemory = new long[128];
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

        for(int i = 0; i < sets; i++) {
            evictionQueues.put(i, new LinkedList<>());
        }

        // mainMemory = new long[] {
        //     12L, 85L, 430L, 76L, 102L, 998L, 543L, 87L,
        //     65L, 34L, 780L, 910L, 456L, 321L, 765L, 100L,
        //     54L, 231L, 82L, 640L, 999L, 15L, 384L, 273L,
        //     96L, 720L, 305L, 28L, 413L, 652L, 210L, 311L,
        //     789L, 102L, 59L, 400L, 212L, 88L, 670L, 198L,
        //     234L, 590L, 600L, 14L, 293L, 712L, 804L, 916L,
        //     723L, 37L, 123L, 404L, 808L, 201L, 172L, 93L,
        //     116L, 411L, 507L, 310L, 67L, 495L, 360L, 821L,
        //     212L, 8L, 999L, 34L, 490L, 778L, 61L, 199L,
        //     284L, 349L, 112L, 415L, 786L, 628L, 319L, 222L,
        //     98L, 765L, 301L, 709L, 5L, 445L, 619L, 276L,
        //     884L, 67L, 151L, 941L, 30L, 481L, 561L, 213L,
        //     315L, 829L, 422L, 113L, 638L, 957L, 395L, 217L,
        //     183L, 374L, 91L, 666L, 540L, 188L, 751L, 370L,
        //     683L, 294L, 78L, 206L, 624L, 849L, 311L, 17L
        // };

        for(int i=0;i<mainMemory.length;i++){
            mainMemory[i] = 1 + rand.nextInt(1000); 
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
            while (running) {
                try {
                    boolean didWork = false;

                    CacheRequest req = cacheRequestQueue.poll();
                    if(req != null) {
                        long[] cache_result = processRequest(req.address, req.action, req.data, req.cacheType, false);
                        req.future.complete(cache_result);
                        didWork = true;
                    } 
                    else {
                        MemoryResponse memResp = memoryResponseQueue.poll();
                        if (memResp != null) {
                            long[] memory_result = processRequest(memResp.address, memResp.action, memResp.data, memResp.cacheType, true);
                            memResp.future.complete(memory_result);
                            didWork = true;
                        }
                    }

                    // Avoid busy waiting if nothing was processed
                    if(!didWork) Thread.sleep(1);
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
                        System.out.println("MSHR IS NOT EMPTY");
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
                            System.out.println("Reading from memory");
                            memoryData = getDataFromMainMemory(req.address);
                            System.out.println("memoryData: " + memoryData.get(0));
                        }
                        else if(req.action.equals("WRITE")) {
                            System.out.println("starting main memory write");
                            writeDataIntoMainMemory(req.address, req.data);
                            memoryData.clear();
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
        System.out.println("action: " + action);
        System.out.println("requestAddress: " + requestAddress);
        System.out.println("cacheType: " + cacheType);
        System.out.println("memResp: " + memRsp);
        System.out.println("size: " + data.size());


        Boolean hit = false, miss = false;
        long output = 0;
        State blockState = State.INVALID;
        AddressLocation req = getLocationInfo(requestAddress);

        if(cacheType == 0) req.index = 0;
        Map<Long, Block> setBlocks = cache[req.index];
        LinkedList<Long> queue = evictionQueues.get(req.index);

        if(setBlocks.containsKey(req.tag)) {
            System.out.println("hooray BOSS TAG IS FOUND");
            Block block = setBlocks.get(req.tag);
            
            // LRU: update on every access
            if(policy == ReplacementPolicy.LRU) {
                queue.remove(req.tag);
                queue.addLast(req.tag);
            }
            else if(policy == ReplacementPolicy.LFU) {
                Map<Long, Integer> freqMap = frequencyCounters.get(req.index);
                freqMap.put(req.tag, freqMap.getOrDefault(req.tag, 0) + 1);
            }

            if(block.state == State.VALID || block.state == State.MODIFIED) {
                hit = true;

                if(action.equals("READ")) {
                    output = block.data[req.offset / 4];
                }
                if(action.equals("WRITE")) {
                    if(memRsp == false) {
                        block.data[req.offset / 4] = data.get(0);
                        if(writePolicyOnHit.equals("WRITE-THROUGH")) {
                            MSHR.add(new MissStateHoldingRegisters(State.VALID, requestAddress, "WRITE", data.get(0)));
                        }
                        if(writePolicyOnHit.equals("WRITE-BACK")) {
                            block.state = State.MODIFIED;
                            writeBackBuffer.put(requestAddress, data.get(0));
                        }
                    }
                }
                blockState = block.state;     

                updateReplacementInfo();
            }
            else if(block.state == State.MISS_PENDING) {
                if(memRsp == true) {
                    hit = true;
                    if(action.equals("READ")) {
                        block.state = State.VALID;
                        block.data = data.stream().mapToLong(Long::longValue).toArray();
                        output = block.data[req.offset / 4];
                    }
                    else if(action.equals("WRITE")) {
                        if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                            block.state = State.MODIFIED;
                            handleWriteAllocate(requestAddress, setBlocks, req.tag);
                        }
                        else {
                            block.state = State.VALID;
                        }
                        output = -1;
                    }               
                    blockState = block.state;     
                }
                else {
                    miss = true;
                    if(action.equals("READ")) {
                        // MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
                        // We need to wait
                    }
                    if(action.equals("WRITE")) {
                        // if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                        //     handleWriteAllocate(requestAddress, setBlocks, req.tag);
                        // }
                        // if(writePolicyOnMiss.equals("WRITE-NO-ALLOCATE")) {
                        //     MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "WRITE", data.get(0)));
                        // }

                        // We need to wait
                    }
                }
            }
            else if(block.state == State.INVALID) {
                miss = true;

                if(memRsp == false) {
                    System.out.println("Misses this time");
                    runEvictionAlgorithm(setBlocks, req.index);

                    setBlocks.put(req.tag, new Block(State.MISS_PENDING));

                    if(action.equals("READ")) {
                        block.state = State.MISS_PENDING;
                        MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
                    }
                    if(action.equals("WRITE")) {
                        if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                            block.state = State.MISS_PENDING;
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
            System.out.println("OOPS BOSS TAG IS NOT FOUND");
            miss = true;
            runEvictionAlgorithm(setBlocks, req.index);

            if(memRsp == false) {
                setBlocks.put(req.tag, new Block(State.MISS_PENDING));
    
                if(policy == ReplacementPolicy.FIFO) {
                    queue.addLast(req.tag);
                } 
                else if(policy == ReplacementPolicy.LRU) {
                    queue.remove(req.tag);
                    queue.addLast(req.tag);
                }
                else if(policy == ReplacementPolicy.LFU) {
                    Map<Long, Integer> freqMap = frequencyCounters.get(req.index);
                    freqMap.put(req.tag, freqMap.getOrDefault(req.tag, 0) + 1);
                }

                System.out.println("just above adding into mshr");
                if(action.equals("READ")) {
                    MSHR.add(new MissStateHoldingRegisters(State.MISS_PENDING, requestAddress, "READ", 0));
                    System.out.println("Added to MSHR queue");
                }
                else if(action.equals("WRITE")) {
                    if(writePolicyOnMiss.equals("WRITE-ALLOCATE")) {
                        handleWriteAllocate(requestAddress, setBlocks, req.tag);
                    }
                    if(writePolicyOnMiss.equals("WRITE-NO-ALLOCATE")) {
                        System.out.println("almost added to mshr");
                        MSHR.add(new MissStateHoldingRegisters(State.INVALID, requestAddress, "WRITE", data.get(0)));
                        System.out.println("added to mshr for write no allocate");
                    }
                }
            }
            else {
                if(action.equals("READ")) {
                    setBlocks.put(req.tag, new Block(State.VALID));
                    setBlocks.get(req.tag).data = data.stream().mapToLong(Long::longValue).toArray();
                    output = setBlocks.get(req.tag).data[req.offset / 4];
                }
                else if(action.equals("WRITE")) {
                    setBlocks.put(req.tag, new Block(State.MODIFIED));
                    setBlocks.get(req.tag).data = data.stream().mapToLong(Long::longValue).toArray();
                    output = 0;
                }
            }

            updateReplacementInfo();
        }

        System.out.println("Giving output");
        long[] response = {hit ? 1 : 0, miss ? 1 : 0, output, blockState.ordinal()};
        System.out.println(response);
        return response;
    }

    private AddressLocation getLocationInfo(long address) {

        int offsetBits = (int) (Math.log(blockSize) / Math.log(2));
        int indexBits = (int) (Math.log(sets) / Math.log(2));
        int tagBits = 40 - indexBits - offsetBits;

        String binary = String.format("%40s", Long.toBinaryString(address)).replace(' ', '0');

        AddressLocation info = new AddressLocation();
        info.tag = Long.parseLong(binary.substring(0, tagBits), 2);
        info.index = indexBits > 0 ? Integer.parseInt(binary.substring(tagBits, tagBits + indexBits), 2) : 0;
        info.offset = Integer.parseInt(binary.substring(tagBits + indexBits), 2);

        return info;
    }

    private List<Long> getDataFromMainMemory(long byteAddress) {
        long wordAddress = byteAddress / 4;
        int blockSizeWords = blockSize / 4;
        long start = (wordAddress / blockSizeWords) * blockSizeWords;
        List<Long> data = new ArrayList<>();
        for (int i = 0; i < blockSizeWords; i++) {
            data.add(mainMemory[(int)(start + i)]);
        }
        return data;
    }

    private void writeDataIntoMainMemory(long address, long data) {
        System.out.printf("main memory (before) : %d\n" , mainMemory[(int)address]);
        mainMemory[(int)address] = data;
        System.out.printf("main memory (after) : %d\n" , mainMemory[(int)address]);
    }

    private void handleWriteAllocate(long address, Map<Long, Block> blocks, long tag) {
        AddressLocation loc = getLocationInfo(address); // Needed to get the set index
        LinkedList<Long> queue = evictionQueues.get(loc.index);

        if(blocks.size() == ways) runEvictionAlgorithm(blocks, loc.index);

        // LOAD BLOCK INTO CACHE
        long[] incomingDataFromMemory = loadBlocksIntoCache(address);
        
        // Write in cache
        // mark cache as modified
        Block block = new Block(State.MODIFIED);
        block.data = incomingDataFromMemory;
        blocks.put(tag, block);

        if (policy == ReplacementPolicy.FIFO) {
            queue.addLast(tag);  // insert new block at end
        } else if (policy == ReplacementPolicy.LRU) {
            queue.remove(tag);   // remove if already exists (just in case)
            queue.addLast(tag);  // insert new block at end
        }
    }

    private long[] loadBlocksIntoCache(long address) {
        long addr = address / 4; // convert byte address to word address
        int blockWords = blockSize / 4; // number of words per block
        long start = (addr / blockWords) * blockWords;

        long[] incomingDataFromMemory = new long[blockWords];
        for (int i = 0; i < blockWords; i++) {
            incomingDataFromMemory[i] = mainMemory[(int)(start + i)];
        }
        return incomingDataFromMemory;
    }


    private void runEvictionAlgorithm(Map<Long, Block> blocks, int set) {
        // LATER TO BE CHANGED BASED ON DIFFERENT ALGORITHMS
        // IMPLEMENTING RANDOM EVICTION HERE

        if (blocks.size() < ways) return; 

        Long keyToRemove = null;
        LinkedList<Long> queue = new LinkedList<>();
        if(policy == ReplacementPolicy.FIFO || policy == ReplacementPolicy.LRU) {
            System.out.println("EVICTION" + set);
            queue = evictionQueues.get(set);
        }
        else queue = evictionQueues.get(evictionQueues.firstKey());

        switch (policy) {
            case RANDOM:
                List<Long> keys = new ArrayList<>(blocks.keySet());
                if(keys.size() > 0) keyToRemove = keys.get(new Random().nextInt(keys.size()));
                break;

            case FIFO:
                if(!queue.isEmpty()) {
                    keyToRemove = queue.pollFirst(); // Remove oldest inserted
                }
                break;

            case LRU:
                if(!queue.isEmpty()) {
                    keyToRemove = queue.pollFirst(); // Least recently used is at front
                }
                break;

            case LFU:
                Map<Long, Integer> freqMap = frequencyCounters.get(set);
                int minFreq = Integer.MAX_VALUE;
                for (Map.Entry<Long, Integer> entry : freqMap.entrySet()) {
                    if (entry.getValue() < minFreq) {
                        minFreq = entry.getValue();
                        keyToRemove = entry.getKey();
                    }
                }
                if (keyToRemove != null) {
                    freqMap.remove(keyToRemove); // Remove from frequency map
                }
                break;
        }

        if(keyToRemove != null) blocks.remove(keyToRemove);

        if(!writeBackBuffer.isEmpty()) {
            for(Map.Entry<Long, Long> e : writeBackBuffer.entrySet()) {
                MSHR.add(new MissStateHoldingRegisters(State.VALID, e.getKey(), "WRITE", e.getValue()));
            }
        }
    }

    private void updateReplacementInfo() {
        
    }

    // setter for replacement policy
    public void setReplacementPolicy(ReplacementPolicy policy) {
        this.policy = policy;
        if(policy == ReplacementPolicy.LFU) {
            frequencyCounters = new HashMap<>();
            for(int i = 0; i < sets; i++) {
                frequencyCounters.put(i, new HashMap<>());

            }
        }
    }

    public long[] getMainMemory() {
        return mainMemory;
    }

    // to fetch cache details
    public Map<Long, Block>[] getCache() {
        return cache;
    }
}