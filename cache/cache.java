package cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Cache {
    @SuppressWarnings("unused")
    private int sets, ways;
    private int cacheSize = 32768; // 32KB
    private int blockSize = 64; // 64 bytes
    protected enum State { INVALID, VALID, MISS_PENDING, MODIFIED };
    private final Random rand = new Random();

    protected static class Block {
        long tag = -1;
        State state = State.INVALID;
        long[] data = new long[16];
    }

    private static class AddressLocation {
        long tag;
        int set;
        int offset;
    }
    
    protected int getBlockSize() { return this.blockSize; }
    protected int getCacheSize() { return this.cacheSize; }
    
    protected void setBlockSize(int newBlockSize) { this.blockSize = newBlockSize; }
    protected void setCacheSize(int newCacheSize) { this.cacheSize = newCacheSize; }

    private Block[][] cache;

    public Cache(int sets, int ways) {
        this.sets = sets;
        this.ways = ways;

        cache = new Block[sets][ways];
        for(int i = 0; i < sets; i++) {
            for(int j = 0; j < ways; j++) {
                cache[i][j] = new Block();
            }
        }
    }

    public Block[][] getCache() {
        return cache;
    }

    private AddressLocation getLocationInfo(long address) {
        String binary = String.format("%40s", Long.toBinaryString(address)).replace(' ', '0');
        AddressLocation info = new AddressLocation();
        info.tag = Long.parseLong(binary.substring(0, 28), 2);
        info.set = Integer.parseInt(binary.substring(28, 34), 2);
        info.offset = Integer.parseInt(binary.substring(34), 2);
        return info;
    }

    private int getNewLocation(int set) {
        // to get the new location within the set
        return rand.nextInt(ways);
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

    public void handleManualRequests(List<Long> addressList, int cacheType) {
        long hit = 0, miss = 0;
        long numOfRequests = addressList.size();

        for(int i = 0; i < numOfRequests; i++) {
            long request = addressList.get(i);
            int type = rand.nextInt(2); // 0 = read, 1 = write

            AddressLocation req = getLocationInfo(request);
            boolean foundInCache = false;

            Block[] setBlocks = cache[(cacheType == 1) ? req.set : 0];
            for(Block block : setBlocks) {
                if(block.tag == req.tag && block.state == State.VALID) {
                    hit++;
                    if (type == 0) {
                        // Read
                        @SuppressWarnings("unused")
                        long readData = block.data[req.offset / 4];
                    } else {
                        // Write
                        block.data[req.offset / 4] = request;
                    }
                    foundInCache = true;
                    break;
                }
            }

            if (foundInCache)
                continue;

            miss++;
            if (type == 0) {
                int pos = getNewLocation(req.set);
                Block block = setBlocks[pos];
                block.state = State.MISS_PENDING;

                block.tag = req.tag;
                block.state = State.VALID;
                List<Long> memData = getDataFromMainMemory(request);
                for (int j = 0; j < 16; j++) {
                    block.data[j] = memData.get(j);
                }
            } else {
                // Write miss - write to main memory
            }
        }

        System.out.println();
        System.out.println("Hit rate  : " + (hit * 1.0 / numOfRequests));
        System.out.println("Miss rate : " + (miss * 1.0 / numOfRequests));
    }
}