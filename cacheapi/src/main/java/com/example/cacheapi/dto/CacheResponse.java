package com.example.cacheapi.dto;
import java.util.List;

public class CacheResponse {
    private List<Long> cacheFinal;
    private long memoryIndex;
    private List<Long> memoryData;
    private String type;
    private int index;
    private long tag;
    private int offset;
    private long block;
    private long data;
    private boolean hit;
    private String oldState;
    private String newState;

    // ✅ Constructor
    public CacheResponse(List<Long> cacheFinal, long memoryIndex, List<Long> memoryData, String type,
                         int index, long tag, int offset, long block, long data,
                         boolean hit, String oldState, String newState) {
        this.cacheFinal = cacheFinal;
        this.memoryIndex = memoryIndex;
        this.memoryData = memoryData;
        this.type = type;
        this.index = index;
        this.tag = tag;
        this.offset = offset;
        this.block = block;
        this.data = data;
        this.hit = hit;
        this.oldState = oldState;
        this.newState = newState;
    }

    public List<Long> getCacheFinal() { return cacheFinal; }
    public void setCacheFinal(List<Long> cacheFinal) { this.cacheFinal = cacheFinal; }

    public long getMemoryIndex() { return memoryIndex; }
    public void setMemoryIndex(int memoryIndex) { this.memoryIndex = memoryIndex; }

    public List<Long> getMemoryData() { return memoryData; }
    public void setMemoryData(List<Long> memoryData) { this.memoryData = memoryData; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public long getTag() { return tag; }
    public void setTag(long tag) { this.tag = tag; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    public long getBlock() { return block; }
    public void setBlock(int block) { this.block = block; }

    public long getData() { return data; }
    public void setData(long data) { this.data = data; }

    public boolean isHit() { return hit; }
    public void setHit(boolean hit) { this.hit = hit; }

    public String getOldState() { return oldState; }
    public void setOldState(String oldState) { this.oldState = oldState; }

    public String getNewState() { return newState; }
    public void setNewState(String newState) { this.newState = newState; }
}