package net.gripps.ccn.core;

public class CCNContentsInfo {

    private CCNContents contents;

    private long totalSize;

    private long remainedSize;

    public CCNContentsInfo(CCNContents contents, long totalSize, long remainedSize) {
        this.contents = contents;
        this.totalSize = totalSize;
        this.remainedSize = remainedSize;
    }

    public CCNContents getContents() {
        return contents;
    }

    public void setContents(CCNContents contents) {
        this.contents = contents;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getRemainedSize() {
        return remainedSize;
    }

    public void setRemainedSize(long remainedSize) {
        this.remainedSize = remainedSize;
    }
}
