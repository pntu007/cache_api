import java.util.*;

import cache.Cache;

public class AssociativeCache extends Cache {
    static int ways = 32;

    public AssociativeCache() {
        super(1, ways);
    }

    public static void main(String[] args) {
        AssociativeCache cache = new AssociativeCache();
        // Block[][] cacheBlocks = cache.getCache();
        List<Long> manualAddresses = Arrays.asList(
            64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
        );
        cache.handleManualRequests(manualAddresses, 0);
    }
}