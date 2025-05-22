import java.util.*;

import cache.Cache;

public class SetAssociativeCache extends Cache {
    static int sets = 64;

    public SetAssociativeCache() {
        super(sets, 8);
    }

    public static void main(String[] args) {
        SetAssociativeCache cache = new SetAssociativeCache();
        // Block[][] cacheBlocks = cache.getCache();
        List<Long> manualAddresses = Arrays.asList(
            64L, 128L, 64L, 256L, 64L, 128L, 512L, 1024L, 64L
        );
        cache.handleManualRequests(manualAddresses,1);
    }
}
