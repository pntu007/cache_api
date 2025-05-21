import java.util.*;

public class SetAssociativeCache {
    private enum State { INVALID, VALID, MISS_PENDING }

    private static class Block {
        long tag = -1;
        State state = State.INVALID;
        long[] data = new long[16];
    }

    private static class AddressLocation {
        long tag;
        int set;
        int offset;
    }

    private final int cacheSize = 32768; // 32KB
    private final int blockSize = 64;    // 64 bytes
    private final int associativity = 8; // 8-way associative
    private final int numSets = cacheSize / (blockSize * associativity); // 64 sets
    private final List<List<Block>> sets;

    private final Random rand = new Random();

    public SetAssociativeCache() {
        sets = new ArrayList<>();
        for (int i = 0; i < numSets; i++) {
            List<Block> set = new ArrayList<>();
            for (int j = 0; j < associativity; j++) {
                set.add(new Block());
            }
            sets.add(set);
        }
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
        return rand.nextInt(associativity);
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

    public void handleManualRequests(List<Long> addressList) {
        long hit = 0, miss = 0;
        long numOfRequests = addressList.size();
        
        for (int i = 0; i < numOfRequests; i++) {

            long request = addressList.get(i);
            int type = rand.nextInt(2); // 0 = read, 1 = write

            AddressLocation req = getLocationInfo(request);
            boolean foundInCache = false;

            List<Block> setBlocks = sets.get(req.set);
            for (Block block : setBlocks) {
                if (block.tag == req.tag && block.state == State.VALID) {
                    hit++;
                    if (type == 0) {
                        // Read
                        long readData = block.data[req.offset / 4];
                    } else {
                        // Write
                        block.data[req.offset / 4] = request;
                    }
                    foundInCache = true;
                    break;
                }
            }

            if (foundInCache) continue;

            miss++;
            if (type == 0) {
                int pos = getNewLocation(req.set);
                Block block = setBlocks.get(pos);
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

    public static void main(String[] args) {
        SetAssociativeCache cache = new SetAssociativeCache();
        List<Long> manualAddresses = Arrays.asList(
            64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
        );
        cache.handleManualRequests(manualAddresses);
    }
}